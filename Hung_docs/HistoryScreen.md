# HistoryScreen — Database Integration and Migration Plan

## 1. Mục tiêu

Nâng cấp `HistoryScreen` từ mock UI thành màn hình lịch sử workout đọc dữ liệu Room thực tế, đồng thời bảo đảm lịch sử đã hoàn thành không thay đổi khi người dùng chỉnh sửa plan.

Luồng kiến trúc bắt buộc:

```text
HistoryScreen
    -> HistoryViewModel
    -> GetWorkoutHistoryUseCase
    -> WorkoutSessionRepository
    -> WorkoutSessionDao
    -> Room
```

Kết quả cần đạt:

- Xóa `sampleWeek` và Weekly Summary hard-code.
- Chỉ hiển thị session `COMPLETED` có `endedAt`.
- Calendar đánh dấu ngày có workout và hỗ trợ chọn ngày/chuyển tháng.
- Danh sách và summary của tuần được chọn đến từ database.
- Lịch sử dùng snapshot tại thời điểm bắt đầu session, không dùng metadata plan hiện tại.
- Có loading, empty và error state.
- Không để UI/domain import Room entity.

## 2. Vấn đề dữ liệu hiện tại

### 2.1. HistoryScreen chưa kết nối database

Hiện màn hình tự tạo calendar bằng `Calendar`, danh sách bằng `sampleWeek`, và chuỗi Weekly Summary cố định. Navigation cũng tạo `HistoryScreen` trực tiếp, không có `HistoryViewModel`.

### 2.2. Join trực tiếp plan sẽ làm sai lịch sử

Nếu History query trực tiếp:

```sql
workout_sessions
JOIN workout_plans
JOIN workout_plan_days
```

thì tên plan, tên day và cover trong lịch sử cũ sẽ đổi theo mỗi lần user edit plan. Đây không phải hành vi phù hợp cho dữ liệu lịch sử.

### 2.3. Exercise snapshot đã có schema nhưng chưa được ghi

Table `workout_session_exercises` đã có:

- `exerciseTitleSnapshot`
- `plannedReps`
- `plannedDurationSec`
- `actualReps`
- `actualDurationSec`

Tuy nhiên, code hiện chỉ khai báo `insertSessionExercises()` mà chưa gọi method này. Vì vậy session mới hiện chưa thực sự lưu exercise snapshot.

### 2.4. Xóa custom plan có thể vi phạm foreign key

`workout_sessions.planId` và `planDayId` dùng `ON DELETE RESTRICT`. Nếu custom plan đã có session, `deletePlan()` có thể bị SQLite từ chối. Luồng delete cần chuyển sang hard-delete khi an toàn và archive khi đã có lịch sử.

## 3. Quyết định thiết kế

### 3.1. Lịch sử là immutable snapshot

Khi tạo session, lưu snapshot của:

- Tên plan.
- Cover URL của plan.
- Day number.
- Tên day.
- Danh sách exercise và target tại thời điểm bắt đầu.
- Settings đang áp dụng (đã có sẵn).

Sau đó History đọc snapshot từ session, không phụ thuộc vào plan/day hiện tại.

### 3.2. Phải thay đổi schema

Schema hiện không có nơi lưu plan/day metadata snapshot. Để bảo toàn lịch sử chính xác, cần nâng database từ version `5` lên `6` và thêm bốn nullable columns vào `workout_sessions`:

```text
planTitleSnapshot       TEXT
planCoverImageSnapshot  TEXT
planDayNumberSnapshot   INTEGER
planDayTitleSnapshot    TEXT
```

Các column nullable để migration tương thích dữ liệu cũ. Session mới phải luôn ghi `planTitleSnapshot` và `planDayNumberSnapshot`; cover/day title được phép null theo dữ liệu nguồn.

Không tạo table mới và không thay đổi foreign key hiện có.

### 3.3. Quy tắc session hiển thị

Chỉ hiển thị:

```text
status = COMPLETED
endedAt IS NOT NULL
```

Không đưa `IN_PROGRESS` hoặc `ABANDONED` vào lịch sử hoàn thành.

### 3.4. Quy tắc calendar

