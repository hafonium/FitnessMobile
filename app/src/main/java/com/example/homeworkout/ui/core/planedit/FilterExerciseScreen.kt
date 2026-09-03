package com.example.homeworkout.ui.core.planedit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.R
import com.example.homeworkout.ui.components.buttons.AppButton

private val bodyParts = listOf("Abs", "Chest", "Back", "Arm", "Leg", "Glutes", "Shoulder")
private val difficulties = listOf("Easy", "Medium", "Hard")
private val types = listOf("Warm up", "Stretch", "Training")
private val equipmentOptions = listOf("No equipment", "Dumbbell", "Band", "Chair", "Bench", "Mat")

/**
 * Filter static bottom popup content.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterExerciseSheetContent(viewModel: ExerciseBrowserViewModel, onDismiss: () -> Unit) {
    val currentState by viewModel.filterState.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    
    var selectedBodyParts by remember { mutableStateOf(currentState.bodyParts) }
    var selectedDifficulty by remember { mutableStateOf(currentState.difficulty) }
    var selectedTypes by remember { mutableStateOf(currentState.types) }
    var selectedEquipment by remember { mutableStateOf(currentState.equipment) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume clicks inside the content area
                    )
                    .padding(top = 24.dp), // allow some spacing from the very top of the screen if content is tall
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("All Exercises", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                selectedBodyParts = emptySet()
                                selectedDifficulty = null
                                selectedTypes = emptySet()
                                selectedEquipment = emptySet()
                            }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Clear Filters", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("${exercises.size} exercises", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Focus Area", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                bodyParts.chunked(2).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowItems.forEach { option ->
                                            val iconResId = when (option) {
                                                "Abs" -> R.drawable.ic_muscle_abs
                                                "Chest" -> R.drawable.ic_muscle_chest
                                                "Back" -> R.drawable.ic_muscle_back
                                                "Arm" -> R.drawable.ic_muscle_arm
                                                "Leg" -> R.drawable.ic_muscle_leg
                                                "Glutes" -> R.drawable.ic_muscle_glutes
                                                "Shoulder" -> R.drawable.ic_muscle_shoulder
                                                else -> R.drawable.ic_muscle_abs
                                            }
                                            
                                            val isSelected = option in selectedBodyParts
                                            
                                            Surface(
                                                onClick = {
                                                    selectedBodyParts = if (isSelected) selectedBodyParts - option else selectedBodyParts + option
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(64.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = iconResId),
                                                        contentDescription = option,
                                                        tint = Color.Unspecified,
                                                        modifier = Modifier.size(40.dp)
                                                    )
                                                    Text(option, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        if (rowItems.size == 1) {
                                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                        FilterSection(title = "Difficulty", options = difficulties, isSelected = { it == selectedDifficulty }) { option ->
                            selectedDifficulty = if (selectedDifficulty == option) null else option
                        }
                        FilterSection(title = "Type", options = types, isSelected = { it in selectedTypes }) { option ->
                            selectedTypes = if (option in selectedTypes) selectedTypes - option else selectedTypes + option
                        }
                        FilterSection(title = "Equipment", options = equipmentOptions, isSelected = { it in selectedEquipment }) { option ->
                            selectedEquipment = if (option in selectedEquipment) selectedEquipment - option else selectedEquipment + option
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppButton(
                                text = "Cancel",
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f).height(56.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            AppButton(text = "Save", onClick = {
                                viewModel.setFilterState(FilterState(
                                    bodyParts = selectedBodyParts,
                                    difficulty = selectedDifficulty,
                                    types = selectedTypes,
                                    equipment = selectedEquipment
                                ))
                                onDismiss()
                            }, modifier = Modifier.weight(1f).height(56.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    options: List<String>,
    isSelected: (String) -> Boolean,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { option ->
                val selected = isSelected(option)
                Surface(
                    onClick = { onToggle(option) },
                    shape = RoundedCornerShape(percent = 50),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
