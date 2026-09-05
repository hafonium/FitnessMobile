# Architecture & Implementation Specification
## Run Persistence, Clean Architecture History & Route Replay with Stadia Maps

**Feature:** GPS Run Persistence, Run History & Route Replay  
**Target Platform:** Android  
**UI Framework:** Jetpack Compose (Material 3)  
**Design System / Theme:** Light Theme (`HomeWorkoutTheme`, `PageBackground`, `CardWhite`, `BrandBlue`)  
**Map Engine:** MapLibre Native Android (OpenGL backend) + Stadia Maps (`outdoors` style)  
**Architecture:** Clean Architecture + MVVM + Manual DI (Strictly aligned with `docs/architecture.md`)  
**Base Package:** `com.example.homeworkout`  

---

# 1. Context & Objective

1. **Current State:** The application records live GPS running sessions via `RunningTrackingService`, calculates live telemetry via `RunningTelemetryCalculator`, and renders live routes with MapLibre Native over Stadia Maps using `BuildConfig.STADIA_API_KEY`.
2. **Target Deliverable:**
   - Compress runner GPS paths into compact ASCII strings using the **Google Encoded Polyline Algorithm**.
   - Persist completed run sessions in **Room Database** (`run_sessions` and `run_points` tables in `AppDatabase.kt`).
   - Connect the persistence and retrieval pipeline through **Clean Architecture Use Cases & Repository pattern**.
   - Implement `RunHistoryScreen` (Jetpack Compose with **Light Theme**) displaying overall running metrics summary and historical run items with delete capability.
   - Implement `RunDetailScreen` (Jetpack Compose with **Light Theme** + MapLibre) that decodes the polyline and fits camera bounds (`LatLngBounds`) to display the historical route over Stadia Outdoors.
   - Integrate all dependencies into **Manual Dependency Injection** in `App.kt` and navigation routes in `Screen.kt` & `ScreenNavigator.kt`.

---

# 2. Design System & Light Theme Palette

The UI adopts the application's default **Light Theme** system defined in `ui/theme/Color.kt`:

| Token | Value | Usage |
|---|---|---|
| `PageBackground` | `#F8F9FB` | Main screen background for history and detail views |
| `CardWhite` | `#FFFFFF` | Card surfaces, metric tiles, and list item containers |
| `BrandBlue` | `#0052FE` | Primary accent, CTA buttons, highlighted metrics (distance) |
| `BrandBlueTint` | `#EEF3FF` | Tinted metric container surfaces and selected chips |
| `BrandBlueDark` | `#0040D6` | Darker blue for contrast on tinted backgrounds |
| `InkBlack` | `#15171B` | Primary headings, metric values, and high-contrast text |
| `SlateGray` | `#8B8D98` | Secondary labels, timestamps, units, and captions |
| `HairlineGray` | `#E9EAEE` | Dividers, card borders, and outlines |
| `CloudGray` | `#F1F2F5` | Muted chip fills and subtle secondary surfaces |
| `StreakRed` | `#FFFF3B30` | Delete action icons and destructive confirmation alerts |

---

# 3. Dependencies & Build Configuration

Ensure the following dependencies are present in `app/build.gradle.kts`:

```kotlin
dependencies {
    // Room DB (local persistence)
    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // MapLibre Native SDK (OpenGL backend for wide compatibility)
    implementation("org.maplibre.gl:android-sdk-opengl:13.4.1")

    // Android Maps Utils (PolyUtil for Google Polyline Encoding/Decoding)
    implementation("com.google.maps.android:android-maps-utils:3.8.2")

    // Lifecycle & Compose
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
}
```

---

# 4. Data Compression: Google Encoded Polyline Algorithm

Create a dedicated utility in `utils/PolylineUtils.kt` to encode `RunPoint` lists into compact polyline strings and decode them into MapLibre-compatible structures (`org.maplibre.android.geometry.LatLng` and `org.maplibre.geojson.Point`).

### `app/src/main/java/com/example/homeworkout/utils/PolylineUtils.kt`

