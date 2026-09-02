package com.example.homeworkout.ui.core.exerciseinfo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.example.homeworkout.domain.models.ExerciseDetail
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.PillShape
import com.example.homeworkout.ui.theme.TileShape
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
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

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
            top = contentPadding.calculateTopPadding() + 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            // Prefer the animated demo GIF as the hero; fall back to the first reference photo,
            // then to the plain icon tile when the exercise has no artwork at all. Any photos not
            // used as the hero show underneath as a small strip.
            val heroFromGif = !detail.exercise.gifUrl.isNullOrBlank()
            val heroUrl = detail.exercise.gifUrl?.takeIf { it.isNotBlank() } ?: detail.imageUrls.firstOrNull()
            val strip = if (heroFromGif) detail.imageUrls else detail.imageUrls.drop(1)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExerciseHero(imageUrl = heroUrl)
                if (strip.isNotEmpty()) {
                    ImageStrip(urls = strip)
                }
            }
        }
        item { Text(detail.exercise.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold) }

        item {
            InfoSection(label = "DURATION") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Adjustable from the plan editor", style = MaterialTheme.typography.bodyMedium)
                    Text("30s", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            InfoSection(label = "INSTRUCTIONS") {
                if (detail.instructions.isEmpty()) {
                    Text("No instructions provided for this exercise.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        detail.instructions.forEach { step ->
                            Text(step, style = MaterialTheme.typography.bodyMedium, color = InkBlack)
                        }
                    }
                }
            }
        }

        item {
            InfoSection(label = "FOCUS AREA") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (detail.primaryMuscles + detail.secondaryMuscles).distinct().forEach { muscle ->
                        MuscleChip(muscle.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }

        item { AppButton(text = "Close", onClick = onClose, modifier = Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun ExerciseHero(imageUrl: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(TileShape)
            .background(CloudGray),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNullOrBlank()) {
            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(72.dp))
        } else {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandBlue)
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(72.dp))
                    }
                }
            )
        }
    }
}

@Composable
private fun ImageStrip(urls: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        urls.forEach { url ->
            SubcomposeAsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.size(72.dp).clip(TileShape).background(CloudGray),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = BrandBlue)
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(28.dp))
                    }
                }
            )
        }
    }
}

@Composable
private fun InfoSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun MuscleChip(text: String) {
    Row(
        modifier = Modifier.clip(PillShape).background(CloudGray).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BrandBlue))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = InkBlack)
    }
}
