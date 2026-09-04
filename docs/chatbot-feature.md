# In-App Chatbot Feature — Implementation Notes

Adds a global, draggable floating chat bubble that opens a popup chat window backed by an LLM
API, so users can ask fitness/workout questions from anywhere in the app. Entirely on-device: no
backend server, no MongoDB — chat sessions and messages live in Room next to everything else,
following the layering in `docs/architecture.md`. Read this before touching `ui/core/chat/`,
`data/remote/groq/`, `ChatRepositoryImpl`, or the `chat_sessions` / `chat_messages` tables.

**Provider history:** this was originally built against Gemini, then switched to Groq (see "Groq
integration" below) after Gemini's Google Cloud project got hit with an unresolvable `403
PERMISSION_DENIED — "Your project has been denied access. Please contact support."` — a known,
widespread Google-side account restriction (see the Google AI Developers forum), unrelated to
this app's code and not fixable by changing keys, projects, or models. GitHub Models was briefly
considered as another alternative but was retired by Microsoft on July 30, 2026. If a provider
switch is ever needed again, `GroqClient` (`data/remote/groq/`) is the one file that needs a real
rewrite; the rest of this doc and the surrounding code (`ChatMessageRole`, the DB notes, etc.)
deliberately talk about "the assistant" / "the LLM provider" rather than naming Groq specifically,
so a future switch doesn't mean hunting a provider's name down across a dozen files again.

## Why no backend

An earlier design used a separate Node.js/Express + MongoDB service for chat (see the
`chatSchema` this feature was originally scoped from). That was dropped in favor of doing
everything inside the Android app: one fewer service to host/maintain, and this app already has
a full local-persistence story via Room. The tradeoff is that the LLM provider's API key ships
inside the app binary (see "API key handling" below) rather than staying server-side.

## Database changes (`docs/db_diagram.dbml` + `data/local/`)

`AppDatabase` is now **version 6** with `.fallbackToDestructiveMigration(dropAllTables = true)` —
this is a pre-release, local-only DB, so the bump just reseeds instead of needing a real
migration path.

Two new tables:

- `chat_sessions` (`ChatSessionEntity`) — one row per conversation. `contextSummary` is a rolling
  summary of the conversation so far (empty for a new session); it is sent to the LLM provider
  instead of full message history on every turn, and the provider returns an updated summary with
  each reply. `title` starts as `"New chat"` and is replaced with a preview of the user's first
  message once they send one.
- `chat_messages` (`ChatMessageEntity`) — one row per message, FK to `chat_sessions` with
  `onDelete = CASCADE` (deleting a session deletes its messages). `role` is the `ChatMessageRole`
  enum (`USER` / `MODEL`) stored natively by Room. This is a local storage/UI label only, kept as
  `USER`/`MODEL` even after the Groq switch (Groq's own OpenAI-style role vocabulary is
  `user`/`assistant`) rather than renamed to match it — renaming a Room-backed enum constant would
  break deserializing any chat row already stored under the old name, for a purely cosmetic gain.

Sessions belong to the app's single local user, resolved via the same `currentUserId()` /
`AppDatabaseSeeder.DEFAULT_USER_EMAIL` idiom used by `SettingsRepositoryImpl` and the other
repositories — nothing chat-specific here, just the established pattern.

## API key handling

`GROQ_API_KEY` is read from `local.properties` (gitignored, never committed) and baked into
`BuildConfig.GROQ_API_KEY` at build time via `app/build.gradle.kts`. **Each developer must add
their own line to their local `local.properties`:**

```
GROQ_API_KEY=your-key-here
```

Get a free key at https://console.groq.com/keys. The build falls back to an empty string if the
property is missing, so the project still compiles without a key — the chat feature just fails
(gracefully — see "Failure handling" below) until one is added.

This protects the key from landing in git history / being visible to anyone with repo access,
which matters since this is a shared team repo. It does **not** protect the key from someone who
decompiles a release APK — the key still ships inside the binary, since the app calls the Groq
REST API directly with no backend proxy in front of it. That's an accepted tradeoff for this
project's scale; if the app ever ships more broadly, moving to a proxied setup (a small backend
that holds the key and forwards chat requests) would close that gap without changing the
app-facing `GroqClient` interface much.

## Groq integration (`data/remote/groq/GroqClient.kt`)

The **first** remote/network dependency in this project (OkHttp 4.12.0 — see
`app/build.gradle.kts`). Talks to Groq's OpenAI-compatible chat completions endpoint: `POST
https://api.groq.com/openai/v1/chat/completions`, model `openai/gpt-oss-20b`.

That specific model isn't arbitrary: Groq's Structured Outputs (strict `json_schema` response
format) is only supported on a handful of models, and `openai/gpt-oss-20b` is one of them — most
Groq models, including the more commonly-recommended `llama-3.3-70b-versatile`, only support the
older, unenforced `json_object` mode. See https://console.groq.com/docs/structured-outputs for
the current model list before changing this. If `openai/gpt-oss-20b` is ever retired, check that
page (and https://console.groq.com/docs/models) for a replacement that still lists Structured
Outputs support — the earlier Gemini integration broke exactly this way, by pinning to a model
name that quietly stopped existing.

One call does double duty: the request asks for structured JSON output (`response_format:
{type: "json_schema", ...}`) so a single round trip returns both `reply` (shown to the user) and
`updatedContext` (the new rolling summary), instead of needing a second call or replaying full
history. The system instruction (unchanged from the original Gemini version — it's
provider-agnostic prompt text) scopes the assistant to fitness/workout topics, asks it to keep
answers short (this is a small mobile chat widget, not a full-page chat app), and tells it to
reply in whichever language the user's message is written in.

### Failure handling

`ChatRepositoryImpl.sendMessage` catches Groq/network failures (bad key, no connection,
malformed response) and persists a friendly in-thread fallback message from the assistant rather
than throwing — the UI has no separate error state to render, it just sees a reply appear like
any other. `CancellationException` is re-thrown, not swallowed, so navigating away / closing the
popup mid-request still cancels cleanly. The real failure reason is always logged to Logcat under
the `ChatRepository` tag too — check there first whenever the fallback message shows up instead
of a real reply.

## UI (`ui/core/chat/`)

- `ChatOverlay.kt` — the draggable bubble (translucent, `BrandBlue`, tap vs. drag disambiguated
  by total movement during the gesture) and the popup panel (session list ↔ message thread,
  themed with the existing `BrandBlue` / `CardWhite` / `CloudGray` / `SheetTopShape` palette from
  `ui/theme/`). Mounted once in `HomeWorkoutApp.kt`, as a sibling to `ScreenNavigator()` inside a
  top-level `Box`, so it floats above every screen rather than being a nav destination. Renders
  in-tree (not via `Dialog`) specifically so it inherits the same window-inset handling
  (`imePadding`/`navigationBarsPadding`) as every other screen instead of needing its own.
- `ChatViewModel.kt` — session list, active session's messages, input/sending state. Lives above
  the nav graph, constructed the same `viewModelFactory { initializer { ... } }` way
  `ScreenNavigator` builds its own screen ViewModels.

## Known limitations / follow-ups

- The bubble's dragged position is in-memory only (resets to the default bottom-right spot on
  app restart, and doesn't re-clamp into bounds on rotation). Fine for a phone-portrait app; worth
  persisting (e.g. into `UserSettingsEntity`) if that ever matters.
- No message editing, regeneration, or streaming — each send is one request/one reply.
- The `MODEL` / `ENDPOINT` constants in `GroqClient` are pinned and will need bumping again if
  Groq ever retires `openai/gpt-oss-20b` — see the "Groq integration" section above.
- Groq's free tier has its own rate limits (check the usage dashboard at console.groq.com) — fine
  for personal/dev use, but worth knowing about before assuming a fallback-error message always
  means a code bug rather than a quota.