```kotlin
package com.example.homeworkout.utils

import com.example.homeworkout.domain.models.running.RunPoint
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import org.maplibre.android.geometry.LatLng as MapLibreLatLng
import org.maplibre.geojson.Point

object PolylineUtils {

    /**
     * Compresses a sequence of [RunPoint] GPS coordinates into a compact ASCII encoded polyline string.
     */
    fun encodePoints(points: List<RunPoint>): String {
        if (points.isEmpty()) return ""
        val gmsPoints = points.map { LatLng(it.latitude, it.longitude) }
        return PolyUtil.encode(gmsPoints)
    }

    /**
     * Decodes an ASCII encoded polyline back to MapLibre GeoJSON [Point] list for LineLayer rendering.
     */
    fun decodeToGeoJsonPoints(encodedPolyline: String): List<Point> {
        if (encodedPolyline.isBlank()) return emptyList()
        val decodedList = PolyUtil.decode(encodedPolyline)
        return decodedList.map { Point.fromLngLat(it.longitude, it.latitude) }
    }

    /**
     * Decodes an ASCII encoded polyline back to MapLibre [MapLibreLatLng] for camera bounds calculation.
     */
    fun decodeToMapLibreLatLng(encodedPolyline: String): List<MapLibreLatLng> {
        if (encodedPolyline.isBlank()) return emptyList()
        val decodedList = PolyUtil.decode(encodedPolyline)
        return decodedList.map { MapLibreLatLng(it.latitude, it.longitude) }
    }
}
```

---

# 5. Local Data Layer (Room)

Following `docs/architecture.md`:
- `@Entity` classes reside in `com.example.homeworkout.data.local.entities`.
- `@Dao` interfaces reside in `com.example.homeworkout.data.local.dao`.
- `AppDatabase.kt` registers all entities and DAOs.

### 5.1. Entity: `app/src/main/java/com/example/homeworkout/data/local/entities/RunSessionEntity.kt`

```kotlin
package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_sessions")
data class RunSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val finishedAt: Long?,
    val activeDurationMillis: Long,
    val runningStartedElapsedRealtimeMillis: Long?,
    val distanceMeters: Double,
    val calories: Double?,
    val weightKg: Double?,
    val status: String,
    val currentSegmentIndex: Int,
    val errorMessage: String?,
    val encodedPolyline: String? = null
)
```

### 5.2. DAO: `app/src/main/java/com/example/homeworkout/data/local/dao/RunningDao.kt`

```kotlin
package com.example.homeworkout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.homeworkout.data.local.entities.RunPointEntity
import com.example.homeworkout.data.local.entities.RunSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunningDao {
    @Query("SELECT * FROM run_sessions ORDER BY id DESC LIMIT 1")
    fun observeLatestSession(): Flow<RunSessionEntity?>

    @Query("SELECT * FROM run_sessions WHERE status IN ('RUNNING', 'PAUSED') ORDER BY id DESC LIMIT 1")
    suspend fun getRecoverableSession(): RunSessionEntity?

    @Query("SELECT * FROM run_sessions WHERE status = 'FINISHED' ORDER BY startedAt DESC")
    fun getAllFinishedSessions(): Flow<List<RunSessionEntity>>

    @Query("SELECT * FROM run_sessions WHERE id = :id LIMIT 1")
    suspend fun getSession(id: Long): RunSessionEntity?

    @Query("SELECT * FROM run_points WHERE sessionId = :sessionId ORDER BY sequence")
    fun observePoints(sessionId: Long): Flow<List<RunPointEntity>>

    @Query("SELECT * FROM run_points WHERE sessionId = :sessionId ORDER BY sequence")
    suspend fun getPoints(sessionId: Long): List<RunPointEntity>

    @Insert suspend fun insertSession(session: RunSessionEntity): Long
    @Insert suspend fun insertPoint(point: RunPointEntity): Long
    @Update suspend fun updateSession(session: RunSessionEntity)

    @Query("DELETE FROM run_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Transaction
    suspend fun appendPointAndProgress(
        point: RunPointEntity,
        distanceMeters: Double,
        activeDurationMillis: Long,
        runningStartedElapsedRealtimeMillis: Long,
        calories: Double?
    ) {
        insertPoint(point)
        val session = getSession(point.sessionId) ?: return
        updateSession(session.copy(
            distanceMeters = distanceMeters,
            activeDurationMillis = activeDurationMillis,
            runningStartedElapsedRealtimeMillis = runningStartedElapsedRealtimeMillis,
            calories = calories
        ))
    }
}
```

