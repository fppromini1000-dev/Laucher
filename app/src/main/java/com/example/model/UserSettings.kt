package com.example.model

data class RegionItem(
    val code: String,
    val flag: String,
    val nameEs: String,
    val nameEn: String,
    val defaultLanguage: String
)

object SupportedLocales {
    val regions = listOf(
        RegionItem("ES", "🇪🇸", "España", "Spain", "es"),
        RegionItem("MX", "🇲🇽", "México", "Mexico", "es"),
        RegionItem("CO", "🇨🇴", "Colombia", "Colombia", "es"),
        RegionItem("AR", "🇦🇷", "Argentina", "Argentina", "es"),
        RegionItem("CL", "🇨🇱", "Chile", "Chile", "es"),
        RegionItem("PE", "🇵🇪", "Perú", "Peru", "es"),
        RegionItem("US_ES", "🌎", "Estados Unidos / LatAm", "United States / LatAm", "es"),
        RegionItem("US", "🇺🇸", "United States", "United States", "en"),
        RegionItem("GB", "🇬🇧", "United Kingdom", "United Kingdom", "en")
    )

    fun getRegion(code: String): RegionItem {
        return regions.firstOrNull { it.code == code } ?: regions.first()
    }
}

data class UserSettings(
    val isOnboardingComplete: Boolean = false,
    val paletteId: String = "nordic",
    val languageCode: String = "es",
    val regionCode: String = "ES",
    val voiceOutputEnabled: Boolean = true,
    val showFiriHabitBar: Boolean = true,
    val useDeviceWallpaper: Boolean = true,
    val userName: String = "Usuario",
    // Configuración de Pantalla de Bloqueo
    val lockSecurityType: String = "SWIPE", // "SWIPE", "PIN", "PATTERN"
    val lockPinCode: String = "",
    val lockPatternCode: String = "",
    val lockClockFont: String = "BOLD", // "BOLD", "SERIF", "ROUNDED", "TECH", "THIN"
    val lockClockColor: String = "#FFFFFF",
    val lockMiniWidgets: List<String> = listOf("WEATHER_MAIN", "BATTERY", "CALENDAR"),
    val lockWallpaperId: String = "TITANIUM_WAVES",
    val lockCustomWallpaperUri: String = "",
    // Fondo de Pantalla Principal (Inicio)
    val homeWallpaperId: String = "HARMONY_NEXT",
    val homeCustomWallpaperUri: String = "",
    // Apps Favoritas en la Pantalla Principal (sustituyen los 3 widgets estáticos)
    val favoriteAppPackageNames: List<String> = emptyList(),
    // Interruptor para apagar o encender Firi
    val isFiriEnabled: Boolean = true,
    // Modo Claro / Modo Oscuro estilo iOS
    val isDarkMode: Boolean = true,
    // Tamaño de iconos estilo iOS Aspecto (NORMAL, MEDIUM, LARGE)
    val appIconSize: String = "NORMAL"
) {
    val isSpanish: Boolean get() = languageCode == "es"
}
