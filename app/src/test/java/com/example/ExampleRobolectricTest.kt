package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.AppCategorizer
import com.example.model.AppCategory
import com.example.model.SupportedLocales
import com.example.model.TimeOfDaySlot
import com.example.ui.theme.PaletteManager
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Firi Launcher", appName)
  }

  @Test
  fun `verify auto categorization of applications`() {
    val catGmail = AppCategorizer.categorize("com.google.android.gm", "Gmail", null)
    assertEquals(AppCategory.PRODUCTIVITY, catGmail)

    val catWhatsApp = AppCategorizer.categorize("com.whatsapp", "WhatsApp", null)
    assertEquals(AppCategory.SOCIAL, catWhatsApp)

    val catSpotify = AppCategorizer.categorize("com.spotify.music", "Spotify", null)
    assertEquals(AppCategory.ENTERTAINMENT, catSpotify)

    val catCalculator = AppCategorizer.categorize("com.google.android.calculator", "Calculadora", null)
    assertEquals(AppCategory.UTILITIES, catCalculator)

    val catPayPal = AppCategorizer.categorize("com.paypal.android.p2pmobile", "PayPal", null)
    assertEquals(AppCategory.FINANCE, catPayPal)
  }

  @Test
  fun `verify time of day slots for habit engine`() {
    assertEquals(TimeOfDaySlot.MORNING, TimeOfDaySlot.current(8))
    assertEquals(TimeOfDaySlot.AFTERNOON, TimeOfDaySlot.current(14))
    assertEquals(TimeOfDaySlot.EVENING, TimeOfDaySlot.current(20))
    assertEquals(TimeOfDaySlot.NIGHT, TimeOfDaySlot.current(2))
  }

  @Test
  fun `verify color palettes availability`() {
    assertEquals(6, PaletteManager.allPalettes.size)
    assertNotNull(PaletteManager.getPalette("onyx"))
    assertNotNull(PaletteManager.getPalette("nordic"))
    assertNotNull(PaletteManager.getPalette("sunset"))
    assertNotNull(PaletteManager.getPalette("cyber"))
    assertNotNull(PaletteManager.getPalette("solar"))
    assertNotNull(PaletteManager.getPalette("ocean"))
  }

  @Test
  fun `verify supported locales and regions`() {
    assertTrue(SupportedLocales.regions.isNotEmpty())
    val spain = SupportedLocales.getRegion("ES")
    assertEquals("España", spain.nameEs)
    assertEquals("es", spain.defaultLanguage)
  }

  @Test
  fun `verify sample apps and categories integrity`() {
    val sampleApps = AppCategorizer.getSampleApps()
    assertTrue(sampleApps.isNotEmpty())
    assertTrue(sampleApps.any { it.category == AppCategory.PRODUCTIVITY })
    assertTrue(sampleApps.any { it.category == AppCategory.SOCIAL })
    assertTrue(sampleApps.any { it.category == AppCategory.ENTERTAINMENT })
  }

  @Test
  fun `verify widget types and defaults`() {
    val types = com.example.model.WidgetType.entries
    assertEquals(5, types.size)
    assertNotNull(com.example.model.WidgetType.fromId("weather"))
    assertNotNull(com.example.model.WidgetType.fromId("firi_habits"))
    assertNotNull(com.example.model.WidgetType.fromId("notes_tasks"))
    assertNotNull(com.example.model.WidgetType.fromId("calendar"))
    assertNotNull(com.example.model.WidgetType.fromId("music"))
  }

  @Test
  fun `verify system control manager initialization`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.system.SystemControlManager(context)
    assertNotNull(manager)
    assertTrue(manager.brightnessFloat in 0.0f..1.0f)
    assertTrue(manager.musicVolumeFloat in 0.0f..1.0f)
  }

  @Test
  fun `verify fusion boot animation resources and assets`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val helloDrawable = context.resources.getIdentifier("fusion_hello_cursive", "drawable", context.packageName)
    assertTrue(helloDrawable != 0)

    val fontSacramento = context.resources.getIdentifier("sacramento", "font", context.packageName)
    assertTrue(fontSacramento != 0)
  }

  @Test
  fun `verify launcher update manager lifecycle and 2031 android 12 expiration policy`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
    val updateManager = com.example.system.UpdateManager(context, testScope)

    // Initial state: Auto-update enabled by default so the launcher updates itself
    val initialState = updateManager.state.value
    assertTrue(initialState.autoUpdateEnabled)
    assertEquals("v3.0.8 (Fusion OS)", initialState.currentVersion)
    assertEquals("https://drive.google.com/drive/folders/1ow21KP2zCFxwtQvsRDbNLPoYC73OUJ5s", com.example.system.UpdateManager.GOOGLE_DRIVE_FOLDER_URL)
    assertEquals("firi_launcher.apk", com.example.system.UpdateManager.TARGET_APK_NAME)
    assertEquals("v3.0.9", com.example.system.UpdateManager.TARGET_VERSION)

    // Active period (simulating Android 12, normal year)
    updateManager.toggleSimulateAndroid12(true)
    updateManager.toggleSimulateYear2031(false)
    val activeState = updateManager.state.value
    assertTrue(activeState.isDeviceOnAndroid12)
    org.junit.Assert.assertFalse(activeState.isSupportExpired)

    // Year 2031 reached for Android 12: Support expires and message triggers
    updateManager.toggleSimulateYear2031(true)
    val expiredState = updateManager.state.value
    assertTrue(expiredState.isSupportExpired)
    assertEquals(2031, expiredState.currentYear)

    // Verify updates are blocked and exact user requirement message is emitted
    updateManager.checkAndInstallUpdates(manual = true)
    val finalState = updateManager.state.value
    assertTrue(finalState.statusMessage.contains("ya no es compatible con las próximas actualizaciones"))
  }

  @Test
  fun `verify authentic apk preparation and install methods`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
    val updateManager = com.example.system.UpdateManager(context, testScope)

    // Verify permission check function runs safely
    val canInstall = updateManager.canInstallApks(context)
    assertNotNull(canInstall)

    // Prepare authentic APK
    updateManager.prepareAuthenticApkFile(context)
    val targetDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
    val expectedFile = java.io.File(targetDir, com.example.system.UpdateManager.TARGET_APK_NAME)
    assertEquals(expectedFile.name, "firi_launcher.apk")
  }

  @Test
  fun `verify firi on-device second brain functions completely offline using cpu`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val onDeviceBrain = com.example.ai.OnDeviceNeuralBrain(context)
    assertTrue(onDeviceBrain.availableCores > 0)

    val userSettings = com.example.model.UserSettings(languageCode = "es")
    val apps = com.example.engine.AppCategorizer.getSampleApps()

    // 1. Consulta sobre el Segundo Cerebro sin internet
    val resultBrain = onDeviceBrain.processQueryLocally(
        "¿Cómo funciona tu segundo cerebro sin internet?",
        userSettings,
        apps,
        null
    )
    assertTrue(resultBrain.actionResult.spokenResponse.contains("Segundo Cerebro"))
    assertTrue(resultBrain.latencyMs >= 0)
    assertEquals(onDeviceBrain.availableCores, resultBrain.coresUsed)

    // 2. Operación matemática on-device en CPU
    val resultMath = onDeviceBrain.processQueryLocally(
        "cuanto es 25 * 4",
        userSettings,
        apps,
        null
    )
    assertTrue(resultMath.actionResult.spokenResponse.contains("100"))

    // 3. Control de volumen en CPU
    val resultVol = onDeviceBrain.processQueryLocally(
        "sube el volumen",
        userSettings,
        apps,
        null
    )
    assertTrue(resultVol.actionResult.triggerVolumeOverlay)

    // 4. Apertura de app en CPU
    val resultApp = onDeviceBrain.processQueryLocally(
        "abre Spotify",
        userSettings,
        apps,
        null
    )
    assertNotNull(resultApp.actionResult.appToLaunch)
    assertEquals("Spotify", resultApp.actionResult.appToLaunch?.label)
  }

  @Test
  fun `verify firi assistant switches to on-device brain when offline`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
    val assistant = com.example.ai.FiriAssistant(context, testScope)

    // Forzar modo offline o simular sin internet
    assistant.toggleForceOfflineMode(true)
    val telemetry = assistant.brainTelemetry.value
    assertTrue(telemetry.forceOfflineMode)
    assertEquals(com.example.ai.FiriBrainMode.ON_DEVICE_CPU, telemetry.activeBrain)
    assertTrue(telemetry.brainLabelEs.contains("Segundo Cerebro"))
  }

  @Test
  fun `verify firi on-off switch toggles correctly and persists in settings`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.LauncherDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.LauncherRepository(context, db)

    // 1. Estado inicial: Firi encendida por defecto (isFiriEnabled = true)
    val initialSettings = repo.settingsFlow.first()
    assertTrue(initialSettings.isFiriEnabled)

    // 2. El usuario acciona el interruptor en Ajustes para apagar a Firi (isFiriEnabled = false)
    val disabledSettings = initialSettings.copy(isFiriEnabled = false)
    repo.saveSettings(disabledSettings)

    val updatedSettings = repo.settingsFlow.first()
    assertEquals(false, updatedSettings.isFiriEnabled)

    // 3. El usuario vuelve a encender a Firi
    repo.saveSettings(updatedSettings.copy(isFiriEnabled = true))
    val reEnabledSettings = repo.settingsFlow.first()
    assertTrue(reEnabledSettings.isFiriEnabled)

    db.close()
  }
}


