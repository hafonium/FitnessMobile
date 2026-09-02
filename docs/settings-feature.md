# Settings Feature — Implementation Notes

Implements the Settings tab: main `SettingsScreen` plus three pushed screens
(`WorkoutSettingsScreen`, `GeneralSettingsScreen`, `VoiceOptionsScreen`), backed by Room, following
the layering in `docs/architecture.md`. Read this before touching anything under
`ui/core/settings/`, `domain/*/settings*`, or `UserSettingsEntity` — several fields moved and one
enum was reshaped.

Also covers the Home screen's **Weekly Goal** feature (`ui/core/editgoal/`, the `WeeklyGoalCard` in
`HomeScreen.kt`) — it persists into the same `user_settings` row and reuses the Settings
`GetSettingsUseCase`/`UpdateSettingsUseCase`, so it's documented here rather than a separate file.

## Database changes (`docs/db_diagram.dbml` + `data/local/`)

`AppDatabase` is now **version 3** with `.fallbackToDestructiveMigration(true)` — this is a
pre-release, local-only DB, so schema bumps just reseed instead of needing a real migration path.
If you add a `Migration`, remove the destructive fallback for the versions you cover.

`user_settings` (`UserSettingsEntity`) gained:
- `musicVolume: Float = 1f`, `soundVolume: Float = 1f` — the "Sound options" sliders on
  `WorkoutSettingsScreen`; `musicEnabled`/`soundEnabled` are the mute switches, these are the level.
- `ttsVoiceName: String? = null` — the specific engine voice id when `ttsVoiceType == CUSTOM`
  (see below). Null for the other three voice types.

`voice_type` enum (`domain/models/enums/UserEnums.kt`) was **reshaped**, not just extended:
`NATURAL` → gone, replaced by `MALE_COACH` / `FEMALE_COACH`; `DEVICE_TTS` kept; `CUSTOM` added.
Anything that pattern-matches on `VoiceType` exhaustively needs the new cases (the compiler will
catch this — `when` over `VoiceType` is exhaustive throughout the settings code).

`WorkoutSessionEntity`'s settings snapshot (`tts_voice_type`, `music_enabled`, `sound_enabled`,
`rest_timer_sec`, `prep_timer_sec`, `coach_video_enabled`) was **not** touched — it still only
snapshots the pre-existing fields, not `musicVolume`/`soundVolume`/`ttsVoiceName`. If a workout
session needs to reproduce the exact volume/custom-voice used, extend that entity too.

`weeklyGoalDays: Int = 6` and `firstDayOfWeek: WeekDay = WeekDay.SUNDAY` already existed on
`UserSettingsEntity` (and in `docs/db_diagram.dbml`) before this pass, but had **no use case and no
real UI** — `EditGoalScreen` was a pure stub that always showed hardcoded `6`/`SUNDAY` and never
read or wrote them. They're now wired end-to-end: exposed on `SettingsPreferences`, mapped in
`SettingsRepositoryImpl.toDomain()`/`applyDomain()`, edited by `EditGoalScreen` via the same
`GetSettingsUseCase`/`UpdateSettingsUseCase` the Settings tab uses. No entity/column changes, no DB
version bump — this is only a case of wiring already-existing schema.

## Domain layer

- `domain/models/SettingsPreferences.kt` — the unified state model for every settings screen:
  `gender`, `musicEnabled`/`musicVolume`, `soundEnabled`/`soundVolume`, `restTimerSec`,
  `prepTimerSec`, `unitSystem`, `keepScreenOn`, `dailyReminderEnabled`/`dailyReminderTime`,
  `ttsVoiceType`, `customVoiceName`.
- `domain/repositories/SettingsRepository.kt` — `observeSettings(): Flow<SettingsPreferences>`,
  `updateSettings(preferences)`, `resetWorkoutProgress()`.
- `domain/usecases/settings/` — `GetSettingsUseCase`, `UpdateSettingsUseCase`,
  `ResetWorkoutProgressUseCase`. Deliberately **not** one use case per field — `UpdateSettingsUseCase`
  takes the whole `SettingsPreferences` object; the ViewModel does `settings.value.copy(...)` and
  passes the result. Keep this pattern for any new field instead of adding another use case.
  `EditGoalViewModel` (Home flow, not Settings tab) reuses these two exact use cases rather than
  adding goal-specific ones — same table, same pattern.
- `domain/usecases/home/GetWeeklyGoalProgressUseCase.kt` (new) — combines
  `SettingsRepository.observeSettings()` (for `weeklyGoalDays`/`firstDayOfWeek`) with
  `WorkoutSessionRepository.observeCompletedSessionTimestamps()` (for which days in that window are
  done) into a `WeeklyGoalProgress` for the Home screen's `WeeklyGoalCard`. All date-bucketing
  (which 7-day window "contains today" starting on the user's chosen first day, matching a
  timestamp to a day) is done here with `java.util.Calendar` — **not** `java.time`, since minSdk 24
  predates unconditional `java.time` support and this project has no core-library-desugaring
  configured. If you add desugaring later, `java.time.LocalDate`/`DayOfWeek` would simplify this
  considerably.
