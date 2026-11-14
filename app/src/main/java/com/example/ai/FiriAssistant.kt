package com.example.ai

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.data.LauncherRepository
import com.example.model.AppCategory
import com.example.model.AppInfo
import com.example.model.HabitPrediction
import com.example.model.UserSettings
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

sealed class FiriVoiceState {
    object Idle : FiriVoiceState()
    object Listening : FiriVoiceState()
    object Thinking : FiriVoiceState()
    data class Speaking(val text: String) : FiriVoiceState()
    data class Error(val message: String) : FiriVoiceState()
}

enum class FiriBrainMode {
    CLOUD_GEMINI,      // Cerebro 1: Nube Gemini (Con internet + Maps + Search + SQL)
    ON_DEVICE_CPU      // Cerebro 2: Procesador local del teléfono (Sin internet + SQL)
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
    val executionTimeMs: Long = 0L,
    val mapPlaces: List<FiriMapPlace> = emptyList(),
    val webSources: List<FiriWebSource> = emptyList(),
    val searchQueries: List<String> = emptyList(),
    val sqlInsightsSummary: String? = null,
    val usedGoogleMaps: Boolean = false,
    val usedGoogleSearch: Boolean = false,
    val usedSqlData: Boolean = false,
    val generatedImagePath: String? = null,
    val sourceImagePath: String? = null,
    val usedImageModel: Boolean = false,
    val imageModelUsed: String? = null
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
    val triggerVolumeOverlay: Boolean = false,
    val mapPlaces: List<FiriMapPlace> = emptyList(),
    val webSources: List<FiriWebSource> = emptyList(),
    val searchQueries: List<String> = emptyList(),
    val sqlInsightsSummary: String? = null,
    val usedGoogleMaps: Boolean = false,
    val usedGoogleSearch: Boolean = false,
    val usedSqlData: Boolean = false,
    val answeredByCloud: Boolean = false,
    val generatedImagePath: String? = null,
    val sourceImagePath: String? = null,
    val usedImageModel: Boolean = false,
    val imageModelUsed: String? = null
)

private data class GeminiGroundedReply(
    val text: String,
    val mapPlaces: List<FiriMapPlace>,
    val webSources: List<FiriWebSource>,
    val searchQueries: List<String>,
    val usedGoogleMaps: Boolean,
    val usedGoogleSearch: Boolean
)

private sealed class CloudCallOutcome {
    data class Success(val reply: GeminiGroundedReply) : CloudCallOutcome()
    data class Failure(val reason: String, val isMissingKey: Boolean = false) : CloudCallOutcome()
}

