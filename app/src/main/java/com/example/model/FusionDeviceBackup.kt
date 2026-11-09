package com.example.model

import com.example.ai.FiriChatMessage

data class FusionDeviceBackup(
    val deviceName: String = "Fusion Phone Pro",
    val systemVersion: String = "Fusion OS 3.0.9",
    val wallpaperId: String = "IOS_ASTRONOMY",
    val lockWallpaperId: String = "IOS_EARTH",
    val lockClockFont: String = "BOLD",
    val lockClockColor: String = "#FFFFFF",
    val lockMiniWidgets: List<String> = listOf("WEATHER_MAIN", "BATTERY", "CALENDAR"),
    val isDarkMode: Boolean = true,
    val isFiriEnabled: Boolean = true,
    val sampleFiriChats: List<FiriChatMessage> = emptyList()
)
