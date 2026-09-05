# UI Design & Implementation Specification
## 12-Week Beginner Running Program

**Feature:** Running Training Program  
**Program:** 12-Week Beginner Running Program  
**Target Platform:** Android  
**UI Framework:** Jetpack Compose  
**Architecture:** Clean Architecture + MVVM (Consistent with HomeWorkout project)  
**Primary Accent:** `#0057FF` (`BrandBlue` / `BrandBlueLight`)  

---

# 1. Objective

Implement a production-ready **Plan Detail & Workout Flow** for the:

**12-Week Beginner Running Program**

The program progressively transitions users from walking and short run/walk intervals toward running continuously for approximately 30–40 minutes.

### Recommended Starting Ability:
The user should be able to comfortably walk for approximately 30 minutes before starting this program.

### Primary Navigation Flow:
```text
Discovery Screen (ui/core/discovery/DiscoveryScreen.kt)
   ↓ (tap Beginner Running card)
Screen.RunningPlanDetail(programId = "beginner-running-12w")
   ↓
RunningPlanDetailScreen
   ↓
Week Accordion Card
   ↓
Workout Session Row
   ↓
Screen.RunningPlayer(programId, sessionId)
   ↓
Running Workout Player / GPS Tracking Screen (RunningTrackingService)
```

The implementation must be:
- data-driven (loaded via domain repository from JSON catalog),
- reusable for future running programs,
- integrated with Room database for progress persistence,
- compatible with GPS workout execution (`RunningTrackingService`),
- compatible with interval countdowns and voice/vibration cues,
- strictly aligned with the existing Clean Architecture of the repository.

> [!NOTE]
> Do not introduce training phases into this program, as the supplied 12-week running curriculum does not define phases.

---

# 2. Entry Point & Navigation Integration

From the existing **Discovery** screen (`DiscoveryScreen.kt` under `ui/core/discovery/`), connect the running program card:

```text
12 WEEKS PROGRAM
BEGINNER RUNNING
```

When tapped:
```text
DiscoveryScreen
  → onOpenRunningPlan("beginner-running-12w")
  → navController.navigate(Screen.RunningPlanDetail.createRoute("beginner-running-12w"))
```

In `ui/navigation/Screen.kt`, define:
```kotlin
object RunningPlanDetail : Screen("running_plan_detail/{programId}") {
    fun createRoute(programId: String) = "running_plan_detail/$programId"
}

object RunningPlayer : Screen("running_player/{programId}/{sessionId}") {
    fun createRoute(programId: String, sessionId: String) = "running_player/$programId/$sessionId"
}
```

The Plan Detail screen loads program data through the domain use case (`GetRunningProgramUseCase`) rather than hard-coding the 12 weeks inside the composable.

---

# 3. Target Screen

## RunningPlanDetailScreen

Primary responsibilities:
- introduce the program (Hero header, cover image, metadata chips),
- show the recommended starting ability (30-min walking base),
- explain running intensity levels (Walk, Easy Run, Steady Run),
- render all 12 weeks via virtualized `LazyColumn`,
- display session structure, interval timelines, and durations,
- show user progress (current week, completed sessions),
- allow previewing upcoming weeks,
- allow starting or continuing the current workout,
- show supplied Coach Tips, Goals, and Milestones,
- support Week 11 optional fourth session,
- show the After Week 12 maintenance schedule,
- show the Important Training Rules section.

Reference logical viewport: `390 × 844 dp` (Mobile Portrait, responsive layout).

---

# 4. Design System & Theme Integration

The UI utilizes the project's design system (`ui/theme/`), adopting dark surface cards with brand blue accents:

## 4.1 Core Color Tokens

