package com.example.homeworkout.ui.core.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.homeworkout.domain.models.ProgressionNode
import com.example.homeworkout.domain.models.enums.ProgressionNodeStatus
import com.example.homeworkout.ui.components.ExerciseThumbnail
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.theme.ProGoldEnd
import com.example.homeworkout.ui.theme.SlateGray

/**
 * Exercise detail bottom sheet shown when a progression node is tapped, mastered or not (Feature
 * Spec §4.C). [onOpenExercise] deep-links to the existing Exercise Information screen — the app
 * has no separate "quick timer" flow, so that screen (demo gif + form instructions) is the
 * closest real practice/re-test surface to send the user to rather than inventing a new one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressionNodeSheet(
    node: ProgressionNode,
    onOpenExercise: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ExerciseThumbnail(size = 64.dp, imageUrl = node.gifUrl)
                Column(modifier = Modifier.weight(1f)) {
                    Text(node.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    StatusChip(node.status)
                }
            }

            if (node.isPlaceholder) {
                Text(
                    "This variant isn't in your exercise library yet - you can still track its mastery target, but there's no demo or logged history for it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGray
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Target form", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                node.formTips.forEach { tip ->
                    Text("• $tip", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Unlock / mastery requirement", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(node.requirementLabel, style = MaterialTheme.typography.bodyMedium)
            }

            node.currentBestLabel?.let { best ->
                Text(best, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = SlateGray)
            }

            if (node.status == ProgressionNodeStatus.LOCKED) {
                Text(
                    "Master the previous node in this branch to unlock this one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGray
                )
            }

            node.exerciseId?.let { exerciseId ->
                val ctaLabel = when (node.status) {
                    ProgressionNodeStatus.MASTERED -> "View Mastery / Re-test"
                    else -> "Practice This Move"
                }
                AppButton(text = ctaLabel, onClick = { onOpenExercise(exerciseId) }, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatusChip(status: ProgressionNodeStatus) {
    val (icon, label, color) = when (status) {
        ProgressionNodeStatus.MASTERED -> Triple(Icons.Default.CheckCircle, "Mastered", ProGoldEnd)
        ProgressionNodeStatus.IN_PROGRESS -> Triple(null, "In progress", MaterialTheme.colorScheme.primary)
        ProgressionNodeStatus.LOCKED -> Triple(Icons.Default.Lock, "Locked", SlateGray)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        icon?.let { Icon(it, contentDescription = null, tint = color, modifier = Modifier.padding(top = 1.dp)) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
    }
}
