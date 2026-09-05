# UI Design & Implementation Specification
## 20-Week Walking for Weight Loss

**Feature:** Walking Training Program  
**Program:** 20-Week Walking for Weight Loss  
**Target Platform:** Android  
**UI Framework:** Jetpack Compose  
**Architecture:** Clean Architecture + MVVM (Consistent with HomeWorkout project)  
**Primary Accent:** `#0057FF` (`BrandBlue` / `BrandBlueLight`)  

---

# 1. Objective

Implement a production-ready **Plan Detail & Workout Flow** for the:

**20-Week Walking for Weight Loss**

program.

The program is designed for beginner users who want to:

- build a sustainable walking habit,
- gradually increase weekly activity,
- improve cardiovascular fitness,
- increase walking endurance,
- introduce brisk and power walking progressively,
- support healthy long-term weight management.

The feature starts when the user selects the program card from the existing **Discovery** screen (`ui/core/discovery/DiscoveryScreen.kt`).

Primary navigation flow:

```text
Discovery Screen
   ↓ (tap Walking card)
Screen.WalkingPlanDetail(programId = "walking-weight-loss-20w")
   ↓
WalkingPlanDetailScreen
   ↓
Week Accordion Card
   ↓
Workout Session Row
   ↓
Screen.WalkingPlayer(programId, sessionId)
   ↓
Walking Workout Player / GPS Tracking Screen (RunningTrackingService)
```

The implementation must be:

- data-driven (loaded via domain repository from JSON catalog),
- reusable for future structured training programs,
- fully integrated with Room database for progress persistence,
- compatible with GPS workout execution (`RunningTrackingService`),
- compatible with interval timers and voice/vibration cues,
- strictly aligned with the existing Clean Architecture of the repository.

Do not hard-code individual weeks or sessions directly inside Compose UI.

---

# 2. Entry Point & Navigation Integration

From the existing **Discovery** screen (`DiscoveryScreen.kt` under `ui/core/discovery/`), connect the walking program card:

```text
20 WEEKS PROGRAM
WALKING FOR WEIGHT LOSS
```

When tapped:

```text
DiscoveryScreen
  → onOpenWalkingPlan("walking-weight-loss-20w")
  → navController.navigate(Screen.WalkingPlanDetail.createRoute("walking-weight-loss-20w"))
```

In `ui/navigation/Screen.kt`, define:

```kotlin
object WalkingPlanDetail : Screen("walking_plan_detail/{programId}") {
    fun createRoute(programId: String) = "walking_plan_detail/$programId"
}

object WalkingPlayer : Screen("walking_player/{programId}/{sessionId}") {
    fun createRoute(programId: String, sessionId: String) = "walking_player/$programId/$sessionId"
}
```

The Plan Detail screen loads the program through its domain use case (`GetWalkingProgramUseCase`) rather than defining the 20 weeks statically in UI.

---

# 3. Target Screen

## WalkingPlanDetailScreen

Primary responsibilities:

- introduce the program (Hero header, metadata, cover image),
- explain intensity levels (Easy, Brisk, Power, Recovery),
- display the 5 training phases,
- render all 20 weeks via virtualized `LazyColumn`,
- display workout sessions with their type, duration, and completion status,
- show user progress (current week, completed sessions),
- allow previewing upcoming weeks,
- allow starting/continuing the current workout session,
- provide access to completed sessions for review,
- show post-program maintenance guidance after Week 20.

Reference logical viewport:

```text
Approximately 390 × 844 dp
Mobile Portrait
```

The implementation must adapt responsively to all standard Android screen sizes.

---

# 4. Design System & Theme Integration

The UI utilizes the project's design system (`ui/theme/`), adopting dark surface cards with the brand blue accent:

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
| `rpeEasy` | `#10B981` | Easy walking |
| `rpeBrisk` | `#0057FF` | Brisk walking |
| `rpePower` | `#F59E0B` | Power walking |
| `rpeRecovery` | `#6B7280` | Recovery walking |
| `milestoneGold` | `#FBBF24` | Milestones |
| `textPrimary` | `#FFFFFF` | Primary text |
| `textSecondary` | `#8E929A` | Secondary text |
| `textTertiary` | `#6F737C` | Low-priority information |
| `borderSubtle` | `#242426` | Borders and dividers |

---

# 5. Typography

Use the project's existing typography (`ui/theme/Type.kt`).

