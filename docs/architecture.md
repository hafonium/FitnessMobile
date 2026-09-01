# Clean Architecture Guidelines

## Architectural Rules & Boundaries

### 1. Data Layer (`data/`)
- `data/local/entities/` holds Room `@Entity` classes — one table row per file, fields only.
- `data/local/dao/` holds Room `@Dao` interfaces — query/insert/update/delete method signatures only.
- `data/local/AppDatabase.kt` is the single `RoomDatabase` — register every new entity/DAO here.
- `data/repositories/` is where you implement the domain repository contracts and map
  Entity <-> domain Model. This is the ONLY place that layer boundary crossing happens.
- There is no `data/remote/` layer in this project — everything is local (Room).

### 2. Domain Layer (`domain/`)
- Pure Kotlin only (no Android SDK, Room, or Compose imports).
- Create Domain Models in `domain/models/` — plain data classes, no Room annotations.
- Create Use Cases in `domain/usecases/[feature]/`. Every Use Case must have a single
  responsibility and expose it via `operator fun invoke(...)`.

### 3. UI Layer (`ui/`)
- **ZERO ENTITY PERMISSION:** Never import or reference classes ending in `Entity` or inside
  `data/local/`. Use Domain Models only.
- You can use or add to the shared components in `ui/components/` — keep reusable widgets
  (buttons, cards, text fields, dialogs) there instead of duplicating them per feature.
- ViewModels must call **Domain Use Cases**, never Repositories directly, and only
  ViewModels can call **Domain Use Cases**.
- No Room/query logic in the UI layer. Expose state via `StateFlow`.
- Add navigational logic in `ui/navigation/`.

---

## Example Reference Files

These files are intentionally left as empty skeletons (`TODO()` bodies / signatures only) so
you can see the exact shape of each layer before filling in the real logic:

- **Room Entity + DAO Pattern:**
  - `app/src/main/java/com/example/homeworkout/data/local/entities/WorkoutEntity.kt`
  - `app/src/main/java/com/example/homeworkout/data/local/dao/WorkoutDao.kt`

- **Repository Implementation Pattern:**
  - `app/src/main/java/com/example/homeworkout/data/repositories/WorkoutRepositoryImpl.kt`

- **Domain Use Case Pattern:**
  - `app/src/main/java/com/example/homeworkout/domain/usecases/home/GetWorkoutsUseCase.kt`
  - `app/src/main/java/com/example/homeworkout/domain/usecases/details/GetWorkoutDetailsUseCase.kt`

- **UI & ViewModel Pattern:**
  - `app/src/main/java/com/example/homeworkout/ui/core/home/`
  - `app/src/main/java/com/example/homeworkout/ui/core/details/`

- **Shared reusable components:**
  - `app/src/main/java/com/example/homeworkout/ui/components/`

- **DI (manual, no Hilt/Koin):**
  - `app/src/main/java/com/example/homeworkout/ui/App.kt` builds the Room database,
    repositories, and use cases as `by lazy` properties, injected into ViewModels via
    `viewModelFactory { initializer { ... } }` at each `composable()` call site in
    `ui/navigation/ScreenNavigator.kt`.
