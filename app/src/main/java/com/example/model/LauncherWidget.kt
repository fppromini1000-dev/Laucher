package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.ui.graphics.vector.ImageVector

enum class WidgetType(
    val id: String,
    val titleEs: String,
    val titleEn: String,
    val descriptionEs: String,
    val descriptionEn: String,
    val icon: ImageVector
) {
    WEATHER(
        id = "weather",
        titleEs = "Clima Minimalista",
        titleEn = "Minimalist Weather",
        descriptionEs = "Temperatura actual, sensación térmica y pronóstico en tiempo real.",
        descriptionEn = "Current temperature, feels like and real-time forecast.",
        icon = Icons.Default.Cloud
    ),
    FIRI_HABITS(
        id = "firi_habits",
        titleEs = "Rutina y Hábitos de Firi",
        titleEn = "Firi Habit Routine",
        descriptionEs = "Acceso rápido por voz a Firi y predicción horaria de tus hábitos.",
        descriptionEn = "Quick voice access to Firi and hourly habit predictions.",
        icon = Icons.Default.Psychology
    ),
    NOTES_TASKS(
        id = "notes_tasks",
        titleEs = "Notas Rápidas y Tareas",
        titleEn = "Quick Notes & Tasks",
        descriptionEs = "Lista de recordatorios y notas rápidas directo en la pantalla.",
        descriptionEn = "Quick checklist and sticky reminders on your home screen.",
        icon = Icons.Default.CheckCircle
    ),
    CALENDAR(
        id = "calendar",
        titleEs = "Agenda y Calendario",
        titleEn = "Agenda & Calendar",
        descriptionEs = "Fecha extendida y próximos eventos programados del día.",
        descriptionEn = "Extended date and today's upcoming scheduled agenda.",
        icon = Icons.Default.CalendarMonth
    ),
    MUSIC(
        id = "music",
        titleEs = "Reproductor Multimedia",
        titleEn = "Media Player",
        descriptionEs = "Controles estéticos de música en reproducción y volumen.",
        descriptionEn = "Aesthetic playback controls for currently playing music.",
        icon = Icons.Default.MusicNote
    );

    fun getDisplayName(isSpanish: Boolean) = if (isSpanish) titleEs else titleEn
    fun getDisplayDesc(isSpanish: Boolean) = if (isSpanish) descriptionEs else descriptionEn

    companion object {
        fun fromId(id: String): WidgetType = entries.firstOrNull { it.id == id } ?: WEATHER
    }
}

data class LauncherWidgetItem(
    val id: String,
    val type: WidgetType,
    val orderIndex: Int = 0,
    val isEnabled: Boolean = true,
    val customData: String = ""
)
