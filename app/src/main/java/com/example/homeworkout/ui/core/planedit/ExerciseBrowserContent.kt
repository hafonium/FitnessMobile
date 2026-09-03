package com.example.homeworkout.ui.core.planedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseBrowserContent(
    viewModel: ExerciseBrowserViewModel,
    topPadding: Dp,
    onExerciseInfo: (Long) -> Unit,
    isActioned: (Long) -> Boolean,
    onActionClick: (Long) -> Unit,
    onOpenFilter: () -> Unit = {}
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.category.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()

    val activeFilters = remember(filterState) {
        val list = mutableListOf<String>()
        list.addAll(filterState.bodyParts)
        filterState.difficulty?.let { list.add(it) }
        list.addAll(filterState.types)
        list.addAll(filterState.equipment)
        list
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = topPadding + 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (activeFilters.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onOpenFilter,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Filtered (${exercises.size})")
                    }
                    TextButton(onClick = { viewModel.setFilterState(FilterState()) }) {
                        Text("Clear")
                    }
                }
            }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeFilters.forEach { filterItem ->
                        InputChip(
                            selected = true,
                            onClick = {
                                val newState = filterState.copy(
                                    bodyParts = filterState.bodyParts - filterItem,
                                    difficulty = if (filterState.difficulty == filterItem) null else filterState.difficulty,
                                    types = filterState.types - filterItem,
                                    equipment = filterState.equipment - filterItem
                                )
                                viewModel.setFilterState(newState)
                            },
                            label = { Text(filterItem) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = "Remove $filterItem")
                            }
                        )
                    }
                }
            }
        }

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
                val added = isActioned(exercise.id)
                ExerciseRow(
                    title = exercise.title,
                    subtitle = "${exercise.equipmentName} · ${exercise.level.name.lowercase()}",
                    onClick = { onExerciseInfo(exercise.id) }
                ) {
                    IconButton(onClick = { onActionClick(exercise.id) }) {
                        Icon(
                            imageVector = if (added) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = if (added) "Selected" else "Select",
                            tint = if (added) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