| Role | Size | Weight | Color |
|---|---:|---:|---|
| Hero Title | 26–28 sp | 800 | `#FFFFFF` |
| Section Title | 20–22 sp | 700 | `#FFFFFF` |
| Phase Title | 20 sp | 700 | `#FFFFFF` |
| Week Title | 16–17 sp | 600 | `#FFFFFF` |
| Session Title | 14–15 sp | 500 | `#E5E7EB` |
| Badge | 11–12 sp | 700 | Accent |
| Metadata | 13–14 sp | 400 | `#8E929A` |
| Coach Tip | 12–13 sp | 400 | `#D1D5DB` |
| CTA | 16 sp | 700 | `#FFFFFF` |

Use Android `sp` units for all text dimensions.

---

# 6. Spacing & Geometry

Use Android `dp` units.

## Radius

```text
heroRadius        = 0.dp
largeCardRadius   = 20.dp
weekCardRadius    = 18.dp
subItemRadius     = 12.dp
pillRadius        = 999.dp
```

## Spacing

```text
pageHorizontalPadding = 16.dp
sectionGap             = 24.dp
cardGap                = 12.dp
cardInnerPadding       = 16.dp
sessionGap             = 8.dp
```

## Controls

```text
Hero height            ≈ 320–360.dp
Session row min height = 46.dp
Primary CTA height     = 52.dp
Floating icon button   = 40 × 40.dp
```

---

# 7. Screen Architecture

Use a single virtualized vertical list via Jetpack Compose.

Recommended Compose structure:

```text
Scaffold
├── TopAppBar / FloatingNavigation
├── LazyColumn
│   ├── HeroHeader
│   ├── ProgramGoalCard
│   ├── IntensityGuideCard
│   ├── PhaseHeader #1
│   ├── WeekAccordionCard #1
│   ├── WeekAccordionCard #2
│   ├── ...
│   ├── PhaseHeader #5
│   ├── WeekAccordionCard #20
│   └── MaintenanceCard
│
└── BottomBar
    └── StickyProgramCTA
```

- Use `LazyColumn` with stable keys (`key = { ... }`) and `contentType`.
- Do not put the entire screen inside an ordinary `Column.verticalScroll()`.
- Do not nest vertically scrolling lists.

---

# 8. Safe Areas & Window Insets

Respect Android system insets:

- Top navigation must account for `WindowInsets.statusBars` or `WindowInsets.safeDrawing`.
- Sticky CTA in `bottomBar` must respect `WindowInsets.navigationBars` to avoid overlapping Android gesture/3-button navigation areas.

---

# 9. Hero Header

## 9.1 Content

Hero contains:

- outdoor walking cover image (`R.drawable.discovery_walking_plan`),
- dark vertical gradient for text legibility,
- Back button (pops back stack to Discovery),
- Bookmark button,
- Share button,
- program badge: `[20 WEEKS PROGRAM]`,
- title: `WALKING FOR WEIGHT LOSS`,
- metadata chips: `20 Weeks • 4–6 Days/Wk • Beginner Friendly`.

Use `Beginner Friendly` instead of simply `Beginner` because later weeks contain substantial training volume despite the program being beginner-accessible at entry.

---

# 10. Hero Gradient

```text
Top:    rgba(0, 0, 0, 0.25)
Middle: rgba(0, 0, 0, 0.55)
Bottom: #000000
```

The gradient ensures high contrast and readability independent of the background cover image.

---

# 11. Floating Navigation Bar

- **Before scrolling (Hero visible):** Transparent / image overlay with circular icon buttons.
- **After scrolling past Hero:** Translucent dark surface (`rgba(0, 0, 0, 0.88)` / `DarkSurface`) with the title `Walking for Weight Loss` smoothly fading in.

---

# 12. Program Goal Card

Display a concise, motivational introduction:

> Build a sustainable walking habit, gradually increase activity, improve cardiovascular fitness, and support healthy long-term weight management.

The copy must avoid promising a guaranteed amount of weight loss, framing results around consistency, active habits, and metabolic health.

---

# 13. Intensity Guide

Reusable `IntensityGuideCard` explaining the 4 walking intensity zones:

### Easy Walk
- **RPE:** 2–3 / 10
- **Description:** Comfortable pace. Able to hold a normal conversation.
- **Color:** `#10B981` (Green)

### Brisk Walk
- **RPE:** 4–5 / 10
- **Description:** Purposeful walking pace. Breathing is deeper, but can speak full sentences.
- **Color:** `#0057FF` (Brand Blue)

