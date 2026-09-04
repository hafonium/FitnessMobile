package com.example.homeworkout.ui.core.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.ActiveWorkoutSummary
import com.example.homeworkout.domain.models.RecommendedPlan
import com.example.homeworkout.domain.models.WeeklyGoalDay
import com.example.homeworkout.domain.models.WeeklyGoalProgress
import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.AppTextField
import com.example.homeworkout.ui.components.PlanThumbnail
import com.example.homeworkout.ui.components.SectionHeader
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.components.buttons.AppButtonVariant
import com.example.homeworkout.ui.components.label
import com.example.homeworkout.ui.theme.AppGradients
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.BrandBlueTint
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.PillShape
import com.example.homeworkout.ui.theme.SlateGray
import com.example.homeworkout.ui.theme.TileShape
import com.example.homeworkout.utils.ScreenWrapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenPlan: (Long) -> Unit,
    onOpenCustomWorkout: () -> Unit,
    onOpenEditGoal: () -> Unit,
    onOpenWorkoutList: (WorkoutCategory) -> Unit,
    onOpenOnboarding: () -> Unit,
    onResumeWorkout: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val challenge by viewModel.challenge.collectAsStateWithLifecycle()
    val weeklyGoalProgress by viewModel.weeklyGoalProgress.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()

    ScreenWrapper {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { HomeHeader(streakCount = streak) }

            item {
                AppTextField(value = "", onValueChange = {}, placeholderText = "Search workouts, plans...")
            }

            item { WeeklyGoalCard(progress = weeklyGoalProgress, onEditGoal = onOpenEditGoal) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "Challenge")
                    ChallengePanel(
                        state = challenge,
                        onOpenPlan = onOpenPlan,
                        onOpenOnboarding = onOpenOnboarding
                    )
                }
            }

            activeWorkout?.let { active ->
                item { ContinueWorkoutCard(active = active, onResume = { onResumeWorkout(active.planId) }) }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Body Focus",
                        actionText = selectedCategory?.let { "See all" },
                        onActionClick = { selectedCategory?.let(onOpenWorkoutList) }
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            BodyFocusChip(label = "All", selected = selectedCategory == null, onClick = { viewModel.selectCategory(null) })
                        }
                        items(WorkoutCategory.entries) { category ->
                            BodyFocusChip(
                                label = category.label(),
                                selected = selectedCategory == category,
                                onClick = { viewModel.selectCategory(category) }
                            )
                        }
                    }
                }
            }

            when (val state = uiState) {
                is HomeUiState.Loading -> item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is HomeUiState.Error -> item {
                    Text("Couldn't load workouts: ${state.message}", color = MaterialTheme.colorScheme.error)
                }

                is HomeUiState.Success -> {
                    if (state.workouts.isEmpty()) {
                        item { Text("No workouts in this category yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(state.workouts, key = { it.id }) { plan ->
                            PlanCard(plan = plan, onClick = { onOpenPlan(plan.id) })
                        }
                    }
                }
            }

            item { CreateYourOwnCard(onClick = onOpenCustomWorkout) }
        }
    }
}

