package com.example.homeworkout.ui.core.exerciseinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.homeworkout.domain.models.ExerciseDetail
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ExerciseThumbnail
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.utils.ScreenWrapper

@Composable
fun ExerciseInfoScreen(
    viewModel: ExerciseInfoViewModel,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenWrapper {
        Scaffold(topBar = { BackTopBar(title = "Exercise Information", onNavigateBack = onClose) }) { padding ->
            when (val state = uiState) {
                is ExerciseInfoUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is ExerciseInfoUiState.Error -> Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text(state.message, color = MaterialTheme.colorScheme.error) }

                is ExerciseInfoUiState.Success -> ExerciseInfoContent(state.detail, padding, onClose)
            }
        }
    }
}

@Composable
private fun ExerciseInfoContent(detail: ExerciseDetail, contentPadding: PaddingValues, onClose: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, bottom = 24.dp,
            top = contentPadding.calculateTopPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ExerciseThumbnail(size = 96.dp, modifier = Modifier.fillMaxWidth().height(160.dp)) }
        item { Text(detail.exercise.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("DURATION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("30s (adjust from the plan editor)", style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("INSTRUCTIONS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                if (detail.instructions.isEmpty()) {
                    Text("No instructions provided for this exercise.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    detail.instructions.forEachIndexed { index, step ->
                        Text("${index + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("FOCUS AREA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (detail.primaryMuscles + detail.secondaryMuscles).distinct().forEach { muscle ->
                        AssistChip(onClick = {}, label = { Text(muscle.replaceFirstChar { it.uppercase() }) })
                    }
                }
            }
        }

        item { AppButton(text = "Close", onClick = onClose, modifier = Modifier.fillMaxWidth()) }
    }
}
