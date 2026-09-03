package com.example.homeworkout.ui.core.planedit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
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
import com.example.homeworkout.ui.components.BackTopBar

/** "Replace it with...": browse the real library for a replacement exercise. */
@Composable
fun AlterExerciseScreen(
    viewModel: ExerciseBrowserViewModel,
    onNavigateBack: () -> Unit,
    onOpenFilter: () -> Unit,
    onExerciseInfo: (Long) -> Unit,
    onReplaceExercise: (Long) -> Unit
) {
    var selectedId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            BackTopBar(title = "Replace it with...", onNavigateBack = onNavigateBack) {
                IconButton(onClick = onOpenFilter) { Icon(Icons.Default.FilterList, contentDescription = "Filter") }
            }
        },
        bottomBar = {
            if (selectedId != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onReplaceExercise(selectedId!!) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp)
                    ) {
                        Text("Replace", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        ExerciseBrowserContent(
            viewModel = viewModel,
            topPadding = padding.calculateTopPadding(),
            onExerciseInfo = onExerciseInfo,
            isActioned = { it == selectedId },
            onActionClick = { selectedId = it }
        )
    }
}