---

# 6. Domain Layer & Clean Architecture Use Cases

Following `docs/architecture.md`:
- Pure Kotlin only (no Android SDK, Room, or Compose imports).
- Domain Models reside in `domain/models/running/`.
- Repository interface resides in `domain/repositories/RunningRepository.kt`.
- Use Cases reside in `domain/usecases/running/` with `operator fun invoke(...)`.

### 6.1. Domain Models: `app/src/main/java/com/example/homeworkout/domain/models/running/RunModels.kt`

```kotlin
package com.example.homeworkout.domain.models.running

enum class RunStatus { RUNNING, PAUSED, FINISHED, ERROR }

data class RunPoint(
    val id: Long = 0,
    val sessionId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float,
    val speedMps: Float?,
    val elapsedRealtimeNanos: Long,
    val sequence: Int,
    val segmentIndex: Int
)

data class RunSession(
    val id: Long,
    val startedAt: Long,
    val finishedAt: Long?,
    val activeDurationMillis: Long,
    val runningStartedElapsedRealtimeMillis: Long?,
    val distanceMeters: Double,
    val calories: Double?,
    val weightKg: Double?,
    val status: RunStatus,
    val currentSegmentIndex: Int,
    val errorMessage: String? = null,
    val encodedPolyline: String? = null,
    val points: List<RunPoint> = emptyList()
) {
    val durationSeconds: Long get() = activeDurationMillis / 1000L

    val avgPaceMinPerKm: Double
        get() {
            val distKm = distanceMeters / 1000.0
            val durationSec = durationSeconds
            return if (distKm > 0.0 && durationSec > 0) (durationSec / 60.0) / distKm else 0.0
        }

    fun activeDurationAt(elapsedRealtimeMillis: Long): Long = activeDurationMillis +
        if (status == RunStatus.RUNNING && runningStartedElapsedRealtimeMillis != null) {
            (elapsedRealtimeMillis - runningStartedElapsedRealtimeMillis).coerceAtLeast(0L)
        } else 0L
}

data class RunningSnapshot(val session: RunSession?)
```

### 6.2. Repository Interface: `app/src/main/java/com/example/homeworkout/domain/repositories/RunningRepository.kt`

```kotlin
package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.running.RunPoint
import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.models.running.RunStatus
import kotlinx.coroutines.flow.Flow

interface RunningRepository {
    fun observeLatestSession(): Flow<RunSession?>
    fun getAllFinishedRuns(): Flow<List<RunSession>>
    suspend fun getRunById(id: Long): RunSession?
    suspend fun getRecoverableSession(): RunSession?
    suspend fun createSession(startedAt: Long, elapsedRealtimeMillis: Long): RunSession
    suspend fun appendPoint(
        point: RunPoint,
        distanceMeters: Double,
        activeDurationMillis: Long,
        runningStartedElapsedRealtimeMillis: Long,
        calories: Double?
    )
    suspend fun updateState(
        id: Long,
        status: RunStatus,
        activeDurationMillis: Long,
        runningStartedElapsedRealtimeMillis: Long?,
        segmentIndex: Int,
        finishedAt: Long? = null,
        errorMessage: String? = null,
        encodedPolyline: String? = null
    )
    suspend fun deleteRun(id: Long)
}
```

### 6.3. Repository Implementation: `app/src/main/java/com/example/homeworkout/data/repositories/RunningRepositoryImpl.kt`

