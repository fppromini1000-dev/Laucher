package com.example.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.BuildConfig
import com.example.model.AppCategory
import com.example.model.AppInfo
import com.example.model.HabitPrediction
import com.example.model.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed class FiriVoiceState {
    object Idle : FiriVoiceState()
    object Listening : FiriVoiceState()
    object Thinking : FiriVoiceState()
    data class Speaking(val text: String) : FiriVoiceState()
    data class Error(val message: String) : FiriVoiceState()
}

enum class FiriBrainMode {
    CLOUD_GEMINI,      // Cerebro 1: Nube Gemini (Con internet)
    ON_DEVICE_CPU      // Cerebro 2: Procesador local del teléfono (Sin internet / On-Device)
}

data class FiriBrainTelemetry(
    val activeBrain: FiriBrainMode = FiriBrainMode.ON_DEVICE_CPU,
    val isConnectedToInternet: Boolean = false,
    val forceOfflineMode: Boolean = false,
    val lastExecutionTimeMs: Long = 12L,
    val processorCores: Int = Runtime.getRuntime().availableProcessors(),
    val brainLabelEs: String = "Segundo Cerebro (Procesador Local)",
    val brainLabelEn: String = "Second Brain (On-Device CPU)"
)

data class FiriChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val brainUsed: FiriBrainMode = FiriBrainMode.ON_DEVICE_CPU,
    val executionTimeMs: Long = 0L
)

data class FiriActionResult(
    val spokenResponse: String,
    val appToLaunch: AppInfo? = null,
    val categoryToFilter: AppCategory? = null,
    val openHabitsDashboard: Boolean = false,
    val openLockScreen: Boolean = false,
    val openControlCenter: Boolean = false,
    val openLockCustomizer: Boolean = false,
    val openSettings: Boolean = false,
    val triggerVolumeOverlay: Boolean = false
)