### Power Walk
- **RPE:** 6–7 / 10
- **Description:** Fast walking pace with active arm drive. Conversation becomes difficult.
- **Color:** `#F59E0B` (Amber)

### Recovery Walk
- **RPE:** 1–2 / 10
- **Description:** Very relaxed walking for recovery and active rest.
- **Color:** `#6B7280` (Slate Gray)

---

# 14. Global Workout Duration Rule

For steady-state workouts (`EASY`, `BRISK`, `RECOVERY`, `LONG_WALK`), the displayed duration represents the **entire workout duration**.
Warm-up and cooldown occur within this duration.

For structured interval workouts (`INTERVAL`), warm-up, repetitions, and cool-down steps are explicitly structured in `steps` and their sum equals the total workout duration.

---

# 15. Program Structure & Hierarchy

```text
TrainingProgram
    ↓
TrainingPhase (5 Phases)
    ↓
TrainingWeek (20 Weeks)
    ↓
WorkoutSession (4–6 Sessions / Week)
    ↓
WorkoutStep (Structured Interval Steps)
```

---

# 16. Five Training Phases

1. **Phase 1 — Build the Habit (Weeks 1–4):** Build consistency and basic walking endurance (90–150 min/wk).
2. **Phase 2 — Increase Activity (Weeks 5–8):** Increase brisk walking volume and introduce controlled walking intervals (170–210 min/wk).
3. **Phase 3 — Build Fitness (Weeks 9–12):** Increase cardiovascular fitness, weekly activity, and endurance (215–275 min/wk).
4. **Phase 4 — Higher-Volume Fitness (Weeks 13–16):** Increase total activity and power intervals to support fitness goals (285–315 min/wk).
5. **Phase 5 — Sustainable Routine (Weeks 17–20):** Peak walking volume and transition into a long-term sustainable routine (320–340 min/wk, tapering to 250–320 min/wk).

---

# 17. Week Accordion Card

Reusable `WeekAccordionCard`:

- **Geometry:** Background `#18191D`, radius `18.dp`, padding `16.dp`, border `1.dp #242426`.
- **Header:** Week number, title, weekly total duration (e.g. `185 min`), session count (e.g. `5 sessions`).
- **Expansion State:**
  - `CURRENT`: Expanded by default, highlighted with `#0057FF` border.
  - `COMPLETED`: Collapsed by default, completed badge shown.
  - `UPCOMING`: Collapsed by default, upcoming tag shown, previewable.

---

# 18. Week & Session States

## Week States
```kotlin
enum class WeekState {
    CURRENT,
    COMPLETED,
    UPCOMING
}
```

## Session States
```kotlin
enum class SessionState {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}
```

## Program States
```kotlin
enum class ProgramEnrollmentStatus {
    NOT_ENROLLED,
    ACTIVE,
    COMPLETED
}
```

---

# 19. Week Completion Rule

A week is marked `COMPLETED` when all **required** sessions are `COMPLETED`.
Optional sessions (e.g., Week 20 Session 6) do not block progression.

Upon week completion:
1. Current week state becomes `COMPLETED`.
2. Next week state becomes `CURRENT`.
3. First session of the new week is highlighted.

---

# 20. Primary CTA State Machine

The sticky bottom CTA adapts dynamically based on enrollment and workout state:

| Program State | Workout State | CTA Label | CTA Action |
|---|---|---|---|
| `NOT_ENROLLED` | - | `START PROGRAM` | Enrolls user, activates Week 1 Session 1 |
| `ACTIVE` | Idle | `START SESSION X` | Opens player for next incomplete session |
| `ACTIVE` | In Progress | `CONTINUE SESSION` | Resumes active running/walking tracker |
| `ACTIVE` | All 20 Weeks Done | `VIEW PROGRAM SUMMARY` | Opens completion summary |

---

# 21. Workout Session Row

Reusable `WorkoutSessionRow`:

- **Layout:** Session number, session title, duration badge, type indicator.
- **Trailing Action:**
  - `Play` icon button for startable sessions.
  - Circular progress / resume button for in-progress session.
  - Green checkmark for completed sessions.
- **Geometry:** Background `#202227`, radius `12.dp`, padding `12.dp`, minHeight `46.dp`.

---

# 22. Coach Tip & Milestone Cards

- **CoachTipCard:** Background `rgba(0, 87, 255, 0.08)`, left border `3.dp #0057FF`, bulb icon.
- **MilestoneCard:** Gold icon, subtle gold tinted card surface, celebrating milestones (e.g. first 60-min walk, 80-min walk, 90-min walk).

