package com.example.homeworkout.ui.core.planedit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.ui.components.BackTopBar

/** "Add Exercises": browse and add exercises from the real library to the current plan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExercisesScreen(
    viewModel: ExerciseBrowserViewModel,
    onNavigateBack: () -> Unit,
    onExerciseInfo: (Long) -> Unit,
    onAddExercises: (List<Long>) -> Unit
) {
    val selectedIds by viewModel.selectedExerciseIds.collectAsStateWithLifecycle()
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BackTopBar(title = "Add Exercises", onNavigateBack = onNavigateBack) {
                IconButton(onClick = { showFilterSheet = true }) { Icon(Icons.Default.FilterList, contentDescription = "Filter") }
            }
        },
        bottomBar = {
            if (selectedIds.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onAddExercises(selectedIds.toList()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp)
                    ) {
                        Text("Add (${selectedIds.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        ExerciseBrowserContent(
            viewModel = viewModel,
            topPadding = padding.calculateTopPadding(),
            onExerciseInfo = onExerciseInfo,
            isActioned = { it in selectedIds },
            onActionClick = { viewModel.toggleSelection(it) },
            onOpenFilter = { showFilterSheet = true }
        )

        if (showFilterSheet) {
            FilterExerciseSheetContent(
                viewModel = viewModel,
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}
