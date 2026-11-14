package com.example.data

import android.content.Context
import com.example.engine.AppCategorizer
import com.example.engine.HabitEngine
import com.example.model.AppCategory
import com.example.model.AppFolder
import com.example.model.AppInfo
import com.example.model.HabitPrediction
import com.example.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LauncherRepository(
    private val context: Context,
    private val database: LauncherDatabase
) {
    private val appDao = database.appCustomizationDao()
    private val habitDao = database.usageHabitDao()
    private val settingsDao = database.userSettingsDao()

    private val widgetDao = database.widgetDao()
    private val firiSqlDao = database.firiSqlDao()

    val savedPlacesFlow: Flow<List<FiriSavedPlaceEntity>> = firiSqlDao.getSavedPlacesFlow().flowOn(Dispatchers.IO)
    val firiMemoryCountFlow: Flow<Int> = firiSqlDao.getMemoryCountFlow().flowOn(Dispatchers.IO)
    val savedPlacesCountFlow: Flow<Int> = firiSqlDao.getSavedPlacesCountFlow().flowOn(Dispatchers.IO)

    val habitEngine = HabitEngine(habitDao)

    // Flow of UserSettings
    val settingsFlow: Flow<UserSettings> = settingsDao.getSettingsFlow().map { entity ->
        if (entity == null) {
            UserSettings()
        } else {
            UserSettings(
                isOnboardingComplete = entity.isOnboardingComplete,
                paletteId = entity.paletteId,
                languageCode = entity.languageCode,
                regionCode = entity.regionCode,
                voiceOutputEnabled = entity.voiceOutputEnabled,
                showFiriHabitBar = entity.showFiriHabitBar,
                useDeviceWallpaper = entity.useDeviceWallpaper,
                userName = entity.userName,
                lockSecurityType = entity.lockSecurityType,
                lockPinCode = entity.lockPinCode,
                lockPatternCode = entity.lockPatternCode,
                lockClockFont = entity.lockClockFont,
                lockClockColor = entity.lockClockColor,
                lockMiniWidgets = when {
                    entity.lockMiniWidgetsString == "EMPTY" -> emptyList()
                    entity.lockMiniWidgetsString.isNotBlank() -> entity.lockMiniWidgetsString.split(",").filter { it.isNotBlank() }
                    else -> listOf("WEATHER_MAIN", "BATTERY", "CALENDAR")
                },
                lockWallpaperId = entity.lockWallpaperId,
                lockCustomWallpaperUri = entity.lockCustomWallpaperUri,
                homeWallpaperId = entity.homeWallpaperId,
                homeCustomWallpaperUri = entity.homeCustomWallpaperUri,
                favoriteAppPackageNames = if (entity.favoriteAppsString.isNotBlank()) entity.favoriteAppsString.split(",") else emptyList(),
                isFiriEnabled = entity.isFiriEnabled,
                isDarkMode = entity.isDarkMode,
                appIconSize = entity.appIconSize
            )
        }
    }.flowOn(Dispatchers.IO)

    // Flow of Home Screen Widgets
    val widgetsFlow: Flow<List<com.example.model.LauncherWidgetItem>> = widgetDao.getAllWidgetsFlow().map { entities ->
        if (entities.isEmpty()) {
            // Default starter widgets
            listOf(
                com.example.model.LauncherWidgetItem(
                    id = "default_weather",
                    type = com.example.model.WidgetType.WEATHER,
                    orderIndex = 0
                ),
                com.example.model.LauncherWidgetItem(
                    id = "default_notes",
                    type = com.example.model.WidgetType.NOTES_TASKS,
                    orderIndex = 1,
                    customData = "Revisar correo;Preparar presentación;Llamar a equipo"
                )
            )
        } else {
            entities.map {
                com.example.model.LauncherWidgetItem(
                    id = it.id,
                    type = com.example.model.WidgetType.fromId(it.typeId),
                    orderIndex = it.orderIndex,
                    isEnabled = it.isEnabled,
                    customData = it.customData
                )
            }.sortedBy { it.orderIndex }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun addWidget(type: com.example.model.WidgetType) = withContext(Dispatchers.IO) {
        val newId = "widget_${type.id}_${System.currentTimeMillis()}"
        val entity = LauncherWidgetEntity(
            id = newId,
            typeId = type.id,
            orderIndex = 99,
            isEnabled = true,
            customData = if (type == com.example.model.WidgetType.NOTES_TASKS) "Mi primera nota en Firi Launcher" else ""
        )
        widgetDao.insertOrUpdateWidget(entity)
    }

    suspend fun removeWidget(id: String) = withContext(Dispatchers.IO) {
        widgetDao.deleteWidget(id)
    }

    suspend fun updateWidgetData(id: String, customData: String, type: com.example.model.WidgetType, order: Int) = withContext(Dispatchers.IO) {
        widgetDao.insertOrUpdateWidget(
            LauncherWidgetEntity(
                id = id,
                typeId = type.id,
                orderIndex = order,
                isEnabled = true,
                customData = customData
            )
        )
    }

    suspend fun reorderWidgets(reorderedList: List<com.example.model.LauncherWidgetItem>) = withContext(Dispatchers.IO) {
        val entities = reorderedList.mapIndexed { index, item ->
            LauncherWidgetEntity(
                id = item.id,
                typeId = item.type.id,
                orderIndex = index,
                isEnabled = item.isEnabled,
                customData = item.customData
            )
        }
        widgetDao.insertAllWidgets(entities)
    }

    // Installed apps merged with user customizations
    fun getCategorizedAppsFlow(): Flow<List<AppInfo>> {
        val installed = AppCategorizer.loadInstalledApps(context)
        return appDao.getAllCustomizations().map { customizations ->
            val customMap = customizations.associateBy { it.packageName }
            installed.map { app ->
                val custom = customMap[app.packageName]
                if (custom != null) {
                    app.copy(
                        category = AppCategory.fromId(custom.categoryId),
                        isPinned = custom.isPinned,
                        isHidden = custom.isHidden,
                        customOrder = custom.customOrder
                    )
                } else {
                    app
                }
            }.filterNot { it.isHidden }
                .sortedWith(
                    compareByDescending<AppInfo> { it.isPinned }
                        .thenBy { it.customOrder }
                        .thenBy { it.label.lowercase() }
                )
        }.flowOn(Dispatchers.IO)
    }

    suspend fun hideOrRemoveApp(packageName: String, currentCategory: AppCategory) = withContext(Dispatchers.IO) {
        val existing = appDao.getCustomization(packageName)
        val updated = existing?.copy(isHidden = true) ?: AppEntity(
            packageName = packageName,
            categoryId = currentCategory.id,
            isHidden = true
        )
        appDao.insertCustomization(updated)
    }

    suspend fun reorderAppsList(reorderedList: List<AppInfo>) = withContext(Dispatchers.IO) {
        reorderedList.forEachIndexed { index, app ->
            val existing = appDao.getCustomization(app.packageName)
            val updated = existing?.copy(customOrder = index) ?: AppEntity(
                packageName = app.packageName,
                categoryId = app.category.id,
                customOrder = index,
                isPinned = app.isPinned
            )
            appDao.insertCustomization(updated)
        }
    }

    suspend fun saveSettings(settings: UserSettings) = withContext(Dispatchers.IO) {
        settingsDao.saveSettings(
            UserSettingsEntity(
                id = 1,
                isOnboardingComplete = settings.isOnboardingComplete,
                paletteId = settings.paletteId,
                languageCode = settings.languageCode,
                regionCode = settings.regionCode,
                voiceOutputEnabled = settings.voiceOutputEnabled,
                showFiriHabitBar = settings.showFiriHabitBar,
                useDeviceWallpaper = settings.useDeviceWallpaper,
                userName = settings.userName,
                lockSecurityType = settings.lockSecurityType,
                lockPinCode = settings.lockPinCode,
                lockPatternCode = settings.lockPatternCode,
                lockClockFont = settings.lockClockFont,
                lockClockColor = settings.lockClockColor,
                lockMiniWidgetsString = if (settings.lockMiniWidgets.isEmpty()) "EMPTY" else settings.lockMiniWidgets.joinToString(","),
                lockWallpaperId = settings.lockWallpaperId,
                lockCustomWallpaperUri = settings.lockCustomWallpaperUri,
                homeWallpaperId = settings.homeWallpaperId,
                homeCustomWallpaperUri = settings.homeCustomWallpaperUri,
                favoriteAppsString = settings.favoriteAppPackageNames.joinToString(","),
                isFiriEnabled = settings.isFiriEnabled,
                isDarkMode = settings.isDarkMode,
                appIconSize = settings.appIconSize
            )
        )
    }

    suspend fun updateAppCategory(packageName: String, category: AppCategory) = withContext(Dispatchers.IO) {
        val existing = appDao.getCustomization(packageName)
        val updated = existing?.copy(categoryId = category.id) ?: AppEntity(
            packageName = packageName,
            categoryId = category.id
        )
        appDao.insertCustomization(updated)
    }

    suspend fun togglePinApp(packageName: String, currentCategory: AppCategory) = withContext(Dispatchers.IO) {
        val existing = appDao.getCustomization(packageName)
        val updated = existing?.copy(isPinned = !existing.isPinned) ?: AppEntity(
            packageName = packageName,
            categoryId = currentCategory.id,
            isPinned = true
        )
        appDao.insertCustomization(updated)
    }

    suspend fun recordAppLaunch(app: AppInfo) = withContext(Dispatchers.IO) {
        habitEngine.recordLaunch(app)
    }

    suspend fun getHabitPredictions(allApps: List<AppInfo>): HabitPrediction = withContext(Dispatchers.IO) {
        habitEngine.getPredictions(allApps)
    }

    suspend fun getHabitSummary(allApps: List<AppInfo>, isSpanish: Boolean): String = withContext(Dispatchers.IO) {
        habitEngine.generateHabitSummary(allApps, isSpanish)
    }

    suspend fun clearHabits() = withContext(Dispatchers.IO) {
        habitDao.clearAllHabits()
    }

    // ==========================================
    // CARPETAS DE APLICACIONES (Persistencia local)
    // ==========================================
    private val folderPrefs = context.getSharedPreferences("launcher_folders_prefs", Context.MODE_PRIVATE)
    private val _foldersFlow = MutableStateFlow<List<AppFolder>>(loadFoldersFromPrefs())
    val foldersFlow: StateFlow<List<AppFolder>> = _foldersFlow.asStateFlow()

    private fun loadFoldersFromPrefs(): List<AppFolder> {
        val raw = folderPrefs.getString("folders_data", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(raw)
            val result = mutableListOf<AppFolder>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val pkgsArray = obj.getJSONArray("apps")
                val pkgs = mutableListOf<String>()
                for (j in 0 until pkgsArray.length()) {
                    pkgs.add(pkgsArray.getString(j))
                }
                result.add(AppFolder(id = id, name = name, appPackageNames = pkgs))
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistFolders(folders: List<AppFolder>) {
        try {
            val jsonArray = JSONArray()
            for (folder in folders) {
                val obj = JSONObject()
                obj.put("id", folder.id)
                obj.put("name", folder.name)
                val pkgsArray = JSONArray()
                folder.appPackageNames.forEach { pkgsArray.put(it) }
                obj.put("apps", pkgsArray)
                jsonArray.put(obj)
            }
            folderPrefs.edit().putString("folders_data", jsonArray.toString()).apply()
            _foldersFlow.value = folders
        } catch (_: Exception) {}
    }

    suspend fun createFolderWithApps(firstPackage: String, secondPackage: String, defaultName: String = "Carpeta"): AppFolder = withContext(Dispatchers.IO) {
        val current = _foldersFlow.value.toMutableList()
        val newFolderId = "folder_${System.currentTimeMillis()}"
        val pkgs = listOf(firstPackage, secondPackage).distinct()
        val folder = AppFolder(id = newFolderId, name = defaultName, appPackageNames = pkgs)
        current.add(folder)
        persistFolders(current)
        folder
    }

    suspend fun addAppToFolder(folderId: String, packageName: String) = withContext(Dispatchers.IO) {
        val current = _foldersFlow.value.map { folder ->
            if (folder.id == folderId) {
                val updatedPkgs = (folder.appPackageNames + packageName).distinct()
                folder.copy(appPackageNames = updatedPkgs)
            } else {
                folder
            }
        }
        persistFolders(current)
    }

    suspend fun removeAppFromFolder(folderId: String, packageName: String) = withContext(Dispatchers.IO) {
        val current = _foldersFlow.value.mapNotNull { folder ->
            if (folder.id == folderId) {
                val updatedPkgs = folder.appPackageNames.filterNot { it == packageName }
                if (updatedPkgs.size <= 1) {
                    // Si queda 1 o 0 apps, la carpeta se disuelve
                    null
                } else {
                    folder.copy(appPackageNames = updatedPkgs)
                }
            } else {
                folder
            }
        }
        persistFolders(current)
    }

    suspend fun renameFolder(folderId: String, newName: String) = withContext(Dispatchers.IO) {
        val current = _foldersFlow.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(name = newName.trim().ifBlank { "Carpeta" })
            } else {
                folder
            }
        }
        persistFolders(current)
    }

    suspend fun deleteFolder(folderId: String) = withContext(Dispatchers.IO) {
        val current = _foldersFlow.value.filterNot { it.id == folderId }
        persistFolders(current)
    }

    // ==========================================
    // INTELIGENCIA SQL DE FIRI (Room / SQLite)
    // ==========================================
    suspend fun buildFiriSqlContext(
        query: String,
        currentHour: Int
    ): com.example.ai.FiriSqlContextSnapshot = withContext(Dispatchers.IO) {
        val topOverall = habitDao.getMostUsedAppsOverall(limit = 5)
        val topOverallText = topOverall.joinToString(", ") { "${it.appName} (${it.totalLaunches}x)" }

        val hourHabits = habitDao.getTopAppsForHour(currentHour, limit = 4)
        val hourAppsText = hourHabits.joinToString(", ") { "${it.appName} (${it.launchCount}x)" }

        val widgetsSnapshot = widgetDao.getAllWidgetsSnapshot()
        val notesText = widgetsSnapshot
            .filter { it.typeId == "NOTES_TASKS" && it.customData.isNotBlank() }
            .joinToString("; ") { it.customData }

        val pinnedText = appDao.getPinnedCustomizations()
            .joinToString(", ") { it.packageName.substringAfterLast('.') }

        val terms = extractSignificantTerms(query)
        val matchedMemories = mutableMapOf<Long, FiriMemoryEntity>()
        val matchedPlaces = mutableMapOf<String, FiriSavedPlaceEntity>()

        for (term in terms) {
            firiSqlDao.searchMemories(term, limit = 3).forEach { matchedMemories[it.id] = it }
            firiSqlDao.searchSavedPlaces(term, limit = 4).forEach { matchedPlaces[it.placeKey] = it }
        }

        val recentMemories = firiSqlDao.getRecentMemories(limit = 5)
        val recentPlaces = firiSqlDao.getRecentPlaces(limit = 6)
        val totalMemories = firiSqlDao.getMemoryCount()
        val totalPlaces = firiSqlDao.getSavedPlacesCount()

        com.example.ai.FiriSqlContextSnapshot(
            topAppsSummary = topOverallText,
            currentHourAppsSummary = hourAppsText,
            notesSummary = notesText,
            pinnedAppsSummary = pinnedText,
            matchingMemories = matchedMemories.values.take(4),
            recentMemories = recentMemories,
            matchingPlaces = matchedPlaces.values.take(5),
            recentPlaces = recentPlaces,
            totalMemoriesCount = totalMemories,
            totalSavedPlacesCount = totalPlaces
        )
    }

    suspend fun searchSavedPlacesInSql(query: String): List<FiriSavedPlaceEntity> = withContext(Dispatchers.IO) {
        val terms = extractSignificantTerms(query)
        if (terms.isEmpty()) return@withContext firiSqlDao.getRecentPlaces(limit = 6)
        val result = linkedMapOf<String, FiriSavedPlaceEntity>()
        for (term in terms) {
            firiSqlDao.searchSavedPlaces(term, limit = 5).forEach { result[it.placeKey] = it }
        }
        result.values.toList()
    }

    suspend fun saveFiriInteractionToSql(
        query: String,
        summaryResponse: String,
        sourceTypes: String,
        places: List<com.example.ai.FiriMapPlace>
    ) = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length >= 3 && summaryResponse.isNotBlank()) {
            val existing = firiSqlDao.findExactMemory(cleanQuery)
            val keywords = extractSignificantTerms(cleanQuery).joinToString(",")
            val entity = if (existing != null) {
                existing.copy(
                    summaryResponse = summaryResponse.take(360),
                    sourceTypes = sourceTypes,
                    hitCount = existing.hitCount + 1,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                FiriMemoryEntity(
                    userQuery = cleanQuery,
                    keywords = keywords,
                    summaryResponse = summaryResponse.take(360),
                    sourceTypes = sourceTypes,
                    hitCount = 1,
                    timestamp = System.currentTimeMillis()
                )
            }
            firiSqlDao.insertOrUpdateMemory(entity)
        }

        for (place in places) {
            val key = (place.placeId?.takeIf { it.isNotBlank() } ?: place.title.lowercase().trim())
                .replace(Regex("\\s+"), "_")
            if (key.isBlank()) continue
            val existingPlace = firiSqlDao.findPlaceByKey(key)
            val entity = FiriSavedPlaceEntity(
                placeKey = key,
                title = place.title,
                address = place.address,
                category = place.category,
                latitude = place.latitude ?: existingPlace?.latitude,
                longitude = place.longitude ?: existingPlace?.longitude,
                mapsUri = place.mapsUri.ifBlank { existingPlace?.mapsUri ?: "" },
                placeId = place.placeId ?: existingPlace?.placeId,
                searchCount = (existingPlace?.searchCount ?: 0) + 1,
                lastSearchedAt = System.currentTimeMillis()
            )
            firiSqlDao.upsertSavedPlace(entity)
        }
    }

    private fun extractSignificantTerms(text: String): List<String> {
        val stopWords = setOf(
            "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "en", "con", "por", "para",
            "que", "qué", "como", "cómo", "donde", "dónde", "cual", "cuál", "quien", "quién", "firi",
            "mapa", "mapas", "busca", "buscar", "dame", "muestra", "muéstrame", "cerca", "mi", "mí", "mis",
            "the", "and", "for", "with", "from", "where", "what", "show", "find", "map", "maps", "near", "me", "my"
        )
        return text.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.length >= 3 && it !in stopWords }
            .distinct()
            .take(5)
    }
}
