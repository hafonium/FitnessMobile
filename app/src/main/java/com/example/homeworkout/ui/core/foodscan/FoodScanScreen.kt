package com.example.homeworkout.ui.core.foodscan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.graphics.scale
import androidx.core.content.FileProvider
import com.example.homeworkout.domain.models.FoodAnalysis
import com.example.homeworkout.domain.models.NutritionEstimate
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.components.buttons.AppButtonVariant
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.SlateGray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

@Composable
fun FoodScanScreen(
    viewModel: FoodScanViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun prepareImage(bitmap: Bitmap) {
        scope.launch {
            imageError = null
            val prepared = runCatching {
                withContext(Dispatchers.Default) { bitmap.prepareForUpload() }
            }.getOrElse { error ->
                imageError = error.message ?: "Could not prepare this image. Choose another photo."
                return@launch
            }
            selectedBitmap = prepared.first
            imageBytes = prepared.second
            viewModel.clearResult()
        }
    }

    fun loadImage(uri: Uri, afterLoad: () -> Unit = {}) {
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                decodeSampledBitmap(uri) { context.contentResolver.openInputStream(it) }
            }
            afterLoad()
            if (bitmap == null) {
                imageError = "This image could not be opened. Choose another photo."
            } else {
                prepareImage(bitmap)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { saved ->
        val uri = pendingCameraUri
        val file = pendingCameraFile
        if (saved && uri != null) {
            loadImage(uri) { file?.delete() }
        } else {
            file?.delete()
        }
        pendingCameraFile = null
        pendingCameraUri = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            loadImage(uri)
        }
    }

    Scaffold(
        topBar = { BackTopBar(title = "Food calorie scanner", onNavigateBack = onNavigateBack) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Take a clear photo of one dish. Spoonacular will estimate its calories and macronutrients.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateGray
                )
            }

            item {
                val bitmap = selectedBitmap
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(CloudGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Restaurant,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = SlateGray
                            )
                            Text("No food photo selected", color = SlateGray)
                        }
                    } else {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Selected food",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImageSourceButton(
                        label = "Camera",
                        onClick = {
                            runCatching {
                                val directory = File(context.cacheDir, "food-photos").apply { mkdirs() }
                                val file = File.createTempFile("meal-", ".jpg", directory)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                pendingCameraFile = file
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            }.onFailure { error ->
                                imageError = error.message ?: "The camera could not be opened."
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ImageSourceButton(
                        label = "Gallery",
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                AppButton(
                    text = if (uiState is FoodScanUiState.Loading) "Analyzing…" else "Analyze food",
                    onClick = { imageBytes?.let(viewModel::analyze) },
                    enabled = imageBytes != null && uiState !is FoodScanUiState.Loading,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            imageError?.let { error -> item { ErrorCard(error) } }

            when (val state = uiState) {
                FoodScanUiState.Idle -> Unit
                FoodScanUiState.Loading -> item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        Text("Estimating nutrition…", modifier = Modifier.padding(start = 12.dp), color = SlateGray)
                    }
                }
                is FoodScanUiState.Error -> item { ErrorCard(state.message) }
                is FoodScanUiState.Success -> item { AnalysisCard(state.analysis) }
            }

            item {
                Text(
                    "Nutrition from a photo is an estimate and can vary with portion size, ingredients, and preparation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ImageSourceButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppButton(
        text = label,
        onClick = onClick,
        modifier = modifier,
        variant = AppButtonVariant.Outlined,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
    )
}

@Composable
private fun ErrorCard(message: String) {
    AppCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
        Text(
            message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun AnalysisCard(analysis: FoodAnalysis) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text(
                    analysis.category.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Recognition confidence ${(analysis.categoryProbability * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGray
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    analysis.calories.value.roundToInt().toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("estimated kcal", style = MaterialTheme.typography.bodyMedium, color = SlateGray)
                Text(
                    "95% range: ${analysis.calories.minimum.roundToInt()}–${analysis.calories.maximum.roundToInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGray
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MacroValue("Protein", analysis.protein)
                MacroValue("Carbs", analysis.carbohydrates)
                MacroValue("Fat", analysis.fat)
            }
            Text(
                "Estimate based on ${analysis.recipesUsed} similar recipes",
                style = MaterialTheme.typography.bodySmall,
                color = SlateGray
            )
        }
    }
}

@Composable
private fun MacroValue(label: String, nutrient: NutritionEstimate) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = SlateGray)
        Text(
            "${nutrient.value.roundToInt()} ${nutrient.unit}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun Bitmap.prepareForUpload(): Pair<Bitmap, ByteArray> {
    val scale = minOf(1f, MAX_IMAGE_DIMENSION.toFloat() / maxOf(width, height))
    val prepared = if (scale < 1f) {
        scale((width * scale).roundToInt(), (height * scale).roundToInt())
    } else {
        this
    }
    val output = ByteArrayOutputStream()
    check(prepared.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
        "Could not prepare this image for upload."
    }
    return prepared to output.toByteArray()
}

private fun decodeSampledBitmap(
    uri: Uri,
    openStream: (Uri) -> java.io.InputStream?
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > DECODE_IMAGE_DIMENSION) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return openStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}

private const val MAX_IMAGE_DIMENSION = 1600
private const val DECODE_IMAGE_DIMENSION = 3200
private const val JPEG_QUALITY = 85
