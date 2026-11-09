package com.example.model

import com.example.R

data class PresetWallpaper(
    val id: String,
    val nameEs: String,
    val nameEn: String,
    val drawableRes: Int,
    val descriptionEs: String
)

object WallpaperManager {
    // Los fondos de pantalla predefinidos de alta calidad:
    // Incluye los 4 fondos estéticos suministrados por el usuario (Ondas Titanium, Gota de Cristal, Ondas Turquesa/Azul y Azul Profundo / Noche)
    // más el fondo oficial HarmonyOS NEXT y Fluido iOS 18.
    val PRESETS: List<PresetWallpaper> = listOf(
        PresetWallpaper(
            id = "TITANIUM_WAVES",
            nameEs = "Titanio Ondas",
            nameEn = "Titanium Curves",
            drawableRes = R.drawable.wallpaper_ios_mud,
            descriptionEs = "Escultura en capas orgánicas y tonos titanio cálido"
        ),
        PresetWallpaper(
            id = "CRYSTAL_DROP",
            nameEs = "Gota de Cristal",
            nameEn = "Crystal Prism",
            drawableRes = R.drawable.wallpaper_ios_glass,
            descriptionEs = "Prisma de vidrio líquido con refracción cromática"
        ),
        PresetWallpaper(
            id = "TURQUOISE_WAVES",
            nameEs = "Brisa Turquesa",
            nameEn = "Turquoise Waves",
            drawableRes = R.drawable.wallpaper_geom_light,
            descriptionEs = "Formas circulares y degradado celeste fresco"
        ),
        PresetWallpaper(
            id = "DEEP_BLUE",
            nameEs = "Azul Profundo",
            nameEn = "Midnight Blue",
            drawableRes = R.drawable.wallpaper_geom_dark,
            descriptionEs = "Composición oscura nocturna en azul cobalto y esmeralda"
        ),
        PresetWallpaper(
            id = "HARMONY_NEXT",
            nameEs = "HarmonyOS NEXT",
            nameEn = "HarmonyOS NEXT",
            drawableRes = R.drawable.harmonyos_next_wallpaper,
            descriptionEs = "Fondo oficial icónico con iluminación pura"
        ),
        PresetWallpaper(
            id = "FLUID_AURORA",
            nameEs = "Aurora Fluida",
            nameEn = "Fluid Aurora",
            drawableRes = R.drawable.lockscreen_wallpaper,
            descriptionEs = "Estilo iOS 18 con degradados vivos y profundos"
        )
    )

    fun getPreset(id: String): PresetWallpaper {
        return PRESETS.firstOrNull { it.id == id } ?: PRESETS.first()
    }
}
