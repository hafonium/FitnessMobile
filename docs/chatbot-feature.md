# In-App Chatbot Feature — Implementation Notes

Adds a global, draggable floating chat bubble that opens a popup chat window backed by an LLM
API, so users can ask fitness/workout questions from anywhere in the app — and, once it knows
enough about the user, propose and help create a real workout plan (see "Plan creation from chat"
below). Entirely on-device: no backend server, no MongoDB — chat sessions and messages live in
Room next to everything else, following the layering in `docs/architecture.md`. Read this before
touching `ui/core/chat/`, `data/remote/groq/`, `ChatRepositoryImpl`,
`CreateCustomPlanViewModel.applyProposal`, or the `chat_sessions` / `chat_messages` tables.

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
- `ChatPanelController.kt` — bridges this overlay and `ScreenNavigator`, which otherwise share no
  state; see "Plan creation from chat" below.

## Plan creation from chat

The assistant can also *create* a workout plan instead of just talking about one: give it enough
of a `FitnessProfile` (goal, experience level, days/week, session length) — either already saved
from onboarding, or gathered conversationally — and, once the user clearly asks for a plan, it
proposes one. The app then opens Create Workout with a draft pre-filled from that proposal for
the user to review and confirm; nothing is written to the database until they tap "Create Plan".

**Design choice: reuse the recommender, don't teach the LLM the scoring algorithm.** The app
already has `RecommendPlanUseCase`, a deterministic Kotlin implementation of the whole plan-
selection strategy in `docs/workout_plan_selection_guide.md` (hard filters, weighted scoring,
fallback ladder). Re-deriving that logic inside an LLM prompt would be slower, less reliable, and
a second place to keep in sync with the guide. So the LLM's job is narrower: hold a conversation,
map what the user says onto `FitnessProfile`'s fields, and signal *when* to create a plan — the
actual template selection still goes through `RecommendPlanUseCase`, exactly like onboarding does.

### Wire format (`GroqClient.kt`)

The structured-output schema grew two fields beyond `reply`/`updatedContext`:
`wantsToCreatePlan` (boolean) and `planProposal` (an object mirroring `FitnessProfile` plus a
suggested `title`/`description`). Both are **always required** by the schema (strict JSON Schema
mode requires every property to be present) — the model fills `planProposal` with placeholder
values even when `wantsToCreatePlan` is false, and the app only reads it when the flag is true.
The request prompt also grows an `APP CONTEXT` block ahead of the usual `PRIOR CONTEXT` /
`NEW USER MESSAGE`: a small static taxonomy (goals, levels, equipment, exercise categories — see
`ChatRepositoryImpl.APP_TAXONOMY`) plus the user's saved `FitnessProfile` (or "none saved yet")
and a one-line-per-plan summary of their existing plans, built fresh on every send via
`ChatRepositoryImpl.buildAppContext()`. This is deliberately *not* the raw 515-exercise dataset
(over 1MB) — the model reasons at the category/muscle/equipment level, never by literal exercise
name, so nothing exercise-specific needs to be serialized into the prompt.

**Never trust the model's structured output at face value**, strict mode or not — the same lesson
as the inline-bullet Markdown bug earlier in this doc's history. `ChatRepositoryImpl`'s
`GroqPlanProposal.toDomain()` defends against it: an unrecognized `primaryGoal`/`experienceLevel`
string falls back to a sane default via `PrimaryGoal.fromKey`/`ExperienceLevel.fromKey`,
`daysPerWeek` is clamped to 2–6, and `sessionMinutes` is snapped to the nearest of the app's five
allowed values (15/20/30/45/60) rather than trusted as-is.

### Bridging chat and navigation (`ChatPanelController.kt`)

`ChatOverlay` and `ScreenNavigator` are siblings inside `HomeWorkoutApp`'s top-level `Box`, each
with its own state and no reference to the other — `ScreenNavigator` owns its `NavController`
internally, and until this feature nothing needed to reach across that boundary.
`ui/core/chat/ChatPanelController.kt` is a small app-scoped singleton (`App.chatPanelController`,
same lifetime as the other repositories) that bridges the two:

- `isOpen: StateFlow<Boolean>` — the chat panel's expanded/collapsed state, promoted here from
  what used to be a local `remember { mutableStateOf(false) }` inside `ChatOverlay`, specifically
  so a chat-triggered navigation can collapse the panel before navigating away and reopen it on
  return.
- `pendingPlanProposal: StateFlow<PlanProposal?>` — set by `ChatViewModel.sendMessage` (via
  `proposePlan`, which also collapses the panel) whenever `SendChatMessageUseCase` returns a
  non-null `PlanProposal`. `ScreenNavigator` observes this at the top of its composable and
  navigates to `Screen.CreateCustomPlan` the moment it goes non-null; the destination itself
  consumes (and clears) it exactly once via `consumePendingPlanProposal()` inside a
  `LaunchedEffect(Unit)`, so a manual "+ Create Workout" entry (from `CustomWorkoutListScreen`)
  sees `null` and behaves exactly as it always has.

`CreateCustomPlanViewModel.applyProposal(proposal)` is what actually seeds the draft: it calls
`RecommendPlanUseCase(proposal.profile)` and, when a system template matches, copies its real
days/exercises in (the same code path as "Start from a template", just triggered automatically
instead of by a manual tap) with the LLM's suggested title/description in place of the template's
own name. When nothing matches — an unusual profile combination, or simply no plans seeded yet —
it still prefills title/description/category/level from the conversation rather than dropping the
user into a generic blank form; `mapGoalToCategory`/`mapExperienceLevelToWorkoutLevel` translate
between `FitnessProfile`'s `PrimaryGoal`/`ExperienceLevel` and the plan-level `WorkoutCategory`/
`WorkoutLevel` enums (these are separate enum families in this codebase — see `WorkoutEnums.kt` /
`FitnessProfile.kt` — not the same values under a different name). Either way the profile is saved
via `SaveFitnessProfileUseCase`, best-effort, matching what onboarding does.

`ScreenNavigator`'s `CreateCustomPlan` composable tracks one more thing locally — `cameFromChat`,
set when `consumePendingPlanProposal()` returns non-null — purely to decide what `onNavigateBack`
and `onPlanCreated` do: only when the screen was reached this way do both branches reopen the
chat panel and pop back to whatever screen was underneath, instead of the screen's normal
behavior (pop to the caller / navigate to the new plan's Details). A manual entry is completely
unaffected.

### Safety

The system instruction explicitly tells the model never to set `wantsToCreatePlan` when the user
mentions an injury, acute pain, or a medical restriction — matching the "hard safety exclusion"
language in `docs/workout_plan_selection_guide.md` §4 Step 1. `PlanProposal`'s
`injuriesOrLimitations` is always built as an empty string for this reason: there is currently no
schema field for the model to report a limitation through, specifically so a plan can never be
auto-proposed around one.

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
- The plan-creation flow (see "Plan creation from chat" above) was verified by compiling and by
  exercising the panel's open/close plumbing directly; the full loop — asking the assistant for a
  plan, the structured output actually parsing, and landing on a correctly pre-filled Create
  Workout screen — still wants an on-device test pass, since it depends on a live Groq response.
- `CreateCustomPlanViewModel.applyProposal`'s "matched a template" path depends on `workout_plans`
  actually being seeded — if that table is empty, every proposal falls back to the blank-header
  path (still usable, just without pre-filled days/exercises).