| Token | Value | Usage |
|---|---|---|
| `canvasBase` | `#000000` | Main screen background |
| `surfaceCard` | `#18191D` | Week / information cards |
| `surfaceSubcard` | `#202227` | Session rows |
| `surfaceRaised` | `#242426` | Secondary surfaces / borders |
| `primaryAccent` | `#0057FF` | Primary CTA / active state / `BrandBlue` |
| `primaryAccentBright` | `#0080FF` | Accent text |
| `primaryAccentTint` | `rgba(0,87,255,0.15)` | Accent backgrounds |
| `intensityWalk` | `#6B7280` | Walking segments |
| `intensityEasyRun` | `#10B981` | Easy running |
| `intensitySteadyRun` | `#0057FF` | Steady running |
| `intensityHigherEffort`| `#F59E0B` | Moderate / slightly faster segments |
| `milestoneGold` | `#FBBF24` | Milestones |
| `textPrimary` | `#FFFFFF` | Primary text |
| `textSecondary` | `#8E929A` | Secondary text |
| `textTertiary` | `#6F737C` | Low-priority metadata |
| `borderSubtle` | `#242426` | Borders and dividers |

---

# 5. Typography

Use the project's existing typography (`ui/theme/Type.kt`).

| Role | Size | Weight | Color |
|---|---:|---:|---|
| Hero Title | 26–28 sp | 800 | `#FFFFFF` |
| Section Title | 20–22 sp | 700 | `#FFFFFF` |
| Week Title | 16–17 sp | 600 | `#FFFFFF` |
| Session Title | 14–15 sp | 500 | `#E5E7EB` |
| Badge | 11–12 sp | 700 | Accent |
| Metadata | 13–14 sp | 400 | `#8E929A` |
| Coach Tip | 12–13 sp | 400 | `#D1D5DB` |
| CTA | 16 sp | 700 | `#FFFFFF` |

Use Android `sp` units for all text dimensions.

---

# 6. Spacing & Geometry

```text
heroRadius            = 0.dp
largeCardRadius       = 20.dp
weekCardRadius        = 18.dp
subItemRadius         = 12.dp
pillRadius            = 999.dp

pageHorizontalPadding = 16.dp
sectionGap            = 24.dp
cardGap               = 12.dp
cardInnerPadding      = 16.dp
sessionGap            = 8.dp

Hero height           ≈ 320–360.dp
Session row min height = 46.dp
Primary CTA height    = 52.dp
Floating icon button  = 40 × 40.dp
```

---

# 7. Screen Architecture

Use a single virtualized `LazyColumn` inside a `Scaffold`:

```text
Scaffold
├── TopAppBar / FloatingNavigation
├── LazyColumn
│   ├── HeroHeader
│   ├── ProgramGoalCard
│   ├── StartingAbilityCard
│   ├── IntensityGuideCard
│   ├── WeekAccordionCard #1
│   ├── WeekAccordionCard #2
│   ├── ...
│   ├── WeekAccordionCard #12
│   ├── PostProgramCard
│   └── TrainingRulesSection
│
└── BottomBar
    └── StickyProgramCTA
```

- Use `LazyColumn` with stable keys (`key = { ... }`) and `contentType`.
- Do not place the entire screen inside an ordinary `Column.verticalScroll()`.
- Do not introduce Phase sections because this program does not define phases.

---

# 8. Safe Areas & Window Insets

- Top navigation accounts for `WindowInsets.statusBars` or `WindowInsets.safeDrawing`.
- Sticky CTA in `bottomBar` accounts for `WindowInsets.navigationBars` to avoid overlapping Android gesture or 3-button navigation areas.

---

# 9. Hero Header & Floating Navigation

### Hero Content:
- Cover image: `R.drawable.discovery_running_plan`
- Vertical gradient overlay (`rgba(0,0,0,0.25)` → `rgba(0,0,0,0.55)` → `#000000`)
- Back, Bookmark, and Share circular action buttons
- Badge: `[12 WEEKS PROGRAM]`
- Title: `BEGINNER RUNNING PROGRAM`
- Metadata chips: `12 Weeks • 3–4 Sessions/Wk • Beginner`

### Floating Navigation Bar:
- **Hero visible:** Transparent overlay with circular action buttons.
- **Scrolled past Hero:** Dark translucent surface (`rgba(0,0,0,0.88)` / `DarkSurface`) with the title smoothly fading in.

---

