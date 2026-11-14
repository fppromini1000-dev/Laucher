package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.FiriAssistant
import com.example.data.LauncherDatabase
import com.example.data.LauncherRepository
import com.example.model.AppCategory
import com.example.model.AppFolder
import com.example.model.AppInfo
import com.example.model.FusionDeviceBackup
import com.example.model.HabitPrediction
import com.example.model.LauncherHomeItem
import com.example.model.UserSettings
import com.example.system.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LauncherRepository(
        application.applicationContext,
        LauncherDatabase.getDatabase(application.applicationContext)
    )

    val firiAssistant = FiriAssistant(application.applicationContext, viewModelScope, repository)
    val updateManager = UpdateManager(application.applicationContext, viewModelScope)

    val userSettings: StateFlow<UserSettings> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserSettings()
        )

    val allApps: StateFlow<List<AppInfo>> = repository.getCategorizedAppsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<AppCategory?>(null)
    val selectedCategory: StateFlow<AppCategory?> = _selectedCategory.asStateFlow()

    private val _currentPrediction = MutableStateFlow<HabitPrediction?>(null)
    val currentPrediction: StateFlow<HabitPrediction?> = _currentPrediction.asStateFlow()

    private val _habitSummary = MutableStateFlow("")
    val habitSummary: StateFlow<String> = _habitSummary.asStateFlow()

    private val _isVoiceOverlayOpen = MutableStateFlow(false)
    val isVoiceOverlayOpen: StateFlow<Boolean> = _isVoiceOverlayOpen.asStateFlow()

    private val _isHabitsDialogOpen = MutableStateFlow(false)
    val isHabitsDialogOpen: StateFlow<Boolean> = _isHabitsDialogOpen.asStateFlow()

    private val _selectedAppForOptions = MutableStateFlow<AppInfo?>(null)
    val selectedAppForOptions: StateFlow<AppInfo?> = _selectedAppForOptions.asStateFlow()

    private val _isSettingsDialogOpen = MutableStateFlow(false)
    val isSettingsDialogOpen: StateFlow<Boolean> = _isSettingsDialogOpen.asStateFlow()

    private val _isAddWidgetDialogOpen = MutableStateFlow(false)
    val isAddWidgetDialogOpen: StateFlow<Boolean> = _isAddWidgetDialogOpen.asStateFlow()

    private val _isLockScreenVisible = MutableStateFlow(false)
    val isLockScreenVisible: StateFlow<Boolean> = _isLockScreenVisible.asStateFlow()

    private val _isControlCenterVisible = MutableStateFlow(false)
    val isControlCenterVisible: StateFlow<Boolean> = _isControlCenterVisible.asStateFlow()

    private val _isPermissionsDialogOpen = MutableStateFlow(false)
    val isPermissionsDialogOpen: StateFlow<Boolean> = _isPermissionsDialogOpen.asStateFlow()

    private val _isLockCustomizerOpen = MutableStateFlow(false)
    val isLockCustomizerOpen: StateFlow<Boolean> = _isLockCustomizerOpen.asStateFlow()

    private val _isFavoriteAppsPickerOpen = MutableStateFlow(false)
    val isFavoriteAppsPickerOpen: StateFlow<Boolean> = _isFavoriteAppsPickerOpen.asStateFlow()

    private val _isVolumeActivationGuideOpen = MutableStateFlow(false)
    val isVolumeActivationGuideOpen: StateFlow<Boolean> = _isVolumeActivationGuideOpen.asStateFlow()

    private val _isBootAnimationPreviewOpen = MutableStateFlow(false)
    val isBootAnimationPreviewOpen: StateFlow<Boolean> = _isBootAnimationPreviewOpen.asStateFlow()

    private val _isUpdatesDialogOpen = MutableStateFlow(false)
    val isUpdatesDialogOpen: StateFlow<Boolean> = _isUpdatesDialogOpen.asStateFlow()

    private val _openingApp = MutableStateFlow<AppInfo?>(null)
    val openingApp: StateFlow<AppInfo?> = _openingApp.asStateFlow()

    val widgets: StateFlow<List<com.example.model.LauncherWidgetItem>> = repository.widgetsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _isAssistantListeningActive = MutableStateFlow(false)
    val isAssistantListeningActive: StateFlow<Boolean> = _isAssistantListeningActive.asStateFlow()

    private val _volumeFraction = MutableStateFlow(0.5f)
    val volumeFraction: StateFlow<Float> = _volumeFraction.asStateFlow()

    private val _isVolumeSliderVisible = MutableStateFlow(false)
    val isVolumeSliderVisible: StateFlow<Boolean> = _isVolumeSliderVisible.asStateFlow()

    fun updateVolumeFraction(fraction: Float) {
        _volumeFraction.value = fraction.coerceIn(0f, 1f)
    }

    fun setVolumeSliderVisible(visible: Boolean) {
        _isVolumeSliderVisible.value = visible
    }

    fun onVolumeTrigger(fraction: Float) {
        _volumeFraction.value = fraction.coerceIn(0f, 1f)
        _isVolumeSliderVisible.value = true
    }

    fun setLockScreenVisible(visible: Boolean) {
        _isLockScreenVisible.value = visible
    }

    fun setControlCenterVisible(visible: Boolean) {
        _isControlCenterVisible.value = visible
    }

    fun setPermissionsDialogOpen(open: Boolean) {
        _isPermissionsDialogOpen.value = open
    }

    fun setLockCustomizerOpen(open: Boolean) {
        _isLockCustomizerOpen.value = open
    }

    fun setFavoriteAppsPickerOpen(open: Boolean) {
        _isFavoriteAppsPickerOpen.value = open
    }

    fun setVolumeActivationGuideOpen(open: Boolean) {
        _isVolumeActivationGuideOpen.value = open
    }

    fun setBootAnimationPreviewOpen(open: Boolean) {
        _isBootAnimationPreviewOpen.value = open
    }

    fun setUpdatesDialogOpen(open: Boolean) {
        _isUpdatesDialogOpen.value = open
    }

    fun updateFavoriteApps(packageNames: List<String>) {
        viewModelScope.launch {
            val updated = userSettings.value.copy(favoriteAppPackageNames = packageNames)
            repository.saveSettings(updated)
        }
    }

    fun setEditMode(enabled: Boolean) {
        _isEditMode.value = enabled
    }

    fun setAddWidgetDialogOpen(open: Boolean) {
        _isAddWidgetDialogOpen.value = open
    }

    fun addWidget(type: com.example.model.WidgetType) {
        viewModelScope.launch {
            repository.addWidget(type)
        }
    }

    fun removeWidget(id: String) {
        viewModelScope.launch {
            repository.removeWidget(id)
        }
    }

    fun updateWidgetData(id: String, customData: String, type: com.example.model.WidgetType, order: Int) {
        viewModelScope.launch {
            repository.updateWidgetData(id, customData, type, order)
        }
    }

    fun toggleDeviceWallpaper(useDeviceWallpaper: Boolean) {
        viewModelScope.launch {
            val updated = userSettings.value.copy(useDeviceWallpaper = useDeviceWallpaper)
            repository.saveSettings(updated)
        }
    }

    fun setAssistantListeningActive(active: Boolean) {
        _isAssistantListeningActive.value = active
    }

    fun removeOrHideApp(app: AppInfo) {
        viewModelScope.launch {
            repository.hideOrRemoveApp(app.packageName, app.category)
        }
    }

    fun moveApp(app: AppInfo, direction: Int) {
        val currentList = filteredApps.value.toMutableList()
        val index = currentList.indexOfFirst { it.packageName == app.packageName }
        if (index != -1) {
            val targetIndex = (index + direction).coerceIn(0, currentList.size - 1)
            if (targetIndex != index) {
                currentList.removeAt(index)
                currentList.add(targetIndex, app)
                viewModelScope.launch {
                    repository.reorderAppsList(currentList)
                }
            }
        }
    }

    fun uninstallApp(context: Context, app: AppInfo) {
        if (app.isSimulated) {
            removeOrHideApp(app)
            val msg = if (userSettings.value.isSpanish) "${app.label} eliminada del launcher" else "${app.label} removed from launcher"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val packageUri = android.net.Uri.parse("package:${app.packageName}")
            val uninstallIntent = Intent(Intent.ACTION_DELETE, packageUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(uninstallIntent)
        } catch (e: Exception) {
            removeOrHideApp(app)
            val msg = if (userSettings.value.isSpanish) "${app.label} ocultada" else "${app.label} hidden"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val filteredApps: StateFlow<List<AppInfo>> = combine(
        allApps,
        _searchQuery,
        _selectedCategory
    ) { apps, query, category ->
        var list = apps
        if (category != null) {
            list = list.filter { it.category == category }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.label.lowercase().contains(q) ||
                        it.packageName.lowercase().contains(q) ||
                        it.category.labelEs.lowercase().contains(q) ||
                        it.category.labelEn.lowercase().contains(q)
            }
        }
        // Pinned apps first, then custom order, then alphabetically
        list.sortedWith(
            compareByDescending<AppInfo> { it.isPinned }
                .thenBy { it.customOrder }
                .thenBy { it.label.lowercase() }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ==========================================
    // CARPETAS Y ELEMENTOS DE LA PANTALLA DE INICIO
    // ==========================================
    val folders: StateFlow<List<AppFolder>> = repository.foldersFlow

    private val _openedFolder = MutableStateFlow<AppFolder?>(null)
    val openedFolder: StateFlow<AppFolder?> = _openedFolder.asStateFlow()

    fun openFolder(folder: AppFolder?) {
        _openedFolder.value = folder
    }

    val homeItems: StateFlow<List<LauncherHomeItem>> = combine(
        filteredApps,
        folders
    ) { apps, folderList ->
        val appMap = apps.associateBy { it.packageName }
        val assignedPackages = mutableSetOf<String>()

        // Carpetas con sus apps correspondientes
        val folderItems = folderList.mapNotNull { folder ->
            val folderApps = folder.appPackageNames.mapNotNull { appMap[it] }
            if (folderApps.isNotEmpty()) {
                assignedPackages.addAll(folder.appPackageNames)
                LauncherHomeItem.Folder(folder = folder, apps = folderApps)
            } else {
                null
            }
        }

        // Apps que están sueltas en la pantalla
        val singleAppItems = apps.filterNot { assignedPackages.contains(it.packageName) }
            .map { LauncherHomeItem.SingleApp(it) }

        // Retornamos las carpetas y apps individuales combinadas
        folderItems + singleAppItems
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun createFolderWithApps(firstApp: AppInfo, secondApp: AppInfo, defaultName: String = "Carpeta") {
        viewModelScope.launch {
            val folder = repository.createFolderWithApps(firstApp.packageName, secondApp.packageName, defaultName)
            _openedFolder.value = folder
        }
    }

    fun addAppToFolder(folderId: String, app: AppInfo) {
        viewModelScope.launch {
            repository.addAppToFolder(folderId, app.packageName)
            val updated = repository.foldersFlow.value.find { it.id == folderId }
            if (_openedFolder.value?.id == folderId) {
                _openedFolder.value = updated
            }
        }
    }

    fun removeAppFromFolder(folderId: String, app: AppInfo) {
        viewModelScope.launch {
            repository.removeAppFromFolder(folderId, app.packageName)
            val updated = repository.foldersFlow.value.find { it.id == folderId }
            _openedFolder.value = updated
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        viewModelScope.launch {
            repository.renameFolder(folderId, newName)
            val updated = repository.foldersFlow.value.find { it.id == folderId }
            if (_openedFolder.value?.id == folderId) {
                _openedFolder.value = updated
            }
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
            if (_openedFolder.value?.id == folderId) {
                _openedFolder.value = null
            }
        }
    }

    fun reorderApps(sourceApp: AppInfo, targetApp: AppInfo) {
        val currentList = filteredApps.value.toMutableList()
        val sourceIndex = currentList.indexOfFirst { it.packageName == sourceApp.packageName }
        val targetIndex = currentList.indexOfFirst { it.packageName == targetApp.packageName }
        if (sourceIndex != -1 && targetIndex != -1 && sourceIndex != targetIndex) {
            currentList.removeAt(sourceIndex)
            currentList.add(targetIndex, sourceApp)
            viewModelScope.launch {
                repository.reorderAppsList(currentList)
            }
        }
    }

    init {
        viewModelScope.launch {
            allApps.collect { apps ->
                if (apps.isNotEmpty()) {
                    refreshHabitPredictions(apps)
                }
            }
        }
        viewModelScope.launch {
            firiAssistant.voiceState.collect { state ->
                if (state is com.example.ai.FiriVoiceState.Listening) {
                    _isAssistantListeningActive.value = true
                } else if (!_isVoiceOverlayOpen.value) {
                    _isAssistantListeningActive.value = false
                }
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun selectCategory(category: AppCategory?) {
        _selectedCategory.value = category
    }

    fun setVoiceOverlayOpen(open: Boolean) {
        _isVoiceOverlayOpen.value = open
        if (!open) {
            _isAssistantListeningActive.value = false
            firiAssistant.stopListening()
            firiAssistant.stopSpeaking()
        }
    }

    fun setHabitsDialogOpen(open: Boolean) {
        _isHabitsDialogOpen.value = open
        if (open) {
            refreshHabitSummary()
        }
    }

    fun setSettingsDialogOpen(open: Boolean) {
        _isSettingsDialogOpen.value = open
    }

    fun selectAppForOptions(app: AppInfo?) {
        _selectedAppForOptions.value = app
    }

    fun launchApp(context: Context, app: AppInfo) {
        viewModelScope.launch {
            repository.recordAppLaunch(app)
            refreshHabitPredictions(allApps.value)
        }
        _openingApp.value = null
        executeSystemAppLaunch(context, app)
    }

    fun closeOpeningApp() {
        _openingApp.value = null
    }

    fun executeSystemAppLaunch(context: Context, app: AppInfo) {
        if (app.isSimulated) {
            return
        }

        try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (_: Exception) {}
    }

    fun updateAppCategory(packageName: String, category: AppCategory) {
        viewModelScope.launch {
            repository.updateAppCategory(packageName, category)
        }
    }

    fun togglePinApp(app: AppInfo) {
        viewModelScope.launch {
            repository.togglePinApp(app.packageName, app.category)
        }
    }

    fun updateUserSettings(settings: UserSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
        }
    }

    fun completeOnboarding(
        paletteId: String,
        languageCode: String,
        regionCode: String,
        homeWallpaperId: String = "HARMONY_NEXT",
        homeCustomUri: String = "",
        lockWallpaperId: String = "TITANIUM_WAVES",
        lockCustomUri: String = "",
        isDarkMode: Boolean = true,
        appIconSize: String = "NORMAL"
    ) {
        viewModelScope.launch {
            val updated = userSettings.value.copy(
                isOnboardingComplete = true,
                paletteId = paletteId,
                languageCode = languageCode,
                regionCode = regionCode,
                homeWallpaperId = homeWallpaperId,
                homeCustomWallpaperUri = homeCustomUri,
                lockWallpaperId = lockWallpaperId,
                lockCustomWallpaperUri = lockCustomUri,
                isDarkMode = isDarkMode,
                appIconSize = appIconSize
            )
            repository.saveSettings(updated)
        }
    }

    fun importFusionBackup(backup: FusionDeviceBackup) {
        viewModelScope.launch {
            val updated = userSettings.value.copy(
                isOnboardingComplete = true,
                homeWallpaperId = backup.wallpaperId,
                lockWallpaperId = backup.lockWallpaperId,
                lockClockFont = backup.lockClockFont,
                lockClockColor = backup.lockClockColor,
                lockMiniWidgets = backup.lockMiniWidgets,
                isDarkMode = backup.isDarkMode,
                isFiriEnabled = backup.isFiriEnabled
            )
            repository.saveSettings(updated)
            if (backup.sampleFiriChats.isNotEmpty()) {
                firiAssistant.setImportedHistory(backup.sampleFiriChats)
            }
        }
    }

    fun clearHabits() {
        viewModelScope.launch {
            repository.clearHabits()
            refreshHabitPredictions(allApps.value)
            refreshHabitSummary()
        }
    }

    private fun refreshHabitPredictions(apps: List<AppInfo>) {
        viewModelScope.launch {
            val prediction = repository.getHabitPredictions(apps)
            _currentPrediction.value = prediction
        }
    }

    fun refreshHabitSummary() {
        viewModelScope.launch {
            val summary = repository.getHabitSummary(allApps.value, userSettings.value.isSpanish)
            _habitSummary.value = summary
        }
    }

    fun toggleFiriEnabled(enabled: Boolean) {
        val current = userSettings.value
        updateUserSettings(current.copy(isFiriEnabled = enabled))
        if (!enabled) {
            firiAssistant.stopListening()
            setVoiceOverlayOpen(false)
            _isAssistantListeningActive.value = false
        }
    }

    fun triggerFiriVoice() {
        if (!userSettings.value.isFiriEnabled) {
            return
        }
        setVoiceOverlayOpen(true)
        _isAssistantListeningActive.value = true
        firiAssistant.startListening(
            userSettings = userSettings.value,
            availableApps = allApps.value,
            currentPrediction = _currentPrediction.value
        ) { action ->
            _isAssistantListeningActive.value = false
            if (action.categoryToFilter != null) {
                selectCategory(action.categoryToFilter)
            }
            if (action.openHabitsDashboard) {
                setHabitsDialogOpen(true)
            }
            if (action.openLockScreen) {
                setLockScreenVisible(true)
            }
            if (action.openControlCenter) {
                setControlCenterVisible(true)
            }
            if (action.appToLaunch != null) {
                launchApp(getApplication(), action.appToLaunch)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        firiAssistant.destroy()
    }
}
