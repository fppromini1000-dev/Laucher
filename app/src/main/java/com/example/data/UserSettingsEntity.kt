package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val isOnboardingComplete: Boolean = false,
    val paletteId: String = "nordic",
    val languageCode: String = "es",
    val regionCode: String = "ES",
    val voiceOutputEnabled: Boolean = true,
    val showFiriHabitBar: Boolean = true,
    val useDeviceWallpaper: Boolean = true,
    val userName: String = "Usuario",
    val lockSecurityType: String = "SWIPE",
    val lockPinCode: String = "",
    val lockPatternCode: String = "",
    val lockClockFont: String = "BOLD",
    val lockClockColor: String = "#FFFFFF",
    val lockMiniWidgetsString: String = "WEATHER_MAIN,BATTERY,CALENDAR",
    val lockWallpaperId: String = "TITANIUM_WAVES",
    val lockCustomWallpaperUri: String = "",
    val homeWallpaperId: String = "HARMONY_NEXT",
    val homeCustomWallpaperUri: String = "",
    val favoriteAppsString: String = "",
    val isFiriEnabled: Boolean = true,
    val isDarkMode: Boolean = true,
    val appIconSize: String = "NORMAL"
)
