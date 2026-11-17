package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.preferences.AppThemeMode

@Immutable
data class NexoraExtendedColors(
    val cardBorder: Color,
    val primaryGlowBorder: Color,
    val cyanGlowBorder: Color,
    val surfaceElevated: Color,
    val surfaceSubtle: Color,
    val healthy: Color,
    val healthyBg: Color,
    val warning: Color,
    val warningBg: Color,
    val critical: Color,
    val criticalBg: Color,
    val isDark: Boolean
)

val LocalNexoraColors = staticCompositionLocalOf {
    NexoraExtendedColors(
        cardBorder = NexoraColors.BorderTranslucent,
        primaryGlowBorder = NexoraColors.BorderAccentSubtle,
        cyanGlowBorder = NexoraColors.BorderCyanSubtle,
        surfaceElevated = NexoraColors.SurfaceElevated,
        surfaceSubtle = NexoraColors.SurfaceSubtle,
        healthy = NexoraColors.StatusHealthy,
        healthyBg = NexoraColors.StatusHealthyBg,
        warning = NexoraColors.StatusWarning,
        warningBg = NexoraColors.StatusWarningBg,
        critical = NexoraColors.StatusCritical,
        criticalBg = NexoraColors.StatusCriticalBg,
        isDark = true
    )
}

private val ObsidianDarkScheme = darkColorScheme(
    primary = NexoraColors.Primary,
    onPrimary = NexoraColors.TextPrimary,
    primaryContainer = NexoraColors.PrimarySubtle,
    onPrimaryContainer = NexoraColors.TextPrimary,
    secondary = NexoraColors.Accent,
    onSecondary = NexoraColors.Background,
    secondaryContainer = NexoraColors.AccentSubtle,
    onSecondaryContainer = NexoraColors.Accent,
    tertiary = NexoraColors.Accent,
    background = NexoraColors.Background,
    onBackground = NexoraColors.TextPrimary,
    surface = NexoraColors.Surface,
    onSurface = NexoraColors.TextPrimary,
    surfaceVariant = NexoraColors.SurfaceElevated,
    onSurfaceVariant = NexoraColors.TextSecondary,
    outline = NexoraColors.BorderTranslucent,
    outlineVariant = NexoraColors.BorderAccentSubtle,
    error = NexoraColors.StatusCritical,
    onError = NexoraColors.TextPrimary
)

private val AmoledDarkScheme = ObsidianDarkScheme.copy(
    background = NexoraColors.BackgroundAmoled,
    surface = NexoraColors.SurfaceAmoled,
    surfaceVariant = NexoraColors.Surface
)

private val PrecisionLightScheme = lightColorScheme(
    primary = NexoraColors.PrimaryMuted,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7FF),
    onPrimaryContainer = Color(0xFF281266),
    secondary = Color(0xFF008B99),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6F7FA),
    onSecondaryContainer = Color(0xFF003B42),
    background = NexoraColors.LightBackground,
    onBackground = NexoraColors.LightTextPrimary,
    surface = NexoraColors.LightSurface,
    onSurface = NexoraColors.LightTextPrimary,
    surfaceVariant = NexoraColors.LightSurfaceElevated,
    onSurfaceVariant = NexoraColors.LightTextSecondary,
    outline = NexoraColors.LightBorder,
    outlineVariant = Color(0x336B42E6),
    error = NexoraColors.StatusCritical,
    onError = Color.White
)

@Composable
fun NexoraTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK_OBSIDIAN,
    content: @Composable () -> Unit
) {
    val isDark = themeMode != AppThemeMode.LIGHT_CLEAN
    val colorScheme = when (themeMode) {
        AppThemeMode.DARK_OBSIDIAN -> ObsidianDarkScheme
        AppThemeMode.AMOLED_BLACK -> AmoledDarkScheme
        AppThemeMode.LIGHT_CLEAN -> PrecisionLightScheme
    }

    val extendedColors = if (isDark) {
        NexoraExtendedColors(
            cardBorder = NexoraColors.BorderTranslucent,
            primaryGlowBorder = NexoraColors.BorderAccentSubtle,
            cyanGlowBorder = NexoraColors.BorderCyanSubtle,
            surfaceElevated = if (themeMode == AppThemeMode.AMOLED_BLACK) NexoraColors.Surface else NexoraColors.SurfaceElevated,
            surfaceSubtle = if (themeMode == AppThemeMode.AMOLED_BLACK) NexoraColors.BackgroundAmoled else NexoraColors.SurfaceSubtle,
            healthy = NexoraColors.StatusHealthy,
            healthyBg = NexoraColors.StatusHealthyBg,
            warning = NexoraColors.StatusWarning,
            warningBg = NexoraColors.StatusWarningBg,
            critical = NexoraColors.StatusCritical,
            criticalBg = NexoraColors.StatusCriticalBg,
            isDark = true
        )
    } else {
        NexoraExtendedColors(
            cardBorder = NexoraColors.LightBorder,
            primaryGlowBorder = Color(0x336B42E6),
            cyanGlowBorder = Color(0x33008B99),
            surfaceElevated = NexoraColors.LightSurfaceElevated,
            surfaceSubtle = NexoraColors.LightBackground,
            healthy = Color(0xFF15803D),
            healthyBg = Color(0x1A15803D),
            warning = Color(0xFFB45309),
            warningBg = Color(0x1AB45309),
            critical = Color(0xFFB91C1C),
            criticalBg = Color(0x1AB91C1C),
            isDark = false
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalNexoraColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NexoraTypography,
            shapes = NexoraShapes,
            content = content
        )
    }
}