```kotlin
package com.example.homeworkout.data.repositories

import com.example.homeworkout.data.local.dao.RunningDao
import com.example.homeworkout.data.local.dao.UserDao
import com.example.homeworkout.data.local.dao.WeightLogDao
import com.example.homeworkout.data.local.entities.RunPointEntity
import com.example.homeworkout.data.local.entities.RunSessionEntity
import com.example.homeworkout.data.local.seed.AppDatabaseSeeder
import com.example.homeworkout.domain.models.running.RunPoint
import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.models.running.RunStatus
import com.example.homeworkout.domain.repositories.RunningRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class RunningRepositoryImpl(
    private val runningDao: RunningDao,
    private val weightLogDao: WeightLogDao,
    private val userDao: UserDao
) : RunningRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeLatestSession(): Flow<RunSession?> = runningDao.observeLatestSession().flatMapLatest { session ->
        if (session == null) flowOf(null)
        else runningDao.observePoints(session.id).map { points -> session.toDomain(points) }
    }

    override fun getAllFinishedRuns(): Flow<List<RunSession>> = runningDao.getAllFinishedSessions().map { list ->
        list.map { entity -> entity.toDomain(emptyList()) }
    }

    override suspend fun getRunById(id: Long): RunSession? {
        val session = runningDao.getSession(id) ?: return null
        val points = runningDao.getPoints(id)
        return session.toDomain(points)
    }

    override suspend fun getRecoverableSession(): RunSession? = runningDao.getRecoverableSession()?.let { session ->
        session.toDomain(runningDao.getPoints(session.id))
    }

    override suspend fun createSession(startedAt: Long, elapsedRealtimeMillis: Long): RunSession {
        getRecoverableSession()?.let { return it }
        val entity = RunSessionEntity(
            startedAt = startedAt,
            finishedAt = null,
            activeDurationMillis = 0,
            runningStartedElapsedRealtimeMillis = elapsedRealtimeMillis,
            distanceMeters = 0.0,
            calories = null,
            weightKg = userDao.getUserByEmail(AppDatabaseSeeder.DEFAULT_USER_EMAIL)
                ?.let { weightLogDao.getLatestWeightLog(it.userId) }
                ?.weightKg,
            status = RunStatus.RUNNING.name,
            currentSegmentIndex = 0,
            errorMessage = null,
            encodedPolyline = null
        )
        val id = runningDao.insertSession(entity)
        return entity.copy(id = id).toDomain(emptyList())
    }

    override suspend fun appendPoint(
        point: RunPoint,
        distanceMeters: Double,
        activeDurationMillis: Long,
        runningStartedElapsedRealtimeMillis: Long,
        calories: Double?
    ) {
        runningDao.appendPointAndProgress(
            point.toEntity(), distanceMeters, activeDurationMillis, runningStartedElapsedRealtimeMillis, calories
        )
    }

    override suspend fun updateState(
        id: Long,
        status: RunStatus,
        activeDurationMillis: Long,
        runningStartedElapsedRealtimeMillis: Long?,
        segmentIndex: Int,
        finishedAt: Long?,
        errorMessage: String?,
        encodedPolyline: String?
    ) {
        val current = runningDao.getSession(id) ?: return
        runningDao.updateSession(current.copy(
            status = status.name,
            activeDurationMillis = activeDurationMillis,
            runningStartedElapsedRealtimeMillis = runningStartedElapsedRealtimeMillis,
            currentSegmentIndex = segmentIndex,
            finishedAt = finishedAt,
            errorMessage = errorMessage,
            encodedPolyline = encodedPolyline ?: current.encodedPolyline
        ))
    }

    override suspend fun deleteRun(id: Long) {
        runningDao.deleteSession(id)
    }

    private fun RunSessionEntity.toDomain(points: List<RunPointEntity>) = RunSession(
        id, startedAt, finishedAt, activeDurationMillis, runningStartedElapsedRealtimeMillis,
        distanceMeters, calories, weightKg, RunStatus.valueOf(status), currentSegmentIndex,
        errorMessage, encodedPolyline, points.map { it.toDomain() }
    )

    private fun RunPointEntity.toDomain() = RunPoint(
        id, sessionId, latitude, longitude, altitudeMeters, accuracyMeters, speedMps,
        elapsedRealtimeNanos, sequence, segmentIndex
    )

    private fun RunPoint.toEntity() = RunPointEntity(
        id, sessionId, latitude, longitude, altitudeMeters, accuracyMeters, speedMps,
        elapsedRealtimeNanos, sequence, segmentIndex
    )
}
```

