package com.example.homeworkout.ui.core.customworkout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ConfirmDialog
import com.example.homeworkout.ui.components.PlanThumbnail
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.BrandBlueTint

/**
 * "Custom Workout" list — the user's own plans (source = CUSTOM), backed by
 * [CustomWorkoutListViewModel]. The FAB opens the "Create Workout" builder; tapping a plan opens
 * the same Workout Screen (plan detail) used for system plans, since a custom plan is just a plan.
 */
@Composable
fun CustomWorkoutListScreen(
    viewModel: CustomWorkoutListViewModel,
    onNavigateBack: () -> Unit,
    onCreatePlan: () -> Unit,
    onOpenPlan: (Long) -> Unit
) {
    val plans by viewModel.customPlans.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<WorkoutModel?>(null) }

    Scaffold(
        topBar = { BackTopBar(title = "Custom Workout", onNavigateBack = onNavigateBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreatePlan,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) { Icon(Icons.Default.Add, contentDescription = "Create custom workout") }
        }
    ) { padding ->
        if (plans.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(BrandBlueTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(32.dp))
                    }
                    Text("No custom workouts yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Tap + to build your own routine.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, bottom = 24.dp, top = padding.calculateTopPadding() + 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(plans, key = { it.id }) { plan ->
                    AppCard(modifier = Modifier.fillMaxWidth().clickable { onOpenPlan(plan.id) }) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            PlanThumbnail(planId = plan.id, coverImageUrl = plan.coverImageUrl, size = 56.dp)
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(plan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(plan.summaryText(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { pendingDelete = plan }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        pendingDelete?.let { plan ->
            ConfirmDialog(
                title = "Delete workout?",
                message = "\"${plan.title}\" will be permanently deleted.",
                confirmLabel = "Delete",
                onConfirm = { viewModel.deletePlan(plan.id) },
                onDismiss = { pendingDelete = null }
            )
        }
    }
}

private fun WorkoutModel.summaryText(): String {
    val days = "$totalDays day" + if (totalDays == 1) "" else "s"
    val exercises = "$totalExercises exercise" + if (totalExercises == 1) "" else "s"
    return "$days · $exercises"
}
