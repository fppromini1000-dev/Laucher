package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.ui.common.WallpaperBackground
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.FusionDeviceBackup
import com.example.model.UserSettings
import com.example.receiver.ScreenStateReceiver
import com.example.system.SystemControlManager
import com.example.ui.launcher.AddWidgetDialog
import com.example.ui.launcher.AppDetailsDialog
import com.example.ui.launcher.FavoriteAppsPickerDialog
import com.example.ui.launcher.FiriVoiceOrbDialog
import com.example.ui.launcher.FolderViewDialog
import com.example.ui.launcher.HabitInsightsDialog
import com.example.ui.launcher.IosAppLibraryView
import com.example.ui.launcher.LauncherScreen
import com.example.ui.launcher.SettingsDialog
import com.example.ui.onboarding.FusionBootAnimationScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.system.ControlCenterView
import com.example.ui.system.LockScreenCustomizerDialog
import com.example.ui.system.LockScreenView
import com.example.ui.system.PermissionsSetupDialog
import com.example.ui.system.UpdatesCenterDialog
import com.example.ui.theme.FiriLauncherTheme
import com.example.ui.theme.PaletteManager
import com.example.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {
    private var screenReceiver: ScreenStateReceiver? = null
    private var currentViewModel: LauncherViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LauncherViewModel = viewModel()
            currentViewModel = viewModel
            val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
            val activePalette = PaletteManager.getPalette(userSettings.paletteId, userSettings.isDarkMode)

            androidx.compose.runtime.DisposableEffect(Unit) {
                screenReceiver = ScreenStateReceiver.register(this@MainActivity) {
                    viewModel.setLockScreenVisible(true)
                }
                onDispose {
                    ScreenStateReceiver.unregister(this@MainActivity, screenReceiver)
                    screenReceiver = null
                }
            }

            FiriLauncherTheme(palette = activePalette) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!userSettings.isOnboardingComplete) {
                        var showBootAnimation by rememberSaveable {
                            mutableStateOf(true)
                        }

                        if (showBootAnimation) {
                            FusionBootAnimationScreen(
                                onComplete = { showBootAnimation = false }
                            )
                        } else {
                            OnboardingScreen(
                                initialPaletteId = userSettings.paletteId,
                                initialLanguage = userSettings.languageCode,
                                initialRegion = userSettings.regionCode,
                                initialHomeWallpaperId = userSettings.homeWallpaperId,
                                initialLockWallpaperId = userSettings.lockWallpaperId,
                                initialDarkMode = userSettings.isDarkMode,
                                onComplete = { palId, lang, region, homeWp, homeCustom, lockWp, lockCustom, isDark, iconSize ->
                                    viewModel.completeOnboarding(
                                        paletteId = palId,
                                        languageCode = lang,
                                        regionCode = region,
                                        homeWallpaperId = homeWp,
                                        homeCustomUri = homeCustom,
                                        lockWallpaperId = lockWp,
                                        lockCustomUri = lockCustom,
                                        isDarkMode = isDark,
                                        appIconSize = iconSize
                                    )
                                },
                                onImportFusionBackup = { backup ->
                                    viewModel.importFusionBackup(backup)
                                }
                            )
                        }
                    } else {
                        LauncherContent(viewModel = viewModel, userSettings = userSettings)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.service.VolumeOverlayService.start(this)
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            val isUp = keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                val am = getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
                if (am != null) {
                    val direction = if (isUp) {
                        android.media.AudioManager.ADJUST_RAISE
                    } else {
                        android.media.AudioManager.ADJUST_LOWER
                    }
                    // Flag 0 suppresses the native Android volume dialog completely
                    am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, direction, 0)
                    val cur = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                    val max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                    val fraction = if (max > 0) cur.toFloat() / max.toFloat() else 0.5f

                    val hasOverlay = android.provider.Settings.canDrawOverlays(this)
                    val hasAccessibility = com.example.service.VolumeAccessibilityService.isAccessibilityEnabled(this)

                    currentViewModel?.updateVolumeFraction(fraction)

                    if (hasOverlay || hasAccessibility) {
                        // Delegar exclusivamente en el overlay del sistema (solo 1 barra)
                        com.example.service.VolumeAccessibilityService.showVolume(this, fraction)
                    } else {
                        // Fallback interno en Compose únicamente si no hay permiso de superposición
                        currentViewModel?.onVolumeTrigger(fraction)
                        if (!com.example.service.VolumeAccessibilityService.isAccessibilityEnabled(this)) {
                            android.widget.Toast.makeText(
                                this,
                                if (currentViewModel?.userSettings?.value?.isSpanish == true)
                                    "Consejo: Para que esta barra aparezca en TikTok, YouTube o juegos, activa el permiso de superposición en Ajustes"
                                else
                                    "Tip: To show this volume bar in TikTok, YouTube or games, enable overlay permission in Settings",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            val am = getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
            if (am != null) {
                val direction = if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
                    android.media.AudioManager.ADJUST_RAISE
                } else {
                    android.media.AudioManager.ADJUST_LOWER
                }
                // Flag 0 suppresses the native Android volume dialog completely
                am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, direction, 0)
                val cur = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                val max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                val fraction = if (max > 0) cur.toFloat() / max.toFloat() else 0.5f

                val hasOverlay = android.provider.Settings.canDrawOverlays(this)
                val hasAccessibility = com.example.service.VolumeAccessibilityService.isAccessibilityEnabled(this)

                currentViewModel?.updateVolumeFraction(fraction)

                if (hasOverlay || hasAccessibility) {
                    com.example.service.VolumeOverlayService.showVolume(this, fraction, userInitiated = true)
                } else {
                    currentViewModel?.onVolumeTrigger(fraction)
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        ScreenStateReceiver.unregister(this, screenReceiver)
        screenReceiver = null
        currentViewModel = null
    }
}

@Composable
fun LauncherContent(
    viewModel: LauncherViewModel,
    userSettings: UserSettings
) {
    val context = LocalContext.current
    val systemControlManager = remember { SystemControlManager(context) }
    val activePalette = PaletteManager.getPalette(userSettings.paletteId, userSettings.isDarkMode)

    val allApps by viewModel.allApps.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val currentPrediction by viewModel.currentPrediction.collectAsStateWithLifecycle()
    val habitSummary by viewModel.habitSummary.collectAsStateWithLifecycle()

    val widgets by viewModel.widgets.collectAsStateWithLifecycle()
    val isAddWidgetDialogOpen by viewModel.isAddWidgetDialogOpen.collectAsStateWithLifecycle()

    val isLockScreenVisible by viewModel.isLockScreenVisible.collectAsStateWithLifecycle()
    val isControlCenterVisible by viewModel.isControlCenterVisible.collectAsStateWithLifecycle()
    val isPermissionsDialogOpen by viewModel.isPermissionsDialogOpen.collectAsStateWithLifecycle()
    val isVolumeActivationGuideOpen by viewModel.isVolumeActivationGuideOpen.collectAsStateWithLifecycle()
    val isBootAnimationPreviewOpen by viewModel.isBootAnimationPreviewOpen.collectAsStateWithLifecycle()
    val isUpdatesDialogOpen by viewModel.isUpdatesDialogOpen.collectAsStateWithLifecycle()
    val isLockCustomizerOpen by viewModel.isLockCustomizerOpen.collectAsStateWithLifecycle()
    val isFavoriteAppsPickerOpen by viewModel.isFavoriteAppsPickerOpen.collectAsStateWithLifecycle()
    val homeItems by viewModel.homeItems.collectAsStateWithLifecycle()
    val openedFolder by viewModel.openedFolder.collectAsStateWithLifecycle()

    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val isAssistantListeningActive by viewModel.isAssistantListeningActive.collectAsStateWithLifecycle()

    val isVoiceOverlayOpen by viewModel.isVoiceOverlayOpen.collectAsStateWithLifecycle()
    val isHabitsDialogOpen by viewModel.isHabitsDialogOpen.collectAsStateWithLifecycle()
    val isSettingsDialogOpen by viewModel.isSettingsDialogOpen.collectAsStateWithLifecycle()
    val selectedAppForOptions by viewModel.selectedAppForOptions.collectAsStateWithLifecycle()

    val volumeFraction by viewModel.volumeFraction.collectAsStateWithLifecycle()
    val isVolumeSliderVisible by viewModel.isVolumeSliderVisible.collectAsStateWithLifecycle()

    val voiceState by viewModel.firiAssistant.voiceState.collectAsStateWithLifecycle()
    val transcription by viewModel.firiAssistant.transcription.collectAsStateWithLifecycle()
    val rmsDb by viewModel.firiAssistant.rmsDb.collectAsStateWithLifecycle()
    val conversationHistory by viewModel.firiAssistant.conversationHistory.collectAsStateWithLifecycle()
    val brainTelemetry by viewModel.firiAssistant.brainTelemetry.collectAsStateWithLifecycle()
    val sqlStats by viewModel.firiAssistant.sqlStats.collectAsStateWithLifecycle()
    val isGeneratingImage by viewModel.firiAssistant.isGeneratingImage.collectAsStateWithLifecycle()
    val imageGenerationStatus by viewModel.firiAssistant.imageGenerationStatus.collectAsStateWithLifecycle()

    var isAppLibraryVisible by rememberSaveable { mutableStateOf(false) }

    // Manejo de botón Atrás
    BackHandler(
        enabled = isAppLibraryVisible || isUpdatesDialogOpen || openedFolder != null || isLockCustomizerOpen || isFavoriteAppsPickerOpen || isControlCenterVisible || isPermissionsDialogOpen || isVolumeActivationGuideOpen || isBootAnimationPreviewOpen || isEditMode || isAddWidgetDialogOpen || isVoiceOverlayOpen || isHabitsDialogOpen || isSettingsDialogOpen || selectedAppForOptions != null || searchQuery.isNotEmpty() || selectedCategory != null
    ) {
        when {
            isAppLibraryVisible -> isAppLibraryVisible = false
            isUpdatesDialogOpen -> viewModel.setUpdatesDialogOpen(false)
            isBootAnimationPreviewOpen -> viewModel.setBootAnimationPreviewOpen(false)
            openedFolder != null -> viewModel.openFolder(null)
            isLockCustomizerOpen -> viewModel.setLockCustomizerOpen(false)
            isFavoriteAppsPickerOpen -> viewModel.setFavoriteAppsPickerOpen(false)
            isControlCenterVisible -> viewModel.setControlCenterVisible(false)
            isPermissionsDialogOpen -> viewModel.setPermissionsDialogOpen(false)
            isVolumeActivationGuideOpen -> viewModel.setVolumeActivationGuideOpen(false)
            isEditMode -> viewModel.setEditMode(false)
            isAddWidgetDialogOpen -> viewModel.setAddWidgetDialogOpen(false)
            isVoiceOverlayOpen -> viewModel.setVoiceOverlayOpen(false)
            isHabitsDialogOpen -> viewModel.setHabitsDialogOpen(false)
            isSettingsDialogOpen -> viewModel.setSettingsDialogOpen(false)
            selectedAppForOptions != null -> viewModel.selectAppForOptions(null)
            searchQuery.isNotEmpty() -> viewModel.onSearchQueryChanged("")
            selectedCategory != null -> viewModel.selectCategory(null)
        }
    }

    // Pantalla Principal del Lanzador
    LauncherScreen(
        palette = activePalette,
        userSettings = userSettings,
        allApps = allApps,
        homeItems = homeItems,
        filteredApps = filteredApps,
        searchQuery = searchQuery,
        selectedCategory = selectedCategory,
        currentPrediction = currentPrediction,
        widgets = widgets,
        isEditMode = isEditMode,
        isAssistantListeningActive = isAssistantListeningActive,
        onSearchChanged = { viewModel.onSearchQueryChanged(it) },
        onSelectCategory = { viewModel.selectCategory(it) },
        onLaunchApp = { app ->
            if (app.packageName == "com.firi.ai" || app.label.contains("Firi", ignoreCase = true)) {
                if (!userSettings.isFiriEnabled) {
                    android.widget.Toast.makeText(
                        context,
                        if (userSettings.isSpanish) "Firi está apagada. Puedes encenderla en Ajustes." else "Firi is turned off. You can enable it in Settings.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.triggerFiriVoice()
                }
            } else {
                viewModel.launchApp(context, app)
            }
        },
        onAppLongPress = { app -> viewModel.selectAppForOptions(app) },
        onSetEditMode = { viewModel.setEditMode(it) },
        onRemoveApp = { app -> viewModel.removeOrHideApp(app) },
        onMoveApp = { app, dir -> viewModel.moveApp(app, dir) },
        onOpenFolder = { viewModel.openFolder(it) },
        onCreateFolderWithApps = { first, second -> viewModel.createFolderWithApps(first, second) },
        onAddAppToFolder = { folderId, app -> viewModel.addAppToFolder(folderId, app) },
        onReorderApps = { first, second -> viewModel.reorderApps(first, second) },
        onDissolveFolder = { viewModel.deleteFolder(it) },
        onOpenAddWidget = { viewModel.setAddWidgetDialogOpen(true) },
        onRemoveWidget = { viewModel.removeWidget(it) },
        onUpdateWidgetData = { id, data, type, order -> viewModel.updateWidgetData(id, data, type, order) },
        onOpenVoiceAssistant = {
            if (!userSettings.isFiriEnabled) {
                android.widget.Toast.makeText(
                    context,
                    if (userSettings.isSpanish) "Firi está apagada. Puedes encenderla desde Ajustes." else "Firi is turned off. You can turn it on in Settings.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                viewModel.triggerFiriVoice()
            }
        },
        onAssistantButtonHold = {
            if (!userSettings.isFiriEnabled) {
                android.widget.Toast.makeText(
                    context,
                    if (userSettings.isSpanish) "Firi está apagada. Actívala en Ajustes." else "Firi is turned off. Enable it in Settings.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                viewModel.setAssistantListeningActive(true)
                viewModel.triggerFiriVoice()
            }
        },
        onOpenHabitInsights = { viewModel.setHabitsDialogOpen(true) },
        onOpenSettings = { viewModel.setSettingsDialogOpen(true) },
        onOpenLockScreen = { viewModel.setLockScreenVisible(true) },
        onOpenControlCenter = { viewModel.setControlCenterVisible(true) },
        onOpenFavoriteAppsPicker = { viewModel.setFavoriteAppsPickerOpen(true) },
        onOpenVolumeActivationGuide = { viewModel.setVolumeActivationGuideOpen(true) },
        onOpenAppLibrary = { isAppLibraryVisible = true }
    )

    // Biblioteca de Apps oficial con diseño iOS 17/18 (Image 2)
    AnimatedVisibility(
        visible = isAppLibraryVisible,
        enter = fadeIn() + slideInVertically { it / 6 },
        exit = fadeOut() + slideOutVertically { it / 6 }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Fondo de Pantalla desenfocado / translúcido estilo iOS App Library (Image 2)
            WallpaperBackground(
                wallpaperId = userSettings.homeWallpaperId,
                customWallpaperUri = userSettings.homeCustomWallpaperUri,
                scrimBrush = Brush.verticalGradient(
                    colors = if (userSettings.isDarkMode) {
                        listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.50f),
                            Color.Black.copy(alpha = 0.70f)
                        )
                    } else {
                        // En modo claro: scrim suave para resaltar el wallpaper sin blanquearlo
                        listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.20f),
                            Color.Black.copy(alpha = 0.40f)
                        )
                    }
                )
            )

            IosAppLibraryView(
                allApps = allApps,
                palette = activePalette,
                userSettings = userSettings,
                onLaunchApp = { app ->
                    isAppLibraryVisible = false
                    viewModel.launchApp(context, app)
                },
                onOpenCategoryFolder = { cat, apps ->
                    isAppLibraryVisible = false
                    viewModel.selectCategory(cat)
                },
                onCloseLibrary = {
                    isAppLibraryVisible = false
                }
            )
        }
    }

    // Diálogo de Añadir Widgets
    if (isAddWidgetDialogOpen) {
        AddWidgetDialog(
            palette = activePalette,
            userSettings = userSettings,
            onAddWidget = { widgetType ->
                viewModel.addWidget(widgetType)
            },
            onDismiss = { viewModel.setAddWidgetDialogOpen(false) }
        )
    }

    // Diálogo de Permisos y Desactivación de Bloqueo del Sistema
    if (isPermissionsDialogOpen) {
        PermissionsSetupDialog(
            systemControlManager = systemControlManager,
            palette = activePalette,
            userSettings = userSettings,
            onTestLockScreen = {
                viewModel.setPermissionsDialogOpen(false)
                viewModel.setLockScreenVisible(true)
            },
            onTestControlCenter = {
                viewModel.setPermissionsDialogOpen(false)
                viewModel.setControlCenterVisible(true)
            },
            onOpenVolumeActivationGuide = {
                viewModel.setPermissionsDialogOpen(false)
                viewModel.setVolumeActivationGuideOpen(true)
            },
            onDismiss = { viewModel.setPermissionsDialogOpen(false) }
        )
    }

    // Centro de Control Estilo HarmonyOS / iOS
    AnimatedVisibility(
        visible = isControlCenterVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        ControlCenterView(
            systemControlManager = systemControlManager,
            palette = activePalette,
            userSettings = userSettings,
            onOpenPermissionsDialog = {
                viewModel.setControlCenterVisible(false)
                viewModel.setPermissionsDialogOpen(true)
            },
            onClose = { viewModel.setControlCenterVisible(false) }
        )
    }

    // Pantalla de Bloqueo Estilo iOS con Personalización y Mini Widgets
    AnimatedVisibility(
        visible = isLockScreenVisible,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
        exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = androidx.compose.animation.core.tween(280)) + fadeOut()
    ) {
        LockScreenView(
            systemControlManager = systemControlManager,
            userSettings = userSettings,
            onUnlock = { viewModel.setLockScreenVisible(false) },
            onOpenCustomizer = { viewModel.setLockCustomizerOpen(true) }
        )
    }

    // Diálogo Personalizador de Pantalla de Bloqueo (iOS 16)
    if (isLockCustomizerOpen) {
        LockScreenCustomizerDialog(
            userSettings = userSettings,
            palette = activePalette,
            onSaveSettings = { updated ->
                viewModel.updateUserSettings(updated)
            },
            onSaveAndLock = { updated ->
                viewModel.updateUserSettings(updated)
                viewModel.setLockCustomizerOpen(false)
                viewModel.setLockScreenVisible(true)
            },
            onDismiss = { viewModel.setLockCustomizerOpen(false) }
        )
    }

    // Selector de Apps Favoritas para la Pantalla Principal
    if (isFavoriteAppsPickerOpen) {
        FavoriteAppsPickerDialog(
            allApps = allApps,
            userSettings = userSettings,
            palette = activePalette,
            onSaveFavorites = { favorites ->
                viewModel.updateFavoriteApps(favorites)
            },
            onDismiss = { viewModel.setFavoriteAppsPickerOpen(false) }
        )
    }

    // Diálogo de Carpeta de Aplicaciones
    openedFolder?.let { folder ->
        val folderApps = remember(folder.appPackageNames, allApps) {
            folder.appPackageNames.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }
        }
        FolderViewDialog(
            folder = folder,
            apps = folderApps,
            palette = activePalette,
            isEditMode = isEditMode,
            onLaunchApp = { app -> viewModel.launchApp(context, app) },
            onRemoveAppFromFolder = { folderId, app -> viewModel.removeAppFromFolder(folderId, app) },
            onRenameFolder = { folderId, newName -> viewModel.renameFolder(folderId, newName) },
            onDissolveFolder = { folderId -> viewModel.deleteFolder(folderId) },
            onDismiss = { viewModel.openFolder(null) }
        )
    }

    // Firi 100% Voice & Multimodal Studio Overlay
    if (isVoiceOverlayOpen) {
        val handleFiriAction: (com.example.ai.FiriActionResult) -> Unit = { action ->
            if (action.categoryToFilter != null) {
                viewModel.selectCategory(action.categoryToFilter)
            }
            if (action.openHabitsDashboard) {
                viewModel.setHabitsDialogOpen(true)
            }
            if (action.openLockScreen) {
                viewModel.setLockScreenVisible(true)
            }
            if (action.openLockCustomizer) {
                viewModel.setVoiceOverlayOpen(false)
                viewModel.setLockCustomizerOpen(true)
            }
            if (action.openSettings) {
                viewModel.setVoiceOverlayOpen(false)
                viewModel.setSettingsDialogOpen(true)
            }
            if (action.openControlCenter) {
                viewModel.setControlCenterVisible(true)
            }
            if (action.triggerVolumeOverlay) {
                val am = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
                if (am != null) {
                    val cur = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                    val max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                    val fraction = if (max > 0) cur.toFloat() / max.toFloat() else 0.5f
                    val hasOverlay = android.provider.Settings.canDrawOverlays(context) ||
                            com.example.service.VolumeAccessibilityService.isAccessibilityEnabled(context)
                    viewModel.updateVolumeFraction(fraction)
                    if (hasOverlay) {
                        com.example.service.VolumeOverlayService.showVolume(context, fraction, userInitiated = true)
                    } else {
                        viewModel.onVolumeTrigger(fraction)
                    }
                }
            }
            if (action.appToLaunch != null) {
                viewModel.launchApp(context, action.appToLaunch)
            }
        }

        FiriVoiceOrbDialog(
            voiceState = voiceState,
            transcription = transcription,
            rmsDb = rmsDb,
            conversationHistory = conversationHistory,
            brainTelemetry = brainTelemetry,
            sqlStats = sqlStats,
            hasLocationPermission = viewModel.firiAssistant.hasLocationPermission(),
            onLocationGranted = { viewModel.firiAssistant.refreshLocationIfPermitted() },
            onToggleForceOfflineMode = { viewModel.firiAssistant.toggleForceOfflineMode(it) },
            palette = activePalette,
            userSettings = userSettings,
            onStartListening = {
                viewModel.firiAssistant.startListening(
                    userSettings = userSettings,
                    availableApps = allApps,
                    currentPrediction = currentPrediction,
                    onAction = handleFiriAction
                )
            },
            onStopListening = { viewModel.firiAssistant.stopListening() },
            onStopSpeaking = { viewModel.firiAssistant.stopSpeaking() },
            onSubmitTextQuery = { query ->
                viewModel.firiAssistant.processQuery(
                    query = query,
                    userSettings = userSettings,
                    availableApps = allApps,
                    currentPrediction = currentPrediction,
                    onAction = handleFiriAction
                )
            },
            isGeneratingImage = isGeneratingImage,
            imageGenerationStatus = imageGenerationStatus,
            onSubmitImageQuery = { query, editUri, ratio ->
                viewModel.firiAssistant.processQuery(
                    query = query,
                    userSettings = userSettings,
                    availableApps = allApps,
                    currentPrediction = currentPrediction,
                    editImageUriOrPath = editUri,
                    aspectRatio = ratio,
                    onAction = handleFiriAction
                )
            },
            onSetHomeWallpaper = { path ->
                viewModel.updateUserSettings(
                    userSettings.copy(
                        homeCustomWallpaperUri = path,
                        homeWallpaperId = "custom"
                    )
                )
                android.widget.Toast.makeText(
                    context,
                    if (userSettings.isSpanish) "Fondo de Inicio actualizado con tu imagen de Firi" else "Home wallpaper updated with your Firi image",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            onSetLockWallpaper = { path ->
                viewModel.updateUserSettings(
                    userSettings.copy(
                        lockCustomWallpaperUri = path,
                        lockWallpaperId = "custom"
                    )
                )
                android.widget.Toast.makeText(
                    context,
                    if (userSettings.isSpanish) "Fondo de Bloqueo actualizado con tu imagen de Firi" else "Lock screen wallpaper updated with your Firi image",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            onClearConversation = { viewModel.firiAssistant.clearConversation() },
            onDismiss = { viewModel.setVoiceOverlayOpen(false) }
        )
    }

    // Habit Insights Dialog
    if (isHabitsDialogOpen) {
        HabitInsightsDialog(
            prediction = currentPrediction,
            summaryText = habitSummary,
            palette = activePalette,
            userSettings = userSettings,
            onLaunchApp = { app -> viewModel.launchApp(context, app) },
            onClearHabits = { viewModel.clearHabits() },
            onDismiss = { viewModel.setHabitsDialogOpen(false) }
        )
    }

    // App Details / Categorization / Reorganize Dialog
    selectedAppForOptions?.let { app ->
        AppDetailsDialog(
            app = app,
            palette = activePalette,
            userSettings = userSettings,
            onUpdateCategory = { newCategory ->
                viewModel.updateAppCategory(app.packageName, newCategory)
                viewModel.selectAppForOptions(null)
            },
            onTogglePin = {
                viewModel.togglePinApp(app)
                viewModel.selectAppForOptions(null)
            },
            onStartEditMode = {
                viewModel.selectAppForOptions(null)
                viewModel.setEditMode(true)
            },
            onDeleteOrHide = {
                viewModel.removeOrHideApp(app)
                viewModel.selectAppForOptions(null)
            },
            onUninstall = {
                viewModel.uninstallApp(context, app)
                viewModel.selectAppForOptions(null)
            },
            onLaunch = {
                viewModel.launchApp(context, app)
                viewModel.selectAppForOptions(null)
            },
            onDismiss = { viewModel.selectAppForOptions(null) }
        )
    }

    // Settings Dialog
    if (isSettingsDialogOpen) {
        SettingsDialog(
            userSettings = userSettings,
            activePalette = activePalette,
            updateManager = viewModel.updateManager,
            firiAssistant = viewModel.firiAssistant,
            onUpdateSettings = { viewModel.updateUserSettings(it) },
            onOpenPermissionsSetup = {
                viewModel.setSettingsDialogOpen(false)
                viewModel.setPermissionsDialogOpen(true)
            },
            onOpenLockCustomizer = {
                viewModel.setSettingsDialogOpen(false)
                viewModel.setLockCustomizerOpen(true)
            },
            onLockScreenNow = {
                viewModel.setSettingsDialogOpen(false)
                viewModel.setLockScreenVisible(true)
            },
            onOpenVolumeActivationGuide = {
                viewModel.setSettingsDialogOpen(false)
                viewModel.setVolumeActivationGuideOpen(true)
            },
            onPreviewBootAnimation = {
                viewModel.setSettingsDialogOpen(false)
                viewModel.setBootAnimationPreviewOpen(true)
            },
            onOpenUpdatesCenter = {
                viewModel.setSettingsDialogOpen(false)
                viewModel.setUpdatesDialogOpen(true)
            },
            onResetOnboarding = {
                viewModel.updateUserSettings(userSettings.copy(isOnboardingComplete = false))
            },
            onDismiss = { viewModel.setSettingsDialogOpen(false) }
        )
    }

    // Centro de Actualizaciones Oficial (CometOS / Firi Launcher - Soporte 5 años Android 12)
    if (isUpdatesDialogOpen) {
        UpdatesCenterDialog(
            updateManager = viewModel.updateManager,
            palette = activePalette,
            userSettings = userSettings,
            onDismiss = { viewModel.setUpdatesDialogOpen(false) }
        )
    }

    // Guía Visual de Activación de Barra de Volumen Global (Accesibilidad)
    if (isVolumeActivationGuideOpen) {
        com.example.ui.system.VolumeActivationGuideDialog(
            palette = activePalette,
            userSettings = userSettings,
            onDismiss = { viewModel.setVolumeActivationGuideOpen(false) }
        )
    }

    // Vista previa de la Animación de Arranque Cinemática (Código + Fusion Estudio)
    if (isBootAnimationPreviewOpen) {
        com.example.ui.onboarding.FusionBootAnimationScreen(
            onComplete = { viewModel.setBootAnimationPreviewOpen(false) }
        )
    }

    // Barra de Volumen CometOS en Compose (únicamente activa como fallback interno si NO está disponible el overlay del sistema)
    val hasSystemVolumeOverlay = android.provider.Settings.canDrawOverlays(context) ||
            com.example.service.VolumeAccessibilityService.isAccessibilityEnabled(context)

    if (!hasSystemVolumeOverlay) {
        com.example.ui.system.CometVolumeSliderCompose(
            volumeFraction = volumeFraction,
            visible = isVolumeSliderVisible,
            onVolumeChanged = { newFraction ->
                systemControlManager.setMusicVolume(newFraction)
                viewModel.updateVolumeFraction(newFraction)
            },
            onDismiss = {
                viewModel.setVolumeSliderVisible(false)
            }
        )
    }
}
