package com.example.homeworkout.ui.core.workoutlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ExerciseThumbnail
import com.example.homeworkout.ui.components.label

/** Category Workout List (e.g. "Build Muscle"), backed by the real seeded plans for that category. */
@Composable
fun WorkoutListScreen(
    viewModel: WorkoutListViewModel,
    onNavigateBack: () -> Unit,
    onOpenPlan: (Long) -> Unit
) {
    val workouts by viewModel.workouts.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(title = viewModel.category.label(), onNavigateBack = onNavigateBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = padding.calculateTopPadding() + 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (workouts.isEmpty()) {
                item { Text("No plans in this category yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(workouts, key = { it.id }) { plan ->
                AppCard(modifier = Modifier.fillMaxWidth().clickable { onOpenPlan(plan.id) }) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ExerciseThumbnail(size = 56.dp)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(plan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${plan.level.label()} · ${plan.totalExercises} exercises",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
