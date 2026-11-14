package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Memoria conversacional y de búsquedas de Firi persistida en SQLite (Room).
 * Permite a Firi recordar consultas previas, contexto y temas consultados
 * para ofrecer respuestas personalizadas tanto en la nube como offline.
 */
@Entity(
    tableName = "firi_sql_memory",
    indices = [
        Index(value = ["keywords"]),
        Index(value = ["timestamp"])
    ]
)
data class FiriMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userQuery: String,
    val keywords: String,
    val summaryResponse: String,
    val sourceTypes: String, // ej. "MAPS,SEARCH,SQL"
    val hitCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Lugares y mapas descubiertos por Firi mediante Google Maps Grounding o Geocoder,
 * almacenados en SQLite (Room) para acceso rápido incluso sin conexión.
 */
@Entity(
    tableName = "firi_saved_places",
    indices = [
        Index(value = ["title"]),
        Index(value = ["lastSearchedAt"])
    ]
)
data class FiriSavedPlaceEntity(
    @PrimaryKey
    val placeKey: String,
    val title: String,
    val address: String = "",
    val category: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val mapsUri: String = "",
    val placeId: String? = null,
    val searchCount: Int = 1,
    val lastSearchedAt: Long = System.currentTimeMillis()
)
