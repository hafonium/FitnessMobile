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

## Loading-state flash fix

`SettingsViewModel` and `EditGoalViewModel` both used `SettingsPreferences()`'s hardcoded defaults
as the `stateIn` initial value (so the screen has *something* to render on the first frame). The
bug: on a fast open, that default value rendered for a frame or two before the real Room-backed
value arrived and replaced it — a switch or wheel would visibly flip right after the screen
appeared. Fix — both ViewModels now also expose `isReady: StateFlow<Boolean>`, flipped to `true` by
an `.onEach {}` on the same `settings`/`GetSettingsUseCase()` flow the instant its first real value
arrives:

```kotlin
private val _isReady = MutableStateFlow(false)
val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

val settings: StateFlow<SettingsPreferences> = getSettingsUseCase()
    .onEach { _isReady.value = true }
    .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsPreferences())
```

Every screen that reads `settings` (`WorkoutSettingsScreen`, `GeneralSettingsScreen`,
`VoiceOptionsScreen`, `EditGoalScreen`) now collects `isReady` too and renders a centered
`CircularProgressIndicator` instead of the real content until it flips — the default values are
never composed onto the screen at all. This only guards the *first* load; `EditGoalScreen`'s
separate "seed-once" `remember`/`LaunchedEffect(settings)` pattern (below) is still what stops
later re-emissions (e.g. right after Save) from clobbering an in-progress edit — the two fixes
address different moments and both are needed. Apply this same `isReady` pattern to any new
settings-backed screen.

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

## Wheel/scroll pickers (`ui/components/WheelPicker.kt`)

Rest timer, Prep timer, and the daily reminder time used to be fixed preset dialogs
(`SingleChoiceDialog` over `[15,20,30,45,60,90]` sec / `[5,10,15,20,30]` sec) or Material3's dial
`TimePicker`. Replaced with a genuine scrollable/snapping wheel picker, built from scratch (no new
dependency) on `LazyColumn` + `rememberSnapFlingBehavior` (from
`androidx.compose.foundation.gestures.snapping`, already available in the pinned Compose Foundation
version — nothing new added to `build.gradle.kts`):

- `WheelColumn(values, selected, onSelect)` — one snapping numeric wheel. Deliberately does **not**
  try to track the exact fling/snap alignment to decide the committed value; instead a
  `LaunchedEffect` watches `listState.isScrollInProgress` via `snapshotFlow` and, the moment
  scrolling settles, picks whichever visible item's center is closest to the viewport center. This
  is robust to whatever snap position `rememberSnapFlingBehavior` happens to pick internally.
- `DurationWheelPicker(totalSeconds, onChange, minSeconds, maxSeconds)` — two `WheelColumn`s
  (minutes 0-59, seconds 0-59) composed as `MM:SS`, clamped to `[minSeconds, maxSeconds]`. Backs
  Rest timer and Prep timer, range `00:05`-`59:59` (`MIN_TIMER_SEC = 5`,
  `MAX_TIMER_SEC = 59*60+59` in `WorkoutSettingsScreen.kt`).
- `ClockWheelPicker(time24, onChange)` — hour(1-12) / minute / AM-PM wheels editing a 24-hour
  `"HH:mm"` string (the storage format is unchanged — only the picker UI changed). Backs the daily
  reminder time in `GeneralSettingsScreen.kt`.
- Both are wrapped in a dialog (`TimerWheelDialog` in `WorkoutSettingsScreen.kt`,
  `ReminderTimePickerDialog` in `GeneralSettingsScreen.kt`) that holds the edit in **local**
  `remember` state and only calls the ViewModel setter on "Set" — scrolling a wheel must not persist
  mid-drag.
- Tile labels changed accordingly: Rest/Prep timer now show `"MM:SS"` (`formatTimer()`) instead of
  `"30 secs"`; reminder time shows `"7:00 AM"` (`formatTimeLabel()`) instead of raw `"07:00"`.

## Daily reminder — real scheduling

Full design (architecture, permissions, `AlarmManager`/`BroadcastReceiver` details, why background
delivery can still fail on some OEMs, and how to test it) has its own file:
**[`docs/notifications.md`](notifications.md)** — read that before touching
`ui/services/ReminderScheduler.kt`, `ui/receivers/`, or the reminder rows on
`GeneralSettingsScreen.kt`.

