package com.example.homeworkout.ui.core.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.ExperienceLevel
import com.example.homeworkout.domain.models.PrimaryGoal
import com.example.homeworkout.domain.models.RecommendedPlan
import com.example.homeworkout.domain.models.enums.ExerciseCategory
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.label
import com.example.homeworkout.ui.components.buttons.AppButton

private val EQUIPMENT_OPTIONS =
    listOf("bodyweight", "bands", "dumbbell", "kettlebells", "exercise ball", "foam roll", "other")

private val MUSCLE_OPTIONS = listOf(
    "abdominals", "abductors", "adductors", "biceps", "calves", "chest", "forearms", "glutes",
    "hamstrings", "lats", "lower back", "middle back", "neck", "quadriceps", "shoulders", "traps", "triceps"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onNavigateBack: () -> Unit,
    onOpenPlan: (Long) -> Unit
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val computing by viewModel.computing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            BackTopBar(
                title = if (result == null) "Find your plan" else "Recommended plan",
                onNavigateBack = { if (result != null) viewModel.backToForm() else onNavigateBack() }
            )
        }
    ) { padding ->
        val currentResult = result
        if (currentResult != null) {
            RecommendationList(
                recommended = currentResult.recommended,
                alternatives = currentResult.alternatives,
                contentPadding = padding,
                onOpenPlan = onOpenPlan,
                onAdjust = viewModel::backToForm
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, bottom = 32.dp,
                    top = padding.calculateTopPadding() + 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Section("What's your main goal?") {
                        PrimaryGoal.entries.forEach { goal ->
                            Choice(goal.label, form.goal == goal) { viewModel.setGoal(goal) }
                        }
                    }
                }
                item {
                    Section("Training experience") {
                        ExperienceLevel.entries.forEach { level ->
                            Choice(level.label, form.level == level) { viewModel.setLevel(level) }
                        }
                    }
                }
                item {
                    Section("How many days per week?") {
                        (2..6).forEach { days ->
                            Choice("$days", form.daysPerWeek == days) { viewModel.setDays(days) }
                        }
                    }
                }
                item {
                    Section("How long is each session?") {
                        listOf(15, 20, 30, 45, 60).forEach { minutes ->
                            Choice("$minutes min", form.sessionMinutes == minutes) { viewModel.setMinutes(minutes) }
                        }
                    }
                }
                item {
                    Section("What equipment do you have?") {
                        EQUIPMENT_OPTIONS.forEach { item ->
                            Choice(item.replaceFirstChar { it.uppercase() }, item in form.equipment) {
                                viewModel.toggleEquipment(item)
                            }
                        }
                    }
                }
                item {
                    Section("Focus areas (optional)") {
                        ExerciseCategory.entries.forEach { category ->
                            Choice(category.label(), category in form.focusCategories) {
                                viewModel.toggleFocusCategory(category)
                            }
                        }
                    }
                }
                item {
                    Section("Target muscles (optional)") {
                        MUSCLE_OPTIONS.forEach { muscle ->
                            Choice(muscle.replaceFirstChar { it.uppercase() }, muscle in form.focusMuscles) {
                                viewModel.toggleFocusMuscle(muscle)
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Injuries, pain or limitations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = form.limitations,
                            onValueChange = viewModel::setLimitations,
                            placeholder = { Text("e.g. sore left knee, avoid overhead") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
                if (error != null) {
                    item { Text(error!!, color = MaterialTheme.colorScheme.error) }
                }
                item {
                    AppButton(
                        text = if (computing) "Finding your plan..." else "Find my plan",
                        onClick = viewModel::submit,
                        enabled = form.canSubmit && !computing,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun RecommendationList(
    recommended: RecommendedPlan,
    alternatives: List<RecommendedPlan>,
    contentPadding: PaddingValues,
    onOpenPlan: (Long) -> Unit,
    onAdjust: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, bottom = 32.dp,
            top = contentPadding.calculateTopPadding() + 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Best match for you", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { PlanMatchCard(recommended, highlighted = true, onClick = { onOpenPlan(recommended.plan.id) }) }
        if (alternatives.isNotEmpty()) {
            item { Text("Other options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(alternatives) { alt ->
                PlanMatchCard(alt, highlighted = false, onClick = { onOpenPlan(alt.plan.id) })
            }
        }
        item {
            AppButton(text = "Adjust my answers", onClick = onAdjust, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PlanMatchCard(item: RecommendedPlan, highlighted: Boolean, onClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.plan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${(item.score * 100).toInt()}% match · ${item.rationale}",
                style = MaterialTheme.typography.bodySmall,
                color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            item.plan.description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
