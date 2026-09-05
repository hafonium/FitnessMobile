package com.example.homeworkout.ui.core.formcheck

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.homeworkout.domain.models.FormAnalysis
import com.example.homeworkout.domain.models.FormCheckObservation
import com.example.homeworkout.domain.models.enums.FormCheckExercise
import com.example.homeworkout.domain.models.enums.FormCheckStatus
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.components.buttons.AppButtonVariant
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.BrandBlueTint
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.PillShape
import com.example.homeworkout.ui.theme.SlateGray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

// Not `private`: shared with FormCheckHistoryScreen.kt (same package) so both screens color/label
// a FormCheckStatus identically instead of duplicating the mapping.
internal val ExcellentGreen = Color(0xFF10B981)
internal val AcceptableAmber = Color(0xFFF59E0B)
internal val NeedsImprovementRed = Color(0xFFEF4444)

internal fun FormCheckStatus.color(): Color = when (this) {
    FormCheckStatus.EXCELLENT -> ExcellentGreen
    FormCheckStatus.ACCEPTABLE -> AcceptableAmber
    FormCheckStatus.NEEDS_IMPROVEMENT -> NeedsImprovementRed
}

internal fun FormCheckStatus.displayLabel(): String =
    name.split('_').joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }

/** Recommended clip length shown on the capture sheet - Gemini reasons over the extracted frame
 * sequence either way, but a short clip keeps the frame count (and payload) small. */
private const val RECOMMENDED_DURATION_SEC = 8

private const val TAG = "FormCheckScreen"
private const val MIN_FRAME_COUNT = 4
private const val MAX_FRAME_COUNT = 6
private const val FRAME_TARGET_WIDTH_PX = 720
private const val FRAME_JPEG_QUALITY = 85

/**
 * Storyboard extraction: samples [MIN_FRAME_COUNT]-[MAX_FRAME_COUNT] frames evenly across the
 * clip (roughly 1 per second, capped either way) via [MediaMetadataRetriever] instead of
 * uploading the raw video, then downscales each to [FRAME_TARGET_WIDTH_PX] wide JPEG bytes.
 * Returns an empty list on any failure (corrupt/unreadable video, zero duration) - the caller
 * treats that the same as "this video could not be processed".
 */
private fun extractFrames(context: Context, uri: Uri): List<ByteArray> {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
            ?.takeIf { it > 0 }
            ?: return emptyList()

        // Integer-only arithmetic on `durationMs` (a fixed property of the file itself) - the same
        // video always yields the same frame count and the same timestamps on every run.
        val frameCount = (durationMs / 1000.0).roundToInt().coerceIn(MIN_FRAME_COUNT, MAX_FRAME_COUNT)
        val segmentMs = durationMs / frameCount

        // Sample the midpoint of each of [frameCount] equal segments spanning the whole clip,
        // rather than the segment edges - avoids landing exactly on a black/incomplete frame at
        // the very start or end while still spacing samples evenly across the full duration.
        (0 until frameCount).mapNotNull { index ->
            val timeUs = ((segmentMs * index) + segmentMs / 2) * 1000L
            // OPTION_CLOSEST_SYNC (not OPTION_CLOSEST): always resolves to the nearest sync/key
            // frame for a given timestamp without decoding forward from it, which is both cheaper
            // and removes decoder-path variance as a source of frame-to-frame inconsistency across
            // runs - the video's own encoding is the only thing that determines the result.
            val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return@mapNotNull null
            frame.toScaledJpegBytes(FRAME_TARGET_WIDTH_PX, FRAME_JPEG_QUALITY)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Frame extraction failed for $uri", e)
        emptyList()
    } finally {
        retriever.release()
    }
}

private fun Bitmap.toScaledJpegBytes(targetWidth: Int, quality: Int): ByteArray {
    val scaled = if (width > targetWidth) {
        val targetHeight = (height.toFloat() * targetWidth / width).roundToInt()
        scale(targetWidth, targetHeight)
    } else {
        this
    }
    try {
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
        return output.toByteArray()
    } finally {
        if (scaled !== this) scaled.recycle()
        recycle()
    }
}