### 6.4. Domain Use Cases (`domain/usecases/running/`)

```kotlin
// GetRunHistoryUseCase.kt
package com.example.homeworkout.domain.usecases.running

import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.repositories.RunningRepository
import kotlinx.coroutines.flow.Flow

class GetRunHistoryUseCase(private val repository: RunningRepository) {
    operator fun invoke(): Flow<List<RunSession>> = repository.getAllFinishedRuns()
}
```

```kotlin
// GetRunDetailUseCase.kt
package com.example.homeworkout.domain.usecases.running

import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.repositories.RunningRepository

class GetRunDetailUseCase(private val repository: RunningRepository) {
    suspend operator fun invoke(id: Long): RunSession? = repository.getRunById(id)
}
```

```kotlin
// DeleteRunUseCase.kt
package com.example.homeworkout.domain.usecases.running

import com.example.homeworkout.domain.repositories.RunningRepository

class DeleteRunUseCase(private val repository: RunningRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteRun(id)
}
```

---

# 7. Saving Runs on Session Finish (`RunningTrackingService.kt`)

When finishing the active GPS session in `RunningTrackingService`:
1. Retrieve recorded `points` from repository/DAO.
2. Compress points into a string via `PolylineUtils.encodePoints(points)`.
3. Update session with `status = FINISHED`, `finishedAt = System.currentTimeMillis()`, and `encodedPolyline = encodedString`.

```kotlin
val encodedRoute = PolylineUtils.encodePoints(session.points)
repository.updateState(
    id = session.id,
    status = RunStatus.FINISHED,
    activeDurationMillis = session.activeDurationMillis,
    runningStartedElapsedRealtimeMillis = null,
    segmentIndex = session.currentSegmentIndex,
    finishedAt = System.currentTimeMillis(),
    encodedPolyline = encodedRoute
)
```

---

# 8. Presentation Layer: Run History Screen (Light Theme)

Package: `com.example.homeworkout.ui.core.running.history`

### 8.1. ViewModel: `RunHistoryViewModel.kt`

```kotlin
package com.example.homeworkout.ui.core.running.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.usecases.running.DeleteRunUseCase
import com.example.homeworkout.domain.usecases.running.GetRunHistoryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RunHistoryUiState(
    val runs: List<RunSession> = emptyList(),
    val totalDistanceKm: Double = 0.0,
    val totalDurationSeconds: Long = 0L,
    val overallAvgPace: Double = 0.0,
    val totalCalories: Double = 0.0,
    val isLoading: Boolean = true
)

class RunHistoryViewModel(
    getRunHistoryUseCase: GetRunHistoryUseCase,
    private val deleteRunUseCase: DeleteRunUseCase
) : ViewModel() {

    val uiState: StateFlow<RunHistoryUiState> = getRunHistoryUseCase()
        .map { runs ->
            val totalMeters = runs.sumOf { it.distanceMeters }
            val totalSeconds = runs.sumOf { it.durationSeconds }
            val distKm = totalMeters / 1000.0
            val avgPace = if (distKm > 0 && totalSeconds > 0) (totalSeconds / 60.0) / distKm else 0.0

            RunHistoryUiState(
                runs = runs,
                totalDistanceKm = distKm,
                totalDurationSeconds = totalSeconds,
                overallAvgPace = avgPace,
                totalCalories = runs.sumOf { it.calories ?: 0.0 },
                isLoading = false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RunHistoryUiState())

    fun deleteRun(id: Long) {
        viewModelScope.launch { deleteRunUseCase(id) }
    }
}
```

### 8.2. Screen Composable: `RunHistoryScreen.kt`