class FiriAssistant(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repository: LauncherRepository? = null
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
    private val imageEngine = FiriImageEngine(context)

    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage: StateFlow<Boolean> = _isGeneratingImage.asStateFlow()

    private val _imageGenerationStatus = MutableStateFlow<Pair<Boolean, String>?>(null)
    val imageGenerationStatus: StateFlow<Pair<Boolean, String>?> = _imageGenerationStatus.asStateFlow()

    private val _forceOfflineMode = MutableStateFlow(false)
    val forceOfflineMode: StateFlow<Boolean> = _forceOfflineMode.asStateFlow()

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    private val _sqlStats = MutableStateFlow(0 to 0) // memoriesCount to savedPlacesCount
    val sqlStats: StateFlow<Pair<Int, Int>> = _sqlStats.asStateFlow()

    private val _brainTelemetry = MutableStateFlow(
        FiriBrainTelemetry(
            activeBrain = if (isInternetAvailable()) FiriBrainMode.CLOUD_GEMINI else FiriBrainMode.ON_DEVICE_CPU,
            isConnectedToInternet = isInternetAvailable(),
            forceOfflineMode = false,
            processorCores = onDeviceBrain.availableCores
        )
    )
    val brainTelemetry: StateFlow<FiriBrainTelemetry> = _brainTelemetry.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(22, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    init {
        initTts()
        observeSqlStats()
        refreshLocationIfPermitted()
    }

    private fun observeSqlStats() {
        val repo = repository ?: return
        scope.launch {
            combine(repo.firiMemoryCountFlow, repo.savedPlacesCountFlow) { memories, places ->
                memories to places
            }.collect { stats ->
                _sqlStats.value = stats
            }
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun refreshLocationIfPermitted() {
        if (!hasLocationPermission()) return
        scope.launch(Dispatchers.IO) {
            val coords = fetchLastKnownLatLng()
            if (coords != null) {
                _userLocation.value = coords
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchLastKnownLatLng(): Pair<Double, Double>? {
        if (!hasLocationPermission()) return null
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val fusedLoc = suspendCancellableCoroutine<Pair<Double, Double>?> { cont ->
                client.lastLocation
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) {
                            cont.resume(loc?.let { it.latitude to it.longitude })
                        }
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }
            if (fusedLoc != null) return fusedLoc

            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            for (provider in providers) {
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null) return loc.latitude to loc.longitude
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun isInternetAvailable(): Boolean {
        if (_forceOfflineMode.value) return false
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val activeCaps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
                if (activeCaps != null && activeCaps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    return true
                }
                @Suppress("DEPRECATION")
                cm.allNetworks.any { net ->
                    val caps = cm.getNetworkCapabilities(net)
                    caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
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
                override fun onStart(utteranceId: String?) {}
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
                            processQuery(text, userSettings, availableApps, currentPrediction, onAction = onAction)
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
        editImageUriOrPath: String? = null,
        aspectRatio: String = "1:1",
        onAction: (FiriActionResult) -> Unit
    ) {
        val resolvedEditSource = editImageUriOrPath ?: findImplicitEditSourceImage(query)
        val isImageRequest = !resolvedEditSource.isNullOrBlank() || looksLikeImageGenerationRequest(query.lowercase())

        _voiceState.value = FiriVoiceState.Thinking
        val userMsg = FiriChatMessage(
            isUser = true,
            text = query,
            sourceImagePath = resolvedEditSource
        )
        _conversationHistory.value = _conversationHistory.value + userMsg

        scope.launch {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val sqlContext = repository?.buildFiriSqlContext(query, currentHour)
            if (_userLocation.value == null && hasLocationPermission()) {
                _userLocation.value = kotlinx.coroutines.withTimeoutOrNull(1500L) {
                    withContext(Dispatchers.IO) { fetchLastKnownLatLng() }
                }
            }

            val hasInternet = isInternetAvailable()
            val useLocalBrain = _forceOfflineMode.value || !hasInternet

            val brainUsed: FiriBrainMode
            val executionMs: Long
            val result: FiriActionResult

            if (isImageRequest) {
                val start = System.currentTimeMillis()
                if (useLocalBrain) {
                    val offlineMsg = if (userSettings.isSpanish) {
                        "Para crear o editar imágenes con Nano Banana 2.1 necesito conexión a internet (y desactivar el modo offline)."
                    } else {
                        "Creating or editing images with Nano Banana 2.1 requires an active internet connection."
                    }
                    result = FiriActionResult(spokenResponse = offlineMsg, answeredByCloud = false)
                    brainUsed = FiriBrainMode.ON_DEVICE_CPU
                    executionMs = (System.currentTimeMillis() - start).coerceAtLeast(10L)
                } else {
                    _isGeneratingImage.value = true
                    _imageGenerationStatus.value = (!resolvedEditSource.isNullOrBlank()) to query
                    try {
                        val imgRes = imageEngine.generateOrEditImage(
                            prompt = query,
                            inputImageUriOrPath = resolvedEditSource,
                            aspectRatio = aspectRatio,
                            isSpanish = userSettings.isSpanish
                        )
                        executionMs = (System.currentTimeMillis() - start).coerceAtLeast(15L)
                        when (imgRes) {
                            is FiriImageResult.Success -> {
                                result = FiriActionResult(
                                    spokenResponse = imgRes.caption,
                                    generatedImagePath = imgRes.filePath,
                                    sourceImagePath = resolvedEditSource,
                                    usedImageModel = true,
                                    imageModelUsed = imgRes.modelUsed,
                                    answeredByCloud = true
                                )
                                brainUsed = FiriBrainMode.CLOUD_GEMINI
                            }
                            is FiriImageResult.Error -> {
                                result = FiriActionResult(
                                    spokenResponse = imgRes.message,
                                    answeredByCloud = false
                                )
                                brainUsed = FiriBrainMode.CLOUD_GEMINI
                            }
                        }
                    } finally {
                        _isGeneratingImage.value = false
                        _imageGenerationStatus.value = null
                    }
                }
            } else if (useLocalBrain) {
                val inference = onDeviceBrain.processQueryLocally(
                    query = query,
                    userSettings = userSettings,
                    availableApps = availableApps,
                    currentPrediction = currentPrediction,
                    sqlContext = sqlContext
                )
                result = inference.actionResult
                executionMs = inference.latencyMs
                brainUsed = FiriBrainMode.ON_DEVICE_CPU
            } else {
                val start = System.currentTimeMillis()
                val analyzed = analyzeQuery(query, userSettings, availableApps, currentPrediction, sqlContext)
                executionMs = (System.currentTimeMillis() - start).coerceAtLeast(10L)
                result = analyzed
                brainUsed = if (analyzed.answeredByCloud) FiriBrainMode.CLOUD_GEMINI else FiriBrainMode.ON_DEVICE_CPU
            }

            // Guardar memoria SQL y lugares encontrados para que Firi aprenda continuamente
            val sourcesTag = buildList {
                if (result.usedSqlData) add("SQL")
                if (result.usedGoogleSearch) add("SEARCH")
                if (result.usedGoogleMaps || result.mapPlaces.isNotEmpty()) add("MAPS")
                if (result.usedImageModel) add("IMAGE")
            }.joinToString(",").ifBlank { if (brainUsed == FiriBrainMode.CLOUD_GEMINI) "CLOUD" else "CPU" }

            repository?.saveFiriInteractionToSql(
                query = query,
                summaryResponse = result.spokenResponse,
                sourceTypes = sourcesTag,
                places = result.mapPlaces
            )

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
                executionTimeMs = executionMs,
                mapPlaces = result.mapPlaces,
                webSources = result.webSources,
                searchQueries = result.searchQueries,
                sqlInsightsSummary = result.sqlInsightsSummary,
                usedGoogleMaps = result.usedGoogleMaps || result.mapPlaces.isNotEmpty(),
                usedGoogleSearch = result.usedGoogleSearch || result.webSources.isNotEmpty(),
                usedSqlData = result.usedSqlData,
                generatedImagePath = result.generatedImagePath,
                sourceImagePath = result.sourceImagePath,
                usedImageModel = result.usedImageModel,
                imageModelUsed = result.imageModelUsed
            )
            _conversationHistory.value = _conversationHistory.value + firiMsg

            _voiceState.value = FiriVoiceState.Speaking(result.spokenResponse)
            if (userSettings.voiceOutputEnabled) {
                speak(result.spokenResponse, userSettings)
            }
            onAction(result)
        }
    }

    private fun looksLikeImageGenerationRequest(lower: String): Boolean {
        if (lower.contains("pantalla de bloqueo") || lower.contains("lock screen")) return false
        val imageTriggers = listOf(
            "genera una imagen", "generar imagen", "genérame una imagen", "generame una imagen",
            "crea una imagen", "crear imagen", "créame una imagen", "creame una imagen",
            "dibuja ", "dibújame", "dibujame", "ilustra ", "ilústrame", "ilustrame",
            "diseña una imagen", "diseñar una imagen", "haz una imagen", "hazme una imagen",
            "edita la imagen", "editar imagen", "edita esta imagen", "edita la foto", "editar foto",
            "genera un fondo", "crea un fondo", "crea una ilustración", "genera una ilustración",
            "generate image", "generate an image", "create an image", "create image",
            "draw ", "illustrate ", "edit this image", "edit image", "edit the image",
            "edit photo", "make an image", "paint a "
        )
        return imageTriggers.any { lower.contains(it) }
    }

    private fun findImplicitEditSourceImage(query: String): String? {
        val lastImage = _conversationHistory.value.lastOrNull { !it.generatedImagePath.isNullOrBlank() }?.generatedImagePath
            ?: return null
        val lower = query.lowercase().trim()
        if (lower.contains("volumen") || lower.contains("volume") || lower.contains("bloqueo") || lower.contains("ajustes")) {
            return null
        }
        val editSignals = listOf(
            "la imagen", "esta imagen", "la foto", "esta foto", "the image", "this image", "the photo",
            "edita ", "edítale", "editale", "cámbiale", "cambiale", "ponle ", "quítale", "quitale",
            "agrégale", "agregale", "hazla ", "hazlo ", "convierte ", "cambia el fondo", "cambia el color",
            "edit it", "make it ", "change the ", "add a ", "remove the "
        )
        return if (editSignals.any { lower.contains(it) }) lastImage else null
    }

    private suspend fun analyzeQuery(
        query: String,
        userSettings: UserSettings,
        availableApps: List<AppInfo>,
        currentPrediction: HabitPrediction?,
        sqlContext: FiriSqlContextSnapshot?
    ): FiriActionResult = withContext(Dispatchers.Default) {
        val lower = query.lowercase().trim()
        val isEs = userSettings.isSpanish

        // 1. Acciones directas y comandos clave del sistema
        if (lower.contains("personalizar pantalla de bloqueo") || lower.contains("editar pantalla de bloqueo") ||
            lower.contains("personalizar bloqueo") || lower.contains("editar bloqueo") ||
            (lower.contains("pantalla de bloqueo") && (lower.contains("no funciona") || lower.contains("editar") || lower.contains("cambiar") || lower.contains("configurar") || lower.contains("abrir") || lower.contains("problema"))) ||
            lower.contains("cambiar reloj de bloqueo") || lower.contains("cambiar estilo de bloqueo") ||
            lower.contains("arreglar pantalla de bloqueo") || (lower.contains("lock screen") && (lower.contains("edit") || lower.contains("customize") || lower.contains("not work") || lower.contains("change"))) ||
            lower == "customize lock screen" || lower == "edit lock screen" || lower == "lock screen settings"
        ) {
            val msg = if (isEs) "Abriendo el editor de pantalla de bloqueo. Aquí tienes vista previa en vivo para personalizar la fuente del reloj, colores, mini widgets, fondo y PIN."
            else "Opening Lock Screen editor with live preview for clock font, colors, widgets, wallpaper and PIN."
            return@withContext FiriActionResult(spokenResponse = msg, openLockCustomizer = true)
        }

        if (lower == "bloquea la pantalla" || lower == "bloquear pantalla" || lower == "apaga la pantalla" ||
            lower == "bloquear" || lower == "lock" || lower == "lock screen"
        ) {
            val msg = if (isEs) "Bloqueando la pantalla ahora." else "Locking screen now."
            return@withContext FiriActionResult(spokenResponse = msg, openLockScreen = true)
        }

        if (lower == "ajustes" || lower == "configuracion" || lower == "configuración" ||
            lower == "abrir ajustes" || lower == "abrir configuracion" || lower == "settings" || lower == "open settings"
        ) {
            val msg = if (isEs) "Abriendo la configuración del launcher." else "Opening launcher settings."
            return@withContext FiriActionResult(spokenResponse = msg, openSettings = true)
        }

        if (lower == "centro de control" || lower == "ajustes rapidos" || lower == "panel de control" ||
            lower == "control center" || lower == "abrir centro de control"
        ) {
            val msg = if (isEs) "Abriendo el Centro de Control." else "Opening Control Center."
            return@withContext FiriActionResult(spokenResponse = msg, openControlCenter = true)
        }

        if (lower == "habitos" || lower == "hábitos" || lower == "rutinas" || lower == "estadísticas" ||
            lower == "habits" || lower == "ver habitos" || lower == "mis habitos"
        ) {
            val msg = if (isEs) "Abriendo tu panel inteligente de hábitos." else "Opening your habits dashboard."
            return@withContext FiriActionResult(spokenResponse = msg, openHabitsDashboard = true, usedSqlData = true)
        }

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
        val openPrefixes = if (isEs) listOf("abre ", "abrir ", "inicia ", "iniciar ", "lanza ", "ejecuta ", "pon ", "entra a ", "ir a ")
        else listOf("open ", "launch ", "start ", "run ", "go to ", "play ")

        for (prefix in openPrefixes) {
            if (lower.startsWith(prefix)) {
                val targetAppName = lower.removePrefix(prefix).trim()
                if (targetAppName.isNotEmpty() && !looksLikeMapOrPlaceRequest(targetAppName)) {
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

        // 3. CONSULTAR A GEMINI AI CON GROUNDING DE GOOGLE MAPS, GOOGLE SEARCH Y DATOS SQL
        val isMapQuery = looksLikeMapOrPlaceRequest(lower)
        val cloudOutcome = callGeminiConversational(
            query = query,
            userSettings = userSettings,
            availableApps = availableApps,
            currentPrediction = currentPrediction,
            sqlContext = sqlContext,
            isMapQuery = isMapQuery
        )

        if (cloudOutcome is CloudCallOutcome.Success && cloudOutcome.reply.text.isNotBlank()) {
            val groundedReply = cloudOutcome.reply
            var cleanText = groundedReply.text
            var openCustomizer = false
            var openSettingsAction = false
            var openLockAction = false
            var openControlAction = false
            var openHabitsAction = false
            val explicitMapTags = mutableListOf<String>()

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

            // Extraer etiquetas opcionales [MAPA: Lugar]
            val mapTagRegex = Regex("\\[MAPA:\\s*([^\\]]+)\\]", RegexOption.IGNORE_CASE)
            mapTagRegex.findAll(cleanText).forEach { m ->
                val placeLabel = m.groupValues[1].trim()
                if (placeLabel.isNotBlank()) explicitMapTags.add(placeLabel)
            }
            cleanText = cleanText.replace(mapTagRegex, "").trim()

            // Enriquecer lugares con Geocoder y respaldo para consultas explícitas de mapa
            val enrichedPlaces = enrichMapPlaces(
                rawPlaces = groundedReply.mapPlaces,
                explicitTags = explicitMapTags,
                query = query,
                isMapQuery = isMapQuery,
                sqlContext = sqlContext,
                isSpanish = isEs
            )

            val appInResponse = availableApps.firstOrNull { app ->
                (cleanText.contains(app.label, ignoreCase = true) || lower.contains(app.label.lowercase())) &&
                    (lower.contains("abre") || lower.contains("open") || lower.contains("lanza") || lower.contains("pon")) &&
                    !isMapQuery
            }

            val usedSql = sqlContext?.hasRichData == true
            val sqlBadgeSummary = if (usedSql) {
                if (isEs) "SQLite (${sqlContext?.totalMemoriesCount ?: 0} recuerdos · ${sqlContext?.totalSavedPlacesCount ?: 0} mapas)"
                else "SQLite (${sqlContext?.totalMemoriesCount ?: 0} memories · ${sqlContext?.totalSavedPlacesCount ?: 0} maps)"
            } else null

            return@withContext FiriActionResult(
                spokenResponse = cleanText,
                appToLaunch = appInResponse,
                openLockCustomizer = openCustomizer,
                openLockScreen = openLockAction,
                openSettings = openSettingsAction,
                openControlCenter = openControlAction,
                openHabitsDashboard = openHabitsAction,
                mapPlaces = enrichedPlaces,
                webSources = groundedReply.webSources,
                searchQueries = groundedReply.searchQueries,
                sqlInsightsSummary = sqlBadgeSummary,
                usedGoogleMaps = groundedReply.usedGoogleMaps || enrichedPlaces.isNotEmpty(),
                usedGoogleSearch = groundedReply.usedGoogleSearch || groundedReply.webSources.isNotEmpty(),
                usedSqlData = usedSql,
                answeredByCloud = true
            )
        }

        // 4. Si la nube falló estando conectados:
        //    - Si la petición es local (matemáticas, SQL/notas, hora, fecha, CPU, apps), respóndela localmente.
        //    - Si requería la nube, informa con claridad el motivo real (clave o error HTTP) sin fingir "sin conexión".
        val failure = cloudOutcome as? CloudCallOutcome.Failure
        if (isLocallyAnswerableQuery(lower, availableApps)) {
            val onDeviceResult = onDeviceBrain.processQueryLocally(
                query = query,
                userSettings = userSettings,
                availableApps = availableApps,
                currentPrediction = currentPrediction,
                sqlContext = sqlContext
            )
            return@withContext onDeviceResult.actionResult.copy(answeredByCloud = false)
        }

        val fallbackPlaces = if (isMapQuery) {
            enrichMapPlaces(
                rawPlaces = emptyList(),
                explicitTags = emptyList(),
                query = query,
                isMapQuery = true,
                sqlContext = sqlContext,
                isSpanish = isEs
            )
        } else emptyList()

        val errorMsg = when {
            failure?.isMissingKey == true -> {
                if (isEs) "Falta configurar tu clave GEMINI_API_KEY en el panel Secrets de AI Studio para activar las respuestas en la nube de Firi y Google Search."
                else "GEMINI_API_KEY is not configured in AI Studio's Secrets panel. Add it to enable Firi Cloud responses and Google Search."
            }
            failure != null -> {
                if (isEs) "No pude obtener respuesta de Gemini Cloud (${failure.reason}). Revisa tu clave GEMINI_API_KEY o intenta de nuevo."
                else "Couldn't reach Gemini Cloud (${failure.reason}). Check your GEMINI_API_KEY or try again."
            }
            else -> {
                if (isEs) "No pude completar la consulta en la nube en este momento. Intenta de nuevo."
                else "Couldn't complete the cloud request right now. Please try again."
            }
        }

        FiriActionResult(
            spokenResponse = errorMsg,
            mapPlaces = fallbackPlaces,
            usedGoogleMaps = fallbackPlaces.isNotEmpty(),
            answeredByCloud = false
        )
    }

    private fun isLocallyAnswerableQuery(lower: String, availableApps: List<AppInfo>): Boolean {
        val localHints = listOf(
            "segundo cerebro", "second brain", "datos sql", "mis datos", "mis notas", "my notes",
            "lugares guardados", "saved places", "hora", "time", "fecha", "date", "cpu", "procesador",
            "nucleos", "núcleos", "calcula", "cuanto es", "cuánto es", "raiz", "raíz", "chiste", "joke",
            "consejo", "productividad", "tip", "habitos", "hábitos", "rutina", "que apps uso"
        )
        if (localHints.any { lower.contains(it) }) return true
        if (Regex("""\d+\s*[\+\-\*\/x%]\s*\d+""").containsMatchIn(lower)) return true
        if (availableApps.any { it.label.lowercase() == lower }) return true
        return false
    }

    private fun looksLikeMapOrPlaceRequest(lowerQuery: String): Boolean {
        val mapKeywords = listOf(
            "mapa", "mapas", "map", "maps", "ubicacion", "ubicación", "donde esta", "dónde está",
            "donde queda", "dónde queda", "como llegar", "cómo llegar", "ruta", "dirección", "direccion",
            "cerca de mi", "cerca de mí", "cerca", "restaurante", "restaurantes", "cafe", "café", "cafes",
            "farmacia", "hospital", "hotel", "hoteles", "parque", "gasolinera", "banco", "museo", "tienda",
            "aeropuerto", "estacion", "estación", "supermercado", "gimnasio", "lugares", "sitios", "zona",
            "near me", "directions", "where is", "coffee", "restaurant", "hospital", "pharmacy", "park", "places"
        )
        return mapKeywords.any { lowerQuery.contains(it) }
    }

    private fun findAppByNameOrSynonym(query: String, apps: List<AppInfo>): AppInfo? {
        val q = query.lowercase().trim()
        return apps.firstOrNull { it.label.lowercase() == q }
            ?: apps.firstOrNull { it.label.lowercase().contains(q) }
            ?: apps.firstOrNull { it.packageName.lowercase().contains(q) }
    }

    private fun getFallbackRegionLatLng(regionCode: String): Pair<Double, Double> {
        return when (regionCode.uppercase()) {
            "MX" -> 19.4326 to -99.1332 // Ciudad de México
            "CO" -> 4.7110 to -74.0721  // Bogotá
            "AR" -> -34.6037 to -58.3816 // Buenos Aires
            "ES" -> 40.4168 to -3.7038   // Madrid
            "GB" -> 51.5072 to -0.1276   // London
            else -> 37.7749 to -122.4194 // San Francisco
        }
    }

    /**
     * Consulta conversacional a Gemini API v1beta con Google Maps, Google Search y datos SQL en vivo.
     */
    private suspend fun callGeminiConversational(
        query: String,
        userSettings: UserSettings,
        availableApps: List<AppInfo>,
        currentPrediction: HabitPrediction?,
        sqlContext: FiriSqlContextSnapshot?,
        isMapQuery: Boolean
    ): CloudCallOutcome = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("FiriAssistant", "GEMINI_API_KEY is not configured or blank")
            return@withContext CloudCallOutcome.Failure("GEMINI_API_KEY missing", isMissingKey = true)
        }

        val isEs = userSettings.isSpanish
        val appListSample = availableApps.take(25).joinToString(", ") { it.label }
        val currentSlot = currentPrediction?.slot?.let { if (isEs) it.titleEs else it.titleEn } ?: "Momento actual"
        val currentTime = SimpleDateFormat("HH:mm, EEEE d MMMM yyyy", if (isEs) Locale.forLanguageTag("es-ES") else Locale.US).format(Date())

        val gpsCoords = _userLocation.value
        val activeCoords = gpsCoords ?: getFallbackRegionLatLng(userSettings.regionCode)
        val locationNote = if (gpsCoords != null) {
            "Ubicación GPS real del usuario: lat=${gpsCoords.first}, lng=${gpsCoords.second}"
        } else {
            "Región configurada del usuario: ${userSettings.regionCode} (lat aprox=${activeCoords.first}, lng aprox=${activeCoords.second})"
        }

        val sqlSummaryBlock = buildString {
            appendLine("=== DATOS EN BASE SQL LOCAL (SQLite / Room) ===")
            if (!sqlContext?.topAppsSummary.isNullOrBlank()) {
                appendLine("- Apps más usadas históricamente (SQL usage_habits): ${sqlContext?.topAppsSummary}")
            }
            if (!sqlContext?.currentHourAppsSummary.isNullOrBlank()) {
                appendLine("- Apps habituales a esta hora (SQL): ${sqlContext?.currentHourAppsSummary}")
            }
            if (!sqlContext?.notesSummary.isNullOrBlank()) {
                appendLine("- Notas y tareas activas del usuario (SQL launcher_widgets): ${sqlContext?.notesSummary}")
            }
            if (!sqlContext?.matchingPlaces.isNullOrEmpty()) {
                val pText = sqlContext?.matchingPlaces?.joinToString("; ") { "${it.title} (${it.address})" }
                appendLine("- Lugares guardados relevantes en SQL: $pText")
            } else if (!sqlContext?.recentPlaces.isNullOrEmpty()) {
                val pText = sqlContext?.recentPlaces?.take(3)?.joinToString("; ") { it.title }
                appendLine("- Lugares consultados recientemente en SQL: $pText")
            }
            if (!sqlContext?.matchingMemories.isNullOrEmpty()) {
                val mText = sqlContext?.matchingMemories?.joinToString(" | ") { "${it.userQuery} -> ${it.summaryResponse.take(120)}" }
                appendLine("- Memoria de búsquedas SQL relacionada: $mText")
            }
        }

        val systemInstruction = """
            Eres Firi, una IA asistente brillante, proactiva, sumamente inteligente y amigable integrada en un launcher de Android.
            Idioma del usuario: ${if (isEs) "Español" else "English"}.
            Nombre del usuario: ${userSettings.userName.ifBlank { if (isEs) "amigo" else "friend" }}.
            Hora y fecha actual: $currentTime.
            Hábito/momento del día: $currentSlot.
            $locationNote.
            Apps instaladas: $appListSample.

            $sqlSummaryBlock

            Capacidades y herramientas activas:
            - Tienes acceso a Google Search en tiempo real para noticias, datos actuales, clima, hechos y búsquedas en la web.
            - Tienes acceso a Google Maps para ubicar lugares, direcciones, restaurantes, tiendas, parques, monumentos y rutas.
            - Si el usuario busca un lugar, mapa o cómo llegar, responde con precisión usando Google Maps y además añade al final hasta 3 etiquetas discretas con formato [MAPA: Nombre exacto del lugar, Ciudad] si identificas lugares concretos para mostrar su tarjeta interactiva de mapa.
            - Aprovecha los datos SQL anteriores cuando aporten valor personal (por ejemplo, si pregunta por sus notas, hábitos, lugares previos o recomendaciones personalizadas).
            - Puedes abrir el editor de pantalla de bloqueo con [ACCION: EDITAR_BLOQUEO], bloquear con [ACCION: BLOQUEAR], abrir ajustes con [ACCION: AJUSTES], centro de control con [ACCION: CENTRO_CONTROL] o hábitos con [ACCION: HABITOS].

            Reglas de estilo:
            1. Responde con naturalidad en 2 a 4 oraciones claras, útiles e inteligentes.
            2. NUNCA uses asteriscos (*), almohadillas (#), guiones de lista ni markdown porque el texto también se lee en voz alta (TTS).
        """.trimIndent()

        val contentsArray = JSONArray()
        val rawHistory = _conversationHistory.value.takeLast(6)
        val validHistory = if (rawHistory.isNotEmpty() && !rawHistory.first().isUser) rawHistory.drop(1) else rawHistory

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
        if (validHistory.lastOrNull()?.text != query) {
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", query))
                })
            })
        }

        val languageCode = if (isEs) {
            when (userSettings.regionCode) {
                "MX" -> "es-MX"
                "CO" -> "es-CO"
                "AR" -> "es-AR"
                else -> "es-ES"
            }
        } else {
            if (userSettings.regionCode == "GB") "en-GB" else "en-US"
        }

        data class GroundingAttempt(
            val model: String,
            val enableMaps: Boolean,
            val enableSearch: Boolean,
            val includeRetrievalConfig: Boolean = false
        )

        val attempts = buildList {
            if (isMapQuery) {
                add(GroundingAttempt("gemini-2.5-flash", enableMaps = true, enableSearch = false, includeRetrievalConfig = true))
                add(GroundingAttempt("gemini-2.5-flash", enableMaps = true, enableSearch = false, includeRetrievalConfig = false))
                add(GroundingAttempt("gemini-2.5-flash", enableMaps = false, enableSearch = true, includeRetrievalConfig = false))
            } else {
                add(GroundingAttempt("gemini-2.5-flash", enableMaps = false, enableSearch = true, includeRetrievalConfig = false))
            }
            add(GroundingAttempt("gemini-2.5-flash", enableMaps = false, enableSearch = false, includeRetrievalConfig = false))
            add(GroundingAttempt("gemini-flash-latest", enableMaps = false, enableSearch = false, includeRetrievalConfig = false))
            add(GroundingAttempt("gemini-2.5-flash-lite", enableMaps = false, enableSearch = false, includeRetrievalConfig = false))
            add(GroundingAttempt("gemini-3.1-flash-lite-preview", enableMaps = false, enableSearch = false, includeRetrievalConfig = false))
            add(GroundingAttempt("gemini-2.0-flash", enableMaps = false, enableSearch = false, includeRetrievalConfig = false))
        }

        var lastError = "No response"
        for (attempt in attempts) {
            try {
                val jsonBody = JSONObject().apply {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", systemInstruction))
                        })
                    })
                    put("contents", contentsArray)
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.65)
                        put("maxOutputTokens", 750)
                    })

                    val toolsArray = JSONArray()
                    if (attempt.enableSearch) {
                        toolsArray.put(JSONObject().put("googleSearch", JSONObject()))
                    }
                    if (attempt.enableMaps) {
                        toolsArray.put(JSONObject().put("googleMaps", JSONObject()))
                    }
                    if (toolsArray.length() > 0) {
                        put("tools", toolsArray)
                    }

                    // retrievalConfig con latLng solo aplica cuando googleMaps está activo
                    if (attempt.enableMaps && attempt.includeRetrievalConfig) {
                        put("toolConfig", JSONObject().apply {
                            put("retrievalConfig", JSONObject().apply {
                                put("latLng", JSONObject().apply {
                                    put("latitude", activeCoords.first)
                                    put("longitude", activeCoords.second)
                                })
                                put("languageCode", languageCode)
                            })
                        })
                    }
                }

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/${attempt.model}:generateContent?key=$apiKey")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val respStr = response.body?.string()
                    if (response.isSuccessful && !respStr.isNullOrBlank()) {
                        val parsed = parseGeminiResponse(respStr)
                        if (parsed != null) {
                            return@withContext CloudCallOutcome.Success(parsed)
                        } else {
                            lastError = "Empty response (${attempt.model})"
                        }
                    } else {
                        lastError = "HTTP ${response.code}"
                        Log.w(
                            "FiriAssistant",
                            "Model ${attempt.model} (maps=${attempt.enableMaps}, search=${attempt.enableSearch}) HTTP ${response.code}: ${respStr?.take(250)}"
                        )
                    }
                }
            } catch (e: Exception) {
                lastError = e.javaClass.simpleName + ": " + (e.message ?: "network error")
                Log.w("FiriAssistant", "Call failed for ${attempt.model}: ${e.message}")
            }
        }
        CloudCallOutcome.Failure(lastError)
    }

    private fun parseGeminiResponse(respStr: String): GeminiGroundedReply? {
        val root = JSONObject(respStr)
        val candidates = root.optJSONArray("candidates") ?: return null
        val firstCandidate = candidates.optJSONObject(0) ?: return null
        val content = firstCandidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts") ?: return null

        val textParts = StringBuilder()
        for (i in 0 until parts.length()) {
            val partObj = parts.optJSONObject(i)
            if (partObj?.optBoolean("thought", false) == true) continue
            val t = partObj?.optString("text")
            if (!t.isNullOrBlank()) {
                textParts.append(t).append(" ")
            }
        }
        val rawText = textParts.toString().trim()
        if (rawText.isBlank()) return null

        val cleanedText = rawText
            .replace(Regex("[*#`_~>]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        val mapPlaces = mutableListOf<FiriMapPlace>()
        val webSources = mutableListOf<FiriWebSource>()
        val searchQueries = mutableListOf<String>()

        val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
        if (groundingMetadata != null) {
            val queriesArr = groundingMetadata.optJSONArray("webSearchQueries")
            if (queriesArr != null) {
                for (i in 0 until queriesArr.length()) {
                    val q = queriesArr.optString(i)
                    if (q.isNotBlank()) searchQueries.add(q)
                }
            }

            val chunks = groundingMetadata.optJSONArray("groundingChunks")
            if (chunks != null) {
                for (i in 0 until chunks.length()) {
                    val chunk = chunks.optJSONObject(i) ?: continue

                    val mapsObj = chunk.optJSONObject("maps")
                    if (mapsObj != null) {
                        val title = mapsObj.optString("title").trim()
                        val uri = mapsObj.optString("uri").ifBlank { mapsObj.optString("sourceUri") }.trim()
                        val placeId = mapsObj.optString("placeId").takeIf { it.isNotBlank() }
                        val parsedCoords = parseCoordinatesFromUri(uri)
                        if (title.isNotBlank()) {
                            mapPlaces.add(
                                FiriMapPlace(
                                    title = title,
                                    address = mapsObj.optString("address", ""),
                                    latitude = parsedCoords?.first,
                                    longitude = parsedCoords?.second,
                                    mapsUri = uri.ifBlank {
                                        "https://www.google.com/maps/search/?api=1&query=${Uri.encode(title)}"
                                    },
                                    placeId = placeId
                                )
                            )
                        }
                    }

                    val webObj = chunk.optJSONObject("web")
                    if (webObj != null) {
                        val title = webObj.optString("title").trim()
                        val uri = webObj.optString("uri").trim()
                        if (uri.isNotBlank()) {
                            val host = try {
                                URI(uri).host?.removePrefix("www.") ?: ""
                            } catch (_: Exception) {
                                ""
                            }
                            webSources.add(
                                FiriWebSource(
                                    title = title.ifBlank { host.ifBlank { "Google Search" } },
                                    uri = uri,
                                    domain = host
                                )
                            )
                        }
                    }
                }
            }
        }

        val distinctPlaces = mapPlaces.distinctBy { it.placeId ?: it.title.lowercase() }
        val distinctSources = webSources.distinctBy { it.uri }.take(5)

        return GeminiGroundedReply(
            text = cleanedText,
            mapPlaces = distinctPlaces,
            webSources = distinctSources,
            searchQueries = searchQueries.distinct(),
            usedGoogleMaps = distinctPlaces.isNotEmpty(),
            usedGoogleSearch = distinctSources.isNotEmpty() || searchQueries.isNotEmpty()
        )
    }

    private fun parseCoordinatesFromUri(uri: String): Pair<Double, Double>? {
        if (uri.isBlank()) return null
        val atRegex = Regex("""@(-?\d+\.\d+),(-?\d+\.\d+)""")
        atRegex.find(uri)?.let { m ->
            val lat = m.groupValues[1].toDoubleOrNull()
            val lng = m.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null) return lat to lng
        }
        val qRegex = Regex("""(?:q|ll|center)=(-?\d+\.\d+),(-?\d+\.\d+)""")
        qRegex.find(uri)?.let { m ->
            val lat = m.groupValues[1].toDoubleOrNull()
            val lng = m.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null) return lat to lng
        }
        return null
    }

    @Suppress("DEPRECATION")
    private suspend fun enrichMapPlaces(
        rawPlaces: List<FiriMapPlace>,
        explicitTags: List<String>,
        query: String,
        isMapQuery: Boolean,
        sqlContext: FiriSqlContextSnapshot?,
        isSpanish: Boolean
    ): List<FiriMapPlace> = withContext(Dispatchers.IO) {
        val combined = rawPlaces.toMutableList()

        // Agregar etiquetas [MAPA: ...] generadas por el modelo si no estaban ya en groundingChunks
        for (tag in explicitTags) {
            if (combined.none { it.title.equals(tag, ignoreCase = true) }) {
                combined.add(
                    FiriMapPlace(
                        title = tag.substringBefore(",").trim(),
                        address = tag,
                        mapsUri = "https://www.google.com/maps/search/?api=1&query=${Uri.encode(tag)}"
                    )
                )
            }
        }

        // Si fue una consulta explícita de mapa y aún no hay lugares, buscar en SQL o construir pin del término buscado
        if (combined.isEmpty() && isMapQuery) {
            val sqlMatch = sqlContext?.matchingPlaces?.firstOrNull()
            if (sqlMatch != null) {
                combined.add(
                    FiriMapPlace(
                        title = sqlMatch.title,
                        address = sqlMatch.address,
                        latitude = sqlMatch.latitude,
                        longitude = sqlMatch.longitude,
                        mapsUri = sqlMatch.mapsUri,
                        placeId = sqlMatch.placeId,
                        fromSqlCache = true
                    )
                )
            } else {
                val cleanedTerm = query.lowercase()
                    .replace(Regex("(dame|muestra|muéstrame|busca|buscar|abre|ver|un|el)?\\s*mapas?\\s*(de|del|para|hacia)?"), "")
                    .replace(Regex("(donde esta|dónde está|como llegar a|cómo llegar a)"), "")
                    .trim()
                    .ifBlank { query }
                combined.add(
                    FiriMapPlace(
                        title = cleanedTerm.replaceFirstChar { it.uppercase() },
                        address = if (isSpanish) "Ubicación en Google Maps" else "Location on Google Maps",
                        mapsUri = "https://www.google.com/maps/search/?api=1&query=${Uri.encode(cleanedTerm)}"
                    )
                )
            }
        }

        // Resolver coordenadas reales con Geocoder para que el mapa interactivo centre los marcadores
        val geocoder = try {
            Geocoder(context, Locale.getDefault())
        } catch (_: Exception) {
            null
        }

        combined.take(5).map { place ->
            if (place.latitude != null && place.longitude != null) {
                place
            } else {
                val searchName = place.address.ifBlank { place.title }
                val geo = try {
                    geocoder?.getFromLocationName(searchName, 1)?.firstOrNull()
                } catch (_: Exception) {
                    null
                }
                if (geo != null) {
                    val formattedAddr = (0..geo.maxAddressLineIndex).joinToString(", ") { geo.getAddressLine(it) }
                    place.copy(
                        address = place.address.ifBlank { formattedAddr },
                        latitude = geo.latitude,
                        longitude = geo.longitude
                    )
                } else {
                    place
                }
            }
        }
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