Short version: the reminder toggle and time picker used to only write to `user_settings` — nothing
ever actually scheduled a notification. Now backed by `AlarmManager.setExactAndAllowWhileIdle` +
`ReminderReceiver`/`BootReceiver` + a high-importance notification channel, wired through
`SettingsViewModel.setDailyReminder()` as a `ui/services/` side effect (same pattern as `TtsService`
— not a domain Use Case, since scheduling a system alarm has nothing to model in the domain layer).
The in-app UI intentionally has **no permission hints or debug buttons** — those were tried during
development and then removed on request to keep Settings clean; the full permission/OS-reliability
model they used to explain now lives in `docs/notifications.md` instead.

## Wheel pickers — infinite loop + 24-hour reminder time

Two follow-up changes to `ui/components/WheelPicker.kt`, prompted by feedback that the wheels
should wrap around (`59 -> 0`, `0 -> 59`) instead of hard-stopping at the ends, and that the
reminder-time picker should be 24-hour (`00-23`) with no AM/PM column:

- **`WheelColumn` now loops.** There is no native "circular list" API in Compose `LazyColumn`, so
  this is the standard trick: the backing list is a virtual `values.size * CYCLES` items
  (`CYCLES = 2000`), each item mapped back to a real value via `values[index % values.size]`, and
  the wheel starts scrolled to the middle of that virtual range. That gives ~1000 loops of scroll
  room in either direction from the start — not literally infinite, but far more than any user will
  reach by scrolling, which is what "wraps around" means in practice for a picker like this.
  `settledValue()` maps the same way (`values[closestVisibleIndex % values.size]`), so committing
  the selected value works identically to before.
- **`ClockWheelPicker` is now `HH:mm`, no AM/PM.** Dropped the hour-12/AM-PM wheel split entirely —
  it's now just two wheels, hour `00-23` and minute `00-59`, both looping via the above. The
  `"HH:mm"` storage format on `SettingsPreferences.dailyReminderTime` is unchanged, only the picker
  UI changed. `GeneralSettingsScreen.formatTimeLabel()` was simplified to match (`"%02d:%02d"`, no
  12-hour conversion).
- **`DurationWheelPicker`'s min-seconds floor removed** (`minSeconds` default `5 -> 0`,
  `WorkoutSettingsScreen.MIN_TIMER_SEC` likewise `5 -> 0`) — minutes and seconds are now both plain
  independent `00-59` wheels with no combined lower clamp, per explicit request. The upper clamp
  (`59*60+59` = `59:59`) is a no-op in practice since that's already the maximum either wheel pair
  can produce.

## Wheel picker bug fix — sibling-column desync (stale `LaunchedEffect` closure)

Reported symptom: scrolling one column (e.g. minute) would reset the *other* column (e.g. hour) back
to a stale value. Root cause was a classic Compose pitfall in `WheelColumn`:

```kotlin
LaunchedEffect(listState) {
    snapshotFlow { listState.isScrollInProgress }
        .map { scrolling -> if (scrolling) null else settledValue(listState, values) }
        .collect { value -> if (value != null && value != selected) onSelect(value) }
}
```

`listState`'s identity never changes across recompositions (`rememberLazyListState` caches it), so
this `LaunchedEffect` launches its coroutine **exactly once** and then runs for the wheel's entire
lifetime. The `selected`/`onSelect` it closes over are plain parameters, not `State` reads — so the
coroutine keeps using whatever values/lambda it captured at that first launch, even though the
composable recomposes with fresh `selected`/`onSelect` every time a *sibling* wheel changes (e.g.
`DurationWheelPicker` passes `onSelect = { m -> onChange((m * 60 + seconds).coerceIn(...)) }` to the
minutes wheel, closing over `seconds` — a new lambda every recomposition, but the seconds wheel's
own long-running coroutine never sees the update). So: scroll hour -> minute wheel recomposes with a
new `onSelect` closing over the new hour -> but minute wheel's `LaunchedEffect` is still running the
*original* coroutine that captured the *old* `onSelect`/`selected` from before hour ever changed ->
next time minute settles, it fires the stale closure and silently reverts hour.

**Fix** — read both through `rememberUpdatedState` instead of closing over them directly:

```kotlin
val currentSelected by rememberUpdatedState(selected)
val currentOnSelect by rememberUpdatedState(onSelect)
LaunchedEffect(listState) {
    snapshotFlow { listState.isScrollInProgress }
        .map { scrolling -> if (scrolling) null else settledValue(listState, values) }
        .collect { value -> if (value != null && value != currentSelected) currentOnSelect(value) }
}
```

`rememberUpdatedState` gives a `State` wrapper that always reflects the latest value passed in, safe
to read from inside a long-lived coroutine/effect — this is the standard fix for "effect closes over
a stale callback" in Compose. Apply this pattern to any future long-running `LaunchedEffect` that
calls back into parent-supplied lambdas.

