# Workout Progress Persistence — Implementation Notes

Lets a user close/kill the app mid-workout without losing progress, then either **Resume** exactly
where they left off or **Restart** from scratch. Read this before touching
`ui/core/player/WorkoutPlayerScreen.kt`/`WorkoutPlayerViewModel.kt`, `WorkoutSessionEntity`/
`WorkoutSessionDao`, or the Home/Detail "Resume"/"Continue" surfaces — several pieces only make
sense together.

No completion-counter ("Completed N times" badge) was built — that part of the original ask was
explicitly dropped in favor of just Start/Resume/Restart.

## The core idea

`workout_sessions` (`WorkoutSessionEntity`) already had `currentPhase`, `currentOrderIndex` and
`phaseRemainingSec` columns before this feature — they just sat unused. The During Workout player
(`WorkoutPlayerScreen`) tracks `phase`/`index`/`remaining` as local Compose `remember` state; this
feature's whole job is keeping that local state and the DB row in sync, in both directions:

- **Write**: every exercise completion, phase change, or pause auto-saves the current spot to the
  session row (still `IN_PROGRESS`).
- **Write (explicit)**: the mid-workout exit dialog's "Save & Exit" does the same save but also
  flips the row to `PAUSED`; "Discard" marks it `ABANDONED` instead.
- **Read**: opening the player with `resume = true` (or the Home/Detail "Continue"/"Resume Workout"
  entry points) fetches that saved spot and seeds the local state from it instead of starting at
  `PREP`/exercise 0.

No DB schema/version change was needed for any of this — `AppDatabase` stays at version 6.
Staleness (see below) is computed from the pre-existing `startedAt` column, not a new timestamp.

## Data layer

`data/local/dao/WorkoutSessionDao.kt`:
- `updateProgress(sessionId, phase, orderIndex, remainingSec, status)` — the auto-save write.
  Guarded with `AND status IN ('IN_PROGRESS', 'PAUSED')`: once a session is `COMPLETED`/`ABANDONED`,
  no stray/late-arriving auto-save can silently revert it back to "active" (see Bugs below for why
  this guard exists).
- `observeLatestActiveSession(userId, inProgress, paused)` — **reactive** (`Flow`, not a one-shot
  suspend read): the most-recent `IN_PROGRESS`/`PAUSED` session across every plan, backing the Home
  "Continue" card. Room's invalidation tracker re-runs this automatically on every write to
  `workout_sessions`, so the card updates live without the screen needing to be re-entered.
- `getLatestSessionForPlan(userId, planId)` (pre-existing) is reused for the per-plan Resume check.

`data/repositories/WorkoutSessionRepositoryImpl.kt` adds:
- `getResumableSession(planId)` / `observeActiveSession()` — both funnel through a shared private
  `resolveResumable(session)`: returns `null` for anything not `IN_PROGRESS`/`PAUSED`, and — the
  **12-hour stale-session rule** — auto-abandons (writes `ABANDONED`) and returns `null` for a
  session whose `startedAt` is more than 12h old, so the user gets a fresh Start instead of resuming
  yesterday's abandoned attempt. `STALE_SESSION_MS` lives as a private companion constant.
- `saveProgress(...)` / `saveAndExit(...)` — thin wrappers over `updateProgress`, differing only in
  the `status` they write (`IN_PROGRESS` vs `PAUSED`).

## Domain layer

- `domain/models/ResumableSession.kt` — `(sessionId, planId, planDayId, phase, orderIndex, remainingSec)`,
  the repository-level shape.
- `domain/models/ActiveWorkoutSummary.kt` — the Home card's display shape: adds `planTitle`,
  `coverImageUrl`, `dayNumber`/`totalDays`, `totalExercises`/`completedExercises`.
- `domain/usecases/player/GetResumableWorkoutUseCase.kt` — per-plan resume check used by both the
  Detail screen (to decide Resume/Restart vs Start) and the player itself (to actually resume):
  joins `WorkoutSessionRepository.getResumableSession(planId)` with
  `WorkoutRepository.getWorkoutPlanDetail(planId)` to resolve the saved `planDayId` back to a real
  `WorkoutPlanDayDetail` (exercise list, day number). Returns `ResumedWorkoutSession` or `null`.
- `domain/usecases/home/GetActiveWorkoutUseCase.kt` — same idea but plan-agnostic and **fully
  reactive** (`Flow<ActiveWorkoutSummary?>`, `flatMapLatest`-chained from
  `observeActiveSession()` into `getWorkoutPlanDetail`). Also where `completedExercises` is derived
  from the raw `orderIndex` — see the off-by-one bug below for why phase matters here.
- `domain/usecases/player/SaveWorkoutProgressUseCase.kt` / `SaveAndExitWorkoutSessionUseCase.kt` —
  one-line wrappers over the repository methods of the same name.
- `AbandonWorkoutSessionUseCase` (pre-existing) is reused as-is for both "Discard" and for retiring
  the old session when the user taps "Restart" on the Detail screen.

## Player screen & ViewModel