@Composable
fun FormCheckScreen(
    viewModel: FormCheckViewModel,
    onNavigateBack: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    // The on-disk temp file backing `videoUri`, only when it came from the camera (null for a
    // gallery pick, whose Uri points to a persistent MediaStore-owned file we never touch). Owned
    // by whichever `videoUri` is current - cleared up the moment it's replaced, never before: the
    // preview player and, later, frame extraction on "Analyze Form" both need it to still exist.
    var activeVideoFile by remember { mutableStateOf<File?>(null) }
    var videoError by remember { mutableStateOf<String?>(null) }
    var isExtractingFrames by remember { mutableStateOf(false) }
    var selectedExercise by remember { mutableStateOf(FormCheckExercise.AUTO_DETECT) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun clearVideo() {
        activeVideoFile?.delete()
        activeVideoFile = null
        videoUri = null
    }

    fun loadVideo(uri: Uri, backingFile: File?, afterLoad: () -> Unit = {}) {
        scope.launch {
            videoError = null
            // Confirm the Uri actually has video content, not just that it opens - a zero-byte
            // file (e.g. a camera app that returned success before finishing its write) would
            // pass a bare "does it open" probe and only fail much later, inside the preview
            // player or the frame extractor, as a much less diagnosable error.
            val hasContent = withContext(Dispatchers.IO) {
                val length = runCatching {
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
                }.getOrNull()
                when {
                    length != null && length > 0L -> true
                    length == 0L -> false
                    else -> runCatching {
                        // Length unknown/unsupported by this content provider (some cloud-backed
                        // gallery pickers) - fall back to confirming the stream yields a byte.
                        context.contentResolver.openInputStream(uri)?.use { it.read(ByteArray(1)) > 0 } ?: false
                    }.getOrDefault(false)
                }
            }
            afterLoad()
            if (!hasContent) {
                backingFile?.delete()
                videoError = "This video could not be opened. Choose another clip."
            } else {
                activeVideoFile?.delete()
                activeVideoFile = backingFile
                videoUri = uri
                viewModel.reset()
            }
        }
    }

    fun analyzeVideo(uri: Uri) {
        scope.launch {
            videoError = null
            isExtractingFrames = true
            val frames = withContext(Dispatchers.Default) { extractFrames(context, uri) }
            isExtractingFrames = false
            if (frames.isEmpty()) {
                videoError = "This video could not be processed. Choose another clip."
            } else {
                viewModel.analyze(frames, selectedExercise)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = pendingCameraUri
        val file = pendingCameraFile
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            // `file` is NOT deleted here - it's the on-disk video the camera just wrote, and
            // `loadVideo` takes ownership of it via `activeVideoFile`, deleting it only once
            // replaced. Deleting it immediately after this callback (as this code used to) left
            // `videoUri` pointing at nothing, which is exactly what broke camera-recorded clips.
            loadVideo(uri, backingFile = file)
        } else {
            // Canceled/failed capture - this temp file was never used, safe to clean up now.
            file?.delete()
        }
        pendingCameraFile = null
        pendingCameraUri = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) loadVideo(uri, backingFile = null)
    }

    Scaffold(
        topBar = {
            BackTopBar(
                title = "AI Video Form Check",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = "Saved form checks")
                    }
                }
            )
        }
    ) { innerPadding ->
        val successState = uiState as? FormCheckUiState.Success

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (successState == null) {
                item {
                    Text(
                        "Record or pick a $RECOMMENDED_DURATION_SEC-second clip of one exercise repetition. " +
                            "Gemini locates the cleanest rep and evaluates your form.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateGray
                    )
                }

                item { VideoPreviewBox(uri = videoUri) }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CaptureSourceButton(
                            label = "Record",
                            onClick = {
                                runCatching {
                                    val directory = File(context.cacheDir, "form-check-videos").apply { mkdirs() }
                                    val file = File.createTempFile("form-check-", ".mp4", directory)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    pendingCameraFile = file
                                    pendingCameraUri = uri
                                    val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                                        putExtra(MediaStore.EXTRA_OUTPUT, uri)
                                        putExtra(MediaStore.EXTRA_DURATION_LIMIT, RECOMMENDED_DURATION_SEC)
                                        putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
                                        addFlags(
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    }
                                    // Some OEM camera apps don't reliably honor Intent-level URI
                                    // permission flags - grant explicitly to every activity that
                                    // can handle this intent as a defensive fallback, the same
                                    // workaround long used for ACTION_IMAGE_CAPTURE on those devices.
                                    context.packageManager.queryIntentActivities(intent, 0).forEach { resolveInfo ->
                                        context.grantUriPermission(
                                            resolveInfo.activityInfo.packageName,
                                            uri,
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    }
                                    cameraLauncher.launch(intent)
                                }.onFailure { error ->
                                    videoError = error.message ?: "The camera could not be opened."
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CaptureSourceButton(
                            label = "Gallery",
                            onClick = { galleryLauncher.launch("video/*") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Exercise (optional)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = InkBlack
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(FormCheckExercise.entries) { exercise ->
                                ExerciseChip(
                                    label = exercise.label,
                                    selected = exercise == selectedExercise,
                                    onClick = { selectedExercise = exercise }
                                )
                            }
                        }
                    }
                }

                item {
                    AppButton(
                        text = when {
                            isExtractingFrames -> "Preparing frames…"
                            uiState is FormCheckUiState.Loading -> "Analyzing…"
                            else -> "Analyze Form"
                        },
                        onClick = { videoUri?.let(::analyzeVideo) },
                        enabled = videoUri != null && !isExtractingFrames && uiState !is FormCheckUiState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                videoError?.let { error -> item { ErrorCard(error) } }

                when (val state = uiState) {
                    FormCheckUiState.Idle, is FormCheckUiState.Success -> Unit
                    FormCheckUiState.Loading -> item { ProcessingIndicator() }
                    is FormCheckUiState.Error -> item { ErrorCard(state.message) }
                }
            } else {
                item {
                    FormAnalysisResult(
                        analysis = successState.analysis,
                        saved = successState.saved,
                        onSaveToHistory = viewModel::saveToHistory,
                        onRetestForm = {
                            clearVideo()
                            videoError = null
                            viewModel.reset()
                        }
                    )
                }
            }
        }
    }
}

/** How the video preview's player is progressing for the currently selected Uri - drives the
 * spinner/placeholder overlay in [VideoPreviewBox] so the user never sees a bare black surface
 * while ExoPlayer is still buffering the first frame. */
private sealed interface VideoPreviewState {
    data object Loading : VideoPreviewState
    data object Ready : VideoPreviewState
    data class Error(val message: String) : VideoPreviewState
}

/** If the player hasn't rendered a first frame or failed within this long, treat it as failed -
 * covers a hung/unresponsive decode rather than leaving the spinner up forever. */
private const val VIDEO_PREVIEW_TIMEOUT_MS = 8_000L

@Composable
private fun VideoPreviewBox(uri: Uri?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(24.dp))
            .background(CloudGray),
        contentAlignment = Alignment.Center
    ) {
        if (uri == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = SlateGray
                )
                Text("No video selected", color = SlateGray)
            }
        } else {
            // Keyed on `uri` so picking a different video starts a fresh Loading state; `retryCount`
            // is bumped (without changing `uri`) to force the player below to rebuild and re-attempt
            // the same Uri when the user taps Retry.
            var previewState by remember(uri) { mutableStateOf<VideoPreviewState>(VideoPreviewState.Loading) }
            var retryCount by remember(uri) { mutableStateOf(0) }

            VideoPreviewPlayer(
                uri = uri,
                retryCount = retryCount,
                modifier = Modifier.fillMaxSize(),
                onReady = { previewState = VideoPreviewState.Ready },
                onError = { message -> previewState = VideoPreviewState.Error(message) }
            )

            LaunchedEffect(uri, retryCount) {
                delay(VIDEO_PREVIEW_TIMEOUT_MS)
                if (previewState is VideoPreviewState.Loading) {
                    previewState = VideoPreviewState.Error("This preview is taking longer than expected.")
                }
            }

            when (val state = previewState) {
                VideoPreviewState.Loading -> VideoPreviewOverlay {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                    Text(
                        "Preparing video preview…",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGray,
                        textAlign = TextAlign.Center
                    )
                }
                is VideoPreviewState.Error -> VideoPreviewOverlay {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = SlateGray
                    )
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGray,
                        textAlign = TextAlign.Center
                    )
                    AppButton(
                        text = "Retry",
                        onClick = {
                            previewState = VideoPreviewState.Loading
                            retryCount++
                        },
                        variant = AppButtonVariant.Outlined,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
                VideoPreviewState.Ready -> Unit
            }
        }
    }
}

