# AI Video Form Check

The Discovery tab contains an **AI Video Form Check** entry. Users record (or pick from the
gallery) a short 5-8 second clip of one bodyweight exercise repetition. Rather than uploading the
raw video, the app extracts 4-6 evenly-spaced still frames client-side (a "storyboard") and sends
those to Gemini as an ordered image sequence: Gemini reasons over the sequence, ignores
setup/idle frames, isolates the active repetition, and returns a structured biomechanical
evaluation - a 0-100 form score, an EXCELLENT/ACCEPTABLE/NEEDS_IMPROVEMENT status, itemized joint
checkpoints, one primary correction tip, and a separate, purely informational recording tip (e.g.
"shoot from the side with better lighting") for the next attempt.

This is an isolated Record -> Process -> Display Result flow (see `ui/core/formcheck/`) - there is
no chat thread or message list, unlike the Groq-based chat assistant. The capture sheet's video
preview uses Media3 `ExoPlayer` (`androidx.media3:media3-exoplayer`/`media3-ui`), not the legacy
`VideoView`/`MediaPlayer` stack, since that surfaced "Cannot display video" for some camera/gallery
container-codec combinations - see `VideoPreviewPlayer` in `FormCheckScreen.kt`.

`VideoPreviewBox` tracks a `VideoPreviewState` (`Loading`/`Ready`/`Error`) per selected `Uri` so the
preview never shows ExoPlayer's underlying `SurfaceView` before it has something to display (that
surface renders black until the first frame lands): a full-cover spinner overlay
("Preparing video preview…") is shown until `Player.Listener.onRenderedFirstFrame` fires, an
`onPlayerError` (or an 8-second timeout with no callback at all) swaps in an opaque error
placeholder with a "Retry" button instead, and only `onRenderedFirstFrame` actually reveals the
player surface.

## Camera capture

Recording via the in-app "Record" button pre-creates an empty temp file under
`cacheDir/form-check-videos/` and hands its `FileProvider` `content://` Uri to
`ACTION_VIDEO_CAPTURE` as `EXTRA_OUTPUT`, rather than relying on whatever Uri the camera activity
might hand back in its result Intent - some camera apps don't return one at all. The capture
Intent carries both `FLAG_GRANT_WRITE_URI_PERMISSION` and `FLAG_GRANT_READ_URI_PERMISSION`, and
the Uri is additionally granted explicitly (via `grantUriPermission`) to every activity that can
resolve `ACTION_VIDEO_CAPTURE`, since some OEM camera apps don't reliably honor Intent-level
permission flags - the same defensive pattern long used for `ACTION_IMAGE_CAPTURE` on those
devices.