@Composable
private fun HomeHeader(streakCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("HOME WORKOUT", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                Icons.Default.LocalFireDepartment,
                contentDescription = "Streak",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(26.dp)
            )
            if (streakCount > 0) {
                Text(
                    "$streakCount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ProTag() {
    Box(modifier = Modifier.clip(PillShape).background(AppGradients.ProBadge).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text("PRO", style = MaterialTheme.typography.labelSmall, color = InkBlack, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CoachTipBubble(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TileShape)
            .background(CloudGray)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(BrandBlueTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(22.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = InkBlack)
    }
}

@Composable
private fun ChallengePanel(
    state: ChallengeState,
    onOpenPlan: (Long) -> Unit,
    onOpenOnboarding: () -> Unit
) {
    when (state) {
        is ChallengeState.Loading -> AppCard(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        is ChallengeState.NeedsProfile -> PromoCard(onClick = onOpenOnboarding) {
            PromoTag("CUSTOMIZED FOR YOU")
            Text(
                "Find your plan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                "Answer a few questions and we'll recommend a plan for your goal, level and schedule.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
            )
            AppButton(
                text = "Get started",
                onClick = onOpenOnboarding,
                variant = AppButtonVariant.OnAccent,
                modifier = Modifier.fillMaxWidth()
            )
        }

        is ChallengeState.Recommended -> ChallengeCarousel(plans = state.plans, onOpenPlan = onOpenPlan)
    }
}

/**
 * Horizontally swipeable row of the recommended plan plus its alternatives (best match first),
 * so the "Challenge" panel is never limited to showing only the single top match. Only ever a
 * couple of cards (best match + up to two alternatives), so a plain scrollable Row — not a lazy
 * one — is used deliberately: it lets `IntrinsicSize.Max` stretch every card to the height of the
 * tallest one, so cards with shorter titles/rationale don't end up a different size.
 */
@Composable
private fun ChallengeCarousel(plans: List<RecommendedPlan>, onOpenPlan: (Long) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = maxWidth * 0.92f
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .height(IntrinsicSize.Max)
                .padding(end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            plans.forEachIndexed { index, item ->
                ChallengeCard(
                    item = item,
                    isBestMatch = index == 0,
                    onClick = { onOpenPlan(item.plan.id) },
                    modifier = Modifier.width(cardWidth).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun ChallengeCard(
    item: RecommendedPlan,
    isBestMatch: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PromoCard(onClick = onClick, modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            PromoTag(if (isBestMatch) "CUSTOMIZED FOR YOU" else "${(item.score * 100).toInt()}% MATCH")
            if (item.plan.requiresPremium) ProTag()
        }
        Text(
            item.plan.title.uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
        )
        Text(
            item.rationale,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                PromoStat(Icons.Default.Schedule, "${item.plan.totalDays} day(s)", "Duration", Modifier.weight(1f))
                PromoStat(Icons.Default.BarChart, item.plan.level.label(), "Level", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                PromoStat(Icons.Default.Adjust, item.plan.category.label(), "Focus", Modifier.weight(1f))
                PromoStat(Icons.Default.FitnessCenter, "${item.plan.totalExercises}", "Exercises", Modifier.weight(1f))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(Color.White)
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("START", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandBlue)
            Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(BrandBlue), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }
    }
}

/**
 * Shown above Body Focus whenever [HomeViewModel.activeWorkout] is non-null, so an in-progress or
 * paused workout is never buried under the rest of the Home feed — one tap resumes it exactly
 * where it was left off (see [com.example.homeworkout.domain.usecases.home.GetActiveWorkoutUseCase]).
 */
@Composable
private fun ContinueWorkoutCard(active: ActiveWorkoutSummary, onResume: () -> Unit) {
    val progress = if (active.totalExercises > 0) {
        (active.completedExercises.toFloat() / active.totalExercises).coerceIn(0f, 1f)
    } else 0f

    AppCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onResume),
        containerColor = BrandBlueTint
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            PlanThumbnail(planId = active.planId, coverImageUrl = active.coverImageUrl, size = 56.dp)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "CONTINUE WORKOUT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue
                )
                Text(
                    active.planTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val dayLabel = if (active.totalDays > 1) "Day ${active.dayNumber} · " else ""
                Text(
                    "$dayLabel${active.completedExercises}/${active.totalExercises} exercises done",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGray
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(PillShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White
                )
            }
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onResume),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Resume workout", tint = Color.White)
            }
        }
    }
}

/** Real weekly-goal progress (from [HomeViewModel.weeklyGoalProgress]), rendered in the same
 * card/pill/coach-tip visual language as the rest of this redesigned Home screen. */
@Composable
private fun WeeklyGoalCard(progress: WeeklyGoalProgress, onEditGoal: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Weekly Goal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${progress.completedDays}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text("/${progress.goalDays}", style = MaterialTheme.typography.titleMedium, color = SlateGray)
                    IconButton(onClick = onEditGoal, modifier = Modifier.size(30.dp).padding(start = 6.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit weekly goal", tint = SlateGray, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (progress.days.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    progress.days.forEach { day -> WeeklyGoalDayPill(day) }
                }
            }

            CoachTipBubble(weeklyGoalMessage(progress))
        }
    }
}

@Composable
private fun WeeklyGoalDayPill(day: WeeklyGoalDay) {
    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
        when {
            day.isCompleted -> Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = "Completed", tint = Color.White, modifier = Modifier.size(18.dp))
            }

            day.isToday -> Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${day.dayOfMonth}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            else -> Text(
                "${day.dayOfMonth}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = SlateGray.copy(alpha = 0.6f)
            )
        }
    }
}

private fun weeklyGoalMessage(progress: WeeklyGoalProgress): String = when {
    progress.completedDays >= progress.goalDays -> "Goal complete! Great work this week."
    progress.completedDays == 0 -> "Let's get started this week!"
    else -> "You're doing great! Don't forget to come here tomorrow."
}

@Composable
private fun PromoCard(onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(com.example.homeworkout.ui.theme.CardShape)
            .background(AppGradients.PromoCard)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.12f),
            modifier = Modifier
                .size(170.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 30.dp, y = 30.dp)
        )
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
private fun PromoTag(text: String) {
    Box(modifier = Modifier.clip(PillShape).background(Color.White.copy(alpha = 0.22f)).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PromoStat(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.size(34.dp).clip(TileShape).background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(value, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(label, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyFocusChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        shape = PillShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = CloudGray,
            labelColor = SlateGray,
            selectedContainerColor = BrandBlueTint,
            selectedLabelColor = BrandBlue
        )
    )
}

@Composable
private fun PlanCard(plan: WorkoutModel, onClick: () -> Unit) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlanThumbnail(planId = plan.id, coverImageUrl = plan.coverImageUrl, size = 56.dp)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(plan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (plan.requiresPremium) ProTag()
                }
                Text(
                    "${plan.level.label()} · ${plan.totalDays} day(s) · ${plan.totalExercises} exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = SlateGray)
        }
    }
}

@Composable
private fun CreateYourOwnCard(onClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(TileShape).background(BrandBlueTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = BrandBlue)
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text("Create your own", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Build a custom plan from any exercise",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AppButton(text = "Go", onClick = onClick)
        }
    }
}