- Mặc định chọn hôm nay.
- Danh sách hiển thị session trong tuần chứa ngày được chọn.
- Weekly Summary dùng cùng khoảng tuần.
- Khi chuyển tháng, query khoảng phủ toàn bộ lưới calendar.
- Dùng `Calendar` để tạo start/end theo timezone thiết bị; không cộng/trừ cố định `24 giờ` vì rủi ro DST.

## 4. Database migration 5 -> 6

### 4.1. Cập nhật entity

Thêm vào `WorkoutSessionEntity` với default `null` để hạn chế ảnh hưởng constructor hiện có:

```kotlin
val planTitleSnapshot: String? = null,
val planCoverImageSnapshot: String? = null,
val planDayNumberSnapshot: Int? = null,
val planDayTitleSnapshot: String? = null
```

Không đặt `NOT NULL` ở version này vì các database đã tồn tại chưa có snapshot lịch sử.

### 4.2. Migration SQL

Thêm `MIGRATION_5_6` trong `AppDatabase.kt`:

```sql
ALTER TABLE workout_sessions ADD COLUMN planTitleSnapshot TEXT;
ALTER TABLE workout_sessions ADD COLUMN planCoverImageSnapshot TEXT;
ALTER TABLE workout_sessions ADD COLUMN planDayNumberSnapshot INTEGER;
ALTER TABLE workout_sessions ADD COLUMN planDayTitleSnapshot TEXT;
```

Sau khi thêm column, backfill best-effort từ dữ liệu plan hiện tại:

```sql
UPDATE workout_sessions
SET
    planTitleSnapshot = (
        SELECT p.title
        FROM workout_plans p
        WHERE p.planId = workout_sessions.planId
    ),
    planCoverImageSnapshot = (
        SELECT p.coverImageUrl
        FROM workout_plans p
        WHERE p.planId = workout_sessions.planId
    ),
    planDayNumberSnapshot = (
        SELECT d.dayNumber
        FROM workout_plan_days d
        WHERE d.planDayId = workout_sessions.planDayId
    ),
    planDayTitleSnapshot = (
        SELECT d.title
        FROM workout_plan_days d
        WHERE d.planDayId = workout_sessions.planDayId
    );
```

Giới hạn cần ghi rõ: dữ liệu cũ chỉ có thể snapshot trạng thái plan tại thời điểm migration; không thể phục hồi tên/cover đã tồn tại trước một lần edit trong quá khứ.

### 4.3. Đăng ký migration

- Đổi `AppDatabase.version` từ `5` thành `6`.
- Đăng ký cả `MIGRATION_4_5` và `MIGRATION_5_6`.
- Database version 4 phải đi theo chuỗi `4 -> 5 -> 6` và giữ dữ liệu.
- Giữ `fallbackToDestructiveMigration` cho các version không được hỗ trợ, nhưng không được dùng destructive migration thay cho đường `5 -> 6`.
- Cập nhật `docs/db_diagram.dbml` với bốn snapshot columns và note về historical snapshot.

### 4.4. Migration tests

Cần test ít nhất:

- Database version 5 có session được nâng lên version 6 mà không mất row.
- Bốn column mới tồn tại và có kiểu/nullability đúng với Room entity.
- Backfill lấy đúng plan/day metadata.
- Giá trị null hợp lệ khi dữ liệu nguồn vốn null.
- Đường migration `4 -> 5 -> 6` vẫn hoạt động.

Nếu project chưa có schema export/migration test infrastructure, cần bật `exportSchema` và cấu hình schema location trước khi dùng `MigrationTestHelper`; không tự giả định migration đúng chỉ vì compile pass.

## 5. Snapshot session khi bắt đầu workout

### 5.1. Tạo projection để đọc nguồn snapshot

Tạo data-layer projection chứa:

```kotlin
data class SessionPlanSnapshotRow(
    val planId: Long,
    val planTitle: String,
    val planCoverImageUrl: String?,
    val planDayId: Long,
    val dayNumber: Int,
    val dayTitle: String?
)
```

Thêm query xác nhận `planDayId` thật sự thuộc `planId`. Không nhận hai row độc lập rồi ghép mà không kiểm tra quan hệ.

