package com.example.homeworkout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.TileShape

/**
 * Exercise/plan thumbnail. Shows the real photo or animated GIF from [imageUrl] (an exercise's
 * `gif_url` or a plan's `coverImageUrl`) when one is available, and falls back to this light
 * rounded icon tile while it loads, on failure, or when no URL is supplied at all.
 */
@Composable
fun ExerciseThumbnail(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    imageUrl: String? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(TileShape)
            .background(CloudGray),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNullOrBlank()) {
            ThumbnailPlaceholder(size)
        } else {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(size / 3),
                        strokeWidth = 2.dp,
                        color = BrandBlue
                    )
                },
                error = { ThumbnailPlaceholder(size) }
            )
        }
    }
}

@Composable
private fun ThumbnailPlaceholder(size: Dp) {
    Icon(
        imageVector = Icons.Default.FitnessCenter,
        contentDescription = null,
        tint = BrandBlue,
        modifier = Modifier.size(size / 2)
    )
}
