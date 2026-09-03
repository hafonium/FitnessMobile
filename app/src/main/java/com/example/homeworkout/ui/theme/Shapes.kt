package com.example.homeworkout.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Material3 corner-radius scale — generous radii to match the storyboard's soft, card-based
 * look (used implicitly by components that don't ask for an explicit shape). */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Named shapes used directly by ui/components — every card/button/tile in the app should pull
 * from here rather than hand-rolling a corner radius. */
val PillShape = RoundedCornerShape(percent = 50)
val CardShape = RoundedCornerShape(20.dp)
val SheetTopShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val TileShape = RoundedCornerShape(14.dp)
