package com.example.homeworkout.ui.core.exerciseinfo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.example.homeworkout.domain.models.ExerciseDetail
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
        Scaffold(
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AppButton(
                            text = "CLOSE",
                            onClick = onClose,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        )
                    }
                }
            }
        ) { padding ->
            when (val state = uiState) {
                is ExerciseInfoUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = BrandBlue) }

                is ExerciseInfoUiState.Error -> Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text(state.message, color = MaterialTheme.colorScheme.error) }

                is ExerciseInfoUiState.Success -> ExerciseInfoContent(state.detail, padding)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseInfoContent(detail: ExerciseDetail, contentPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 32.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Title
        item {
            Text(
                text = detail.exercise.title.uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Hero Image placeholder
        item {
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

        // Repeats / Duration
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader("DURATION")
                Text(
                    text = "30s", // Placeholder for actual duration
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Instructions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionHeader("INSTRUCTIONS")
                if (detail.instructions.isEmpty()) {
                    Text(
                        text = "No instructions provided for this exercise.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    detail.instructions.forEachIndexed { index, step ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFFE8F0FE), CircleShape), // Light blue background for badge
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandBlue, // Use BrandBlue for number
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f
                            )
                        }
                    }
                }
            }
        }

        // Focus Area
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionHeader("FOCUS AREA")
                
                val allMuscles = (detail.primaryMuscles + detail.secondaryMuscles).distinct()
                if (allMuscles.isEmpty()) {
                    Text("Full Body", style = MaterialTheme.typography.bodyLarge)
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        allMuscles.forEach { muscle ->
                            MuscleChip(muscle.replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }
        }
        
        // Add a bit of spacing at the bottom
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = BrandBlue
    )
}

@Composable
private fun ExerciseHero(imageUrl: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp) // Updated to match the requested layout size
            .clip(RoundedCornerShape(16.dp)) // Modern rounding
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
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).background(CloudGray),
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
private fun MuscleChip(text: String) {
    Row(
        modifier = Modifier.clip(PillShape).background(CloudGray).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Circle,
            contentDescription = null,
            tint = BrandBlue,
            modifier = Modifier.size(10.dp)
        )
        Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = InkBlack)
    }
}
