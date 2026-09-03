package com.example.homeworkout.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.homeworkout.R
import com.example.homeworkout.ui.theme.TileShape

/**
 * A small set of themed stock photos used as a plan's hero/thumbnail image whenever it has no
 * real `coverImageUrl` of its own yet — true for every plan today, system and user-created alike.
 * [planThemeDrawableRes] picks one deterministically from the plan's id, so a given plan always
 * shows the same theme (rather than a different one on every recomposition) while different plans
 * still land on different, arbitrary-looking themes.
 */
private val planThemeDrawables = listOf(
    R.drawable.plan_theme_halloween,
    R.drawable.plan_theme_dog,
    R.drawable.plan_theme_christmas,
    R.drawable.plan_theme_new_year,
    R.drawable.plan_theme_intense
)

fun planThemeDrawableRes(planId: Long): Int {
    val index = ((planId % planThemeDrawables.size) + planThemeDrawables.size) % planThemeDrawables.size
    return planThemeDrawables[index.toInt()]
}

/**
 * Small plan thumbnail for cards/list rows (Home, Workout List): the real [coverImageUrl] via
 * [ExerciseThumbnail] when the plan has one, otherwise its themed stock photo instead of the
 * generic icon tile every plan would otherwise show today.
 */
@Composable
fun PlanThumbnail(planId: Long, coverImageUrl: String?, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    if (coverImageUrl.isNullOrBlank()) {
        Box(modifier = modifier.size(size).clip(TileShape), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = planThemeDrawableRes(planId)),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }
    } else {
        ExerciseThumbnail(modifier = modifier, size = size, imageUrl = coverImageUrl)
    }
}