# 10. Information Cards

### Program Goal Card:
> Progress safely from walking and short jogging intervals to running continuously for approximately 30–40 minutes.

### Starting Ability Card:
> You should be able to comfortably walk for approximately 30 minutes before starting this program.

### Intensity Guide Card (`IntensityGuideCard`):
1. **Walk (RPE 2–3 / 10):** Comfortable walking.
2. **Easy Run (RPE 3–4 / 10):** Very relaxed jogging. Able to speak in full sentences.
3. **Steady Run (RPE 5–6 / 10):** Comfortably challenging effort.

*Rule:* Most running in this program should remain easy. Avoid racing during training.

---

# 11. Workout Duration & Range Rules

1. Workout durations are derived strictly from the supplied steps.
2. Do not automatically add warm-up/cooldown time where none is specified (e.g., Week 9 Session 1 is 25 min easy run → duration is 25 min).
3. Where a range is specified (e.g., Week 7 Session 3: `30–35 min`, Week 12 Session 3: `45–55 min`), preserve the range in the data model and UI.

---

# 12. Program Structure & States

```text
RunningTrainingProgram (12 Weeks, 3 required sessions/wk + 1 optional session in Week 11)
    ↓
RunningTrainingWeek (Weeks 1–12)
    ↓
RunningWorkoutSession
    ↓
RunningWorkoutStep (Timed, TimedRange, Repeat)
```

### States:
```kotlin
enum class WeekState { CURRENT, COMPLETED, UPCOMING }
enum class SessionState { NOT_STARTED, IN_PROGRESS, COMPLETED }
enum class ProgramEnrollmentStatus { NOT_ENROLLED, ACTIVE, COMPLETED }
```

### Week Completion Rule:
A week becomes `COMPLETED` when all **required** sessions are completed.
Week 11 Session 4 is optional (`isOptional = true`) and does not block progression.

---

# 13. Primary CTA State Machine

| Program State | Session State | CTA Label | Action |
|---|---|---|---|
| `NOT_ENROLLED` | - | `START PROGRAM` | Enrolls user, activates Week 1 Session 1 |
| `ACTIVE` | Idle | `START SESSION X` | Opens player for next incomplete session |
| `ACTIVE` | In Progress | `CONTINUE SESSION` | Resumes active running workout |
| `COMPLETED` | - | `VIEW PROGRAM SUMMARY` | Opens completion summary |

---

# 14. Session Row & Timeline Visualizer

- **WorkoutSessionRow:** Displays session index, title, duration badge (or range), optional tag (Week 11 Session 4), and trailing action (Play, Resume, or Completed checkmark).
- **WorkoutTimelineVisualizer:** Renders a proportional timeline for interval sessions driven directly from `RunningWorkoutStep` data:
  - Walk: `#6B7280`
  - Easy Run: `#10B981`
  - Steady Run: `#0057FF`
  - Moderate / Faster: `#F59E0B`

---

# 15. Complete 12-Week Program Data

### Week 1 — First Running Steps
- **Sessions:** 3 | **Session Duration:** 34 min | **Weekly Total:** 102 min
- **All 3 Sessions:** 5m Warm-up Walk → 8 × (1m Easy Run + 2m Walk) → 5m Cool-down Walk
- **Goal:** Introduce running without excessive fatigue.
- **Coach Tip:** Run slower than you think you need to.

### Week 2 — Build Running Time
- **Sessions:** 3 | **Session Duration:** 38 min | **Weekly Total:** 114 min
- **All 3 Sessions:** 5m Warm-up Walk → 7 × (2m Easy Run + 2m Walk) → 5m Cool-down
- **Goal:** Become comfortable switching between running and walking.

### Week 3 — Longer Running Intervals
- **Sessions:** 3 | **Session Duration:** 40 min | **Weekly Total:** 120 min
- **All 3 Sessions:** 5m Warm-up Walk → 6 × (3m Easy Run + 2m Walk) → 5m Cool-down
- **Coach Tip:** Focus on relaxed breathing and short, comfortable strides.