- `domain/models/WeeklyGoalProgress.kt` (new) — `WeeklyGoalProgress(goalDays, completedDays, days)`
  where `days: List<WeeklyGoalDay>` is exactly 7 entries (`dayOfMonth`, `isToday`, `isCompleted`).
- `domain/repositories/WorkoutSessionRepository.kt` (new) — until now there was no domain-layer
  repository over `WorkoutSessionDao` at all (only `SettingsRepositoryImpl` reached into it
  directly, for "Restart progress"). This adds the first real one:
  `observeCompletedSessionTimestamps(fromMillis, toMillis): Flow<List<Long>>`. Extend this
  interface rather than reaching into `WorkoutSessionDao` from a new use case directly.

## Data layer

- `data/repositories/SettingsRepositoryImpl.kt` — maps `UserSettingsEntity` + `UserEntity.gender`
  (two different tables) into one `SettingsPreferences`. Important: `updateSettings()` reads the
  **existing** `UserSettingsEntity` first and `.copy()`s only the fields `SettingsPreferences`
  covers (`applyDomain()`), so `weeklyGoalDays`/`firstDayOfWeek`/`coachVideoEnabled`/
  `healthConnectEnabled` — fields on the entity with no UI yet — are preserved rather than reset to
  defaults on every save. If you add a new `SettingsPreferences` field, add it to both `toDomain()`
  and `applyDomain()` or it'll silently not persist.
- This app has one local user (seeded by `AppDatabaseSeeder`, email
  `AppDatabaseSeeder.DEFAULT_USER_EMAIL`). `SettingsRepositoryImpl.currentUserId()` resolves it by
  that email — don't assume a fixed id.
- `WorkoutSessionDao.deleteAllSessionsForUser(userId)` (new) backs the destructive "Restart
  progress" action; relies on the existing `CASCADE` FK from `workout_session_exercises` to clear
  both tables.
- `WorkoutSessionDao.observeCompletedSessionEndTimes(userId, status, fromMillis, toMillis)` (new) —
  returns `endedAt` for `COMPLETED` sessions in a time range; backs the weekly-goal day tracker.
  Filters `endedAt IS NOT NULL` in SQL so the Kotlin return type can stay `List<Long>` (non-null)
  instead of `List<Long?>`.
