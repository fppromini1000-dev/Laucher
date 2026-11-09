package com.example.model

enum class TimeOfDaySlot(
    val id: String,
    val titleEs: String,
    val titleEn: String,
    val startHour: Int,
    val endHour: Int,
    val iconEmoji: String,
    val defaultTone: String
) {
    MORNING(
        id = "morning",
        titleEs = "Mañana Activa",
        titleEn = "Active Morning",
        startHour = 5,
        endHour = 11,
        iconEmoji = "☀️",
        defaultTone = "Enfoque matutino y noticias"
    ),
    AFTERNOON(
        id = "afternoon",
        titleEs = "Tarde Productiva",
        titleEn = "Productive Afternoon",
        startHour = 12,
        endHour = 17,
        iconEmoji = "💼",
        defaultTone = "Trabajo, estudio y coordinación"
    ),
    EVENING(
        id = "evening",
        titleEs = "Noche Relajada",
        titleEn = "Relaxed Evening",
        startHour = 18,
        endHour = 22,
        iconEmoji = "🌙",
        defaultTone = "Ocio, descanso y redes"
    ),
    NIGHT(
        id = "night",
        titleEs = "Modo Nocturno",
        titleEn = "Night Mode",
        startHour = 23,
        endHour = 4,
        iconEmoji = "🌌",
        defaultTone = "Tranquilidad y desconexión"
    );

    companion object {
        fun current(hour: Int): TimeOfDaySlot {
            return when (hour) {
                in 5..11 -> MORNING
                in 12..17 -> AFTERNOON
                in 18..22 -> EVENING
                else -> NIGHT
            }
        }
    }
}

data class HabitPrediction(
    val slot: TimeOfDaySlot,
    val topApps: List<AppInfo>,
    val confidenceScore: Float,
    val explanationEs: String,
    val explanationEn: String
)