### Week 4 — Five-Minute Runs
- **Sessions:** 3 | **Session Duration:** 45 min | **Weekly Total:** 135 min
- **All 3 Sessions:** 5m Warm-up → 5 × (5m Easy Run + 2m Walk) → 5m Cool-down
- **Milestone:** Run continuously for 5 minutes.

### Week 5 — Increase Continuous Running
- **Sessions:** 3 | **Weekly Total:** 115 min
- **Session 1 (42 min):** 5m Warm-up → 4 × (6m Run + 2m Walk) → 5m Cool-down
- **Session 2 (40 min):** 5m Warm-up → 3 × (8m Run + 2m Walk) → 5m Cool-down
- **Session 3 (33 min):** 5m Warm-up → 10m Run + 3m Walk + 10m Run → 5m Cool-down
- **Milestone:** Complete your first continuous 10-minute run.

### Week 6 — Build Endurance
- **Sessions:** 3 | **Weekly Total:** 112 min
- **Session 1 (39 min):** 5m Warm-up → 10m Run + 2m Walk + 10m Run + 2m Walk + 5m Run → 5m Cool-down
- **Session 2 (37 min):** 5m Warm-up → 12m Run + 3m Walk + 12m Run → 5m Cool-down
- **Session 3 (36 min):** 5m Warm-up → 15m Continuous Easy Run + 3m Walk + 8m Easy Run → 5m Cool-down
- **Goal:** Begin feeling comfortable during longer uninterrupted runs.

### Week 7 — Twenty-Minute Run
- **Sessions:** 3 | **Weekly Total:** 106–111 min
- **Session 1 (37 min):** 5m Warm-up → 15m Run + 2m Walk + 10m Run → 5m Cool-down
- **Session 2 (39 min):** 5m Warm-up → 18m Continuous Run + 3m Walk + 8m Run → 5m Cool-down
- **Session 3 (30–35 min):** 5m Warm-up → 20m Continuous Easy Run → 5–10m Cool-down Walk
- **Milestone:** Run continuously for 20 minutes.

### Week 8 — Build the Running Base
- **Sessions:** 3 | **Weekly Total:** 99 min
- **Session 1 (30 min):** 5m Warm-up → 20m Continuous Easy Run → 5m Cool-down
- **Session 2 (34 min):** 5m Warm-up → 4 × (5m Easy Run + 1m Walk) → 5m Cool-down
- **Session 3 (35 min):** 5m Warm-up → 25m Continuous Easy Run → 5m Cool-down
- **Goal:** Transition from run/walk training toward continuous running.

### Week 9 — Thirty-Minute Goal
- **Sessions:** 3 | **Weekly Total:** 83 min
- **Session 1 (25 min):** 25m Easy Run
- **Session 2 (28 min):** 20m Easy Run → 4 × (30s Slightly Faster + 90s Easy)
- **Session 3 (30 min):** 30m Continuous Easy Run
- **Milestone:** Run continuously for 30 minutes.

### Week 10 — Build Strength and Endurance
- **Sessions:** 3 | **Weekly Total:** 92 min
- **Session 1 (25 min):** 25m Easy Run
- **Session 2 (35 min):** 10m Easy → 4 × (3m Steady Run + 2m Easy Run) → 5m Easy
- **Session 3 (32 min):** 32m Continuous Easy Run
- **Goal:** Introduce controlled changes in running intensity.

### Week 11 — Stronger Runner
- **Sessions:** 3 required + 1 optional | **Weekly Total:** 95 min (req) / 115 min (with opt)
- **Session 1 (30 min):** 30m Easy Run
- **Session 2 (25 min):** 25m Recovery Run or Run/Walk
- **Session 3 (40 min):** 10m Easy → 5 × (3m Steady + 2m Easy) → 5m Cool-down
- **Optional Session 4 (20 min):** 20m Very Easy Run (`isOptional = true`)
- **Note:** Long-run target: 35 minutes continuous.