---

# 23. Interval Visualizer

For workouts of type `INTERVAL`, render an `IntervalVisualizer` bar constructed directly from `steps`:

- **Easy segment:** `#10B981`
- **Power segment:** `#F59E0B`
- **Brisk segment:** `#0057FF`
- **Recovery segment:** `#6B7280`

The visualizer shows the proportional breakdown: warm-up → repeated sets → cool-down.

---

# 24. Interval Progression Specification

- **Weeks 6–8 (35 min):** 5m Easy + 5 × (2m Power + 3m Easy) + 5m Easy
- **Week 9 (40 min):** 5m Easy + 5 × (3m Power + 3m Easy) + 5m Easy
- **Weeks 10–11 (40 min):** 5m Easy + 6 × (3m Power + 2m Easy) + 5m Easy
- **Weeks 12–13 (45 min):** 5m Easy + 5 × (4m Power + 3m Easy) + 5m Easy
- **Weeks 14–17 (50 min):** 5m Easy + 5 × (5m Power + 3m Easy) + 5m Easy
- **Week 18 (52 min):** 5m Easy + 6 × (5m Power + 2m Easy) + 5m Easy
- **Week 19 (55 min):** 5m Easy + 5 × (6m Power + 3m Easy) + 5m Easy
- **Week 20 (50 min):** 5m Easy + 5 × (5m Power + 3m Easy) + 5m Easy

---

# 25. Complete 20-Week Program Data Structure

### Phase 1 — Build the Habit
- **Week 1 (90 min, 4 sessions):** 4 × Easy Walk (20m, 20m, 25m, 25m).
- **Week 2 (110 min, 4 sessions):** 4 × Easy Walk (25m, 25m, 30m, 30m).
- **Week 3 (115 min, 4 sessions):** 30m Easy, 20m Intro Brisk (5m Easy + 10m Brisk + 5m Easy), 30m Easy, 35m Long Easy.
- **Week 4 (150 min, 5 sessions):** 30m Easy, 25m Brisk, 30m Easy, 25m Brisk, 40m Long Walk.

### Phase 2 — Increase Activity
- **Week 5 (170 min, 5 sessions):** 30m Easy, 30m Brisk, 35m Easy, 30m Brisk, 45m Long Walk.
- **Week 6 (185 min, 5 sessions):** 35m Intervals (5 × [2m Power + 3m Easy]), 35m Easy, 35m Brisk, 30m Easy, 50m Long Walk.
- **Week 7 (195 min, 5 sessions):** 35m Easy, 35m Brisk, 35m Easy, 35m Intervals (5 × [2m Power + 3m Easy]), 55m Long Walk.
- **Week 8 (210 min, 5 sessions):** 40m Easy, 35m Brisk, 40m Easy, 35m Intervals (5 × [2m Power + 3m Easy]), 60m Long Walk.

### Phase 3 — Build Fitness
- **Week 9 (215 min, 5 sessions):** 40m Easy, 40m Brisk, 35m Recovery, 40m Intervals (5 × [3m Power + 3m Easy]), 60m Long Walk.
- **Week 10 (220 min, 5 sessions):** 40m Intervals (6 × [3m Power + 2m Easy]), 40m Easy, 40m Brisk, 35m Recovery, 65m Long Walk.
- **Week 11 (260 min, 6 sessions):** 40m Easy, 45m Brisk, 30m Recovery, 40m Intervals (6 × [3m Power + 2m Easy]), 40m Brisk, 65m Long Walk.
- **Week 12 (275 min, 6 sessions):** 40m Easy, 45m Brisk, 35m Recovery, 45m Intervals (5 × [4m Power + 3m Easy]), 40m Brisk, 70m Long Walk.

### Phase 4 — Higher-Volume Fitness
- **Week 13 (285 min, 6 sessions):** 45m Easy, 45m Brisk, 35m Recovery, 45m Intervals (5 × [4m Power + 3m Easy]), 45m Brisk, 70m Long Walk.
- **Week 14 (295 min, 6 sessions):** 50m Intervals (5 × [5m Power + 3m Easy]), 45m Easy, 45m Brisk, 35m Recovery, 45m Brisk, 75m Long Walk.
- **Week 15 (300 min, 6 sessions):** 45m Easy, 50m Brisk, 35m Recovery, 50m Intervals (5 × [5m Power + 3m Easy]), 45m Brisk, 75m Long Walk.
- **Week 16 (315 min, 6 sessions):** 45m Easy, 50m Brisk, 40m Recovery, 50m Intervals (5 × [5m Power + 3m Easy]), 50m Brisk, 80m Long Walk.