`FormCheckScreen` used to delete that temp file immediately after the camera activity returned
(`loadVideo(uri) { file?.delete() }`), on the theory that whatever needed the file had already
read it - true for the Food Calorie Scanner's photo flow (which decodes the whole image into a
`Bitmap` synchronously first), but not here: frame extraction is deliberately deferred until
"Analyze Form" is tapped, so the file was being deleted before the preview player, let alone frame
extraction, ever opened it. This is why gallery picks worked (their Uri points to a persistent
MediaStore file nothing ever deletes) and camera recordings didn't. Fixed by tracking
`activeVideoFile` - the temp file backing the *current* `videoUri`, owned by whichever selection is
active - and only deleting it once it's actually replaced (a new recording/pick, or "Re-test
Form"), never right after capture. `loadVideo` also now confirms the Uri has actual content
(`ContentResolver.openAssetFileDescriptor(uri, "r")?.length`, falling back to a one-byte stream
read when a content provider doesn't report a length) before accepting it, catching a zero-byte
file - from a camera app that reports success before finishing its write - immediately instead of
failing later inside the player or the extractor.

## API key setup

Add the following entry to the project-root `local.properties` file:

```properties
GEMINI_API_KEY=your_api_key_here
```

Alternatively, expose `GEMINI_API_KEY` as an environment variable before building. The
environment variable takes precedence over `local.properties`.

`local.properties` is ignored by Git. Never commit the real key. `BuildConfig.GEMINI_API_KEY` is
baked in at build time (see `app/build.gradle.kts`) and is the only place the raw key should ever
be referenced from Kotlin code. As with the Spoonacular and Groq keys, this direct-from-app call
is fine for a prototype but `BuildConfig` values can still be extracted from an APK - route the
Gemini request through a backend before distributing a production build.

## Frame extraction (storyboard sampling)

`FormCheckScreen.extractFrames` (invoked from "Analyze Form", not eagerly on video selection) uses
`android.media.MediaMetadataRetriever` against the selected `Uri`:

1. Reads the clip's duration (`METADATA_KEY_DURATION`).
2. Picks a frame count in `4..6`, roughly one per second of clip duration (`durationSec.roundToInt().coerceIn(4, 6)`).
3. Samples the midpoint of each of that many equal-length segments spanning the full clip
   (`getFrameAtTime(timeUs, OPTION_CLOSEST)`) - segment midpoints rather than edges, so a sample
   never lands exactly on a black/incomplete frame at the very start or end.
4. Downscales each frame to 720px wide (preserving aspect ratio) and JPEG-encodes it
   (quality 85), recycling every intermediate `Bitmap`.
5. Releases the `MediaMetadataRetriever` in a `finally` block.

Extraction runs on `Dispatchers.Default` from a `rememberCoroutineScope()` launch; a failure
(corrupt file, zero duration, decode error) returns an empty list, which the screen treats as "this
video could not be processed" rather than throwing.

## Gemini usage

`data/remote/gemini/GeminiFormCheckApi` calls Gemini's `generateContent` REST endpoint directly
with plain OkHttp (`https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=...`)
rather than the `com.google.ai.client.generativeai` Kotlin SDK. That SDK has been dropped from the
project entirely (no dependency in `app/build.gradle.kts` anymore): in practice it 404'd on this
API surface every time Google retired a model alias faster than an SDK update landed
(`gemini-1.5-flash`, then `gemini-2.0-flash`, both "is not found"/"is no longer available"
errors), and it occasionally surfaced raw gRPC/serialization exceptions instead of a clean HTTP
error. A raw REST call only ever depends on the model-name string in the URL - bumping
`MODEL_NAME` (currently `gemini-3.6-flash`) is the entire fix next time, with no SDK version to
chase and no gRPC error surface at all. `GeminiFormCheckApi` also strips a `models/` prefix from
the model name before building the URL, in case a fully-qualified name (as returned by
`ListModels`, below) is ever passed in directly.

Don't guess which alias is live for a given API key from Google's deprecation messages alone -
`gemini-2.5-flash` 404'd with "no longer available to new users" while still being *listed* by
`ListModels`, which is exactly the trap this diagnostic exists to avoid.
`GeminiFormCheckApi.logAvailableModels()` GETs the `ListModels` endpoint
(`https://generativelanguage.googleapis.com/v1beta/models?key=...`) and logs every model name this
key can currently call, plus whether each supports `generateContent`. It's a debug-only diagnostic,
never called from the analysis flow itself: `App.kt`'s `formCheckRepository` initializer fires it
once, in `applicationScope`, guarded by `BuildConfig.DEBUG`, the first time anything touches the
Form Check feature's dependencies (i.e. the first time that screen is opened in a debug build) -
check Logcat for tag `GeminiFormCheckApi` after that to see the real list. `MODEL_NAME` was last
verified against this project's own key that way on 2026-09-05 (`gemini-3.6-flash` confirmed listed
with `generateContent: true`) - re-run it and update the constant to match if it 404s again, rather
than trusting a model name out of an error message that hasn't been cross-checked against
`ListModels` first.