### Week 12 — Graduation Week
- **Sessions:** 3 | **Weekly Total:** 100–110 min
- **Session 1 (30 min):** 30m Easy Run
- **Session 2 (25 min):** 10m Easy → 10m Moderate → 5m Easy
- **Session 3 (45–55 min):** 5m Walking Warm-up → 35–40m Continuous Easy Run → 5–10m Walking Cool-down
- **Milestone:** Complete a continuous 35–40 minute run.

---

# 16. After Week 12 & Training Rules

### After Week 12 Routine:
- **Day 1:** Easy Run (30–40 min)
- **Day 2:** Recovery or Rest
- **Day 3:** Quality Run (Intervals, Tempo, or Hills)
- **Day 4:** Rest
- **Day 5:** Easy Run (30–40 min)
- **Day 6:** Long Run (40–60 min)
- **Day 7:** Rest or Walking
- *Guidance:* Increase weekly volume gradually rather than increasing both distance and intensity simultaneously.

### Important Training Rules:
1. **Run Easy:** Most beginner runs should feel comfortable.
2. **Rest Between Runs:** Avoid running all sessions on consecutive days (e.g. Mon/Wed/Sat).
3. **Pain Rule:** Stop exercising if experiencing sharp/worsening pain, chest pain, or severe dizziness.
4. **Progression / Repeat Week:** If a week feels too difficult, use `REPEAT WEEK` to repeat it without deleting previous GPS history.

---

# 17. Repo Architecture & Codebase Alignment

The feature follows the project's multi-tier Clean Architecture and manual Dependency Injection:

```text
app/src/main/java/com/example/homeworkout/
│
├── data/
│   ├── catalog/
│   │   ├── RunningProgramCatalogParser.kt       # JSON parser for 12-week running catalog
│   │   └── RunningProgramCatalogSource.kt       # Reads assets/beginner_running_12w.json
│   │
│   ├── local/
│   │   ├── AppDatabase.kt                       # Room DB instance
│   │   ├── dao/
│   │   │   └── RunningProgramDao.kt             # Progress queries & updates
│   │   └── entities/
│   │       ├── RunningProgramProgressEntity.kt  # User enrollment & current week
│   │       └── RunningSessionProgressEntity.kt  # Session status & timestamps
│   │
│   └── repositories/
│       ├── RunningProgramRepositoryImpl.kt      # Catalog & static program loader
│       └── RunningProgressRepositoryImpl.kt     # Room-backed progress repository
│
├── domain/
│   ├── models/
│   │   └── running/                             # Domain models
│   │       ├── RunningTrainingProgram.kt
│   │       ├── RunningTrainingWeek.kt
│   │       ├── RunningWorkoutSession.kt
│   │       ├── RunningWorkoutStep.kt
│   │       ├── RunningIntensity.kt
│   │       └── RunningProgramProgress.kt
│   │
│   ├── repositories/
│   │   ├── RunningProgramRepository.kt          # Program catalog interface
│   │   └── RunningProgressRepository.kt         # Progress tracking interface
│   │
│   └── usecases/
│       └── running/
│           ├── GetRunningProgramUseCase.kt      # Retrieves program hierarchy
│           ├── GetRunningProgressUseCase.kt     # Observes user progress Flow
│           ├── EnrollRunningProgramUseCase.kt   # Starts/enrolls program
│           ├── CompleteRunningSessionUseCase.kt # Marks session complete & updates week
│           └── RepeatRunningWeekUseCase.kt      # Resets current week progress
│
├── running/
│   └── service/
│       └── RunningTrackingService.kt            # Reusable foreground GPS tracking service
│
└── ui/
    ├── App.kt                                   # Process-level Manual DI container
    │
    ├── navigation/
    │   ├── Screen.kt                            # Screen.RunningPlanDetail & Screen.RunningPlayer
    │   └── ScreenNavigator.kt                   # Route mapping & ViewModel factories
    │
    └── core/
        └── running/
            ├── plan/
            │   ├── RunningPlanDetailScreen.kt   # 12-week Compose view
            │   ├── RunningPlanDetailViewModel.kt
            │   └── components/
            │       ├── PlanHeroHeader.kt
            │       ├── ProgramGoalCard.kt
            │       ├── StartingAbilityCard.kt
            │       ├── RunningIntensityGuideCard.kt
            │       ├── WeekAccordionCard.kt
            │       ├── WorkoutSessionRow.kt
            │       ├── WorkoutTimelineVisualizer.kt
            │       ├── CoachTipCard.kt
            │       ├── MilestoneCard.kt
            │       ├── PostProgramCard.kt
            │       ├── TrainingRulesSection.kt
            │       └── StickyProgramCTA.kt
            │
            └── player/
                ├── RunningPlayerScreen.kt       # GPS & Interval execution screen
                └── RunningPlayerViewModel.kt    # Interval cues + telemetry
```

