package com.example.homeworkout.ui.core.formcheck

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.FormAnalysis
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.SlateGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lists saved AI Video Form Check results ("Save to History" on [FormCheckScreen]'s result
 * screen) - the only place they're readable back, since [com.example.homeworkout.domain.repositories.FormCheckRepository.saveResult]
 * only ever wrote to `form_check_results` with nothing reading it back until this screen. Tapping
 * a row expands it in place to the same score/checkpoints/tips detail
 * ([FormAnalysisDetails]) shown right after "Analyze Form" - a saved result reads back exactly
 * like it looked the moment it was analyzed, not just its score. */
@Composable
fun FormCheckHistoryScreen(
    viewModel: FormCheckHistoryViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { BackTopBar(title = "Saved Form Checks", onNavigateBack = onNavigateBack) }
    ) { innerPadding ->
        when (val state = uiState) {
            FormCheckHistoryUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            is FormCheckHistoryUiState.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    state.message,
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            is FormCheckHistoryUiState.Loaded -> if (state.results.isEmpty()) {
                EmptyState(modifier = Modifier.fillMaxSize().padding(innerPadding))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.results, key = { it.id }) { result -> SavedResultRow(result) }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Icon(Icons.Default.History, contentDescription = null, tint = SlateGray)
        Text(
            "No saved form checks yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "Run a form check and tap \"Save to History\" on the result to keep it here.",
            style = MaterialTheme.typography.bodyMedium,
            color = SlateGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SavedResultRow(result: FormAnalysis) {
    var expanded by remember { mutableStateOf(false) }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        result.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = InkBlack
                    )
                    Text(
                        formatDateTime(result.analyzedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGray
                    )
                }
                Text(
                    "${result.score}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = result.status.color()
                )
                StatusBadge(status = result.status, modifier = Modifier.padding(start = 10.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Hide details" else "Show details",
                    tint = SlateGray,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (expanded) {
                HorizontalDivider(color = HairlineGray)
                FormAnalysisDetails(analysis = result, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

private fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(timestamp))
