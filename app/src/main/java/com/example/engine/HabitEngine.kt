package com.example.engine

import com.example.data.UsageHabitDao
import com.example.data.UsageHabitEntity
import com.example.model.AppCategory
import com.example.model.AppInfo
import com.example.model.HabitPrediction
import com.example.model.TimeOfDaySlot
import java.util.Calendar

class HabitEngine(private val usageHabitDao: UsageHabitDao) {

    suspend fun recordLaunch(app: AppInfo) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val day = calendar.get(Calendar.DAY_OF_WEEK)

        val existing = usageHabitDao.findHabit(app.packageName, hour, day)
        if (existing != null) {
            usageHabitDao.insertOrUpdateHabit(
                existing.copy(
                    launchCount = existing.launchCount + 1,
                    lastUsedTimestamp = System.currentTimeMillis(),
                    appName = app.label
                )
            )
        } else {
            usageHabitDao.insertOrUpdateHabit(
                UsageHabitEntity(
                    packageName = app.packageName,
                    appName = app.label,
                    hourOfDay = hour,
                    dayOfWeek = day,
                    launchCount = 1,
                    lastUsedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun getPredictions(allApps: List<AppInfo>): HabitPrediction {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentSlot = TimeOfDaySlot.current(currentHour)

        // Query habits for the current slot range
        val recordedSlotHabits = usageHabitDao.getTopAppsForHourRange(
            currentSlot.startHour,
            if (currentSlot == TimeOfDaySlot.NIGHT) 23 else currentSlot.endHour,
            limit = 6
        )

        val predictedApps = mutableListOf<AppInfo>()

        // 1. Add recorded habit apps if available
        for (habit in recordedSlotHabits) {
            val matchingApp = allApps.firstOrNull { it.packageName == habit.packageName }
            if (matchingApp != null && predictedApps.none { it.packageName == matchingApp.packageName }) {
                predictedApps.add(matchingApp)
            }
        }

        // 2. If not enough learned habits yet, recommend category-aligned apps for this time slot
        if (predictedApps.size < 4) {
            val slotPreferredCategories = when (currentSlot) {
                TimeOfDaySlot.MORNING -> listOf(AppCategory.PRODUCTIVITY, AppCategory.UTILITIES, AppCategory.HEALTH)
                TimeOfDaySlot.AFTERNOON -> listOf(AppCategory.PRODUCTIVITY, AppCategory.UTILITIES, AppCategory.CREATIVITY)
                TimeOfDaySlot.EVENING -> listOf(AppCategory.SOCIAL, AppCategory.ENTERTAINMENT, AppCategory.GAMES)
                TimeOfDaySlot.NIGHT -> listOf(AppCategory.HEALTH, AppCategory.ENTERTAINMENT, AppCategory.UTILITIES)
            }

            for (cat in slotPreferredCategories) {
                val candidate = allApps.firstOrNull {
                    it.category == cat && predictedApps.none { p -> p.packageName == it.packageName }
                }
                if (candidate != null) {
                    predictedApps.add(candidate)
                }
                if (predictedApps.size >= 5) break
            }
        }

        val hasRealData = recordedSlotHabits.isNotEmpty()
        val confidence = if (hasRealData) 0.88f else 0.65f

        val explanationEs = if (hasRealData) {
            "Firi ha aprendido que en este horario (${currentSlot.titleEs}) sueles abrir estas aplicaciones con frecuencia."
        } else {
            "Sugerencias inteligentes de Firi para tu ${currentSlot.titleEs}. Aprenderé tus rutinas conforme uses el launcher."
        }

        val explanationEn = if (hasRealData) {
            "Firi learned that during ${currentSlot.titleEn} you frequently open these apps."
        } else {
            "Firi smart suggestions for your ${currentSlot.titleEn}. I'll adapt to your daily routines as you use the launcher."
        }

        return HabitPrediction(
            slot = currentSlot,
            topApps = predictedApps.take(5),
            confidenceScore = confidence,
            explanationEs = explanationEs,
            explanationEn = explanationEn
        )
    }

    suspend fun generateHabitSummary(allApps: List<AppInfo>, isSpanish: Boolean): String {
        val calendar = Calendar.getInstance()
        val currentSlot = TimeOfDaySlot.current(calendar.get(Calendar.HOUR_OF_DAY))
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        val topHabits = usageHabitDao.getTopAppsForHourRange(0, 23, limit = 5)
        if (topHabits.isEmpty()) {
            return if (isSpanish) {
                "Hola, soy Firi. Aún estoy calibrando tus hábitos. Abre algunas aplicaciones a lo largo del día y aprenderé tus momentos preferidos para cada una."
            } else {
                "Hello, I am Firi. I am still learning your habits. Open apps throughout your day and I will adapt to your favorite routines."
            }
        }

        val topNames = topHabits.joinToString(", ") { it.appName }
        return if (isSpanish) {
            "Análisis de Firi: En tu ${currentSlot.titleEs} (${currentHour}:00h), tus aplicaciones más frecuentes son: $topNames. Tu rutina diaria se va consolidando positivamente."
        } else {
            "Firi's Analysis: During your ${currentSlot.titleEn} (${currentHour}:00h), your most frequent apps are: $topNames. Your daily routine is building up nicely."
        }
    }
}