---

# 18. Domain Models

Package: `com.example.homeworkout.domain.models.running`

```kotlin
package com.example.homeworkout.domain.models.running

data class RunningTrainingProgram(
    val id: String,
    val title: String,
    val durationWeeks: Int,
    val goal: String,
    val startingAbility: String,
    val intensityGuide: List<RunningIntensityDefinition>,
    val weeks: List<RunningTrainingWeek>,
    val postProgram: PostProgramRunningPlan?,
    val trainingRules: List<TrainingRule>
)

data class RunningTrainingWeek(
    val id: String,
    val weekNumber: Int,
    val title: String,
    val weeklyDurationMinutes: Int?,
    val weeklyDurationRange: IntRange?,
    val sessions: List<RunningWorkoutSession>,
    val goal: String?,
    val coachTip: String?,
    val milestone: String?,
    val note: String?
)

data class RunningWorkoutSession(
    val id: String,
    val sessionIndex: Int,
    val title: String,
    val durationMinutes: Int?,
    val durationRangeMinutes: IntRange?,
    val isOptional: Boolean = false,
    val steps: List<RunningWorkoutStep> = emptyList()
)

sealed interface RunningWorkoutStep {
    data class Timed(
        val label: String,
        val durationSeconds: Int,
        val intensity: RunningIntensity? = null
    ) : RunningWorkoutStep

    data class TimedRange(
        val label: String,
        val minDurationSeconds: Int,
        val maxDurationSeconds: Int,
        val intensity: RunningIntensity? = null
    ) : RunningWorkoutStep

    data class Repeat(
        val count: Int,
        val steps: List<RunningWorkoutStep>
    ) : RunningWorkoutStep
}

enum class RunningIntensity {
    WALK,
    EASY_RUN,
    STEADY_RUN,
    MODERATE,
    SLIGHTLY_FASTER
}

data class RunningIntensityDefinition(
    val intensity: RunningIntensity,
    val name: String,
    val rpe: String,
    val description: String,
    val colorHex: String
)

data class PostProgramRunningPlan(
    val title: String,
    val subtitle: String,
    val guidance: String,
    val days: List<String>
)

data class TrainingRule(
    val title: String,
    val description: String
)
```

---

# 19. Room Persistence & Progress Models

Domain Progress Model:
```kotlin
package com.example.homeworkout.domain.models.running

data class RunningProgramProgress(
    val programId: String,
    val status: ProgramEnrollmentStatus,
    val currentWeekNumber: Int,
    val completedSessions: Set<String>,
    val activeSessionId: String? = null
)
```

Room Entities (`com.example.homeworkout.data.local.entities`):
```kotlin
package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "running_program_progress")
data class RunningProgramProgressEntity(
    @PrimaryKey val programId: String,
    val status: String,
    val currentWeekNumber: Int,
    val activeSessionId: String?,
    val enrolledAt: Long,
    val completedAt: Long?
)

@Entity(tableName = "running_session_progress", primaryKeys = ["programId", "sessionId"])
data class RunningSessionProgressEntity(
    val programId: String,
    val sessionId: String,
    val weekNumber: Int,
    val status: String,
    val startedAt: Long?,
    val completedAt: Long?,
    val durationSeconds: Int?,
    val distanceMeters: Double?
)
```

---

