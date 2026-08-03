package aman.zurutial.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Brand fallback palette (used when Dynamic Color / Material You is unavailable) ----

val BrandPrimary = Color(0xFFB388FF)
val BrandSecondary = Color(0xFFD7C2FF)

val LightPrimary = Color(0xFF7A4FE0)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFEADDFF)
val LightOnPrimaryContainer = Color(0xFF25005A)
val LightSecondary = Color(0xFF6B5B95)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFEADDFF)
val LightOnSecondaryContainer = Color(0xFF231043)
val LightTertiary = Color(0xFF2E7D74)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightBackground = Color(0xFFFFFBFF)
val LightSurface = Color(0xFFFFFBFF)
val LightSurfaceVariant = Color(0xFFE7E0EC)
val LightOutline = Color(0xFF79747E)

// True-black AMOLED dark surfaces
val AmoledBlack = Color(0xFF000000)
val AmoledSurfaceContainer = Color(0xFF0D0A12)
val AmoledSurfaceContainerHigh = Color(0xFF15101C)
val AmoledSurfaceContainerHighest = Color(0xFF1D1726)

// Standard (non-AMOLED) dark surfaces — softer, matches typical Material dark theme
val DarkBase = Color(0xFF141018)
val DarkSurfaceContainer = Color(0xFF1D1826)
val DarkSurfaceContainerHigh = Color(0xFF272130)
val DarkSurfaceContainerHighest = Color(0xFF322B3B)

val DarkPrimary = Color(0xFFCCB4FF)
val DarkOnPrimary = Color(0xFF3A1D77)
val DarkPrimaryContainer = Color(0xFF5A3D9A)
val DarkOnPrimaryContainer = Color(0xFFEADDFF)
val DarkSecondary = Color(0xFFD3C2FF)
val DarkOnSecondary = Color(0xFF362A50)
val DarkSecondaryContainer = Color(0xFF4D4068)
val DarkOnSecondaryContainer = Color(0xFFEADDFF)
val DarkTertiary = Color(0xFF8FDACF)
val DarkOnTertiary = Color(0xFF00382F)
val DarkOutline = Color(0xFF948F99)

// ---- Semantic status colors (sync chip, connection quality, etc.) ----
// Material3 has no built-in "success" role — these are app-level tokens layered on top.

val SyncGreen = Color(0xFF6FCF7A)
val SyncGreenContainer = Color(0xFF163819)
val SyncOrange = Color(0xFFFFB25E)
val SyncOrangeContainer = Color(0xFF3E2A0C)
val SyncRed = Color(0xFFFF6E6E)
val SyncRedContainer = Color(0xFF3E1717)