### 5.2. Đọc exercise snapshot source

Thêm suspend query lấy toàn bộ exercise của day theo `orderIndex`, gồm:

- `exerciseId`
- `orderIndex`
- `exerciseTitle`
- `targetReps`
- `targetDurationSec`

Không truyền `WorkoutPlanExerciseEntity` hoặc DAO row lên domain/UI.

### 5.3. Cập nhật createSession

`WorkoutSessionRepositoryImpl.createSession(planId, planDayId)` phải thực hiện trong một Room transaction:

1. Resolve local user.
2. Đọc user settings.
3. Đọc plan/day snapshot source.
4. Đọc exercise rows của day.
5. Insert `WorkoutSessionEntity` với settings snapshot và plan/day snapshot.
6. Insert các `WorkoutSessionExerciseEntity` ứng với session vừa tạo.
7. Trả về `sessionId` chỉ khi toàn bộ transaction thành công.

Nếu plan/day không tồn tại, không tạo session mồ côi. Repository phải trả failure rõ ràng hoặc giữ contract nullable; use case chuyển thành empty/error state thích hợp.

Để dùng `database.withTransaction`, constructor của `WorkoutSessionRepositoryImpl` cần nhận `AppDatabase` và DAO cần thiết. Wiring trong `App.kt` phải được cập nhật.

### 5.4. Các luồng phải dùng chung createSession

Kiểm tra cả ba entry point:

- `StartWorkoutSessionUseCase`
- `StartSpecificWorkoutDayUseCase`
- `RestartWorkoutDayUseCase`

Cả ba phải tiếp tục đi qua cùng `createSession()` để không có session nào thiếu snapshot.

## 6. Chính sách edit và delete plan

### 6.1. Edit plan

Sau khi session được tạo:

- Edit plan title/cover không đổi snapshot session cũ.
- Edit day title/number không đổi snapshot session cũ.
- Add/replace/delete/reorder exercise không đổi `workout_session_exercises` của session cũ.
- Session mới lấy phiên bản plan mới nhất.

History Detail trong tương lai phải đọc `workout_session_exercises`, không đọc `workout_plan_exercises` hiện tại.

### 6.2. Delete custom plan

Thêm DAO transaction `deleteOrArchiveCustomPlan(planId)`:

1. Xác nhận plan có source `CUSTOM`.
2. Đếm mọi `workout_sessions` tham chiếu plan, không chỉ completed session.
3. Nếu count bằng 0: hard-delete plan.
4. Nếu count lớn hơn 0: `UPDATE workout_plans SET isActive = 0`.

Việc archive dùng column `isActive` hiện có, không cần migration bổ sung. `observePlanSummaries()` đã lọc `isActive = 1`, vì vậy plan archive biến mất khỏi danh sách nhưng foreign key và lịch sử vẫn còn nguyên.

`DeleteCustomWorkoutPlanUseCase` có thể giữ tên hiện tại để tránh lan rộng API, nhưng repository/DAO phải document rằng “delete” có thể là archive.

## 7. Data layer cho History

### 7.1. WorkoutHistoryRow

Tạo:

```text
data/local/dao/relations/WorkoutHistoryRow.kt
```

```kotlin
data class WorkoutHistoryRow(
    val sessionId: Long,
    val planTitleSnapshot: String?,
    val planCoverImageSnapshot: String?,
    val planDayNumberSnapshot: Int?,
    val planDayTitleSnapshot: String?,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Int?,
    val caloriesBurned: Double?
)
```

History query không join `workout_plans` hoặc `workout_plan_days` đối với session mới. Dữ liệu legacy đã được backfill trong migration; nếu snapshot vẫn null, repository dùng fallback trung tính như `Workout`, không đọc metadata mutable hiện tại.

### 7.2. DAO query

```sql
SELECT
    sessionId,
    planTitleSnapshot,
    planCoverImageSnapshot,
    planDayNumberSnapshot,
    planDayTitleSnapshot,
    startedAt,
    endedAt,
    durationSeconds,
    caloriesBurned
FROM workout_sessions
WHERE userId = :userId
  AND status = :completedStatus
  AND endedAt IS NOT NULL
  AND endedAt >= :fromMillis
  AND endedAt < :toMillis
ORDER BY endedAt DESC;
```