### Phase 5 — Sustainable Routine
- **Week 17 (320 min, 6 sessions):** 45m Easy, 50m Brisk, 40m Recovery, 50m Intervals (5 × [5m Power + 3m Easy]), 50m Brisk, 85m Long Walk.
- **Week 18 (322 min, 6 sessions):** 52m Intervals (6 × [5m Power + 2m Easy]), 45m Easy, 50m Brisk, 40m Recovery, 50m Brisk, 85m Long Walk.
- **Week 19 (340 min, 6 sessions):** 50m Easy, 55m Brisk, 40m Recovery, 55m Intervals (5 × [6m Power + 3m Easy]), 50m Brisk, 90m Long Walk.
- **Week 20 (250–320 min, 5 req + 1 opt):** 45m Easy, 50m Brisk, 45m Recovery, 50m Intervals (5 × [5m Power + 3m Easy]), 60–90m Long Walk, + Optional 40m Brisk.

### After Week 20 (MaintenanceCard)
- 2 Brisk Walks + 1 Interval Walk + 2 Easy Walks + 1 Long Walk + 1 Rest Day (250–320 min/week).

---

# 26. Repo Architecture & Codebase Alignment

The walking feature follows the exact multi-tier Clean Architecture and manual Dependency Injection conventions of the HomeWorkout project:

```text
app/src/main/java/com/example/homeworkout/
│
├── data/
│   ├── catalog/
│   │   ├── WalkingProgramCatalogParser.kt       # JSON parser for training programs
│   │   └── WalkingProgramCatalogSource.kt       # Reads assets/walking_weight_loss_20w.json
│   │
│   ├── local/
│   │   ├── AppDatabase.kt                       # Room DB instance (version bump & entities)
│   │   ├── dao/
│   │   │   └── WalkingProgramDao.kt             # Progress queries & mutations
│   │   └── entities/
│   │       ├── WalkingProgramProgressEntity.kt  # User enrollment & current week
│   │       └── WalkingSessionProgressEntity.kt  # Session status & completion timestamps
│   │
│   └── repositories/
│       ├── WalkingProgramRepositoryImpl.kt      # Catalog & static program loader
│       └── WalkingProgressRepositoryImpl.kt     # Room-backed progress repository
│
├── domain/
│   ├── models/
│   │   └── walking/                             # Domain models
│   │       ├── TrainingProgram.kt
│   │       ├── TrainingPhase.kt
│   │       ├── TrainingWeek.kt
│   │       ├── WorkoutSession.kt
│   │       ├── WorkoutStep.kt
│   │       ├── WalkingIntensity.kt
│   │       ├── ProgramProgress.kt
│   │       └── SessionProgress.kt
│   │
│   ├── repositories/
│   │   ├── WalkingProgramRepository.kt          # Program catalog interface
│   │   └── WalkingProgressRepository.kt         # Progress tracking interface
│   │
│   └── usecases/
│       └── walking/
│           ├── GetWalkingProgramUseCase.kt      # Retrieves program hierarchy
│           ├── GetWalkingProgressUseCase.kt     # Observes user progress StateFlow/Flow
│           ├── EnrollWalkingProgramUseCase.kt   # Starts/enrolls program
│           ├── CompleteWalkingSessionUseCase.kt # Marks session complete & evaluates week
│           └── StartWalkingSessionUseCase.kt    # Prepares session for player
│
├── running/
│   └── service/
│       └── RunningTrackingService.kt            # Reusable foreground GPS tracking service
│
└── ui/
    ├── App.kt                                   # Process-level Manual DI container
    │
    ├── navigation/
    │   ├── Screen.kt                            # Screen.WalkingPlanDetail & Screen.WalkingPlayer
    │   └── ScreenNavigator.kt                   # NavHost route mapping & ViewModel factories
    │
    └── core/
        └── walking/
            ├── plan/
            │   ├── WalkingPlanDetailScreen.kt   # Main 20-week Compose view
            │   ├── WalkingPlanDetailViewModel.kt
            │   └── components/
            │       ├── PlanHeroHeader.kt
            │       ├── ProgramGoalCard.kt
            │       ├── IntensityGuideCard.kt
            │       ├── PhaseHeader.kt
            │       ├── WeekAccordionCard.kt
            │       ├── WorkoutSessionRow.kt
            │       ├── IntervalVisualizer.kt
            │       ├── CoachTipCard.kt
            │       ├── MilestoneCard.kt
            │       ├── MaintenanceCard.kt
            │       └── StickyProgramCTA.kt
            │
            └── player/
                ├── WalkingPlayerScreen.kt       # GPS & Interval execution screen
                └── WalkingPlayerViewModel.kt    # Interval cues + telemetry + completion
```

