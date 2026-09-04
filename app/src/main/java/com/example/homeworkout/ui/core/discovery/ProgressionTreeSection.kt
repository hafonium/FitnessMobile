package com.example.homeworkout.ui.core.discovery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.homeworkout.domain.models.ProgressionNode
import com.example.homeworkout.domain.models.enums.ProgressionNodeStatus
import com.example.homeworkout.ui.components.ExerciseThumbnail
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.ProGoldEnd
import com.example.homeworkout.ui.theme.SlateGray

private val MasteredGold = ProGoldEnd

/** Vertical skill-tree path for one [com.example.homeworkout.domain.models.enums.ProgressionBranch]. */
@Composable
fun ProgressionTreeSection(
    nodes: List<ProgressionNode>,
    onNodeClick: (ProgressionNode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        nodes.forEachIndexed { index, node ->
            if (index > 0) {
                ConnectorLine(dashed = node.status == ProgressionNodeStatus.LOCKED)
            }
            ProgressionNodeRow(node = node, onClick = { onNodeClick(node) })
        }
    }
}

@Composable
private fun ConnectorLine(dashed: Boolean) {
    val color = if (dashed) HairlineGray else MasteredGold
    Box(modifier = Modifier.padding(start = 27.dp)) {
        Canvas(modifier = Modifier.size(width = 4.dp, height = 20.dp)) {
            val effect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) else null
            drawLine(
                color = color,
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = size.width,
                pathEffect = effect,
                cap = Stroke.DefaultCap
            )
        }
    }
}

@Composable
private fun ProgressionNodeRow(node: ProgressionNode, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NodeCircle(node)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                node.name,
                fontWeight = FontWeight.Bold,
                color = if (node.status == ProgressionNodeStatus.LOCKED) SlateGray else InkBlack
            )
            Text(
                node.statusLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = when (node.status) {
                    ProgressionNodeStatus.MASTERED -> MasteredGold
                    ProgressionNodeStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                    ProgressionNodeStatus.LOCKED -> SlateGray
                }
            )
        }
    }
}

@Composable
private fun NodeCircle(node: ProgressionNode) {
    val size = 48.dp
    when (node.status) {
        ProgressionNodeStatus.MASTERED -> Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MasteredGold.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(size - 6.dp)
                    .clip(CircleShape)
                    .background(MasteredGold),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = "Mastered", tint = Color.White)
            }
        }

        ProgressionNodeStatus.IN_PROGRESS -> Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            ExerciseThumbnail(size = size - 10.dp, imageUrl = node.gifUrl)
        }

        ProgressionNodeStatus.LOCKED -> Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(CloudGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = SlateGray, modifier = Modifier.size(20.dp))
        }
    }
}

private fun ProgressionNode.statusLabel(): String = when (status) {
    ProgressionNodeStatus.MASTERED -> "Mastered"
    ProgressionNodeStatus.LOCKED -> "Locked - master the previous node first"
    ProgressionNodeStatus.IN_PROGRESS -> when {
        targetReps != null -> "${bestReps ?: 0}/$targetReps reps  ($completionsMeetingTarget/$targetCompletions)"
        targetHoldSeconds != null -> "${bestHoldSeconds ?: 0}/${targetHoldSeconds}s  ($completionsMeetingTarget/$targetCompletions)"
        else -> "In progress"
    }
}
