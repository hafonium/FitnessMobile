package com.example.homeworkout.ui.core.achievements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.BadgeProgress
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.BadgeDetailDialog
import com.example.homeworkout.ui.components.BadgeMedallion
import com.example.homeworkout.ui.components.progressLabel
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.SlateGray

@Composable
fun AchievementsScreen(
    viewModel: AchievementsViewModel,
    onNavigateBack: () -> Unit
) {
    val badges by viewModel.badges.collectAsStateWithLifecycle()
    var selectedBadge by remember { mutableStateOf<BadgeProgress?>(null) }

    Scaffold(
        topBar = { BackTopBar(title = "Achievements", onNavigateBack = onNavigateBack) }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${badges.count { it.isUnlocked }} / ${badges.size} unlocked",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Every completed workout moves you closer to the next milestone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateGray
                    )
                }
            }

            items(badges, key = { it.definition.id }) { badge ->
                AppCard(
                    modifier = Modifier.fillMaxWidth().clickable { selectedBadge = badge },
                    containerColor = if (badge.isUnlocked) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BadgeMedallion(badge = badge)
                        Text(
                            badge.definition.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            minLines = 2
                        )
                        LinearProgressIndicator(
                            progress = { badge.progressFraction },
                            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small),
                            trackColor = CloudGray
                        )
                        Text(
                            badge.progressLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    selectedBadge?.let { badge ->
        BadgeDetailDialog(badge = badge, onDismiss = { selectedBadge = null })
    }
}