```kotlin
package com.example.homeworkout.ui.core.running.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.BrandBlueTint
import com.example.homeworkout.ui.theme.CardWhite
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.PageBackground
import com.example.homeworkout.ui.theme.SlateGray
import com.example.homeworkout.ui.theme.StreakRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RunHistoryScreen(
    viewModel: RunHistoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (runId: Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            BackTopBar(
                title = "Lịch sử chạy bộ",
                onBack = onNavigateBack
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PageBackground)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = BrandBlue
                )
            } else if (state.runs.isEmpty()) {
                Text(
                    text = "Chưa có lượt chạy nào được ghi lại.",
                    modifier = Modifier.align(Alignment.Center),
                    color = SlateGray,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "summary_banner") {
                        SummaryMetricBanner(state)
                    }

                    item(key = "header_title") {
                        Text(
                            text = "Danh sách buổi chạy (${state.runs.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = InkBlack,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    items(state.runs, key = { it.id }) { run ->
                        RunItemCard(
                            run = run,
                            onClick = { onNavigateToDetail(run.id) },
                            onDelete = { viewModel.deleteRun(run.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricBanner(state: RunHistoryUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HairlineGray, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandBlueTint)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "TỔNG TÍCH LŨY",
                color = SlateGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format(Locale.getDefault(), "%.2f km", state.totalDistanceKm),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BrandBlue
            )
            HorizontalDivider(color = HairlineGray, modifier = Modifier.padding(vertical = 12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BannerSubMetric("Thời gian", formatSeconds(state.totalDurationSeconds))
                BannerSubMetric("Pace TB", formatPace(state.overallAvgPace))
                BannerSubMetric("Calo", String.format(Locale.getDefault(), "%.0f kcal", state.totalCalories))
            }
        }
    }
}

@Composable
private fun BannerSubMetric(label: String, value: String) {
    Column {
        Text(text = label, color = SlateGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = InkBlack, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RunItemCard(
    run: RunSession,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HairlineGray, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(run.startedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGray,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = StreakRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%.2f km", run.distanceMeters / 1000.0),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandBlue
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MetricText("Thời gian", formatSeconds(run.durationSeconds))
                    MetricText("Pace", formatPace(run.avgPaceMinPerKm))
                    MetricText("Calo", String.format(Locale.getDefault(), "%.0f kcal", run.calories ?: 0.0))
                }
            }
        }
    }
}

@Composable
private fun MetricText(label: String, value: String) {
    Column(horizontalAlignment = Alignment.End) {
        Text(text = label, fontSize = 11.sp, color = SlateGray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkBlack)
    }
}

private fun formatTimestamp(timestampMs: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
    return sdf.format(Date(timestampMs))
}

private fun formatSeconds(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

private fun formatPace(pace: Double): String {
    if (pace <= 0.0 || pace.isInfinite() || pace.isNaN()) return "--'--\""
    val mins = pace.toInt()
    val secs = ((pace - mins) * 60).toInt()
    return String.format(Locale.getDefault(), "%02d'%02d\"", mins, secs)
}
```

---

# 9. Presentation Layer: Run Detail Screen with Replay Map (Light Theme)

Package: `com.example.homeworkout.ui.core.running.detail`

Decodes the run's encoded polyline, initializes MapLibre Native with the Stadia Outdoors style, plots the route on a `LineLayer`, and bounds the camera to fit the entire route.

### 9.1. ViewModel: `RunDetailViewModel.kt`

```kotlin
package com.example.homeworkout.ui.core.running.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.usecases.running.GetRunDetailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RunDetailUiState {
    data object Loading : RunDetailUiState
    data class Success(val run: RunSession) : RunDetailUiState
    data object Error : RunDetailUiState
}

class RunDetailViewModel(
    private val runId: Long,
    private val getRunDetailUseCase: GetRunDetailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<RunDetailUiState>(RunDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadRun()
    }

    private fun loadRun() {
        viewModelScope.launch {
            val run = getRunDetailUseCase(runId)
            if (run != null) {
                _uiState.value = RunDetailUiState.Success(run)
            } else {
                _uiState.value = RunDetailUiState.Error
            }
        }
    }
}
```

### 9.2. Composable Screen: `RunDetailScreen.kt`