/** Solid, fully-opaque cover over the player surface - ExoPlayer's underlying `SurfaceView`
 * renders black until its first frame, so this (not a translucent scrim) is what actually
 * prevents the black-screen flash while loading or on failure. */
@Composable
private fun VideoPreviewOverlay(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CloudGray)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        content = content
    )
}

/**
 * ExoPlayer (Media3) preview, looping and muted. Replaces an earlier VideoView/MediaPlayer
 * implementation that could surface "Cannot display video" for some camera/gallery
 * container-codec combinations - ExoPlayer resolves `content://` Uris and codecs more reliably.
 * Reports readiness/failure via [onReady]/[onError] rather than rendering its own placeholder, so
 * [VideoPreviewBox] can overlay a single consistent spinner/error state above the player surface.
 */
@Composable
private fun VideoPreviewPlayer(
    uri: Uri,
    retryCount: Int,
    modifier: Modifier = Modifier,
    onReady: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember(uri, retryCount) {
        // A one-shot read-URI grant (a gallery pick via GetContent, or our own FileProvider
        // camera-capture Uri) doesn't always survive being reopened after a recomposition -
        // request a durable grant where the source supports it. Uris that don't support a
        // persistable grant (most of the above) throw here; that's expected and harmless.
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            // Fires once the first decoded frame actually reaches the surface - the accurate
            // signal that there's something other than black to show, as opposed to
            // STATE_READY (which can fire before any frame has been rendered).
            override fun onRenderedFirstFrame() {
                onReady()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer failed to load $uri", error)
                onError(error.message ?: "This video could not be previewed.")
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx -> PlayerView(ctx).apply { useController = false } },
        update = { view -> view.player = exoPlayer }
    )
}