API trả `Flow<List<WorkoutHistoryRow>>` để UI tự cập nhật khi một session hoàn thành.

### 7.3. Repository

Thêm vào `WorkoutSessionRepository`:

```kotlin
fun observeCompletedHistory(
    fromMillis: Long,
    toMillis: Long
): Flow<List<WorkoutHistoryEntry>>
```

`WorkoutSessionRepositoryImpl` resolve local user, gọi DAO và map row sang domain. Title fallback:

```text
planDayTitleSnapshot nếu có
else "Day {planDayNumberSnapshot} – {planTitleSnapshot}"
else planTitleSnapshot
else "Workout"
```

## 8. Domain layer

### 8.1. Models

Tạo `domain/models/WorkoutHistory.kt`:

```kotlin
data class WorkoutHistoryEntry(
    val sessionId: Long,
    val title: String,
    val imageUrl: String?,
    val startedAt: Long,
    val completedAt: Long,
    val durationSeconds: Int?,
    val caloriesBurned: Double?
)

data class WorkoutHistorySummary(
    val workoutCount: Int,
    val totalDurationSeconds: Long,
    val totalCaloriesBurned: Double?
)

data class WorkoutHistoryPeriod(
    val sessions: List<WorkoutHistoryEntry>,
    val workoutDayStarts: Set<Long>,
    val weeklySessions: List<WorkoutHistoryEntry>,
    val weeklySummary: WorkoutHistorySummary
)
```

### 8.2. GetWorkoutHistoryUseCase

Tạo `domain/usecases/history/GetWorkoutHistoryUseCase.kt` để:

- Quan sát completed sessions trong query range.
- Chuẩn hóa `completedAt` về local calendar day.
- Tạo tập ngày có workout cho calendar.
- Lọc session của tuần được chọn.
- Tính workout count, total duration và total calories.
- Không format text/locale trong domain.

Calories rule:

- Không session nào có calories -> summary calories là `null`.
- Có ít nhất một giá trị -> cộng các giá trị non-null.

## 9. Presentation layer

### 9.1. HistoryViewModel

Tạo `ui/core/history/HistoryViewModel.kt` và `HistoryUiState` chứa:

- `isLoading`
- `selectedDayMillis`
- `visibleYear`, `visibleMonth`
- `calendarDays`
- `weeklySessions`
- `weeklySummary`
- `errorMessage`

Events:

```kotlin
fun selectDay(dayStartMillis: Long)
fun showPreviousMonth()
fun showNextMonth()
fun retry()
```

Dùng `flatMapLatest` khi month/day query range đổi, rồi `stateIn(viewModelScope, WhileSubscribed(5_000), initialState)`.

### 9.2. Shared WorkoutCalendar

Tạo `ui/components/WorkoutCalendar.kt`. Component chỉ nhận state/callback, không biết ViewModel hoặc database.

Mỗi calendar day gồm:

- Timestamp đầu ngày.
- Day of month.
- Có thuộc visible month không.
- Có phải hôm nay không.
- Có được chọn không.
- Có workout không.

UI:

- Header tháng/năm và previous/next.
- Selected day dùng primary color.
- Ngày có workout có dot/ring.
- Ngày ngoài tháng giảm alpha.
- Weekday header dùng locale hoặc quy ước first day of week thống nhất với app settings.

### 9.3. Refactor HistoryScreen

Signature mới:

```kotlin
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit
)
```

Xóa:

- `HistoryEntry` private mock model.
- `sampleWeek`.
- `currentMonthGrid()` khỏi Screen.
- Weekly Summary hard-code.
- `CalendarCell` hard-code.

Screen dùng `collectAsStateWithLifecycle()` và render:

- Loading indicator.
- Shared calendar.
- Weekly summary từ state.
- Empty/error state.
- History items từ Room.

Mỗi item hiển thị completed date/time, snapshot title, duration, calories và snapshot cover URL.

## 10. Calories upstream

