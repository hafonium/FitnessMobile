package com.example.homeworkout.ui.core.planedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.Exercise
import com.example.homeworkout.domain.models.enums.ExerciseCategory
import com.example.homeworkout.ui.components.AppTextField
import com.example.homeworkout.ui.components.ExerciseRow
import com.example.homeworkout.ui.components.label

/**
 * Shared search + category-chip + result-list content for the Add Exercises and Alter Workout
 * Exercise screens, backed by the real exercise library via [ExerciseBrowserViewModel].
 */
@Composable
fun ExerciseBrowserContent(
    viewModel: ExerciseBrowserViewModel,
    topPadding: Dp,
    onExerciseInfo: (Long) -> Unit
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.category.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    var addedIds by remember { mutableStateOf(setOf<Long>()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = topPadding + 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            AppTextField(value = query, onValueChange = viewModel::setQuery, placeholderText = "Search exercises")
        }
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    FilterChip(selected = selectedCategory == null, onClick = { viewModel.setCategory(null) }, label = { Text("All") })
                }
                items(ExerciseCategory.entries) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.setCategory(category) },
                        label = { Text(category.label()) }
                    )
                }
            }
        }
        if (exercises.isEmpty()) {
            item { Text("No exercises match this search.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(exercises, key = { it.id }) { exercise: Exercise ->
                val added = exercise.id in addedIds
                ExerciseRow(
                    title = exercise.title,
                    subtitle = "${exercise.equipmentName} · ${exercise.level.name.lowercase()}",
                    onClick = { onExerciseInfo(exercise.id) }
                ) {
                    IconButton(onClick = { addedIds = if (added) addedIds - exercise.id else addedIds + exercise.id }) {
                        Icon(
                            imageVector = if (added) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = if (added) "Added" else "Add",
                            tint = if (added) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