`WorkoutPlayerViewModel`:
- Constructor gains `resume: Boolean`. `beginDay()` calls `GetResumableWorkoutUseCase(planId)` first
  when `resume == true`; on a hit, `PlayerUiState.Ready` is seeded with `initialPhase`/
  `initialOrderIndex`/`initialRemainingSec` from the saved session instead of the `PREP`/`0`/`null`
  defaults. On a miss (nothing resumable, or it just expired) it falls through to the normal
  fresh-start path unchanged.
- `saveProgress(...)` and `saveAndExit(...)` both take an `onDone: () -> Unit` completion callback
  rather than being pure fire-and-forget — see the navigation race bug below for why.
- `showExerciseInfo(exerciseId)` / `dismissExerciseInfo()` drive a small `ExerciseInfoSheetState`
  (Hidden/Loading/Loaded/Error), backed by the same `GetExerciseDetailUseCase` the standalone
  Exercise Info screen uses.

`WorkoutPlayerScreen` (`PlayerContent`):
- `phase`/`index`/`remaining` `remember` state now seeds from `initialPhase`/`initialOrderIndex`
  (clamped to a valid range)/`initialRemainingSec` instead of hardcoded `PREP`/`0`/`PREP_SECONDS`.
- The existing `LaunchedEffect(phase, index)` (already fired once per phase/exercise transition) is
  the auto-save point — it now also calls `onSaveProgress(phase, index, remaining)`, **except when
  `phase == COMPLETED`** (see Bugs). `onTogglePause` also saves when transitioning into `paused`.
- `BackHandler(enabled = phase != COMPLETED) { showQuitDialog = true }` — the hardware/gesture back
  button now opens the same guard dialog as the on-screen close (X) button; previously only the X
  button did.
- The quit dialog's two buttons are **"Save & Exit"** (`onSaveAndExit`, then closes once the write
  finishes) and **"Discard"** (`onAbandon`, same pattern) — replacing the old bare "Quit"/"Keep
  going". Tapping outside the dialog still cancels back into the workout.
- The exercise-info (ⓘ) button no longer navigates anywhere — see "In-place exercise info" below.

## Detail screen — dynamic Start / Resume / Restart

`DetailViewModel.resumable: StateFlow<ResumedWorkoutSession?>` (same lazy
`flow { emit(...) }.stateIn(WhileSubscribed(5_000))` pattern as the pre-existing `nextDay`) drives
`DetailScreen`'s top-level action button:
- Non-null **and** its day matches what "Start" would play next → two buttons, **"Resume Workout"**
  (primary) and **"Restart"** (tonal), replacing the single "Start" button.
- Otherwise → the original "Start" / "Start · Day N" button, unchanged.
- "Restart" calls `DetailViewModel.restart(oldSessionId)` (fires `AbandonWorkoutSessionUseCase` on
  the old row) *before* navigating to a fresh session for that same day — so the old row doesn't
  linger as resumable.
- Per-day panels in a multi-day plan's exercise list still only show a plain "Start" link — Resume/
  Restart is only on the primary action, by design (kept out of scope deliberately).

## Home screen — "Continue Workout" card

`HomeViewModel.activeWorkout: StateFlow<ActiveWorkoutSummary?>` collects
`GetActiveWorkoutUseCase()` directly (it's a `Flow` all the way down — no `flow { emit(...) }`
one-shot wrapper, unlike `resumable` above; see the staleness bug below for why that distinction
matters here specifically). `HomeScreen` renders a `ContinueWorkoutCard` — plan thumbnail, plan
title, "Day X · N/M exercises done", a progress bar, tap-anywhere-or-play-button — as a `LazyColumn`
item inserted right before the "Body Focus" section, only when `activeWorkout != null`. Tapping it
navigates straight into the player with `resume = true`.

## In-place exercise info (no navigation)

Tapping the ⓘ icon during a workout opens the exercise's gif/instructions/muscles as a
`ModalBottomSheet` layered over the player, instead of navigating to `Screen.ExerciseInfo`. The
content composable (`ExerciseInfoContent` in `ui/core/exerciseinfo/ExerciseInfoScreen.kt`) was
changed from `private` to `internal` so `WorkoutPlayerScreen` can reuse it directly — same rendering
as the standalone Exercise Info screen, just hosted differently. `WorkoutPlayerScreen` no longer
takes an `onExerciseInfo` navigation callback at all; `ScreenNavigator`'s Player route was
simplified accordingly. See "Navigating away disposed the timer" below for *why* this had to change.

## Navigation

`Screen.Player` route gained a `resume: Boolean = false` query param
(`"player/{planId}?planDayId={planDayId}&resume={resume}"`). Three entry points now exist:
- **Start** (fresh): `Screen.Player.createRoute(planId, planDayId)`, `resume` omitted.
- **Resume**: `Screen.Player.createRoute(planId, resume = true)` — from either the Detail screen or
  the Home "Continue Workout" card.
- **Restart**: same as Start, but only after `DetailViewModel.restart(oldSessionId)` has abandoned
  the previous row.

## Bugs found and fixed along the way

Worth reading before changing any of the pieces above — each of these was a real, reproduced bug,
not a hypothetical.

