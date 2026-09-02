package com.example.homeworkout.ui.theme

import androidx.compose.ui.graphics.Brush

/** Reusable gradient brushes matching the storyboard's blue CTA pills, the dark "Restart" pill,
 * the blue "Challenge" promo card and the gold "PRO" badge. */
object AppGradients {
    val PrimaryButton = Brush.horizontalGradient(listOf(BrandBlueLight, BrandBlueDark))
    val DarkButton = Brush.verticalGradient(listOf(CharcoalStart, CharcoalEnd))
    val PromoCard = Brush.linearGradient(listOf(BrandBlueLight, BrandBlue, BrandBlueDark))
    val ProBadge = Brush.horizontalGradient(listOf(ProGoldStart, ProGoldEnd))
}