```kotlin
package com.example.homeworkout.ui.core.running.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.BuildConfig
import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.CardWhite
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.PageBackground
import com.example.homeworkout.ui.theme.SlateGray
import com.example.homeworkout.utils.PolylineUtils
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import java.util.Locale

private const val DETAIL_ROUTE_SOURCE_ID = "detail_route_source"
private const val DETAIL_ROUTE_LAYER_ID = "detail_route_layer"

@Composable
fun RunDetailScreen(
    viewModel: RunDetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            BackTopBar(
                title = "Chi tiết buổi chạy",
                onBack = onBack
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PageBackground)
        ) {
            when (val current = state) {
                is RunDetailUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = BrandBlue
                )
                is RunDetailUiState.Error -> Text(
                    text = "Không tìm thấy dữ liệu lượt chạy",
                    modifier = Modifier.align(Alignment.Center),
                    color = SlateGray,
                    style = MaterialTheme.typography.bodyLarge
                )
                is RunDetailUiState.Success -> RunDetailContent(run = current.run)
            }
        }
    }
}

@Composable
private fun RunDetailContent(run: RunSession) {
    val stadiaStyleUri = "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=${BuildConfig.STADIA_API_KEY}"

    val geoJsonPoints = remember(run.encodedPolyline, run.points) {
        if (!run.encodedPolyline.isNullOrBlank()) {
            PolylineUtils.decodeToGeoJsonPoints(run.encodedPolyline)
        } else {
            run.points.map { org.maplibre.geojson.Point.fromLngLat(it.longitude, it.latitude) }
        }
    }

    val mapLibreLatLngs = remember(run.encodedPolyline, run.points) {
        if (!run.encodedPolyline.isNullOrBlank()) {
            PolylineUtils.decodeToMapLibreLatLng(run.encodedPolyline)
        } else {
            run.points.map { org.maplibre.android.geometry.LatLng(it.latitude, it.longitude) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Map Preview taking top portion of screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapView(context).apply {
                        onCreate(null)
                        getMapAsync { map ->
                            map.uiSettings.isAttributionEnabled = true
                            map.uiSettings.isLogoEnabled = true

                            map.setStyle(Style.Builder().fromUri(stadiaStyleUri)) { style ->
                                if (geoJsonPoints.size >= 2) {
                                    val lineString = LineString.fromLngLats(geoJsonPoints)
                                    val source = GeoJsonSource(
                                        DETAIL_ROUTE_SOURCE_ID,
                                        FeatureCollection.fromFeature(Feature.fromGeometry(lineString))
                                    )
                                    style.addSource(source)

                                    val layer = LineLayer(DETAIL_ROUTE_LAYER_ID, DETAIL_ROUTE_SOURCE_ID).apply {
                                        setProperties(
                                            lineColor("#0052FE"),
                                            lineWidth(5f),
                                            lineCap("round"),
                                            lineJoin("round")
                                        )
                                    }
                                    style.addLayer(layer)

                                    // Fit camera precisely around the entire decoded bounds
                                    val boundsBuilder = LatLngBounds.Builder()
                                    mapLibreLatLngs.forEach { boundsBuilder.include(it) }
                                    runCatching {
                                        map.easeCamera(
                                            CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100),
                                            1000
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        // Metrics Card taking bottom portion of screen
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, HairlineGray, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = String.format(Locale.getDefault(), "%.2f km", run.distanceMeters / 1000.0),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandBlue
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Tổng cự ly", color = SlateGray, fontSize = 13.sp)

                HorizontalDivider(color = HairlineGray, modifier = Modifier.padding(vertical = 16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailMetricItem("Thời gian", formatSeconds(run.durationSeconds))
                    DetailMetricItem("Pace TB", formatPace(run.avgPaceMinPerKm))
                    DetailMetricItem("Calo tiêu thụ", String.format(Locale.getDefault(), "%.0f kcal", run.calories ?: 0.0))
                }
            }
        }
    }
}

@Composable
private fun DetailMetricItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 12.sp, color = SlateGray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = InkBlack)
    }
}

private fun formatSeconds(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

private fun formatPace(pace: Double): String {
    if (pace <= 0.0 || pace.isInfinite() || pace.isNaN()) return "--'--\""
    val mins = pace.toInt()
    val secs = ((pace - mins) * 60).toInt()
    return String.format(Locale.getDefault(), "%02d'%02d\"", mins, secs)
}
```

