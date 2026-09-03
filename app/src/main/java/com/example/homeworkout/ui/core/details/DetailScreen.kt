package com.example.homeworkout.ui.core.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.SubcomposeAsyncImage
import com.example.homeworkout.domain.models.PlanExerciseSummary
import com.example.homeworkout.domain.models.WorkoutPlanDayDetail
import com.example.homeworkout.domain.models.WorkoutPlanDetail
import com.example.homeworkout.domain.usecases.player.ResolvedPlanDay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.planThemeDrawableRes
import com.example.homeworkout.ui.components.ExerciseRow
import com.example.homeworkout.ui.components.SectionHeader
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.theme.AppGradients
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.BrandBlueTint
import com.example.homeworkout.ui.theme.CardShape
import com.example.homeworkout.ui.theme.CardWhite
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.PillShape
import com.example.homeworkout.ui.theme.SlateGray
import com.example.homeworkout.utils.ScreenWrapper

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onNavigateBack: () -> Unit,
    onStartWorkout: (planId: Long, planDayId: Long?) -> Unit,
    onEditExercises: (Long) -> Unit,
    onOpenExerciseInfo: (Long) -> Unit,
    onOpenWorkoutSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nextDay by viewModel.nextDay.collectAsStateWithLifecycle()
    val topBarTitle = (uiState as? DetailUiState.Success)?.detail?.plan?.title ?: "Workout"

    ScreenWrapper {
        Scaffold(
            topBar = {
                BackTopBar(title = topBarTitle, onNavigateBack = onNavigateBack) {
                    IconButton(onClick = onOpenWorkoutSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Workout settings")
                    }
                }
            }
        ) { padding ->
            when (val state = uiState) {
                is DetailUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

                is DetailUiState.NotFound -> Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text("This workout could not be found.") }

                is DetailUiState.Error -> Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text("Couldn't load workout: ${state.message}", color = MaterialTheme.colorScheme.error) }

                is DetailUiState.Success -> PlanDetailContent(
                    detail = state.detail,
                    nextDay = nextDay,
                    contentPadding = padding,
                    onStartWorkout = { planDayId -> onStartWorkout(state.detail.plan.id, planDayId) },
                    onEditExercises = { onEditExercises(state.detail.plan.id) },
                    onOpenExerciseInfo = onOpenExerciseInfo
                )
            }
        }
    }
}

@Composable
private fun PlanDetailContent(
    detail: WorkoutPlanDetail,
    nextDay: ResolvedPlanDay?,
    contentPadding: PaddingValues,
    onStartWorkout: (planDayId: Long?) -> Unit,
    onEditExercises: () -> Unit,
    onOpenExerciseInfo: (Long) -> Unit
) {
    val allExercises = detail.days.flatMap { it.exercises }
    val estimatedMinutes = (allExercises.sumOf { (it.targetDurationSec ?: 30) } / 60).coerceAtLeast(1)
    val isMultiDay = detail.days.size > 1
    val nextDayNumber = nextDay?.day?.dayNumber

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, bottom = 24.dp,
            top = contentPadding.calculateTopPadding() + 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HeroBanner(planId = detail.plan.id, coverImageUrl = detail.plan.coverImageUrl) }
        item {
            Text(detail.plan.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                StatChip(Icons.Default.Schedule, "$estimatedMinutes mins")
                StatChip(Icons.Default.FitnessCenter, "${allExercises.size} Exercises")
            }
        }
        item {
            AppButton(
                text = if (isMultiDay && nextDayNumber != null) "Start · Day $nextDayNumber" else "Start",
                onClick = { onStartWorkout(null) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            SectionHeader(title = "Exercises", actionText = "Edit", onActionClick = onEditExercises)
        }

        if (isMultiDay) {
            detail.days.forEachIndexed { dayIndex, day ->
                item(key = "day-${day.planDayId}") {
                    val isNext = day.dayNumber == nextDayNumber
                    val panelColor = if (isNext) BrandBlueTint else if (dayIndex % 2 == 1) CloudGray else CardWhite
                    DayGroupPanel(
                        day = day,
                        isNext = isNext,
                        panelColor = panelColor,
                        onStartDay = { onStartWorkout(day.planDayId) },
                        onOpenExerciseInfo = onOpenExerciseInfo
                    )
                }
            }
        } else {
            items(allExercises, key = { it.planExerciseId }) { exercise ->
                ExerciseRow(
                    title = exercise.title,
                    subtitle = exercise.subtitleText(),
                    imageUrl = exercise.imageUrl,
                    onClick = { onOpenExerciseInfo(exercise.exerciseId) }
                )
            }
        }
    }
}

/** One day's exercises grouped into a single tinted panel, so a long multi-day plan's exercise
 * list reads as clearly separated days instead of one long flat list. [panelColor] alternates
 * between plain days so consecutive groups stay visually distinct; the day "Start" would actually
 * play next gets [BrandBlueTint] plus a "NEXT" badge regardless of the alternation. */
@Composable
private fun DayGroupPanel(
    day: WorkoutPlanDayDetail,
    isNext: Boolean,
    panelColor: Color,
    onStartDay: () -> Unit,
    onOpenExerciseInfo: (Long) -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth(), containerColor = panelColor) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    buildString {
                        append("DAY ${day.dayNumber}")
                        if (!day.title.isNullOrBlank()) append(" · ${day.title}")
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = InkBlack
                )
                if (isNext) {
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(BrandBlue)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("NEXT", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onStartDay)
            ) {
                Text("Start", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = BrandBlue)
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Start day ${day.dayNumber}",
                    tint = BrandBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        day.exercises.forEachIndexed { index, exercise ->
            ExerciseRow(
                title = exercise.title,
                subtitle = exercise.subtitleText(),
                imageUrl = exercise.imageUrl,
                onClick = { onOpenExerciseInfo(exercise.exerciseId) },
                modifier = Modifier.padding(horizontal = 8.dp),
                showDivider = index != day.exercises.lastIndex
            )
        }
    }
}

@Composable
private fun HeroBanner(planId: Long, coverImageUrl: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(CardShape)
    ) {
        if (coverImageUrl.isNullOrBlank()) {
            ThemedHeroImage(planId)
        } else {
            SubcomposeAsyncImage(
                model = coverImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { HeroBannerFallback() },
                error = { ThemedHeroImage(planId) }
            )
        }
    }
}

@Composable
private fun ThemedHeroImage(planId: Long) {
    Image(
        painter = painterResource(id = planThemeDrawableRes(planId)),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds
    )
}

@Composable
private fun HeroBannerFallback() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppGradients.DarkButton),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.FitnessCenter,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(64.dp)
        )
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = SlateGray, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = SlateGray, fontWeight = FontWeight.SemiBold)
    }
}

private fun PlanExerciseSummary.subtitleText(): String = when {
    targetReps != null -> "x$targetReps"
    targetDurationSec != null -> "${targetDurationSec}s"
    else -> ""
}