---

# 27. Domain Models

Package: `com.example.homeworkout.domain.models.walking`

```kotlin
package com.example.homeworkout.domain.models.walking

data class TrainingProgram(
    val id: String,
    val title: String,
    val subtitle: String?,
    val durationWeeks: Int,
    val level: String,
    val goal: String,
    val phases: List<TrainingPhase>,
    val intensityGuide: List<IntensityDefinition>,
    val maintenanceTip: String?
)

data class TrainingPhase(
    val id: String,
    val index: Int,
    val title: String,
    val weekStart: Int,
    val weekEnd: Int,
    val goal: String,
    val weeks: List<TrainingWeek>
)

data class TrainingWeek(
    val id: String,
    val weekNumber: Int,
    val title: String,
    val targetMinutes: Int?,
    val targetMinutesRange: IntRange?,
    val sessions: List<WorkoutSession>,
    val coachTip: String?,
    val milestone: String?
)

data class WorkoutSession(
    val id: String,
    val sessionIndex: Int,
    val title: String,
    val type: WorkoutType,
    val durationMinutes: Int?,
    val durationRangeMinutes: IntRange?,
    val isOptional: Boolean = false,
    val steps: List<WorkoutStep> = emptyList()
)

enum class WorkoutType {
    EASY,
    BRISK,
    POWER,
    RECOVERY,
    LONG_WALK,
    INTERVAL
}

enum class WalkingIntensity {
    EASY,
    BRISK,
    POWER,
    RECOVERY
}

sealed interface WorkoutStep {
    data class Timed(
        val intensity: WalkingIntensity,
        val durationSeconds: Int
    ) : WorkoutStep

    data class Repeat(
        val count: Int,
        val steps: List<WorkoutStep>
    ) : WorkoutStep
}

data class IntensityDefinition(
    val intensity: WalkingIntensity,
    val name: String,
    val rpe: String,
    val description: String,
    val colorHex: String
)
```

---

# 28. Progress Domain & Room Persistence Models

Domain Models:
```kotlin
package com.example.homeworkout.domain.models.walking

data class ProgramProgress(
    val programId: String,
    val status: ProgramEnrollmentStatus,
    val currentWeekNumber: Int,
    val completedSessions: Set<String>,
    val activeSessionId: String? = null
)

enum class ProgramEnrollmentStatus {
    NOT_ENROLLED,
    ACTIVE,
    COMPLETED
}
```

Room Entities (`com.example.homeworkout.data.local.entities`):
```kotlin
package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "walking_program_progress")
data class WalkingProgramProgressEntity(
    @PrimaryKey val programId: String,
    val status: String,
    val currentWeekNumber: Int,
    val activeSessionId: String?,
    val enrolledAt: Long,
    val completedAt: Long?
)

@Entity(tableName = "walking_session_progress", primaryKeys = ["programId", "sessionId"])
data class WalkingSessionProgressEntity(
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

# 29. Repositories & Use Cases

### Repository Interfaces (`domain/repositories/`)
```kotlin
package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.walking.ProgramProgress
import com.example.homeworkout.domain.models.walking.TrainingProgram
import kotlinx.coroutines.flow.Flow

interface WalkingProgramRepository {
    suspend fun getProgram(programId: String): TrainingProgram?
}

interface WalkingProgressRepository {
    fun observeProgress(programId: String): Flow<ProgramProgress>
    suspend fun enroll(programId: String)
    suspend fun markSessionCompleted(programId: String, sessionId: String, weekNumber: Int)
    suspend fun setActiveSession(programId: String, sessionId: String?)
}
```

### Use Cases (`domain/usecases/walking/`)
- `GetWalkingProgramUseCase(private val repository: WalkingProgramRepository)`
- `GetWalkingProgressUseCase(private val repository: WalkingProgressRepository)`
- `EnrollWalkingProgramUseCase(private val repository: WalkingProgressRepository)`
- `CompleteWalkingSessionUseCase(private val progressRepo: WalkingProgressRepository, private val programRepo: WalkingProgramRepository)`
- `StartWalkingSessionUseCase(private val progressRepo: WalkingProgressRepository)`

---

# 30. Manual Dependency Injection (`ui/App.kt`)

In `App.kt`, register lazy singletons following the existing codebase pattern:

```kotlin
// In App.kt
val walkingProgramRepository: WalkingProgramRepository by lazy {
    WalkingProgramCatalogSource(this)
}