1. **`Save & Exit` silently didn't persist.** The quit dialog called `onSaveAndExit(...)` (a
   fire-and-forget `viewModelScope.launch`) and then `onClose()` on the very next line. Popping the
   back stack clears the `WorkoutPlayerViewModel` (cancelling `viewModelScope`) almost always before
   the launched coroutine got to run. Fix: `saveAndExit`/`abandonSession` take an `onDone` callback;
   the screen passes `onClose` as that callback so navigation happens *after* the write completes,
   not before it starts.

2. **Finishing a workout could silently un-complete it.** `finishCurrentExercise()` sets
   `phase = COMPLETED` and calls `onComplete()` (which finalizes the session: `status = COMPLETED`,
   `endedAt`, duration) — but that same `phase` change *also* re-triggers the shared auto-save
   `LaunchedEffect(phase, index)`, which unconditionally wrote `status = IN_PROGRESS`. The two
   writes raced; whichever's DB write landed last won, so a just-finished session could flip back to
   "active" and get stuck there. Fixed two ways: the auto-save effect now skips firing when
   `phase == COMPLETED`, and `updateProgress`'s SQL additionally guards
   `AND status IN ('IN_PROGRESS', 'PAUSED')` as defense-in-depth against any similar race.

3. **`onClose` was a no-op when the player was entered from Home.** Wired as
   `navController.popBackStack(Screen.Details.route, inclusive = false)` — "pop until you find
   Details." That only holds when the player is opened from the Detail screen. The Home "Continue
   Workout" card navigates **Home → Player** directly, so `Details` was never on the back stack;
   `popBackStack(route, ...)` silently does nothing when the target isn't present. Every
   `onClose`-driven button (Keep exercising, Do it later, and Save & Exit/Discard via their
   `onDone`) looked broken when entered that way, while "Restart" (which never navigates, only
   resets local state) kept working — that asymmetry is what pointed at `onClose` specifically.
   Fixed to a plain `navController.popBackStack()`, which is correct regardless of entry point.

4. **Continue Workout's exercise count was off by one during REST.** The saved `orderIndex` names
   the exercise the player is *currently on*. That equals the completed count in `PREP`/`EXERCISE`
   (0..index-1 are done), but in `REST` the index hasn't advanced yet — it still names the exercise
   that *just* finished. `GetActiveWorkoutUseCase` now adds 1 in `REST`/`COMPLETED`.

5. **The Continue Workout card went stale after the first update.** `HomeViewModel.activeWorkout`
   was originally `flow { emit(getActiveWorkoutUseCase()) }.stateIn(...)` — a one-shot snapshot.
   Home is a persistent bottom-nav destination whose `ViewModel` survives navigating into the player
   and back out, so that single cached emission never refreshed after the first time — it reflected
   whatever was true when Home first loaded and then appeared "stuck" through every subsequent
   exercise. Fixed by making the whole chain genuinely reactive (`observeLatestActiveSession` as a
   Room `Flow`, `observeActiveSession()`, `GetActiveWorkoutUseCase` returning `Flow` end to end) so
   every `saveProgress`/`saveAndExit`/`abandonSession`/`completeSession` write pushes a fresh value
   automatically. (`DetailViewModel.resumable` intentionally keeps the one-shot pattern — Detail is
   re-entered/recreated on every visit, so staleness never manifests there the same way.)

6. **Navigating to Exercise Info reset the whole workout.** `Screen.ExerciseInfo` was a separate nav
   destination; Navigation-Compose only keeps the *current* destination's UI composed, so pushing it
   disposed `WorkoutPlayerScreen`'s composition — including the local `remember`ed `phase`/`index`/
   `remaining`/`paused` and the running timer `LaunchedEffect`s and TTS/tick sounds. Coming back
   re-ran the composable from scratch, reseeding from `initialPhase`/`initialOrderIndex` (the
   session's original start/resume point), which looked exactly like the workout restarting. Fixed
   by rendering exercise info as an in-place `ModalBottomSheet` (see above) instead of navigating —
   the underlying composition, and everything running inside it, is never torn down.

## Known limitations / follow-ups

- No completion counter — dropped from scope on request; only Start/Resume/Restart exist.
- "Set" in the original ask doesn't map onto anything in this app's data model — a plan exercise has
  reps/duration, not sets, so all "which set" tracking here is really "which exercise" (`orderIndex`
  into the day's exercise list). If set-level tracking is ever wanted, it needs schema work first
  (`workout_session_exercises` logs one result per exercise per session, no set breakdown).
- Multi-day plans' per-day panels (`DayGroupPanel` in `DetailScreen.kt`) don't get their own
  Resume/Restart buttons — only the primary top-level action does. A day other than the "next" one
  always shows a plain "Start" link even if it happens to have its own resumable session.
- Stale-session expiry (12h) is computed from `startedAt`, not a separate "last activity" timestamp
  — fine in practice since a real workout never runs anywhere near that long, but worth knowing if
  the threshold is ever tightened.
- Compiled and verified via `./gradlew :app:compileDebugKotlin` throughout; not yet exercised
  end-to-end on an emulator/device (kill-process-mid-workout-then-relaunch in particular should be
  verified manually before shipping).
