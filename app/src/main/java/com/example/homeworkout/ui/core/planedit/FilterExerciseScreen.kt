package com.example.homeworkout.ui.core.planedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.components.buttons.AppButtonVariant
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.BrandBlueTint
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.PillShape
import com.example.homeworkout.ui.theme.SlateGray

private val bodyParts = listOf("Back", "Arm", "Leg", "Glutes", "Shoulder", "Chest", "Core")
private val difficulties = listOf("Easy", "Medium", "Hard")
private val types = listOf("Warm up", "Stretch", "Training")
private val equipmentOptions = listOf("No equipment", "Dumbbell", "Band", "Chair", "Bench", "Mat")

/**
 * "Filter Exercise": lets you narrow the exercise library by body part, difficulty, type and
 * equipment. Static — selections here are local to the screen and Cancel/Save both just close it.
 */
@Composable
fun FilterExerciseScreen(onNavigateBack: () -> Unit) {
    var selectedBodyParts by remember { mutableStateOf(setOf<String>()) }
    var selectedDifficulty by remember { mutableStateOf<String?>(null) }
    var selectedTypes by remember { mutableStateOf(setOf<String>()) }
    var selectedEquipment by remember { mutableStateOf(setOf<String>()) }

    Scaffold(topBar = { BackTopBar(title = "Filter", onNavigateBack = onNavigateBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = padding.calculateTopPadding() + 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                FilterSection(title = "Body Part", options = bodyParts, isSelected = { it in selectedBodyParts }) { option ->
                    selectedBodyParts = if (option in selectedBodyParts) selectedBodyParts - option else selectedBodyParts + option
                }
            }
            item {
                FilterSection(title = "Difficulty", options = difficulties, isSelected = { it == selectedDifficulty }) { option ->
                    selectedDifficulty = if (selectedDifficulty == option) null else option
                }
            }
            item {
                FilterSection(title = "Type", options = types, isSelected = { it in selectedTypes }) { option ->
                    selectedTypes = if (option in selectedTypes) selectedTypes - option else selectedTypes + option
                }
            }
            item {
                FilterSection(title = "Equipment", options = equipmentOptions, isSelected = { it in selectedEquipment }) { option ->
                    selectedEquipment = if (option in selectedEquipment) selectedEquipment - option else selectedEquipment + option
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppButton(
                        text = "Cancel",
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f),
                        variant = AppButtonVariant.Outlined
                    )
                    AppButton(
                        text = "Save",
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f),
                        variant = AppButtonVariant.Primary
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    options: List<String>,
    isSelected: (String) -> Boolean,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = isSelected(option),
                    onClick = { onToggle(option) },
                    label = { Text(option) },
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