`WorkoutSessionEntity` đã có `caloriesBurned`, nhưng `completeSession()` hiện chưa ghi trường này.

Task liên quan:

1. Chọn công thức calories dựa trên dữ liệu user/workout có thật.
2. Tính calories trong domain use case hoặc service phù hợp.
3. Ghi `caloriesBurned` cùng transaction/update khi complete session.
4. Không backfill dữ liệu cũ bằng số giả; giữ `null` và hiển thị `— Kcal`.

Task calories không chặn History dùng database, nhưng cần hoàn thành để summary có Kcal thật cho session mới.

## 11. Dependency injection và navigation

### App.kt

- Cập nhật constructor `WorkoutSessionRepositoryImpl` với `AppDatabase`, `WorkoutPlanDao` hoặc dependency cần cho transaction snapshot.
- Đăng ký `GetWorkoutHistoryUseCase`.
- Các use case streak, badge, weekly goal tiếp tục dùng cùng repository instance.

### ScreenNavigator.kt

Tạo `HistoryViewModel` bằng `viewModelFactory`, inject `getWorkoutHistoryUseCase`, rồi truyền ViewModel vào `HistoryScreen`.

Không thay đổi `Screen.History.route` hoặc callback `onOpenHistory`.

## 12. Impact analysis

| Khu vực | Ảnh hưởng | Xử lý |
|---|---|---|
| Existing database | Có | Migration `5 -> 6`, backfill best-effort, không xóa dữ liệu |
| `WorkoutSessionEntity` constructors/copy | Có | Column mới nullable với default `null`; createSession ghi explicit snapshot |
| Start workout | Có | Tạo session và exercise snapshots trong một transaction |
| Start specific day | Có gián tiếp | Tiếp tục gọi createSession chung |
| Restart workout day | Có gián tiếp | Session restart cũng phải lấy snapshot mới tại thời điểm restart |
| Resume/progress resolution | Không đáng kể | Các khóa `planId`, `planDayId`, status giữ nguyên |
| Player UI | Không thay đổi contract chính | Vẫn nhận resolved day; session creation có thêm persistence nội bộ |
| Plan exercise edit | Có lợi | Session cũ đọc snapshot, session mới đọc plan mới |
| Custom plan delete | Có | Hard-delete nếu chưa có session, archive nếu đã có session |
| Home/workout lists | Không đáng kể | Query đã lọc `isActive = 1`, plan archive tự ẩn |
| Reset progress | Cần kiểm tra | Xóa session sẽ cascade session exercises; không tự kích hoạt lại plan đã archive |
| Streak | Không | Vẫn dùng `status` và `endedAt` |
| Badges/achievement totals | Không | Các query aggregate hiện tại không phụ thuộc snapshot columns |
| Weekly goal | Không | Vẫn dùng completed session timestamps |
| ReportScreen | Không | Weight/BMI/badges giữ nguyên; History callback giữ nguyên |
| Seeder | Không đáng kể | Không tạo session nên không cần snapshot data |
| DBML/documentation | Có | Thêm bốn columns và note snapshot |
| Repository tests/fakes | Có | Constructor và interface mới phải được cập nhật |

## 13. Testing plan

### 13.1. Migration

- Version 5 -> 6 giữ nguyên users/plans/sessions.
- Snapshot columns được tạo và backfill đúng.
- Version 4 -> 5 -> 6 chạy liên tiếp.
- Room schema validation pass.

### 13.2. Session snapshot

- createSession ghi plan/day snapshot.
- createSession tạo đúng thứ tự exercise snapshots.
- Edit title/cover/day/exercises sau đó không đổi session cũ.
- Restart tạo snapshot theo trạng thái plan tại thời điểm restart.
- Một lỗi insert exercise làm rollback cả session.

### 13.3. Delete/archive

- Custom plan chưa có session được hard-delete.
- Custom plan có session được archive.
- System plan không bị delete/archive qua custom-plan use case.
- History vẫn đọc được sau archive.

### 13.4. History DAO/use case

- Chỉ lấy completed sessions trong `[fromMillis, toMillis)`.
- Sort `endedAt DESC`.
- Không thay đổi output khi plan hiện tại được edit.
- Group đúng local day/week, kể cả boundary tháng/năm.
- Summary xử lý duration/calories null đúng.