val walkingProgressRepository: WalkingProgressRepository by lazy {
    WalkingProgressRepositoryImpl(database.walkingProgramDao())
}

val getWalkingProgramUseCase by lazy { GetWalkingProgramUseCase(walkingProgramRepository) }
val getWalkingProgressUseCase by lazy { GetWalkingProgressUseCase(walkingProgressRepository) }
val enrollWalkingProgramUseCase by lazy { EnrollWalkingProgramUseCase(walkingProgressRepository) }
val completeWalkingSessionUseCase by lazy {
    CompleteWalkingSessionUseCase(walkingProgressRepository, walkingProgramRepository)
}
val startWalkingSessionUseCase by lazy { StartWalkingSessionUseCase(walkingProgressRepository) }
```

---

# 31. Navigation Wiring (`ui/navigation/ScreenNavigator.kt`)

In `ScreenNavigator.kt`, wire routes via `viewModelFactory`:

```kotlin
// Plan Detail Screen
composable(
    route = Screen.WalkingPlanDetail.route,
    arguments = listOf(navArgument("programId") { type = NavType.StringType })
) { entry ->
    val programId = entry.arguments?.getString("programId") ?: "walking-weight-loss-20w"
    val vm: WalkingPlanDetailViewModel = viewModel(key = "walking-plan-$programId", factory = viewModelFactory {
        initializer {
            WalkingPlanDetailViewModel(
                programId = programId,
                getProgramUseCase = appInstance.getWalkingProgramUseCase,
                getProgressUseCase = appInstance.getWalkingProgressUseCase,
                enrollUseCase = appInstance.enrollWalkingProgramUseCase,
                startSessionUseCase = appInstance.startWalkingSessionUseCase
            )
        }
    })
    WalkingPlanDetailScreen(
        viewModel = vm,
        onNavigateBack = { navController.popBackStack() },
        onStartSession = { progId, sessId ->
            navController.navigate(Screen.WalkingPlayer.createRoute(progId, sessId))
        }
    )
}