# 20. Repositories & Use Cases

```kotlin
package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.running.RunningProgramProgress
import com.example.homeworkout.domain.models.running.RunningTrainingProgram
import kotlinx.coroutines.flow.Flow

interface RunningProgramRepository {
    suspend fun getProgram(programId: String): RunningTrainingProgram?
}

interface RunningProgressRepository {
    fun observeProgress(programId: String): Flow<RunningProgramProgress>
    suspend fun enroll(programId: String)
    suspend fun markSessionCompleted(programId: String, sessionId: String, weekNumber: Int)
    suspend fun setActiveSession(programId: String, sessionId: String?)
    suspend fun resetWeekProgress(programId: String, weekNumber: Int)
}
```

Use Cases in `com.example.homeworkout.domain.usecases.running`:
- `GetRunningProgramUseCase(private val repository: RunningProgramRepository)`
- `GetRunningProgressUseCase(private val repository: RunningProgressRepository)`
- `EnrollRunningProgramUseCase(private val repository: RunningProgressRepository)`
- `CompleteRunningSessionUseCase(private val progressRepo: RunningProgressRepository, private val programRepo: RunningProgramRepository)`
- `RepeatRunningWeekUseCase(private val progressRepo: RunningProgressRepository)`

---

# 21. Manual Dependency Injection (`ui/App.kt`)

Register lazy singletons on `App.kt`:

```kotlin
// Repositories
val runningProgramRepository: RunningProgramRepository by lazy {
    RunningProgramCatalogSource(this)
}

val runningProgressRepository: RunningProgressRepository by lazy {
    RunningProgressRepositoryImpl(database.runningProgramDao())
}

// Use Cases
val getRunningProgramUseCase by lazy { GetRunningProgramUseCase(runningProgramRepository) }
val getRunningProgressUseCase by lazy { GetRunningProgressUseCase(runningProgressRepository) }
val enrollRunningProgramUseCase by lazy { EnrollRunningProgramUseCase(runningProgressRepository) }
val completeRunningSessionUseCase by lazy {
    CompleteRunningSessionUseCase(runningProgressRepository, runningProgramRepository)
}
val repeatRunningWeekUseCase by lazy { RepeatRunningWeekUseCase(runningProgressRepository) }
```

---

# 22. Navigation Wiring (`ui/navigation/ScreenNavigator.kt`)

```kotlin
// Plan Detail
composable(
    route = Screen.RunningPlanDetail.route,
    arguments = listOf(navArgument("programId") { type = NavType.StringType })
) { entry ->
    val programId = entry.arguments?.getString("programId") ?: "beginner-running-12w"
    val vm: RunningPlanDetailViewModel = viewModel(key = "running-plan-$programId", factory = viewModelFactory {
        initializer {
            RunningPlanDetailViewModel(
                programId = programId,
                getProgramUseCase = appInstance.getRunningProgramUseCase,
                getProgressUseCase = appInstance.getRunningProgressUseCase,
                enrollUseCase = appInstance.enrollRunningProgramUseCase,
                repeatWeekUseCase = appInstance.repeatRunningWeekUseCase
            )
        }
    })
    RunningPlanDetailScreen(
        viewModel = vm,
        onNavigateBack = { navController.popBackStack() },
        onStartSession = { progId, sessId ->
            navController.navigate(Screen.RunningPlayer.createRoute(progId, sessId))
        }
    )
}

// Running Player
composable(
    route = Screen.RunningPlayer.route,
    arguments = listOf(
        navArgument("programId") { type = NavType.StringType },
        navArgument("sessionId") { type = NavType.StringType }
    )
) { entry ->
    val programId = entry.arguments?.getString("programId") ?: return@composable
    val sessionId = entry.arguments?.getString("sessionId") ?: return@composable
    val vm: RunningPlayerViewModel = viewModel(key = "running-player-$sessionId", factory = viewModelFactory {
        initializer {
            RunningPlayerViewModel(
                programId = programId,
                sessionId = sessionId,
                getProgramUseCase = appInstance.getRunningProgramUseCase,
                completeSessionUseCase = appInstance.completeRunningSessionUseCase,
                observeRunningSession = appInstance.observeRunningSessionUseCase,
                ttsService = appInstance.ttsService,
                tickSoundPlayer = appInstance.tickSoundPlayer
            )
        }
    })
    RunningPlayerScreen(
        viewModel = vm,
        onClose = { navController.popBackStack() }
    )
}
```