The request body is built by hand as JSON (`buildRequestBody`): each extracted frame becomes one
`{"inline_data": {"mime_type": "image/jpeg", "data": "<base64>"}}` part (via
`android.util.Base64.encodeToString(frame, Base64.NO_WRAP)`), in chronological (list) order,
followed by one `{"text": ...}` prompt part - no raw video bytes ever leave the device, and no
`Bitmap` objects cross into this layer (frames arrive as already-JPEG-encoded `ByteArray`s from
`FormCheckScreen.extractFrames`). The system instruction is sent as a top-level `systemInstruction`
field (mirroring the REST API's own shape) rather than folded into the prompt text. The exercise
chip the user picks on the capture sheet (or "Auto-Detect") is passed as a hint in the prompt
text; the system instruction always asks Gemini to confirm or correct the exercise name from what
it actually observes across the sequence. The reply's `candidates[0].content.parts[*].text` are
joined (`extractResponseText`) before JSON-decoding, since Gemini occasionally splits one reply
across multiple text parts.

The system instruction explicitly forbids refusing a low-visibility clip (poor lighting, a
sub-optimal angle, a partially out-of-frame body): Gemini must always return a best-effort
analysis, marking anything it genuinely can't see per-observation ("Unable to fully assess due to
angle/lighting") rather than declining the whole request. `recordingTip` is a separate schema
field for "do it better next time" advice - it's informational only and never a reason to hold
back the analysis itself.

## Deterministic evaluation

Identical input frames used to produce visibly different scores across runs (e.g. 90 vs 65 on the
same video) because `temperature = 0.2` let Gemini sample rather than always pick its
highest-probability output. Fixed on three levels:

1. **`generationConfig.temperature = 0.0`** in `buildRequestBody` - greedy decoding, no sampling.
2. **An explicit deductive scoring rubric** in `SYSTEM_INSTRUCTION`: score starts at a 100-point
   baseline, a major fault (severe lumbar sagging, elbow flare beyond 70°, depth below 50% ROM)
   deducts 20, a minor fault (head/neck misalignment, hip shift, a lockout pause) deducts 10, no
   observed fault defaults to 95, and every deduction must be named in an `observations` entry's
   `feedback` - Gemini computes the number instead of estimating it. A fixed score-to-status
   mapping (90-100 EXCELLENT, 70-89 ACCEPTABLE, below 70 NEEDS_IMPROVEMENT) is spelled out too, so
   `score` and `status` can't disagree with each other run to run.
3. **`FormCheckScreen.extractFrames`** already computed its sample timestamps with pure integer
   arithmetic on the video's own duration metadata (deterministic by construction - the same file
   always yields the same timestamps), but now retrieves each frame with
   `MediaMetadataRetriever.OPTION_CLOSEST_SYNC` instead of `OPTION_CLOSEST`: it resolves straight
   to the nearest sync/key frame rather than decoding forward from one, removing decoder-path
   variance as a possible source of frame-to-frame inconsistency.

Temperature 0 does not guarantee bit-for-bit identical output on every possible input (a
sufficiently ambiguous frame sequence can still land on either side of a threshold), but it removes
the deliberate randomness that made the same video score differently on repeat requests.

A saved result ("Save to History" CTA) is persisted locally in the `form_check_results` Room
table (`data/local/entities/FormCheckResultEntity.kt`) - the full result, not just the score:
`observationsJson` (the joint checkpoints), `primaryCorrectionTip`, and `recordingTip` are all
written by `FormCheckRepositoryImpl.saveResult` and reconstructed back into a real `FormAnalysis`
by its `FormCheckResultEntity.toDomain()` mapping - see `docs/architecture.md` for the database
registration convention.

It's readable back via the history icon in `FormCheckScreen`'s top bar (`Icons.Default.History`),
which opens `FormCheckHistoryScreen` (`Screen.FormCheckHistory`) - a list of saved results (exercise
name, score, status, date), newest first, backed by `GetFormCheckHistoryUseCase` ->
`FormCheckRepository.observeHistory()` -> `FormCheckResultDao.observeResults()`. Tapping a row
expands it in place to the full result detail - score gauge, joint checkpoints, primary correction,
recording tip - via `FormAnalysisDetails`, a composable factored out of `FormCheckScreen`'s live
result view specifically so a saved result renders identically to how it looked the moment it was
analyzed, rather than a second, divergent implementation. There's no delete yet.

## Error handling

`GeminiFormCheckApi.parseResponse` is deliberately lenient - a missing or odd field in an
otherwise-valid JSON response degrades to a sensible default (e.g. a generic exercise name, a
50 score, an "Unable to fully assess..." observation) instead of failing outright, since the
system instruction already asks the model never to refuse.

A thrown `GeminiFormCheckException` is reserved for genuine technical failures: a network error
(no connectivity/DNS/timeout - `IOException`, never reaches the server), a non-2xx HTTP response,
an empty response body, or a response that isn't valid JSON at all (`sanitizeJson` strips markdown
code fences before that JSON attempt). Every one of those `catch` blocks logs the *full* detail via
`Log.e` first - for an HTTP failure specifically, `Log.e(TAG, "Status: ${response.code}, Body: $responseBody")`
captures the exact status code and raw error body - so a quota/rate error, a stale-model 404, and a
malformed response are each fully diagnosable in Logcat, then throws a short UI-safe summary:
`httpErrorMessage` maps the HTTP status code itself (404 -> "model no longer available";
401/403 -> "check GEMINI_API_KEY"; 429 -> "usage limit reached"; 5xx -> "temporarily unavailable";
anything else falls back to the error body truncated to 180 characters). This exists specifically
so a verbose HTTP error body or JSON parse error can never dump raw detail onto the inline error
card the screen shows (the same pattern as the Food Calorie Scanner's error card) - Logcat, not
the UI, is where the full detail lives.
