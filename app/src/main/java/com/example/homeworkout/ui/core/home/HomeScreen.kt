package com.example.homeworkout.ui.core.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.AppTextField
import com.example.homeworkout.ui.components.ExerciseThumbnail
import com.example.homeworkout.ui.components.SectionHeader
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.components.label
import com.example.homeworkout.utils.ScreenWrapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenPlan: (Long) -> Unit,
    onOpenCustomWorkout: () -> Unit,
    onOpenEditGoal: () -> Unit,
    onOpenWorkoutList: (WorkoutCategory) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    ScreenWrapper {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("HOME WORKOUT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = "Streak", tint = MaterialTheme.colorScheme.secondary)
                }
            }

            item {
                AppTextField(value = "", onValueChange = {}, placeholderText = "Search workouts, plans...")
            }

            item { WeeklyGoalCard(onEditGoal = onOpenEditGoal) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(
                        title = "Body Focus",
                        actionText = selectedCategory?.let { "See all" },
                        onActionClick = { selectedCategory?.let(onOpenWorkoutList) }
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { viewModel.selectCategory(null) },
                                label = { Text("All") }
                            )
                        }
                        items(WorkoutCategory.entries) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { viewModel.selectCategory(category) },
                                label = { Text(category.label()) }
                            )
                        }
                    }
                }
            }

            when (val state = uiState) {
                is HomeUiState.Loading -> item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
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

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("Create your own", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        AppButton(text = "Go", onClick = onOpenCustomWorkout)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyGoalCard(onEditGoal: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Weekly Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("1/6", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onEditGoal) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit weekly goal")
                    }
                }
            }
            Text(
                "You're doing great! Don't forget to come here tomorrow.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
            ExerciseThumbnail(size = 56.dp)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(plan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${plan.level.label()} · ${plan.totalDays} day(s) · ${plan.totalExercises} exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