class FiriAssistant(
    private val context: Context,
    private val scope: CoroutineScope
) : TextToSpeech.OnInitListener {

    private val _voiceState = MutableStateFlow<FiriVoiceState>(FiriVoiceState.Idle)
    val voiceState: StateFlow<FiriVoiceState> = _voiceState.asStateFlow()

    private val _transcription = MutableStateFlow("")
    val transcription: StateFlow<String> = _transcription.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _conversationHistory = MutableStateFlow<List<FiriChatMessage>>(emptyList())
    val conversationHistory: StateFlow<List<FiriChatMessage>> = _conversationHistory.asStateFlow()

    val onDeviceBrain = OnDeviceNeuralBrain(context)

    private val _forceOfflineMode = MutableStateFlow(false)
    val forceOfflineMode: StateFlow<Boolean> = _forceOfflineMode.asStateFlow()

    private val _brainTelemetry = MutableStateFlow(
        FiriBrainTelemetry(
            activeBrain = if (isInternetAvailable()) FiriBrainMode.CLOUD_GEMINI else FiriBrainMode.ON_DEVICE_CPU,
            isConnectedToInternet = isInternetAvailable(),
            forceOfflineMode = false,
            processorCores = onDeviceBrain.availableCores
        )
    )
    val brainTelemetry: StateFlow<FiriBrainTelemetry> = _brainTelemetry.asStateFlow()

    fun isInternetAvailable(): Boolean {
        if (_forceOfflineMode.value) return false
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val network = cm.activeNetwork
                val capabilities = cm.getNetworkCapabilities(network)
                capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun toggleForceOfflineMode(force: Boolean) {
        _forceOfflineMode.value = force
        val online = if (force) false else isInternetAvailable()
        val brain = if (online) FiriBrainMode.CLOUD_GEMINI else FiriBrainMode.ON_DEVICE_CPU
        _brainTelemetry.value = _brainTelemetry.value.copy(
            forceOfflineMode = force,
            isConnectedToInternet = online,
            activeBrain = brain,
            brainLabelEs = if (brain == FiriBrainMode.CLOUD_GEMINI) "Cerebro 1: Nube Gemini" else "Segundo Cerebro: Procesador Local (${onDeviceBrain.availableCores} núcleos)",
            brainLabelEn = if (brain == FiriBrainMode.CLOUD_GEMINI) "Brain 1: Gemini Cloud" else "Second Brain: On-Device CPU (${onDeviceBrain.availableCores} cores)"
        )
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    // Timeout rápido para respuestas inmediatas sin bloquear la experiencia
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    init {
        initTts()
    }

    private fun initTts() {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("FiriAssistant", "Error initializing TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    // Estado ya manejado
                }

                override fun onDone(utteranceId: String?) {
                    if (_voiceState.value is FiriVoiceState.Speaking) {
                        _voiceState.value = FiriVoiceState.Idle
                    }
                }

                override fun onError(utteranceId: String?) {
                    if (_voiceState.value is FiriVoiceState.Speaking) {
                        _voiceState.value = FiriVoiceState.Idle
                    }
                }
            })
        } else {
            Log.w("FiriAssistant", "TTS initialization failed with status: $status")
        }
    }

    fun clearConversation() {
        _conversationHistory.value = emptyList()
        _transcription.value = ""
        _voiceState.value = FiriVoiceState.Idle
        stopSpeaking()
    }

    fun setImportedHistory(messages: List<FiriChatMessage>) {
        _conversationHistory.value = messages
    }

    fun startListening(
        userSettings: UserSettings,
        availableApps: List<AppInfo>,
        currentPrediction: HabitPrediction?,
        onAction: (FiriActionResult) -> Unit
    ) {
        stopSpeaking()
        _transcription.value = ""
        _voiceState.value = FiriVoiceState.Listening

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceState.value = FiriVoiceState.Error(
                if (userSettings.isSpanish) "Reconocimiento de voz no disponible en este dispositivo."
                else "Speech recognition not available on this device."
            )
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _voiceState.value = FiriVoiceState.Listening
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        _rmsDb.value = rmsdB.coerceIn(0f, 10f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _voiceState.value = FiriVoiceState.Thinking
                    }

                    override fun onError(error: Int) {
                        Log.w("FiriAssistant", "Speech error code: $error")
                        val errMsg = if (userSettings.isSpanish) {
                            when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No alcancé a escucharte bien. Toca el micrófono para hablar o escribe abajo."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo agotado. Toca el micrófono o escribe tu mensaje."
                                else -> "Toca el micrófono o escribe para conversar con Firi."
                            }
                        } else {
                            when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that. Tap mic or type below."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out. Tap mic or type below."
                                else -> "Tap mic or type to chat with Firi."
                            }
                        }
                        _voiceState.value = FiriVoiceState.Error(errMsg)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim() ?: ""
                        _transcription.value = text

                        if (text.isNotEmpty()) {
                            processQuery(text, userSettings, availableApps, currentPrediction, onAction)
                        } else {
                            _voiceState.value = FiriVoiceState.Idle
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        partialMatches?.firstOrNull()?.let {
                            _transcription.value = it
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val localeCode = if (userSettings.isSpanish) {
                when (userSettings.regionCode) {
                    "MX" -> "es-MX"
                    "CO" -> "es-CO"
                    "AR" -> "es-AR"
                    else -> "es-ES"
                }
            } else {
                if (userSettings.regionCode == "GB") "en-GB" else "en-US"
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("FiriAssistant", "Error starting speech recognition", e)
            _voiceState.value = FiriVoiceState.Error(e.message ?: "Voice recognition error")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
    }

    fun processQuery(
        query: String,
        userSettings: UserSettings,
        availableApps: List<AppInfo>,
        currentPrediction: HabitPrediction?,
        onAction: (FiriActionResult) -> Unit
    ) {
        _voiceState.value = FiriVoiceState.Thinking
        val userMsg = FiriChatMessage(isUser = true, text = query)
        _conversationHistory.value = _conversationHistory.value + userMsg

        scope.launch {
            val hasInternet = isInternetAvailable()
            val useLocalBrain = _forceOfflineMode.value || !hasInternet

            var brainUsed: FiriBrainMode
            var executionMs: Long
            val result: FiriActionResult

            if (useLocalBrain) {
                // SEGUNDO CEREBRO: Ejecución 100% On-Device en el procesador del teléfono
                val inference = onDeviceBrain.processQueryLocally(
                    query = query,
                    userSettings = userSettings,
                    availableApps = availableApps,
                    currentPrediction = currentPrediction
                )
                result = inference.actionResult
                executionMs = inference.latencyMs
                brainUsed = FiriBrainMode.ON_DEVICE_CPU
            } else {
                // PRIMER CEREBRO: Nube Gemini (Con internet)
                val start = System.currentTimeMillis()
                val analyzed = analyzeQuery(query, userSettings, availableApps, currentPrediction)
                executionMs = (System.currentTimeMillis() - start).coerceAtLeast(10L)
                result = analyzed
                brainUsed = FiriBrainMode.CLOUD_GEMINI
            }

            // Actualizar telemetría del cerebro activo
            _brainTelemetry.value = _brainTelemetry.value.copy(
                activeBrain = brainUsed,
                isConnectedToInternet = hasInternet,
                lastExecutionTimeMs = executionMs,
                brainLabelEs = if (brainUsed == FiriBrainMode.CLOUD_GEMINI) "Cerebro 1: Nube Gemini" else "Segundo Cerebro: Procesador Local (${onDeviceBrain.availableCores} núcleos)",
                brainLabelEn = if (brainUsed == FiriBrainMode.CLOUD_GEMINI) "Brain 1: Gemini Cloud" else "Second Brain: On-Device CPU (${onDeviceBrain.availableCores} cores)"
            )

            val firiMsg = FiriChatMessage(
                isUser = false,
                text = result.spokenResponse,
                brainUsed = brainUsed,
                executionTimeMs = executionMs
            )
            _conversationHistory.value = _conversationHistory.value + firiMsg

            _voiceState.value = FiriVoiceState.Speaking(result.spokenResponse)
            if (userSettings.voiceOutputEnabled) {
                speak(result.spokenResponse, userSettings)
            }
            onAction(result)
        }
    }

    private suspend fun analyzeQuery(
        query: String,
        userSettings: UserSettings,
        availableApps: List<AppInfo>,
        currentPrediction: HabitPrediction?
    ): FiriActionResult = withContext(Dispatchers.Default) {
        val lower = query.lowercase().trim()
        val isEs = userSettings.isSpanish

        // 1. Acciones directas y comandos clave del sistema
        if (lower.contains("personalizar pantalla de bloqueo") || lower.contains("editar pantalla de bloqueo") ||
            lower.contains("personalizar bloqueo") || lower.contains("editar bloqueo") ||
            (lower.contains("pantalla de bloqueo") && (lower.contains("no funciona") || lower.contains("editar") || lower.contains("cambiar") || lower.contains("configurar") || lower.contains("abrir") || lower.contains("problema"))) ||
            lower.contains("cambiar reloj de bloqueo") || lower.contains("cambiar estilo de bloqueo") ||
            lower.contains("arreglar pantalla de bloqueo") || (lower.contains("lock screen") && (lower.contains("edit") || lower.contains("customize") || lower.contains("not work") || lower.contains("change"))) ||
            lower == "customize lock screen" || lower == "edit lock screen" || lower == "lock screen settings") {
            val msg = if (isEs) "Abriendo el editor de pantalla de bloqueo. Aquí tienes vista previa en vivo para personalizar la fuente del reloj, colores, mini widgets, fondo y PIN."
            else "Opening Lock Screen editor with live preview for clock font, colors, widgets, wallpaper and PIN."
            return@withContext FiriActionResult(spokenResponse = msg, openLockCustomizer = true)
        }

        if (lower == "bloquea la pantalla" || lower == "bloquear pantalla" || lower == "apaga la pantalla" ||
            lower == "bloquear" || lower == "lock" || lower == "lock screen") {
            val msg = if (isEs) "Bloqueando la pantalla ahora." else "Locking screen now."
            return@withContext FiriActionResult(spokenResponse = msg, openLockScreen = true)
        }

        if (lower == "ajustes" || lower == "configuracion" || lower == "configuración" ||
            lower == "abrir ajustes" || lower == "abrir configuracion" || lower == "settings" || lower == "open settings") {
            val msg = if (isEs) "Abriendo la configuración del launcher." else "Opening launcher settings."
            return@withContext FiriActionResult(spokenResponse = msg, openSettings = true)
        }

        if (lower == "centro de control" || lower == "ajustes rapidos" || lower == "panel de control" ||
            lower == "control center" || lower == "abrir centro de control") {
            val msg = if (isEs) "Abriendo el Centro de Control." else "Opening Control Center."
            return@withContext FiriActionResult(spokenResponse = msg, openControlCenter = true)
        }

        if (lower == "habitos" || lower == "hábitos" || lower == "rutinas" || lower == "estadísticas" ||
            lower == "habits" || lower == "ver habitos" || lower == "mis habitos") {
            val msg = if (isEs) "Abriendo tu panel inteligente de hábitos." else "Opening your habits dashboard."
            return@withContext FiriActionResult(spokenResponse = msg, openHabitsDashboard = true)
        }

        // Comandos de audio y volumen ("sube el volumen", "bajar volumen", "mute")
        if (lower.contains("sube el volumen") || lower.contains("subir volumen") || lower.contains("aumenta el volumen") || lower == "volume up") {
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_RAISE, 0)
            } catch (_: Exception) {}
            val msg = if (isEs) "Subiendo el volumen de audio." else "Turning volume up."
            return@withContext FiriActionResult(spokenResponse = msg, triggerVolumeOverlay = true)
        }

        if (lower.contains("baja el volumen") || lower.contains("bajar volumen") || lower.contains("disminuye el volumen") || lower == "volume down") {
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_LOWER, 0)
            } catch (_: Exception) {}
            val msg = if (isEs) "Bajando el volumen de audio." else "Turning volume down."
            return@withContext FiriActionResult(spokenResponse = msg, triggerVolumeOverlay = true)
        }

        if (lower.contains("silencia el movil") || lower.contains("silenciar") || lower.contains("mutear") || lower == "mute" || lower == "silencio") {
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, 0, 0)
            } catch (_: Exception) {}
            val msg = if (isEs) "Dispositivo silenciado." else "Muted audio."
            return@withContext FiriActionResult(spokenResponse = msg, triggerVolumeOverlay = true)
        }

        if (lower.contains("barra de volumen") || lower.contains("muestra el volumen") || lower.contains("mostrar volumen") || lower.contains("probar volumen") || lower == "volumen" || lower == "volume") {
            val msg = if (isEs) "Mostrando la barra de volumen CometOS de Firi." else "Showing Firi CometOS volume bar."
            return@withContext FiriActionResult(spokenResponse = msg, triggerVolumeOverlay = true)
        }

        // 2. Patrón explícito de apertura de aplicación ("abre...", "inicia...", "open...")
        val openPrefixes = if (isEs) listOf("abre ", "abrir ", "inicia ", "iniciar ", "lanza ", "ejecuta ", "ir a ", "pon ", "entra a ")
        else listOf("open ", "launch ", "start ", "run ", "go to ", "play ")

        for (prefix in openPrefixes) {
            if (lower.startsWith(prefix)) {
                val targetAppName = lower.removePrefix(prefix).trim()
                if (targetAppName.isNotEmpty()) {
                    val matchingApp = findAppByNameOrSynonym(targetAppName, availableApps)
                    if (matchingApp != null) {
                        val response = if (isEs) "Abriendo ${matchingApp.label} para ti."
                        else "Opening ${matchingApp.label} for you."
                        return@withContext FiriActionResult(
                            spokenResponse = response,
                            appToLaunch = matchingApp
                        )
                    }
                }
            }
        }

        // 3. CONSULTAR A GEMINI AI (Inteligencia conversacional de vanguardia en vivo)
        val geminiResponse = callGeminiConversational(query, userSettings, availableApps, currentPrediction)
        if (geminiResponse != null && geminiResponse.isNotBlank()) {
            var cleanText = geminiResponse
            var openCustomizer = false
            var openSettingsAction = false
            var openLockAction = false
            var openControlAction = false
            var openHabitsAction = false

            if (cleanText.contains("[ACCION: EDITAR_BLOQUEO]")) {
                openCustomizer = true
                cleanText = cleanText.replace("[ACCION: EDITAR_BLOQUEO]", "").trim()
            }
            if (cleanText.contains("[ACCION: BLOQUEAR]")) {
                openLockAction = true
                cleanText = cleanText.replace("[ACCION: BLOQUEAR]", "").trim()
            }
            if (cleanText.contains("[ACCION: AJUSTES]")) {
                openSettingsAction = true
                cleanText = cleanText.replace("[ACCION: AJUSTES]", "").trim()
            }
            if (cleanText.contains("[ACCION: CENTRO_CONTROL]")) {
                openControlAction = true
                cleanText = cleanText.replace("[ACCION: CENTRO_CONTROL]", "").trim()
            }
            if (cleanText.contains("[ACCION: HABITOS]")) {
                openHabitsAction = true
                cleanText = cleanText.replace("[ACCION: HABITOS]", "").trim()
            }

            // Detección contextual adicional para abrir app o pantalla
            val appInResponse = availableApps.firstOrNull { app ->
                (cleanText.contains(app.label, ignoreCase = true) || lower.contains(app.label.lowercase())) &&
                        (lower.contains("abre") || lower.contains("open") || lower.contains("lanza") || lower.contains("pon"))
            }

            return@withContext FiriActionResult(
                spokenResponse = cleanText,
                appToLaunch = appInResponse,
                openLockCustomizer = openCustomizer,
                openLockScreen = openLockAction,
                openSettings = openSettingsAction,
                openControlCenter = openControlAction,
                openHabitsDashboard = openHabitsAction
            )
        }

        // 4. MOTOR CONVERSACIONAL LOCAL AVANZADO (Segundo Cerebro - Procesador Local)
        val onDeviceResult = onDeviceBrain.processQueryLocally(query, userSettings, availableApps, currentPrediction)
        onDeviceResult.actionResult
    }

    private fun findAppByNameOrSynonym(query: String, apps: List<AppInfo>): AppInfo? {
        val q = query.lowercase().trim()
        return apps.firstOrNull { it.label.lowercase() == q }
            ?: apps.firstOrNull { it.label.lowercase().contains(q) }
            ?: apps.firstOrNull { it.packageName.lowercase().contains(q) }
    }

    /**
     * Consulta conversacional a Gemini API con el modelo ultrarrápido gemini-3.1-flash-lite-preview.
     */
    private suspend fun callGeminiConversational(
        query: String,
        userSettings: UserSettings,
        availableApps: List<AppInfo>,
        currentPrediction: HabitPrediction?
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("FiriAssistant", "GEMINI_API_KEY is not configured or blank")
            return@withContext null
        }

        val isEs = userSettings.isSpanish
        val appListSample = availableApps.take(30).joinToString(", ") { it.label }
        val currentSlot = currentPrediction?.slot?.let { if (isEs) it.titleEs else it.titleEn } ?: "Momento actual"
        val currentTime = SimpleDateFormat("HH:mm, EEEE d MMMM", if (isEs) Locale.forLanguageTag("es-ES") else Locale.US).format(Date())

        val systemInstruction = """
            Eres Firi, una IA asistente brillante, sumamente inteligente, elocuente y amigable para un launcher de Android.
            Idioma del usuario: ${if (isEs) "Español" else "English"}.
            Nombre del usuario: ${userSettings.userName.ifBlank { if (isEs) "amigo" else "friend" }}.
            Hora y fecha actual: $currentTime.
            Hábito/momento del día: $currentSlot.
            Apps instaladas: $appListSample.

            Capacidades del launcher que controlas:
            - Puedes abrir cualquier app instalada.
            - Puedes abrir el editor de la pantalla de bloqueo (donde el usuario cambia reloj, mini-widgets, fondo y PIN) usando la etiqueta de control: [ACCION: EDITAR_BLOQUEO].
            - Puedes bloquear la pantalla inmediatamente usando la etiqueta de control: [ACCION: BLOQUEAR].
            - Puedes abrir los ajustes del launcher usando la etiqueta de control: [ACCION: AJUSTES].
            - Puedes abrir el centro de control usando la etiqueta de control: [ACCION: CENTRO_CONTROL].
            - Puedes abrir el panel de hábitos usando la etiqueta de control: [ACCION: HABITOS].

            Reglas fundamentales:
            1. Inteligencia genuina: Responde con profundidad, precisión, elegancia y simpatía a cualquier pregunta (ciencia, filosofía, ideas, consejos, historia, tecnología o conversación casual). Nunca des respuestas secas ni robóticas.
            2. Si el usuario te dice que eres tonta, inútil o que algo no funciona (por ejemplo la pantalla de bloqueo): Demuestra inmediatamente tu valor, responde con empatía y humor inteligente, aclara el problema y ofrécele resolverlo o llévalo directo a la acción. Si pregunta por la pantalla de bloqueo o desea personalizarla, explícale que ya tiene una vista previa en vivo y usa [ACCION: EDITAR_BLOQUEO].
            3. Síntesis para voz: Tus respuestas deben tener de 2 a 4 oraciones sustanciosas y agradables.
            4. Formato para TTS: NUNCA utilices asteriscos (*), almohadillas (#), guiones (-) ni marcas de formato markdown, pues tu texto será leído en voz alta.
        """.trimIndent()

        // Historial multi-turno bien estructurado (siempre iniciando con un turno 'user')
        val contentsArray = JSONArray()
        val rawHistory = _conversationHistory.value.takeLast(6)
        val validHistory = if (rawHistory.isNotEmpty() && !rawHistory.first().isUser) {
            rawHistory.drop(1)
        } else {
            rawHistory
        }

        for (msg in validHistory) {
            if (msg.text.isNotBlank()) {
                contentsArray.put(JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", msg.text))
                    })
                })
            }
        }

        // Asegurar que la consulta actual esté como último turno user
        if (validHistory.lastOrNull()?.text != query) {
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", query))
                })
            })
        }

        val jsonBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                })
            })
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.75)
                put("maxOutputTokens", 600)
            })
        }

        // Modelos de respuesta en orden de velocidad y fiabilidad comprobadas
        val modelCandidates = listOf(
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-3.1-flash-lite-preview",
            "gemini-flash-latest"
        )

        for (modelName in modelCandidates) {
            try {
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val respStr = response.body?.string() ?: continue
                    val root = JSONObject(respStr)
                    val candidates = root.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")

                    if (parts != null && parts.length() > 0) {
                        val textParts = StringBuilder()
                        for (i in 0 until parts.length()) {
                            val partObj = parts.optJSONObject(i)
                            val t = partObj?.optString("text")
                            if (!t.isNullOrBlank()) {
                                textParts.append(t).append(" ")
                            }
                        }
                        val fullText = textParts.toString().trim()
                        if (fullText.isNotBlank()) {
                            // Limpieza de formato markdown para lectura TTS cristalina
                            val cleaned = fullText
                                .replace(Regex("[*#`_~>]"), "")
                                .replace(Regex("\\s+"), " ")
                                .trim()
                            return@withContext cleaned
                        }
                    }
                } else {
                    Log.w("FiriAssistant", "Model $modelName returned HTTP ${response.code}")
                }
            } catch (e: Exception) {
                Log.w("FiriAssistant", "Call failed for model $modelName: ${e.message}")
            }
        }
        null
    }

    fun speak(text: String, userSettings: UserSettings) {
        if (!isTtsReady || textToSpeech == null) return

        val locale = if (userSettings.isSpanish) {
            when (userSettings.regionCode) {
                "MX" -> Locale.forLanguageTag("es-MX")
                "CO" -> Locale.forLanguageTag("es-CO")
                "AR" -> Locale.forLanguageTag("es-AR")
                else -> Locale.forLanguageTag("es-ES")
            }
        } else {
            if (userSettings.regionCode == "GB") Locale.UK else Locale.US
        }

        textToSpeech?.language = locale
        val utteranceId = "firi_${System.currentTimeMillis()}"
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {
        }
        if (_voiceState.value is FiriVoiceState.Speaking) {
            _voiceState.value = FiriVoiceState.Idle
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {
        }
    }
}
