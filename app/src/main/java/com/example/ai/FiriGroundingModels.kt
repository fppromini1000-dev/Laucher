package com.example.ai

import com.example.data.FiriMemoryEntity
import com.example.data.FiriSavedPlaceEntity

/**
 * Lugar geográfico retornado por Google Maps Grounding, Geocoder o caché SQL.
 */
data class FiriMapPlace(
    val title: String,
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val mapsUri: String = "",
    val placeId: String? = null,
    val category: String = "",
    val fromSqlCache: Boolean = false
)

/**
 * Fuente web retornada por Google Search Grounding.
 */
data class FiriWebSource(
    val title: String,
    val uri: String,
    val domain: String = ""
)

/**
 * Resumen instantáneo de datos SQL locales (hábitos, notas, lugares y recuerdos)
 * inyectado en el contexto de Firi (Nube y Segundo Cerebro).
 */
data class FiriSqlContextSnapshot(
    val topAppsSummary: String = "",
    val currentHourAppsSummary: String = "",
    val notesSummary: String = "",
    val pinnedAppsSummary: String = "",
    val matchingMemories: List<FiriMemoryEntity> = emptyList(),
    val recentMemories: List<FiriMemoryEntity> = emptyList(),
    val matchingPlaces: List<FiriSavedPlaceEntity> = emptyList(),
    val recentPlaces: List<FiriSavedPlaceEntity> = emptyList(),
    val totalMemoriesCount: Int = 0,
    val totalSavedPlacesCount: Int = 0
) {
    val hasRichData: Boolean
        get() = topAppsSummary.isNotBlank() ||
            notesSummary.isNotBlank() ||
            matchingMemories.isNotEmpty() ||
            matchingPlaces.isNotEmpty() ||
            recentPlaces.isNotEmpty()
}