@Composable
private fun CaptureSourceButton(
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
private fun ExerciseChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(if (selected) BrandBlueTint else CloudGray)
            .border(if (selected) 1.5.dp else 0.dp, BrandBlue.copy(alpha = if (selected) 1f else 0f), PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) BrandBlue else SlateGray
        )
    }
}

@Composable
private fun ProcessingIndicator() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
        Text(
            "Evaluating movement kinematics and joint angles…",
            style = MaterialTheme.typography.bodyMedium,
            color = SlateGray,
            textAlign = TextAlign.Center
        )
    }
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
private fun FormAnalysisResult(
    analysis: FormAnalysis,
    saved: Boolean,
    onSaveToHistory: () -> Unit,
    onRetestForm: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FormAnalysisDetails(analysis)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppButton(
                text = if (saved) "Saved" else "Save to History",
                onClick = onSaveToHistory,
                enabled = !saved,
                variant = AppButtonVariant.Outlined,
                modifier = Modifier.weight(1f)
            )
            AppButton(
                text = "Re-test Form",
                onClick = onRetestForm,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** The read-only body of a form-check result - score, status, joint checkpoints, correction tip,
 * recording tip - with no action buttons. Shared between the live result screen ([FormAnalysisResult],
 * right after "Analyze Form") and [FormCheckHistoryScreen] (a saved result, expanded from the
 * history list) so a saved result looks exactly like it did the moment it was analyzed. Not
 * `private`: called from FormCheckHistoryScreen.kt (same package). */
@Composable
internal fun FormAnalysisDetails(analysis: FormAnalysis, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    analysis.exerciseName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                FormScoreGauge(score = analysis.score, color = analysis.status.color())
                StatusBadge(status = analysis.status)
            }
        }

        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Joint Checkpoints",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                analysis.observations.forEach { observation -> ObservationRow(observation) }
            }
        }

        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(
                        "Primary correction",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray
                    )
                    Text(analysis.primaryCorrectionTip, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (analysis.recordingTip.isNotBlank()) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = SlateGray)
                    Column {
                        Text(
                            "Recording tip",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = SlateGray
                        )
                        Text(analysis.recordingTip, style = MaterialTheme.typography.bodyMedium, color = SlateGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormScoreGauge(score: Int, color: Color, modifier: Modifier = Modifier) {
    val sweep = 360f * (score.coerceIn(0, 100) / 100f)
    Box(modifier = modifier.size(140.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val stroke = 14.dp.toPx()
            drawArc(
                color = CloudGray,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = Size(size.width - stroke, size.height - stroke),
                topLeft = Offset(stroke / 2, stroke / 2)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = Size(size.width - stroke, size.height - stroke),
                topLeft = Offset(stroke / 2, stroke / 2)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = InkBlack)
            Text("/ 100", style = MaterialTheme.typography.bodySmall, color = SlateGray)
        }
    }
}

// Not `private`: shared with FormCheckHistoryScreen.kt (same package).
@Composable
internal fun StatusBadge(status: FormCheckStatus, modifier: Modifier = Modifier) {
    val color = status.color()
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            status.displayLabel().uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ObservationRow(observation: FormCheckObservation) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            if (observation.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (observation.isCorrect) ExcellentGreen else NeedsImprovementRed
        )
        Column {
            Text(observation.jointArea, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(observation.feedback, style = MaterialTheme.typography.bodySmall, color = SlateGray)
        }
    }
}
