package com.example.homeworkout.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Brand palette (matches docs/system.pdf) ----

// Primary "electric blue" — the app's single accent color: CTAs, links, selected states,
// progress rings/timers, the active bottom-nav tab.
val BrandBlue = Color(0xFF0052FE)
val BrandBlueLight = Color(0xFF3F82FF)
val BrandBlueDark = Color(0xFF0040D6)
val BrandBlueTint = Color(0xFFEEF3FF) // very light blue surface: rest screen bg, selected chip fill
val BrandBlueTintStrong = Color(0xFFDCE7FF)

// Neutrals
val InkBlack = Color(0xFF15171B) // headings / primary text
val SlateGray = Color(0xFF8B8D98) // secondary / caption text
val HairlineGray = Color(0xFFE9EAEE) // dividers, hairlines, outlined-button borders
val CloudGray = Color(0xFFF1F2F5) // filled inputs, unselected chips, muted tiles
val PageBackground = Color(0xFFF8F9FB)
val CardWhite = Color(0xFFFFFFFF)

// Dark pill (secondary buttons: "Restart")
val CharcoalStart = Color(0xFF3A3A3C)
val CharcoalEnd = Color(0xFF1C1C1E)

// Status accents used sparingly around the app
val StreakRed = Color(0xFFFF3B30)
val ProGoldStart = Color(0xFFFFE1A8)
val ProGoldEnd = Color(0xFFFFB65C)
val SuccessGreen = Color(0xFF34C759)

// Settings icon-tile colors (first group in the storyboard's Settings screen)
val SettingsGreen = Color(0xFF43A047)
val SettingsBlue = Color(0xFF4285F4)
val SettingsOrange = Color(0xFFFB8C00)
val SettingsTeal = Color(0xFF00BCD4)
val SettingsPurple = Color(0xFF7C4DFF)
val SettingsSlate = Color(0xFF64748B) // second group: share/rate/feedback/remove ads

// ---- Dark theme (kept for completeness — the storyboard only shows a light UI, so the light
// palette below is what HomeWorkoutTheme actually uses by default) ----
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2A2A2E)
val DarkPrimary = BrandBlueLight
val DarkSecondary = ProGoldEnd
val DarkTertiary = Color(0xFF64B5F6)
val DarkTextPrimary = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFFA0A0AB)

// ---- Light theme ----
val LightBackground = PageBackground
val LightSurface = CardWhite
val LightPrimary = BrandBlue
val LightSecondary = ProGoldEnd
val LightTertiary = Color(0xFF1565C0)
val LightTextPrimary = InkBlack
val LightTextSecondary = SlateGray