### 13.5. ViewModel/UI

- Initial/loading/data/empty/error/retry.
- Chọn ngày và chuyển tháng đổi query range.
- Calendar đánh dấu ngày có workout.
- UI hiển thị snapshot title/cover.
- Không còn sample data khi Room rỗng.

## 14. Thứ tự triển khai

1. Cập nhật DBML và viết migration test cho version 5.
2. Thêm snapshot fields vào `WorkoutSessionEntity` và `MIGRATION_5_6`.
3. Tạo plan/day và exercise snapshot projections/queries.
4. Refactor `createSession()` thành transaction ghi session + exercises.
5. Sửa delete custom plan thành delete-or-archive và thêm test.
6. Tạo `WorkoutHistoryRow` và History DAO query.
7. Mở rộng repository và domain models.
8. Tạo `GetWorkoutHistoryUseCase` và unit tests.
9. Tạo `HistoryViewModel` và tests.
10. Tạo shared `WorkoutCalendar`.
11. Refactor `HistoryScreen` và xóa mock.
12. Wire dependency trong `App.kt` và `ScreenNavigator.kt`.
13. Chạy compile, unit tests, migration tests và Compose/instrumentation tests.
14. Thực hiện calories upstream nếu cần Kcal thật.

## 15. Acceptance criteria

- Database nâng từ 5 lên 6 mà không mất dữ liệu.
- Session mới luôn có plan title/day number snapshot và đầy đủ exercise snapshots.
- Edit plan sau khi hoàn thành không làm thay đổi History cũ.
- Delete plan đã có session không còn gây foreign-key failure và không làm mất History.
- `HistoryScreen` có ViewModel, StateFlow và lifecycle-aware collection.
- Không còn `sampleWeek` hoặc Weekly Summary hard-code.
- Calendar/list/summary đều lấy từ Room.
- Loading, empty và error state hoạt động.
- Calories null không bị biến thành dữ liệu giả.
- UI/domain không import Room entity hoặc DAO.
- Streak, badge, weekly goal, start/restart/resume và reset progress không regression.
- Build và toàn bộ test liên quan pass.

## 16. Các file dự kiến thay đổi

```text
docs/db_diagram.dbml                                                               [modify]
app/src/main/java/com/example/homeworkout/data/local/AppDatabase.kt                [modify]
app/src/main/java/com/example/homeworkout/data/local/entities/WorkoutSessionEntity.kt [modify]
app/src/main/java/com/example/homeworkout/data/local/dao/relations/SessionSnapshotRows.kt [new]
app/src/main/java/com/example/homeworkout/data/local/dao/relations/WorkoutHistoryRow.kt [new]
app/src/main/java/com/example/homeworkout/data/local/dao/WorkoutPlanDao.kt          [modify]
app/src/main/java/com/example/homeworkout/data/local/dao/WorkoutSessionDao.kt       [modify]
app/src/main/java/com/example/homeworkout/data/repositories/WorkoutRepositoryImpl.kt [modify]
app/src/main/java/com/example/homeworkout/data/repositories/WorkoutSessionRepositoryImpl.kt [modify]
app/src/main/java/com/example/homeworkout/domain/models/WorkoutHistory.kt           [new]
app/src/main/java/com/example/homeworkout/domain/repositories/WorkoutSessionRepository.kt [modify]
app/src/main/java/com/example/homeworkout/domain/usecases/history/GetWorkoutHistoryUseCase.kt [new]
app/src/main/java/com/example/homeworkout/ui/core/history/HistoryViewModel.kt       [new]
app/src/main/java/com/example/homeworkout/ui/core/history/HistoryScreen.kt          [modify]
app/src/main/java/com/example/homeworkout/ui/components/WorkoutCalendar.kt          [new]
app/src/main/java/com/example/homeworkout/ui/navigation/ScreenNavigator.kt          [modify]
app/src/main/java/com/example/homeworkout/ui/App.kt                                 [modify]
```

Test files migration, DAO, repository, use case, ViewModel và Compose UI được thêm tương ứng theo test structure của project.