- `data/repositories/WorkoutSessionRepositoryImpl.kt` (new) — implements
  `WorkoutSessionRepository`, same `currentUserId()`-by-seeded-email pattern as
  `SettingsRepositoryImpl` (intentionally duplicated rather than extracted into a shared helper —
  it's 3 lines in two places, not worth an abstraction for).

## UI layer

- `ui/core/settings/SettingsViewModel.kt` — shared by all three pushed screens (Workout/General/
  Voice). Exposes `settings: StateFlow<SettingsPreferences>` (`SharingStarted.Eagerly`, so it's
  live as soon as the ViewModel exists, not just once collected — deliberate, since settings must
  be current the instant a screen renders) plus one setter per field and `resetWorkoutProgress()`.
  Each pushed screen gets its **own** ViewModel instance from `ScreenNavigator` (no shared nav-scope
  key) — they all read the same Room-backed flow, so state stays consistent across screens without
  needing instance sharing.
- `ui/services/TtsService.kt` — wraps `android.speech.tts.TextToSpeech`. Voice selection strategy,
  in priority order:
  1. **Real distinct engine voice** — `genderVoiceFor()` scans `TextToSpeech.getVoices()` for a
     voice whose `name` contains "male"/"female" for the current locale (this is how engines like
     Google's TTS label their real per-locale voice models). If found, uses it directly at neutral
     pitch — this is a genuinely different voice model, not a pitch trick.
  2. **Forced pitch shift fallback** — only when the engine has zero gendered voices. Synthesizes
     to a temp WAV (`synthesizeToFile`) and plays it back via `MediaPlayer` with
     `PlaybackParams.setPitch()`, which is applied by the Android media framework at playback time
     — this works even when the TTS engine itself ignores `TextToSpeech.setPitch()` (many do).
  3. **Custom voice** (`VoiceType.CUSTOM`) — `listVoices()` exposes every offline voice the engine
     reports (deduped by `name` — some engines report duplicate entries with the same name, which
     will crash a `LazyColumn` keyed on name if you don't dedupe first) so the user can browse and
     pick a specific one via `SettingsViewModel.setCustomVoice(name)`; persisted as
     `customVoiceName`.
  - All calls into `tts.voices`/`Voice` fields are wrapped in `runCatching` — some engines throw
    instead of returning null from `getVoices()`. Never remove those guards without re-testing on
    a bare-bones engine.
  - `hasGenderedVoices()` / `engineDiagnostics()` exist purely for the in-app hint text and
    troubleshooting (shown on `VoiceOptionsScreen` when the installed engine has no real
    male/female voices) — not required for correctness.
- `ui/components/Dialogs.kt` (new) — generic `SingleChoiceDialog<T>` (gender/timer/unit pickers)
  and `ConfirmDialog` (destructive-action confirm, e.g. "Restart progress"). Reuse these for any
  new single-choice or confirm dialog rather than writing another `AlertDialog` inline.
- `ui/components/SettingsRows.kt` — `SettingsNavRow` gained an optional `icon: ImageVector?`
  leading-icon parameter.
- Screens:
  - `SettingsScreen` — landing screen. Only 5 tiles: Workout Settings, General Settings, Voice
    Options, and that's it as of the last edit — "Suggest Other Features" and "Language Options"
    were removed on request, along with the "Sync to Health Connect" switch and the entire second
    section ("Share with friends" / "Rate us" / "Feedback" / "Remove Ads") from the original
    mock — those are gone, not hidden. Don't re-add without checking with the user first.
  - `WorkoutSettingsScreen` — Gender (single-choice dialog), Music (switch), Rest/Prep timer
    (single-choice dialogs, options `[15,20,30,45,60,90]` / `[5,10,15,20,30]` sec), Sound options
    (dialog with mute switches + volume sliders), Restart progress (destructive confirm).
  - `GeneralSettingsScreen` — daily reminder (switch + Material3 `TimePicker` dialog, format
    `"HH:mm"`), Metric/Imperial (single-choice dialog), Keep screen on (switch), Privacy Policy
    (opens `PRIVACY_POLICY_URL` — **currently a placeholder `https://example.com/privacy-policy`,
    replace before shipping**).
  - `VoiceOptionsScreen` — radio rows for Male/Female Coach/Device TTS/Custom voice, engine
    diagnostics line, "Preview / Test Voice" button, and (only when the engine lacks gendered
    voices) a hint + button to open the system TTS settings screen. The Custom voice row opens a
    `ModalBottomSheet` (`VoiceBrowserSheet`) listing every voice from `TtsService.listVoices()`
    with a per-row preview button.
- `ui/core/editgoal/EditGoalScreen.kt` + `EditGoalViewModel.kt` (Home flow, `Screen.EditGoal`) —
  "Set your weekly goal": 1-7 training-day chip picker + first-day-of-week chip picker, now
  **actually persisted** (previously a pure stub, see the DB-changes section above). Fixed bug: the
  screen used to always show hardcoded `6`/`SUNDAY` regardless of what was stored, so reopening it
  looked like it silently reset your choice. Fix pattern — seed the editable `remember` state from
  the loaded `SettingsPreferences` **exactly once** (`seededFromStore` flag inside a
  `LaunchedEffect(settings)`), never again afterwards. Reuse this "seed-once" pattern for any
  future screen that edits a subset of persisted state locally before an explicit Save; the
  alternative (re-seeding from the flow on every emission) will fight the user's own edits and/or
  get overwritten by the flow re-emitting right after Save persists.
- `ui/core/home/HomeScreen.kt` `WeeklyGoalCard` — was **fully hardcoded** (literal `"1/6"` text and
  a literal encouragement string, no state at all). Now driven by
  `HomeViewModel.weeklyGoalProgress: StateFlow<WeeklyGoalProgress>` (from
  `GetWeeklyGoalProgressUseCase`): real `completedDays/goalDays` counter, a 7-day pill row
  (`WeeklyGoalDayPill` — filled circle + checkmark for a completed day, outlined circle for today
  if not yet completed, plain number otherwise) matching `docs/img.png`, and a message that varies
  by progress (`weeklyGoalMessage()`).

## DI wiring

`ui/App.kt`: `settingsRepository`, `ttsService`, `getSettingsUseCase`, `updateSettingsUseCase`,
`resetWorkoutProgressUseCase` added as `by lazy` properties, same pattern as the existing
repositories/use cases. `ScreenNavigator.kt`: each of the three pushed settings routes builds its
own `SettingsViewModel` via `viewModelFactory { initializer { ... } }`, injecting those four
dependencies from `appInstance`.

## Known limitations / follow-ups

- Voice quality is entirely dependent on whatever TTS engine is installed on the device/emulator.
  A bare-bones engine (e.g. some emulator system images) has no gendered voices at all, so
  Male/Female fall back to the pitch-shift trick, which sounds subtly different at best. There is
  **no code fix** for this — it's a real capability ceiling of the installed engine. The in-app
  hint points the user at installing/switching to a fuller engine (e.g. Google's Text-to-Speech).
- `PRIVACY_POLICY_URL` in `GeneralSettingsScreen.kt` is a placeholder and needs a real URL.
- No UI yet for `UserSettingsEntity.coachVideoEnabled` / `healthConnectEnabled` — the repository
  preserves them on save (see `applyDomain()` above) but nothing edits them.
- The weekly-goal day tracker only reflects `WorkoutSessionEntity` rows with `status = COMPLETED`
  and a non-null `endedAt`. A session that's `IN_PROGRESS`/`PAUSED`/`ABANDONED` never marks a day
  done, by design — but there's currently no explicit test coverage confirming session-completion
  writes `endedAt` correctly from the workout player flow; verify that before relying on this for
  anything beyond the Home card.
