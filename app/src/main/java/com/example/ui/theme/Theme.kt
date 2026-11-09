package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalLauncherPalette = staticCompositionLocalOf { PaletteManager.IOS_DEFAULT }

@Composable
fun FiriLauncherTheme(
    palette: LauncherPalette = PaletteManager.IOS_DEFAULT,
    content: @Composable () -> Unit
) {
    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.surfaceVariant,
            onPrimaryContainer = palette.primary,
            secondary = palette.secondary,
            onSecondary = palette.onPrimary,
            secondaryContainer = palette.surfaceVariant,
            onSecondaryContainer = palette.secondary,
            tertiary = palette.tertiary,
            onTertiary = palette.onPrimary,
            background = palette.background,
            onBackground = palette.onBackground,
            surface = palette.surface,
            onSurface = palette.onSurface,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.onSurface.copy(alpha = 0.7f),
            outline = palette.cardBorder
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            primaryContainer = palette.surfaceVariant,
            onPrimaryContainer = palette.primary,
            secondary = palette.secondary,
            onSecondary = Color.White,
            secondaryContainer = palette.surfaceVariant,
            onSecondaryContainer = palette.secondary,
            tertiary = palette.tertiary,
            onTertiary = Color.White,
            background = palette.background,
            onBackground = palette.onBackground,
            surface = palette.surface,
            onSurface = palette.onSurface,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.onSurface.copy(alpha = 0.7f),
            outline = palette.cardBorder
        )
    }

    CompositionLocalProvider(LocalLauncherPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
