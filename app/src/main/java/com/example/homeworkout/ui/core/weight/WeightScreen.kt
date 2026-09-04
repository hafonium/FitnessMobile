package com.example.homeworkout.ui.core.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.BmiCategory
import com.example.homeworkout.domain.models.WeightDashboard
import com.example.homeworkout.domain.usecases.report.RecordWeightUseCase
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.WeightLineChart
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.CardShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WeightSurface = Color(0xFF18191D)
private val WeightSecondary = Color(0xFF8E929A)
private val WeightGrid = Color(0xFF2B2D33)
private val BmiColors = listOf(
    Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF00D2D3),
    Color(0xFFFDD835), Color(0xFFFF9800), Color(0xFFFF2D55)
)

@Composable
fun WeightScreen(viewModel: WeightViewModel, onNavigateBack: () -> Unit) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    var showRecordDialog by remember { mutableStateOf(false) }
    var showHeightDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Black,
        topBar = { WeightTopBar(onNavigateBack) }
    ) { padding ->
        val data = dashboard
        if (data == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { CurrentWeightCard(data = data, onRecord = { showRecordDialog = true }) }
                item {
                    WeightAnalyticsCard(
                        data = data,
                        onEditCurrent = { showRecordDialog = true }
                    )
                }
                item { BmiCard(data = data, onEditHeight = { showHeightDialog = true }) }
            }
        }
    }

    if (showRecordDialog) {
        RecordWeightDialog(
            currentWeightKg = dashboard?.currentWeightKg,
            currentHeightCm = dashboard?.heightCm,
            onSave = { weight, height -> viewModel.recordWeight(weight, height) },
            onDismiss = { showRecordDialog = false }
        )
    }
    if (showHeightDialog) {
        DecimalInputDialog(
            title = "Edit height",
            label = "Height (cm)",
            initialValue = dashboard?.heightCm,
            validRange = RecordWeightUseCase.MIN_HEIGHT_CM..RecordWeightUseCase.MAX_HEIGHT_CM,
            onSave = viewModel::updateHeight,
            onDismiss = { showHeightDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = { Text("Weight", fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
}

@Composable
private fun CurrentWeightCard(data: WeightDashboard, onRecord: () -> Unit) {
    DarkCard {
        Text("Current Weight", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(16.dp))
        if (data.currentWeightKg == null) {
            Text("No weight recorded yet", color = WeightSecondary)
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatWeight(data.currentWeightKg), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(" kg", style = MaterialTheme.typography.titleMedium, color = WeightSecondary, modifier = Modifier.padding(bottom = 4.dp))
            }
            data.currentLoggedAt?.let {
                Text(SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it)), color = WeightSecondary)
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            AppButton(text = "Record", onClick = onRecord, modifier = Modifier.width(170.dp))
        }
    }
}

@Composable
private fun WeightAnalyticsCard(data: WeightDashboard, onEditCurrent: () -> Unit) {
    DarkCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Current", color = WeightSecondary, style = MaterialTheme.typography.bodySmall)
                Text(
                    data.currentWeightKg?.let { "${formatWeight(it)} kg" } ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            Row(
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onEditCurrent).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Record", color = WeightSecondary)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Edit, contentDescription = "Record weight", tint = WeightSecondary, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        if (data.chartRecords.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text("Record weight to see your trend", color = WeightSecondary)
            }
        } else {
            Text(
                SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(data.chartRecords.last().loggedAt)),
                color = WeightSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            WeightLineChart(
                records = data.chartRecords,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                showLatestTooltip = true,
                gridColor = WeightGrid
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                data.chartRecords.forEach { record ->
                    Text(
                        SimpleDateFormat("d", Locale.getDefault()).format(Date(record.loggedAt)),
                        color = WeightSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SummaryMetric("Last 7 Days", formatChange(data.lastSevenDaysChangeKg))
            SummaryMetric("Avg.", data.averageWeightKg?.let(::formatWeight) ?: "—")
            SummaryMetric("BMI", data.bmi?.let { "%.1f".format(it) } ?: "—", data.bmiCategory == BmiCategory.HEALTHY)
        }
    }
}

@Composable
private fun RowScope.SummaryMetric(label: String, value: String, healthy: Boolean = false) {
    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = WeightSecondary, style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (healthy) {
                Box(Modifier.size(8.dp).background(BmiColors[2], CircleShape))
                Spacer(Modifier.width(5.dp))
            }
            Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BmiCard(data: WeightDashboard, onEditHeight: () -> Unit) {
    DarkCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("BMI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Row(
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onEditHeight).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit height", color = WeightSecondary)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Edit, contentDescription = "Edit height", tint = WeightSecondary, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        BmiSpectrum(data.bmi)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val color = bmiColor(data.bmi)
            Box(Modifier.size(10.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(data.bmiCategory?.label ?: "Add weight and height to calculate BMI", color = Color.White)
        }
        data.heightCm?.let {
            Spacer(Modifier.height(6.dp))
            Text("Height ${formatWeight(it)} cm", color = WeightSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BmiSpectrum(bmi: Double?) {
    val fraction = (((bmi ?: 15.0) - 15.0) / 25.0).coerceIn(0.0, 1.0).toFloat()
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(65.dp)) {
        if (bmi != null) {
            Box(
                modifier = Modifier
                    .offset(x = (maxWidth - 44.dp) * fraction)
                    .width(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF3A3C43))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("%.1f".format(bmi), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val weights = listOf(1f, 2.5f, 6.5f, 5f, 5f, 5f)
            BmiColors.forEachIndexed { index, color ->
                Box(Modifier.weight(weights[index]).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("15", "16", "18.5", "25", "30", "35", "40").forEach {
                Text(it, color = WeightSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun DarkCard(content: @Composable ColumnScope.() -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), shape = CardShape, containerColor = WeightSurface) {
        Column(modifier = Modifier.padding(22.dp), content = content)
    }
}

@Composable
private fun RecordWeightDialog(
    currentWeightKg: Double?,
    currentHeightCm: Double?,
    onSave: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var weightText by remember(currentWeightKg) { mutableStateOf(currentWeightKg?.let(::formatWeight).orEmpty()) }
    var heightText by remember(currentHeightCm) { mutableStateOf(currentHeightCm?.let(::formatWeight).orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record weight") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it; error = null },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it; error = null },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val weight = weightText.toDecimalOrNull()
                val height = heightText.toDecimalOrNull()
                when {
                    weight == null || weight !in RecordWeightUseCase.MIN_WEIGHT_KG..RecordWeightUseCase.MAX_WEIGHT_KG -> error = "Enter a weight from 20 to 500 kg."
                    height == null || height !in RecordWeightUseCase.MIN_HEIGHT_CM..RecordWeightUseCase.MAX_HEIGHT_CM -> error = "Enter a height from 50 to 300 cm."
                    else -> { onSave(weight, height); onDismiss() }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DecimalInputDialog(
    title: String,
    label: String,
    initialValue: Double?,
    validRange: ClosedFloatingPointRange<Double>,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(initialValue) { mutableStateOf(initialValue?.let(::formatWeight).orEmpty()) }
    var hasError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; hasError = false },
                label = { Text(label) },
                isError = hasError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val value = text.toDecimalOrNull()
                if (value == null || value !in validRange) hasError = true else { onSave(value); onDismiss() }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatWeight(value: Double): String = "%.1f".format(value)

private fun String.toDecimalOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

private fun formatChange(value: Double?): String = when {
    value == null -> "—"
    value < 0 -> "↓ ${formatWeight(-value)}"
    value > 0 -> "↑ ${formatWeight(value)}"
    else -> "0.0"
}

private fun bmiColor(bmi: Double?): Color = when {
    bmi == null -> WeightSecondary
    bmi < 16.0 -> BmiColors[0]
    bmi < 18.5 -> BmiColors[1]
    bmi < 25.0 -> BmiColors[2]
    bmi < 30.0 -> BmiColors[3]
    bmi < 35.0 -> BmiColors[4]
    else -> BmiColors[5]
}