// Walking Player / Interval & GPS Screen
composable(
    route = Screen.WalkingPlayer.route,
    arguments = listOf(
        navArgument("programId") { type = NavType.StringType },
        navArgument("sessionId") { type = NavType.StringType }
    )
) { entry ->
    val programId = entry.arguments?.getString("programId") ?: return@composable
    val sessionId = entry.arguments?.getString("sessionId") ?: return@composable
    val vm: WalkingPlayerViewModel = viewModel(key = "walking-player-$sessionId", factory = viewModelFactory {
        initializer {
            WalkingPlayerViewModel(
                programId = programId,
                sessionId = sessionId,
                getProgramUseCase = appInstance.getWalkingProgramUseCase,
                completeSessionUseCase = appInstance.completeWalkingSessionUseCase,
                observeRunningSession = appInstance.observeRunningSessionUseCase,
                ttsService = appInstance.ttsService,
                tickSoundPlayer = appInstance.tickSoundPlayer
            )
        }
    })
    WalkingPlayerScreen(
        viewModel = vm,
        onClose = { navController.popBackStack() }
    )
}
```

---

# 32. GPS Player & Interval Integration

When launching a walking session:
1. `WalkingPlayerScreen` communicates with `RunningTrackingService` to record real-time GPS, elapsed time, distance, and pace.
2. If the workout is an `INTERVAL` session:
   - The player runs a countdown timer driven by `WorkoutStep.Timed` and `WorkoutStep.Repeat`.
   - On transition between zones (e.g. Easy → Power), `TtsService` and audio cues notify the user.
   - Live telemetry and interval progress display simultaneously.
3. If GPS is temporarily lost or unavailable:
   - Interval countdown continues without interruption.
   - GPS data recovers automatically upon signal acquisition.
4. On session finish:
   - `RunningTrackingService` finalizes and stores the route points.
   - `CompleteWalkingSessionUseCase` updates Room progress.
   - Navigation returns to `WalkingPlanDetailScreen` showing updated checkmarks.

---

# 33. State Management & Lifecycle

`WalkingPlanDetailViewModel` exposes an immutable `StateFlow<WalkingPlanUiState>` collected with `collectAsStateWithLifecycle()`:

```kotlin
data class WalkingPlanUiState(
    val isLoading: Boolean = true,
    val program: TrainingProgram? = null,
    val progress: ProgramProgress? = null,
    val expandedWeeks: Set<Int> = emptySet(),
    val errorMessage: String? = null
)
```

- **Expansion State:** Handled in ViewModel UI state (default expands `currentWeekNumber`).
- **Data Safety:** Progress state is fully decoupled from static program catalog data.

---

# 34. Engineering Constraints & Guidelines

- Do not hard-code 20 Week cards directly in Compose.
- Do not parse display strings (e.g. `"20 min Easy Walk"`) to extract duration or type; use strongly typed domain models.
- Store static catalog data in `assets/walking_weight_loss_20w.json`.
- Keep user progress strictly in Room database tables.
- Use `LazyColumn` with stable keys.
- Do not add extra warm-up/cooldown minutes to steady-state walks; duration already includes them.
- Week 20 optional Session 6 must never block program completion.
- Always provide accessibility content descriptions for interactive controls.

---

# 35. Implementation Order

1. **Catalog & Assets:**
   - Add `assets/walking_weight_loss_20w.json` containing complete 20-week program definitions.
   - Create `WalkingProgramCatalogParser` and `WalkingProgramCatalogSource` in `data/catalog/`.

2. **Domain Models & Interfaces:**
   - Create models under `domain/models/walking/`.
   - Define `WalkingProgramRepository` and `WalkingProgressRepository` in `domain/repositories/`.
   - Create use cases under `domain/usecases/walking/`.

3. **Data Layer & Room:**
   - Add `WalkingProgramProgressEntity` and `WalkingSessionProgressEntity` to `data/local/entities/`.
   - Add `WalkingProgramDao` to `data/local/dao/`.
   - Register entities and DAO in `AppDatabase.kt`.
   - Implement `WalkingProgramRepositoryImpl` and `WalkingProgressRepositoryImpl` in `data/repositories/`.

4. **DI & Navigation:**
   - Register repository singletons and use cases in `ui/App.kt`.
   - Add `Screen.WalkingPlanDetail` and `Screen.WalkingPlayer` to `ui/navigation/Screen.kt`.
   - Add route composables to `ui/navigation/ScreenNavigator.kt`.


5. **Presentation Layer (Plan Detail):**
   - Create `WalkingPlanDetailViewModel` and `WalkingPlanDetailScreen` in `ui/core/walking/plan/`.
   - Build UI components: `PlanHeroHeader`, `ProgramGoalCard`, `IntensityGuideCard`, `PhaseHeader`, `WeekAccordionCard`, `WorkoutSessionRow`, `IntervalVisualizer`, `CoachTipCard`, `MilestoneCard`, `MaintenanceCard`, `StickyProgramCTA`.

6. **Presentation Layer (Player Integration):**
   - Create `WalkingPlayerScreen` and `WalkingPlayerViewModel` in `ui/core/walking/player/`.
   - Connect interval step timer with `RunningTrackingService`, `TtsService`, and `TickSoundPlayer`.

7. **Discovery Integration:**
   - Update `DiscoveryScreen.kt` to trigger navigation to `Screen.WalkingPlanDetail` when the walking plan card is clicked.

---

# 36. Acceptance Criteria

1. Tapping the Walking card on the Discovery screen navigates to `Screen.WalkingPlanDetail`.
2. Hero header displays 20 Weeks, 4–6 Sessions/Wk, Beginner Friendly, with Back, Bookmark, and Share buttons.
3. Intensity Guide renders Easy, Brisk, Power, and Recovery zones.
4. All 5 phases and 20 weeks are rendered dynamically via `LazyColumn`.
5. Every interval workout parses structured executable steps and renders the `IntervalVisualizer`.
6. Week accordion correctly expands `CURRENT` week and collapses completed/upcoming weeks.
7. Sticky CTA adapts across `START PROGRAM`, `START SESSION X`, `CONTINUE SESSION`, and `VIEW PROGRAM SUMMARY`.
8. Program enrollment and session completions persist reliably in Room database across app restarts.
9. Week 20 optional Session 6 does not block program completion.
10. Walking Player executes interval countdown cues alongside live GPS tracking via `RunningTrackingService`.