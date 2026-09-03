package com.example.homeworkout.ui.core.customworkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.models.enums.WorkoutLevel
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ExerciseRow
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.components.label
import com.example.homeworkout.ui.core.planedit.AddExercisesScreen
import com.example.homeworkout.ui.core.planedit.ExerciseBrowserViewModel
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.BrandBlueTint
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.PillShape
import com.example.homeworkout.ui.theme.SlateGray

/**
 * "Create Workout": title/category/level, then one or more days each with its own exercise list —
 * added via the same Add Exercises browser used to edit a real plan, shown here as an in-screen
 * overlay (no navigation) so [CreateCustomPlanViewModel]'s in-memory draft survives the round
 * trip. Nothing reaches the database until "Create Plan" is tapped.
 */
@Composable
fun CreateCustomPlanScreen(
    viewModel: CreateCustomPlanViewModel,
    exerciseBrowserViewModel: ExerciseBrowserViewModel,
    onNavigateBack: () -> Unit,
    onExerciseInfo: (Long) -> Unit,
    onPlanCreated: (Long) -> Unit
) {
    var pickerForDay by remember { mutableStateOf<Int?>(null) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    val dayLocalId = pickerForDay

    if (dayLocalId != null) {
        AddExercisesScreen(
            viewModel = exerciseBrowserViewModel,
            onNavigateBack = { pickerForDay = null },
            onExerciseInfo = onExerciseInfo,
            onAddExercises = { exerciseIds ->
                viewModel.addExercisesToDay(dayLocalId, exerciseIds)
                exerciseBrowserViewModel.clearSelection()
                pickerForDay = null
            }
        )
        return
    }

    val form by viewModel.form.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val importingTemplate by viewModel.isImportingTemplate.collectAsStateWithLifecycle()
    val saving = saveState is SaveState.Saving

    if (showTemplatePicker) {
        AlertDialog(
            onDismissRequest = { if (!importingTemplate) showTemplatePicker = false },
            title = { Text("Choose a template") },
            text = {
                if (templates.isEmpty()) {
                    Text("No plan templates are available.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(templates, key = { it.id }) { template ->
                            AppCard(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = !importingTemplate) {
                                    viewModel.importTemplate(template.id)
                                    showTemplatePicker = false
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(template.title, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${template.totalDays} days · ${template.totalExercises} exercises · ${template.level.label()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Clone template")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTemplatePicker = false }) { Text("Cancel") } }
        )
    }

    LaunchedEffect(saveState) {
        (saveState as? SaveState.Saved)?.let { onPlanCreated(it.planId) }
    }

    Scaffold(topBar = { BackTopBar(title = "Create Workout", onNavigateBack = onNavigateBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 24.dp, top = padding.calculateTopPadding() + 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = BrandBlue)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Start from a template", fontWeight = FontWeight.Bold)
                            Text(
                                "Clone all days and exercises, then customize your own copy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (importingTemplate) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            TextButton(onClick = { showTemplatePicker = true }) { Text("Choose") }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = form.title,
                    onValueChange = viewModel::setTitle,
                    label = { Text("Plan name") },
                    placeholder = { Text("e.g. My Push Pull Legs") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = HairlineGray
                    )
                )
            }
            item {
                OutlinedTextField(
                    value = form.description,
                    onValueChange = viewModel::setDescription,
                    label = { Text("Description (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = HairlineGray
                    )
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(WorkoutCategory.entries) { cat ->
                            FilterChip(
                                selected = form.category == cat,
                                onClick = { viewModel.setCategory(cat) },
                                label = { Text(cat.label()) },
                                shape = PillShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = CloudGray,
                                    labelColor = SlateGray,
                                    selectedContainerColor = BrandBlueTint,
                                    selectedLabelColor = BrandBlue
                                )
                            )
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Level", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(WorkoutLevel.entries) { lvl ->
                            FilterChip(
                                selected = form.level == lvl,
                                onClick = { viewModel.setLevel(lvl) },
                                label = { Text(lvl.label()) },
                                shape = PillShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = CloudGray,
                                    labelColor = SlateGray,
                                    selectedContainerColor = BrandBlueTint,
                                    selectedLabelColor = BrandBlue
                                )
                            )
                        }
                    }
                }
            }
            item {
                Text("Days", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            itemsIndexed(form.days, key = { _, day -> day.localId }) { index, day ->
                DayBuilderCard(
                    day = day,
                    dayNumber = index + 1,
                    canRemove = form.days.size > 1,
                    onTitleChange = { viewModel.setDayTitle(day.localId, it) },
                    onRemoveDay = { viewModel.removeDay(day.localId) },
                    onAddExercises = {
                        exerciseBrowserViewModel.clearSelection()
                        pickerForDay = day.localId
                    },
                    onRemoveExercise = { exerciseId -> viewModel.removeExercise(day.localId, exerciseId) },
                    onRepsChange = { exerciseId, reps -> viewModel.updateReps(day.localId, exerciseId, reps) },
                    onDurationChange = { exerciseId, seconds -> viewModel.updateDuration(day.localId, exerciseId, seconds) }
                )
            }
            item {
                TextButton(onClick = viewModel::addDay) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Add Day", modifier = Modifier.padding(start = 4.dp))
                }
            }
            item {
                AppButton(
                    text = if (saving) "Creating..." else "Create Plan",
                    onClick = viewModel::save,
                    enabled = form.canSave && !saving,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DayBuilderCard(
    day: DraftDay,
    dayNumber: Int,
    canRemove: Boolean,
    onTitleChange: (String) -> Unit,
    onRemoveDay: () -> Unit,
    onAddExercises: () -> Unit,
    onRemoveExercise: (Long) -> Unit,
    onRepsChange: (Long, Int) -> Unit,
    onDurationChange: (Long, Int) -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DAY $dayNumber", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (canRemove) {
                    IconButton(onClick = onRemoveDay, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove day", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            OutlinedTextField(
                value = day.title,
                onValueChange = onTitleChange,
                placeholder = { Text("Day title (optional), e.g. Push Day") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = HairlineGray
                )
            )
            if (day.exercises.isEmpty()) {
                Text(
                    "No exercises yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column {
                    day.exercises.forEachIndexed { index, exercise ->
                        ExerciseRow(
                            title = exercise.title,
                            subtitle = exercise.targetDurationSec?.let { "$it sec" } ?: "x${exercise.targetReps ?: 10}",
                            imageUrl = exercise.imageUrl,
                            showDivider = index != day.exercises.lastIndex
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalIconButton(
                                    onClick = {
                                        val duration = exercise.targetDurationSec
                                        if (duration != null) {
                                            if (duration > 5) onDurationChange(exercise.exerciseId, duration - 5)
                                        } else {
                                            val reps = exercise.targetReps ?: 10
                                            if (reps > 1) onRepsChange(exercise.exerciseId, reps - 1)
                                        }
                                    },
                                    modifier = Modifier.size(28.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = CloudGray,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) { Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp)) }
                                Text(
                                    exercise.targetDurationSec?.let { "${it}s" } ?: "${exercise.targetReps ?: 10}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                FilledTonalIconButton(
                                    onClick = {
                                        val duration = exercise.targetDurationSec
                                        if (duration != null) onDurationChange(exercise.exerciseId, duration + 5)
                                        else onRepsChange(exercise.exerciseId, (exercise.targetReps ?: 10) + 1)
                                    },
                                    modifier = Modifier.size(28.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = CloudGray,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) { Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp)) }
                                IconButton(onClick = { onRemoveExercise(exercise.exerciseId) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove exercise", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            TextButton(onClick = onAddExercises) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Add Exercises", modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}
