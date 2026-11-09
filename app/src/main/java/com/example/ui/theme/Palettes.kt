package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class LauncherPalette(
    val id: String,
    val nameEs: String,
    val nameEn: String,
    val descriptionEs: String,
    val descriptionEn: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onPrimary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val accentGlow: Color,
    val cardBorder: Color,
    val isDark: Boolean = true
)

object PaletteManager {
    // Paleta oficial de iOS (Apple Blue & Frosted Glass)
    val IOS_DEFAULT = LauncherPalette(
        id = "ios",
        nameEs = "iOS Clásico",
        nameEn = "Classic iOS",
        descriptionEs = "Estilo Apple puro: azul icónico, superficies translúcidas y tipografía nítida.",
        descriptionEn = "Pure Apple style: iconic blue, translucent frosted glass, and crisp typography.",
        primary = Color(0xFF007AFF),
        secondary = Color(0xFF5856D6),
        tertiary = Color(0xFF34C759),
        background = Color(0xFF000000),
        surface = Color(0xFF1C1C1E),
        surfaceVariant = Color(0xFF2C2C2E),
        onPrimary = Color(0xFFFFFFFF),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFF2F2F7),
        accentGlow = Color(0x40007AFF),
        cardBorder = Color(0x30FFFFFF),
        isDark = true
    )

    val ONYX_MINIMAL = LauncherPalette(
        id = "onyx",
        nameEs = "Titanio Negro",
        nameEn = "Black Titanium",
        descriptionEs = "Monocromático, titanio y obsidiana pura.",
        descriptionEn = "Monochrome, titanium and pure obsidian.",
        primary = Color(0xFFECEFF4),
        secondary = Color(0xFFD8DEE9),
        tertiary = Color(0xFF88C0D0),
        background = Color(0xFF0C0E12),
        surface = Color(0xFF161920),
        surfaceVariant = Color(0xFF222631),
        onPrimary = Color(0xFF0C0E12),
        onBackground = Color(0xFFF0F2F5),
        onSurface = Color(0xFFE4E7EB),
        accentGlow = Color(0x33ECEFF4),
        cardBorder = Color(0x22FFFFFF),
        isDark = true
    )

    val NORDIC_PINE = LauncherPalette(
        id = "nordic",
        nameEs = "Verde Alpino",
        nameEn = "Alpine Green",
        descriptionEs = "Verde salvia, musgo nórdico y calma orgánica.",
        descriptionEn = "Sage green, nordic moss and organic calm.",
        primary = Color(0xFF52B788),
        secondary = Color(0xFF74C69D),
        tertiary = Color(0xFFB7E4C7),
        background = Color(0xFF0B1411),
        surface = Color(0xFF13201C),
        surfaceVariant = Color(0xFF1B2F29),
        onPrimary = Color(0xFF081C15),
        onBackground = Color(0xFFE8F1EC),
        onSurface = Color(0xFFDCE7E1),
        accentGlow = Color(0x3352B788),
        cardBorder = Color(0x2252B788),
        isDark = true
    )

    val SUNSET_AMBER = LauncherPalette(
        id = "sunset",
        nameEs = "Oro y Ámbar",
        nameEn = "Gold & Amber",
        descriptionEs = "Terracota cálido, ámbar crepuscular y bronce.",
        descriptionEn = "Warm terracotta, twilight amber and bronze.",
        primary = Color(0xFFE07A5F),
        secondary = Color(0xFFF4A261),
        tertiary = Color(0xFFE9C46A),
        background = Color(0xFF140F11),
        surface = Color(0xFF20161A),
        surfaceVariant = Color(0xFF2E1F25),
        onPrimary = Color(0xFF1F0E09),
        onBackground = Color(0xFFF9ECE8),
        onSurface = Color(0xFFF0DDD7),
        accentGlow = Color(0x33E07A5F),
        cardBorder = Color(0x25E07A5F),
        isDark = true
    )

    val CYBER_NEON = LauncherPalette(
        id = "cyber",
        nameEs = "Púrpura Profundo",
        nameEn = "Deep Purple",
        descriptionEs = "Violeta eléctrico, cian sintetizador y noche digital.",
        descriptionEn = "Electric violet, synth cyan and digital night.",
        primary = Color(0xFFB5179E),
        secondary = Color(0xFF7209B7),
        tertiary = Color(0xFF4CC9F0),
        background = Color(0xFF090714),
        surface = Color(0xFF130F26),
        surfaceVariant = Color(0xFF1F173D),
        onPrimary = Color(0xFFFFFFFF),
        onBackground = Color(0xFFF1EEFF),
        onSurface = Color(0xFFE2DCF8),
        accentGlow = Color(0x33B5179E),
        cardBorder = Color(0x28B5179E),
        isDark = true
    )

    val SOLAR_FLARE = LauncherPalette(
        id = "solar",
        nameEs = "Titanio Natural",
        nameEn = "Natural Titanium",
        descriptionEs = "Oro tostado, champaña y carbono cálido.",
        descriptionEn = "Toasted gold, champagne and warm carbon.",
        primary = Color(0xFFE9C46A),
        secondary = Color(0xFFF4A261),
        tertiary = Color(0xFFFFD166),
        background = Color(0xFF13110E),
        surface = Color(0xFF1E1B16),
        surfaceVariant = Color(0xFF2C2720),
        onPrimary = Color(0xFF201705),
        onBackground = Color(0xFFFAF4EA),
        onSurface = Color(0xFFEDE4D5),
        accentGlow = Color(0x33E9C46A),
        cardBorder = Color(0x22E9C46A),
        isDark = true
    )

    val OCEAN_BREEZE = LauncherPalette(
        id = "ocean",
        nameEs = "Azul Pacífico",
        nameEn = "Pacific Blue",
        descriptionEs = "Azul zafiro, espuma marina y profundidad abisal.",
        descriptionEn = "Sapphire blue, marine foam and abyssal depth.",
        primary = Color(0xFF0096C7),
        secondary = Color(0xFF48CAE4),
        tertiary = Color(0xFF90E0EF),
        background = Color(0xFF08101A),
        surface = Color(0xFF0E1C2B),
        surfaceVariant = Color(0xFF152A40),
        onPrimary = Color(0xFF02121E),
        onBackground = Color(0xFFE5F4FB),
        onSurface = Color(0xFFD3EBF5),
        accentGlow = Color(0x330096C7),
        cardBorder = Color(0x220096C7),
        isDark = true
    )

    val allPalettes = listOf(
        IOS_DEFAULT,
        NORDIC_PINE,
        ONYX_MINIMAL,
        SUNSET_AMBER,
        CYBER_NEON,
        SOLAR_FLARE,
        OCEAN_BREEZE
    )

    fun getPalette(id: String, isDarkMode: Boolean = true): LauncherPalette {
        val base = allPalettes.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: IOS_DEFAULT
        return if (isDarkMode) {
            base
        } else {
            // Adaptar a Modo Claro puro estilo iOS (fondo F2F2F7, tarjetas blancas, texto oscuro)
            base.copy(
                background = Color(0xFFF2F2F7),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFE5E5EA),
                onBackground = Color(0xFF1C1C1E),
                onSurface = Color(0xFF1C1C1E),
                cardBorder = Color(0x1F000000),
                accentGlow = base.primary.copy(alpha = 0.22f),
                isDark = false
            )
        }
    }

    fun getPalette(id: String): LauncherPalette = getPalette(id, isDarkMode = true)
}