---

# 23. GPS Player & Interval Integration

1. `RunningPlayerScreen` integrates with `RunningTrackingService` to track real-time route, distance, pace, and duration.
2. Interval sessions run countdown timers driven by `RunningWorkoutStep.Timed` and `Repeat` with voice cues from `TtsService`.
3. If GPS is temporarily lost, interval countdowns continue without interruption.
4. On session completion, `CompleteRunningSessionUseCase` persists the completion in Room, activates the next session/week, and updates UI state.

---

# 24. Engineering Constraints

- Do not introduce training phases.
- Do not add distance targets (e.g. 5K) — this program is strictly duration-based.
- Do not add extra warm-up/cooldown time where none is specified.
- Week 11 optional Session 4 must never block week completion.
- Week 11's 35-min long-run target is an informational note, not an invented extra workout.
- Do not hard-code 12 weeks directly in Compose; load from `assets/beginner_running_12w.json`.
- Progress must be persisted in Room and survive app restarts.

---

# 25. Implementation Order

1. **Catalog & Assets:**
   - Create `assets/beginner_running_12w.json` with all 12 weeks and structured steps.
   - Create `RunningProgramCatalogParser` and `RunningProgramCatalogSource` in `data/catalog/`.

2. **Domain Layer:**
   - Define models under `domain/models/running/`.
   - Define repository interfaces and use cases in `domain/repositories/` and `domain/usecases/running/`.

3. **Data Layer & Room:**
   - Create Room entities and `RunningProgramDao` in `data/local/`.
   - Register in `AppDatabase.kt`.
   - Implement repositories in `data/repositories/`.

4. **DI & Navigation:**
   - Register singletons and use cases in `ui/App.kt`.
   - Add routes to `ui/navigation/Screen.kt` and `ScreenNavigator.kt`.

5. **Presentation Layer (Plan Detail):**
   - Create `RunningPlanDetailViewModel` and `RunningPlanDetailScreen` in `ui/core/running/plan/`.
   - Implement components: `PlanHeroHeader`, `ProgramGoalCard`, `StartingAbilityCard`, `RunningIntensityGuideCard`, `WeekAccordionCard`, `WorkoutSessionRow`, `WorkoutTimelineVisualizer`, `CoachTipCard`, `MilestoneCard`, `PostProgramCard`, `TrainingRulesSection`, `StickyProgramCTA`.

6. **Presentation Layer (Player Integration):**
   - Create `RunningPlayerScreen` and `RunningPlayerViewModel` in `ui/core/running/player/`.
   - Wire step countdowns with `RunningTrackingService`, `TtsService`, and `TickSoundPlayer`.

7. **Discovery Integration:**
   - Connect the running card in `DiscoveryScreen.kt` to `Screen.RunningPlanDetail`.

---

# 26. Acceptance Criteria

1. Tapping the Running card on Discovery navigates to `Screen.RunningPlanDetail`.
2. Hero displays 12 Weeks, 3–4 Sessions/Wk, Beginner, with Back, Bookmark, and Share buttons.
3. Program Goal and Starting Ability cards are clearly presented.
4. Intensity Guide displays Walk, Easy Run, and Steady Run with corresponding RPE values.
5. All 12 weeks render dynamically via virtualized `LazyColumn` without phase groupings.
6. Structured run/walk workouts render `WorkoutTimelineVisualizer` and execute step countdowns in player.
7. Week 11 Session 4 is optional and does not block program completion.
8. Week 7 and Week 12 duration ranges are preserved.
9. Repeat Week feature allows restarting a week without wiping GPS history.
10. Program enrollment and session completions persist reliably in Room database across app restarts.