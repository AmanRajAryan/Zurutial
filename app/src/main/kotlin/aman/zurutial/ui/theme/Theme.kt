package aman.zurutial.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    background = DarkBase,
    surface = DarkBase,
    surfaceContainerLowest = AmoledBlack,
    surfaceContainerLow = DarkSurfaceContainer,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline
)

private val AmoledColorScheme = DarkColorScheme.copy(
    background = AmoledBlack,
    surface = AmoledBlack,
    surfaceContainerLowest = AmoledBlack,
    surfaceContainerLow = AmoledSurfaceContainer,
    surfaceContainer = AmoledSurfaceContainer,
    surfaceContainerHigh = AmoledSurfaceContainerHigh,
    surfaceContainerHighest = AmoledSurfaceContainerHighest
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline
)

/**
 * App-level color tokens that don't fit Material3's role system (sync status,
 * gradients). Exposed via [LocalZurutialColors] so components can read them the
 * same way they'd read MaterialTheme.colorScheme.
 */
data class ZurutialExtendedColors(
    val syncGreen: Color,
    val syncGreenContainer: Color,
    val syncOrange: Color,
    val syncOrangeContainer: Color,
    val syncRed: Color,
    val syncRedContainer: Color,
    val heroGradient: List<Color>,
    val heroGradientSecondary: List<Color>
)

val LocalZurutialColors = androidx.compose.runtime.staticCompositionLocalOf {
    ZurutialExtendedColors(
        syncGreen = SyncGreen,
        syncGreenContainer = SyncGreenContainer,
        syncOrange = SyncOrange,
        syncOrangeContainer = SyncOrangeContainer,
        syncRed = SyncRed,
        syncRedContainer = SyncRedContainer,
        heroGradient = listOf(BrandPrimary, BrandSecondary),
        heroGradientSecondary = listOf(Color(0xFF6EE7DE), Color(0xFF7FA3FF))
    )
}

object ZurutialTheme {
    val extendedColors: ZurutialExtendedColors
        @Composable
        get() = LocalZurutialColors.current
}

@Composable
fun ComposeEmptyActivityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    pureBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && pureBlack) {
                base.copy(
                    background = AmoledBlack,
                    surface = AmoledBlack,
                    surfaceContainerLowest = AmoledBlack
                )
            } else base
        }
        darkTheme && pureBlack -> AmoledColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extendedColors = ZurutialExtendedColors(
        syncGreen = SyncGreen,
        syncGreenContainer = SyncGreenContainer,
        syncOrange = SyncOrange,
        syncOrangeContainer = SyncOrangeContainer,
        syncRed = SyncRed,
        syncRedContainer = SyncRedContainer,
        heroGradient = if (darkTheme) listOf(Color(0xFF5B2FBF), Color(0xFF9A6BFF))
            else listOf(Color(0xFF7A4FE0), Color(0xFFB18CFF)),
        heroGradientSecondary = if (darkTheme) listOf(Color(0xFF1F6E63), Color(0xFF3D8FDB))
            else listOf(Color(0xFF2E9E8F), Color(0xFF4E8CE0))
    )

    androidx.compose.runtime.CompositionLocalProvider(LocalZurutialColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = ZurutialShapes,
            content = content
        )
    }
}