---

# 10. Manual Dependency Injection (`ui/App.kt`)

In `app/src/main/java/com/example/homeworkout/ui/App.kt`, register lazy properties following the project's manual DI architecture:

```kotlin
// Repositories
val runningRepository: RunningRepository by lazy {
    RunningRepositoryImpl(database.runningDao(), database.weightLogDao(), database.userDao())
}

// Use Cases
val getRunHistoryUseCase by lazy { GetRunHistoryUseCase(runningRepository) }
val getRunDetailUseCase by lazy { GetRunDetailUseCase(runningRepository) }
val deleteRunUseCase by lazy { DeleteRunUseCase(runningRepository) }
val observeRunningSessionUseCase by lazy { ObserveRunningSessionUseCase(runningRepository) }
```

---

# 11. Navigation Integration (`Screen.kt` & `ScreenNavigator.kt`)

### 11.1. Destinations in `ui/navigation/Screen.kt`

```kotlin
// In Screen.kt
object RunHistory : Screen("run_history")
object RunDetail : Screen("run_detail/{runId}") {
    fun createRoute(runId: Long) = "run_detail/$runId"
}
```

### 11.2. Route Mapping in `ui/navigation/ScreenNavigator.kt`

```kotlin
// Run History Screen
composable(Screen.RunHistory.route) {
    val vm: RunHistoryViewModel = viewModel(factory = viewModelFactory {
        initializer {
            RunHistoryViewModel(
                appInstance.getRunHistoryUseCase,
                appInstance.deleteRunUseCase
            )
        }
    })
    RunHistoryScreen(
        viewModel = vm,
        onNavigateBack = { navController.popBackStack() },
        onNavigateToDetail = { runId ->
            navController.navigate(Screen.RunDetail.createRoute(runId))
        }
    )
}

// Run Detail Screen
composable(
    route = Screen.RunDetail.route,
    arguments = listOf(navArgument("runId") { type = NavType.LongType })
) { entry ->
    val runId = entry.arguments?.getLong("runId") ?: return@composable
    val vm: RunDetailViewModel = viewModel(key = "run-detail-$runId", factory = viewModelFactory {
        initializer {
            RunDetailViewModel(
                runId = runId,
                getRunDetailUseCase = appInstance.getRunDetailUseCase
            )
        }
    })
    RunDetailScreen(
        viewModel = vm,
        onBack = { navController.popBackStack() }
    )
}
```

---

# 12. Verification & Acceptance Checklist

1. **Light Theme Visual Integrity:** Verify that `RunHistoryScreen` and `RunDetailScreen` use `PageBackground` (`#F8F9FB`), `CardWhite` (`#FFFFFF`), `InkBlack` (`#15171B`), `SlateGray` (`#8B8D98`), and `HairlineGray` borders, providing a cohesive look with the rest of the application.
2. **Encoded Polyline Compression:** Verify in Room Inspection that `encodedPolyline` in `run_sessions` is saved as a compact ASCII string upon session finish.
3. **Clean Layer Separation:** Ensure no Room entities (`RunSessionEntity`, `RunPointEntity`) are imported in Jetpack Compose UI files. All UI state flows strictly through `GetRunHistoryUseCase` and `GetRunDetailUseCase`.
4. **MapLibre Dynamic Framing:** In `RunDetailScreen`, opening a completed run must auto-pan and zoom (`LatLngBounds` with padding 100px) to show the full start-to-finish line without manual user dragging.
5. **Stadia Attribution:** Confirm the MapLibre logo and attribution are clearly retained on the detail map view.
6. **Manual DI Integrity:** Confirm all use cases and repositories are instantiated as singletons on `App.kt` and injected via `viewModelFactory`.