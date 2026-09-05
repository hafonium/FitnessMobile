package com.example.homeworkout.ui.core.foodscan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.homeworkout.domain.models.FoodLogEntry
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.theme.SlateGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun FoodLogHistoryScreen(
    viewModel: FoodLogHistoryViewModel,
    onNavigateBack: () -> Unit
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { BackTopBar(title = "Food log history", onNavigateBack = onNavigateBack) }
    ) { innerPadding ->
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No meals logged yet.", color = SlateGray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(logs.size) { index -> FoodLogRow(logs[index]) }
            }
        }
    }
}

@Composable
private fun FoodLogRow(entry: FoodLogEntry) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    entry.category.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${entry.caloriesKcal.roundToInt()} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date(entry.loggedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = SlateGray
            )
            Text(
                "P ${entry.proteinG.roundToInt()}g · C ${entry.carbsG.roundToInt()}g · F ${entry.fatG.roundToInt()}g",
                style = MaterialTheme.typography.bodySmall,
                color = SlateGray
            )
        }
    }
}