Also removed the redundant preview `Text` (`"MM:SS"`/`"HH:mm"`) that sat above each wheel picker in
`TimerWheelDialog` (`WorkoutSettingsScreen.kt`) and `ReminderTimePickerDialog`
(`GeneralSettingsScreen.kt`) — the centered wheel values already show the selection, so the text was
purely redundant. Both dialogs already followed the "local state until Set is tapped" pattern
correctly (a local `remember`ed value seeded from the current setting, only pushed to the ViewModel
on "Set"; "Cancel"/dismiss just closes without calling `onConfirm`) — that part didn't need fixing,
only the underlying `WheelColumn` desync bug that corrupted the local state while scrolling.

## Wheel picker performance pass — jank/stutter during fling

Reported symptom: a visible stutter/lag spike right before the wheel snaps into place. Four changes
to `WheelColumn`, all in the "don't do expensive work on every scroll frame" direction:

- **Highlighted-item tracking moved to `derivedStateOf`.** Previously each item's `isSelected` was
  `value == selected`, comparing against the `selected` *parameter* — which only changes after a
  settle, so it wasn't itself a jank source, but it also meant the highlight didn't visually track
  the wheel while scrolling (dead weight, not a perf bug). Replaced with:
  ```kotlin
  val centeredIndex: State<Int?> = remember(listState) { derivedStateOf { centeredVirtualIndex(listState) } }
  ```
  `listState.layoutInfo` is itself a snapshot `State` that changes on **every** scroll frame during a
  fling — reading it directly in the composition body (as the old `settledValue()` helper did, just
  gated to only run at settle-time) is fine for a one-shot read, but `derivedStateOf` is the correct
  tool when something reads a fast-changing value continuously: its recorded output (here, "which
  virtual index is nearest the viewport center") only changes when the nearest item actually flips —
  at `ITEM_HEIGHT = 40.dp` of travel, not every pixel/frame — so item composables reading
  `centeredIndex.value` for their highlight state skip recomposition on most scroll frames, not just
  most of the time by accident.
  Each item in the `items(virtualCount) { index -> ... }` lambda now reads `centeredIndex.value`
  directly (`isSelected = index == centeredIndex.value`) rather than the `selected` parameter — only
  the ~2 currently-composed items whose highlighted state actually flips recompose per index change,
  not all visible items, and not the whole column.
- **Settle-commit reuses the same derived state** instead of re-scanning `visibleItemsInfo` a second
  time in the `LaunchedEffect`: `centeredIndex.value?.let { values[it % size] }` at the moment
  `isScrollInProgress` flips to `false`. One source of truth for "what's centered," read exactly
  once per settle, never during the scroll itself.
- **Pre-formatted strings.** `formatted = remember(values, format) { values.map(format) }` builds the
  `"00"`.."59"`/`"23"` label strings once per `WheelColumn` instance instead of calling
  `"%02d".format(it)` (a `java.util.Formatter` parse) for every item composed while flinging.
- **One layout node removed per row.** Items used to be `Box(fillMaxWidth+height, contentAlignment =
  Center) { Text(...) }` — now a single `Text` with `Modifier.fillMaxWidth().height(ITEM_HEIGHT)
  .wrapContentHeight(Alignment.CenterVertically)` and `textAlign = TextAlign.Center`, doing the same
  centering with one less measure/layout pass per visible row.
- **Unchanged, and worth confirming stayed that way:** snapping is still 100% native
  (`rememberSnapFlingBehavior(listState)`) — no manual coroutine delays or spring animations were
  ever added that could fight user touch input; that part of the original implementation was already
  correct.

## "Keep the screen on" — now actually applied

Previously stored but never read anywhere outside `SettingsRepositoryImpl`. `MainActivity.kt` gained
`KeepScreenOnEffect()`, a small composable (`LocalView.current.keepScreenOn = settings.keepScreenOn`
inside a `LaunchedEffect` collecting `App.settingsRepository.observeSettings()` directly — this one
reads the repository straight from the Activity/App layer rather than through a ViewModel, since
it's a single fire-and-forget window-flag side effect with no screen-local state, not worth a
ViewModel for) called once from `HomeWorkoutApp()`, so it's live for the whole app regardless of
which screen is on top.

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

`ui/App.kt`: `settingsRepository`, `ttsService`, `reminderScheduler`, `getSettingsUseCase`,
`updateSettingsUseCase`, `resetWorkoutProgressUseCase` added as `by lazy` properties, same pattern
as the existing repositories/use cases. `ScreenNavigator.kt`: each of the three pushed settings
routes builds its own `SettingsViewModel` via `viewModelFactory { initializer { ... } }`, injecting
those five dependencies (four use-case/service objects plus `reminderScheduler`) from
`appInstance`.

## General Settings — feature guide & testing

What each row on `GeneralSettingsScreen` actually does, and how to verify it on an emulator/device.

### "Remind me to work out every day" + "Reminder time"
**What it does, how it's tested, and why it can still fail to fire on some phones even when the
code is correct:** see **[`docs/notifications.md`](notifications.md)** — it's substantial enough
(permission model, `AlarmManager` design, confirmed OEM background-kill caveats, manual test
recipe) to warrant its own file rather than duplicating it here.

### "Metric & Imperial Units"
**What it does:** Persists `UnitSystemType.METRIC`/`IMPERIAL` to `user_settings.unitSystem` via the
same `SingleChoiceDialog` pattern as Gender. **Not yet consumed anywhere else in the app** — no
screen currently formats weights/distances differently based on this value (grep for
`UnitSystemType`/`unitSystem` turns up only the model, repository mapping, and this screen). It's a
real persisted preference, not a display-only stub, but there's no unit-aware display logic wired
to it yet.
**How to test:** Change the selection, back out of the app entirely (swipe away from Recents),
reopen, and confirm General Settings still shows your last choice — that's the extent of currently
observable behavior.

### "Keep the screen on"
**What it does:** Persists `keepScreenOn` and, app-wide (not just while this screen is open),
`MainActivity`'s `KeepScreenOnEffect()` sets `View.keepScreenOn` to match it — this is the standard
Android mechanism the display uses to decide whether to dim/lock on inactivity.
**How to test:** Enable it, then leave the emulator sitting on any screen without touching it for
longer than the emulator's configured screen-timeout (Settings > Display > Screen timeout on the
emulator, or just watch — it won't dim/lock). Disable it and the screen should time out normally
again. This is externally visible without Logcat — just watch the display.

### "Privacy Policy"
**What it does:** Opens `PRIVACY_POLICY_URL` (currently the placeholder
`https://example.com/privacy-policy`, see Known limitations) via `Intent.ACTION_VIEW` in the
device/emulator's browser.
**How to test:** Tap it, confirm a browser opens to that URL. Nothing is persisted; this row has no
state of its own.

## Reminder debugging history (moved)

The full debugging history for the daily reminder — the foreground-vs-background symptom, the
diagnostic logging added to `ReminderScheduler`/`ReminderReceiver`, and the confirmed root cause
(OEM background-process killers on BBK-brand phones, found via live device logcat) — is now in
**[`docs/notifications.md`](notifications.md)**, including the exact manual per-OEM settings needed
(Vivo/Oppo/OnePlus/Realme/Xiaomi/Samsung). The in-app permission hints and debug test button that
were added during that investigation were later removed from `GeneralSettingsScreen` on request to
keep the screen clean — the explanations they used to provide inline now live in that doc instead.

## Known limitations / follow-ups

- On BBK-brand OEMs (Vivo/Oppo/OnePlus/Realme) and similarly aggressive skins (Xiaomi/Samsung),
  reliable background delivery of the reminder **requires the user to manually whitelist the app**
  in that brand's own battery/autostart settings — see
  [`docs/notifications.md`](notifications.md) for the confirmed root cause (found via live device
  logcat, not speculative) and exact per-OEM steps. There is no code-only fix for this.
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
- `unitSystem` (Metric/Imperial) is real persisted state but is not yet consumed by any
  weight/distance display in the app — there's simply no such display to convert yet. Wire it in
  when one is added, rather than assuming it already affects something.
- Exact daily-reminder alarms depend on `AlarmManager.canScheduleExactAlarms()` (API 31+, mitigated
  by declaring `USE_EXACT_ALARM` — see `docs/notifications.md`). `ReminderScheduler` degrades to an
  inexact alarm rather than failing when it's unavailable, but the app currently has **no in-app UI**
  to request this or the notification/battery-optimization permissions (removed on request — see
  `docs/notifications.md` for how to re-add it if a future task needs it). The user has to grant
  these via system Settings directly if the OS didn't auto-grant them.
- The reminder/alarm/notification pipeline **has** been exercised on a real device (see
  `docs/notifications.md`'s root-cause section, confirmed via live logcat) — but `keepScreenOn` and
  the rest of General Settings have only been verified via `gradlew assembleDebug` (compile success),
  not on-device.
