package com.example.ai

import android.content.Context
import android.location.Geocoder
import android.media.AudioManager
import android.net.Uri
import com.example.model.AppInfo
import com.example.model.HabitPrediction
import com.example.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis

/**
 * Resultado de la inferencia ejecutada por el Segundo Cerebro (Procesador Local On-Device).
 */
data class OnDeviceInferenceResult(
    val actionResult: FiriActionResult,
    val latencyMs: Long,
    val coresUsed: Int,
    val engineName: String = "Firi Neural CPU Core",
    val isOfflineExecution: Boolean = true
)

/**
 * Segundo Cerebro de Firi AI:
 * Motor de inteligencia artificial que corre 100% dentro del procesador del teléfono (CPU/NPU)
 * apoyado en la base de datos SQLite local (Room) cuando no hay internet.
 */
class OnDeviceNeuralBrain(private val context: Context) {

    val availableCores: Int = Runtime.getRuntime().availableProcessors()

    /**
     * Procesa la consulta del usuario en el procesador local multihilo con contexto SQL.
     */
    suspend fun processQueryLocally(
        query: String,
        userSettings: UserSettings,
        availableApps: List<AppInfo>,
        currentPrediction: HabitPrediction?,
        sqlContext: FiriSqlContextSnapshot? = null
    ): OnDeviceInferenceResult = withContext(Dispatchers.Default) {
        var actionResult: FiriActionResult
        val timeMs = measureTimeMillis {
            actionResult = analyzeLocally(
                query = query.trim(),
                userSettings = userSettings,
                availableApps = availableApps,
                currentPrediction = currentPrediction,
                sqlContext = sqlContext
            )
        }
        OnDeviceInferenceResult(
            actionResult = actionResult,
            latencyMs = timeMs.coerceAtLeast(4L),
            coresUsed = availableCores,
            engineName = "Firi Neural CPU ($availableCores núcleos)"
        )
    }

    private fun analyzeLocally(
        query: String,
        userSettings: UserSettings,
        availableApps: List<AppInfo>,
        currentPrediction: HabitPrediction?,
        sqlContext: FiriSqlContextSnapshot?
    ): FiriActionResult {
        val lower = query.lowercase().trim()
        val isEs = userSettings.isSpanish
        val name = userSettings.userName.ifBlank { if (isEs) "amigo" else "friend" }

        // 1. CONSULTA EXPLÍCITA SOBRE EL SEGUNDO CEREBRO / MODO SIN INTERNET
        if (lower.contains("segundo cerebro") || lower.contains("segundo procesador") ||
            lower.contains("sin internet") || lower.contains("modo offline") ||
            lower.contains("como funcionas sin internet") || lower.contains("cerebro local") ||
            lower.contains("second brain") || lower.contains("offline brain") || lower.contains("offline mode")
        ) {
            val sqlCount = sqlContext?.let { it.totalMemoriesCount + it.totalSavedPlacesCount } ?: 0
            val msg = if (isEs) {
                "¡Estás usando mi Segundo Cerebro! Me ejecuto directamente en los $availableCores núcleos de tu procesador junto a tu base SQLite local ($sqlCount registros SQL guardados). Incluso sin internet puedo abrir apps, consultar tus mapas guardados, leer tus notas y resolver cálculos al instante."
            } else {
                "You are using my Second Brain! I run directly on your phone's $availableCores CPU cores paired with your local SQLite database ($sqlCount SQL records stored). Even offline, I can launch apps, recall saved maps, read your notes, and solve math instantly."
            }
            return FiriActionResult(spokenResponse = msg, usedSqlData = sqlCount > 0)
        }

        // 2. CONTROL DEL SISTEMA Y LAUNCHER
        if (lower.contains("personalizar pantalla de bloqueo") || lower.contains("editar pantalla de bloqueo") ||
            lower.contains("personalizar bloqueo") || lower.contains("editar bloqueo") ||
            (lower.contains("pantalla de bloqueo") && (lower.contains("no funciona") || lower.contains("editar") || lower.contains("cambiar") || lower.contains("configurar"))) ||
            lower == "customize lock screen" || lower == "edit lock screen"
        ) {
            val msg = if (isEs) "Segundo Cerebro (CPU): Abriendo el editor de pantalla de bloqueo con vista previa en vivo."
            else "Second Brain (CPU): Opening lock screen live editor."
            return FiriActionResult(spokenResponse = msg, openLockCustomizer = true)
        }

        if (lower == "bloquea la pantalla" || lower == "bloquear pantalla" || lower == "apaga la pantalla" ||
            lower == "bloquear" || lower == "lock" || lower == "lock screen"
        ) {
            val msg = if (isEs) "Segundo Cerebro: Bloqueando la pantalla del dispositivo." else "Second Brain: Locking device screen."
            return FiriActionResult(spokenResponse = msg, openLockScreen = true)
        }

        if (lower == "ajustes" || lower == "configuracion" || lower == "configuración" ||
            lower == "abrir ajustes" || lower == "settings" || lower == "open settings"
        ) {
            val msg = if (isEs) "Segundo Cerebro: Abriendo la configuración de Firi Launcher." else "Second Brain: Opening Firi Launcher settings."
            return FiriActionResult(spokenResponse = msg, openSettings = true)
        }

        if (lower == "centro de control" || lower == "panel de control" || lower == "control center" || lower == "abrir centro de control") {
            val msg = if (isEs) "Segundo Cerebro: Desplegando el Centro de Control." else "Second Brain: Opening Control Center."
            return FiriActionResult(spokenResponse = msg, openControlCenter = true)
        }

        if (lower == "habitos" || lower == "hábitos" || lower == "mis habitos" || lower == "rutinas" || lower == "habits") {
            val msg = if (isEs) "Segundo Cerebro: Abriendo tu panel de hábitos analizado en SQL." else "Second Brain: Opening your SQL-analyzed habit dashboard."
            return FiriActionResult(spokenResponse = msg, openHabitsDashboard = true, usedSqlData = true)
        }

        // Comandos de audio y volumen
        if (lower.contains("sube el volumen") || lower.contains("subir volumen") || lower.contains("aumenta el volumen") || lower == "volume up") {
            adjustVolume(increase = true)
            val msg = if (isEs) "Segundo Cerebro: Volumen aumentado con la barra Fusion OS." else "Second Brain: Volume increased with Fusion OS bar."
            return FiriActionResult(spokenResponse = msg, triggerVolumeOverlay = true)
        }

        if (lower.contains("baja el volumen") || lower.contains("bajar volumen") || lower.contains("disminuye el volumen") || lower == "volume down") {
            adjustVolume(increase = false)
            val msg = if (isEs) "Segundo Cerebro: Volumen reducido." else "Second Brain: Volume decreased."
            return FiriActionResult(spokenResponse = msg, triggerVolumeOverlay = true)
        }

        if (lower.contains("silencia") || lower.contains("mutear") || lower == "mute" || lower == "silencio") {
            muteVolume()
            val msg = if (isEs) "Segundo Cerebro: Dispositivo silenciado." else "Second Brain: Device muted."
            return FiriActionResult(spokenResponse = msg, triggerVolumeOverlay = true)
        }

        if (lower.contains("barra de volumen") || lower.contains("muestra el volumen") || lower == "volumen" || lower == "volume") {
            val msg = if (isEs) "Segundo Cerebro: Mostrando barra de volumen global." else "Second Brain: Displaying global volume overlay."
            return FiriActionResult(spokenResponse = msg, triggerVolumeOverlay = true)
        }

        // 3. CONSULTAS DE DATOS SQL (Memoria, Notas, Hábitos y Lugares guardados)
        if (lower.contains("datos sql") || lower.contains("mis datos") || lower.contains("base de datos") ||
            lower.contains("que recuerdas") || lower.contains("qué recuerdas") || lower.contains("mis recuerdos") ||
            lower.contains("sql") || lower.contains("my data")
        ) {
            val places = sqlContext?.recentPlaces?.map {
                FiriMapPlace(
                    title = it.title,
                    address = it.address,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    mapsUri = it.mapsUri,
                    placeId = it.placeId,
                    category = it.category,
                    fromSqlCache = true
                )
            } ?: emptyList()

            val msg = buildString {
                if (isEs) {
                    append("Consulté tu base de datos SQL local: tienes ${sqlContext?.totalMemoriesCount ?: 0} recuerdos y ${sqlContext?.totalSavedPlacesCount ?: 0} mapas guardados.")
                    if (!sqlContext?.topAppsSummary.isNullOrBlank()) {
                        append(" Tus apps más usadas son ${sqlContext?.topAppsSummary}.")
                    }
                    if (!sqlContext?.notesSummary.isNullOrBlank()) {
                        append(" Tus notas activas dicen: ${sqlContext?.notesSummary}.")
                    }
                } else {
                    append("Checked your local SQL database: ${sqlContext?.totalMemoriesCount ?: 0} memories and ${sqlContext?.totalSavedPlacesCount ?: 0} saved maps stored.")
                    if (!sqlContext?.topAppsSummary.isNullOrBlank()) {
                        append(" Most used apps: ${sqlContext?.topAppsSummary}.")
                    }
                    if (!sqlContext?.notesSummary.isNullOrBlank()) {
                        append(" Active notes: ${sqlContext?.notesSummary}.")
                    }
                }
            }
            return FiriActionResult(
                spokenResponse = msg,
                mapPlaces = places.take(4),
                sqlInsightsSummary = "SQLite Room · ${sqlContext?.totalMemoriesCount ?: 0} recuerdos · ${sqlContext?.totalSavedPlacesCount ?: 0} mapas",
                usedSqlData = true,
                usedGoogleMaps = places.isNotEmpty()
            )
        }

        if (lower.contains("mis notas") || lower.contains("mis tareas") || lower.contains("notas") || lower.contains("recordatorios") || lower == "my notes") {
            val notes = sqlContext?.notesSummary.orEmpty()
            val msg = if (notes.isNotBlank()) {
                if (isEs) "Según tus datos SQL, tus notas actuales son: $notes."
                else "According to your SQL data, your current notes are: $notes."
            } else {
                if (isEs) "Revisé la tabla SQL de widgets y aún no tienes notas guardadas."
                else "Checked your SQL widget table and you don't have saved notes yet."
            }
            return FiriActionResult(
                spokenResponse = msg,
                sqlInsightsSummary = "Tabla launcher_widgets (SQL)",
                usedSqlData = true
            )
        }

        // 4. BÚSQUEDA DE MAPAS EN SQL / GEOCODER LOCAL (Offline o respaldo)
        val isMapIntent = lower.contains("mapa") || lower.contains("map") ||
            lower.contains("donde esta") || lower.contains("dónde está") ||
            lower.contains("ubicacion") || lower.contains("ubicación") ||
            lower.contains("como llegar") || lower.contains("cómo llegar") ||
            lower.contains("lugares guardados") || lower.contains("saved places")
        if (isMapIntent) {
            val cachedPlaces = (sqlContext?.matchingPlaces ?: emptyList())
                .ifEmpty { sqlContext?.recentPlaces ?: emptyList() }
                .map {
                    FiriMapPlace(
                        title = it.title,
                        address = it.address,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        mapsUri = it.mapsUri,
                        placeId = it.placeId,
                        category = it.category,
                        fromSqlCache = true
                    )
                }

            val cleanTarget = extractPlaceSearchTerm(query)
            val resolvedPlace = if (cachedPlaces.isNotEmpty() && (lower.contains("guardad") || lower.contains("saved"))) {
                cachedPlaces
            } else {
                val matchingCached = cachedPlaces.filter {
                    cleanTarget.isNotBlank() && (
                        it.title.lowercase().contains(cleanTarget.lowercase()) ||
                            it.address.lowercase().contains(cleanTarget.lowercase())
                        )
                }
                if (matchingCached.isNotEmpty()) {
                    matchingCached
                } else {
                    val fallbackQuery = cleanTarget.ifBlank { query }
                    val geo = tryGeocodeOffline(fallbackQuery)
                    listOf(
                        geo ?: FiriMapPlace(
                            title = fallbackQuery.replaceFirstChar { it.uppercase() },
                            address = if (isEs) "Búsqueda directa en Google Maps" else "Direct search in Google Maps",
                            mapsUri = "https://www.google.com/maps/search/?api=1&query=${Uri.encode(fallbackQuery)}"
                        )
                    )
                }
            }

            val firstTitle = resolvedPlace.firstOrNull()?.title ?: query
            val msg = if (isEs) {
                if (resolvedPlace.any { it.fromSqlCache }) {
                    "Encontré $firstTitle en tu historial de mapas SQL. Aquí tienes la tarjeta del mapa para abrir la ruta o explorar ubicaciones guardadas."
                } else {
                    "Preparé el mapa de $firstTitle en modo local. Toca la tarjeta para ver la vista previa o abrir navegación directa en Google Maps."
                }
            } else {
                "Prepared the map card for $firstTitle locally. Tap the card to preview or open direct navigation in Google Maps."
            }

            return FiriActionResult(
                spokenResponse = msg,
                mapPlaces = resolvedPlace,
                sqlInsightsSummary = if (resolvedPlace.any { it.fromSqlCache }) "Caché SQL de lugares" else null,
                usedGoogleMaps = true,
                usedSqlData = resolvedPlace.any { it.fromSqlCache }
            )
        }

        // 5. APERTURA DE APLICACIONES LOCALES
        val openPrefixes = if (isEs) listOf("abre ", "abrir ", "inicia ", "iniciar ", "lanza ", "ejecuta ", "pon ", "entra a ", "ir a ")
        else listOf("open ", "launch ", "start ", "run ", "play ", "go to ")

        for (prefix in openPrefixes) {
            if (lower.startsWith(prefix)) {
                val targetAppName = lower.removePrefix(prefix).trim()
                if (targetAppName.isNotEmpty()) {
                    val matchingApp = findAppByName(targetAppName, availableApps)
                    if (matchingApp != null) {
                        val response = if (isEs) "Segundo Cerebro: Abriendo ${matchingApp.label} al instante."
                        else "Second Brain: Launching ${matchingApp.label} instantly."
                        return FiriActionResult(spokenResponse = response, appToLaunch = matchingApp)
                    }
                }
            }
        }

        val directApp = findAppByName(lower, availableApps)
        if (directApp != null && (lower.length >= 4 || lower == directApp.label.lowercase())) {
            val response = if (isEs) "Segundo Cerebro: Ejecutando ${directApp.label} desde el procesador."
            else "Second Brain: Launching ${directApp.label} from local CPU."
            return FiriActionResult(spokenResponse = response, appToLaunch = directApp)
        }

        // 6. RECUERDOS SQL COINCIDENTES CON LA CONSULTA
        val matchedMemory = sqlContext?.matchingMemories?.firstOrNull()
        if (matchedMemory != null) {
            val msg = if (isEs) {
                "Desde tu memoria SQL local recuerdo esto sobre '${matchedMemory.userQuery}': ${matchedMemory.summaryResponse}"
            } else {
                "From local SQL memory regarding '${matchedMemory.userQuery}': ${matchedMemory.summaryResponse}"
            }
            return FiriActionResult(
                spokenResponse = msg,
                sqlInsightsSummary = "Memoria SQL (${matchedMemory.hitCount}x)",
                usedSqlData = true
            )
        }

        // 7. MOTOR MATEMÁTICO Y CALCULADORA ON-DEVICE
        val mathResult = tryEvaluateMath(lower)
        if (mathResult != null) {
            val msg = if (isEs) "Segundo Cerebro (Cálculo CPU): $mathResult"
            else "Second Brain (CPU Math): $mathResult"
            return FiriActionResult(spokenResponse = msg)
        }

        // 8. CONVERSIONES DE UNIDADES
        val conversionResult = tryConvertUnits(lower, isEs)
        if (conversionResult != null) {
            val msg = if (isEs) "Segundo Cerebro (Conversión): $conversionResult"
            else "Second Brain (Conversion): $conversionResult"
            return FiriActionResult(spokenResponse = msg)
        }

        // 9. FECHA, HORA Y TELEMETRÍA DE HARDWARE
        val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        if (lower.contains("hora") || lower.contains("qué hora") || lower == "time") {
            val msg = if (isEs) "Segundo Cerebro: Son las $timeNow." else "Second Brain: It is currently $timeNow."
            return FiriActionResult(spokenResponse = msg)
        }

        if (lower.contains("fecha") || lower.contains("que dia es") || lower.contains("qué día es") || lower == "date") {
            val dateFormatted = SimpleDateFormat("EEEE, d 'de' MMMM", if (isEs) Locale.forLanguageTag("es-ES") else Locale.US).format(Date())
            val msg = if (isEs) "Segundo Cerebro: Hoy es $dateFormatted." else "Second Brain: Today is $dateFormatted."
            return FiriActionResult(spokenResponse = msg)
        }

        if (lower.contains("procesador") || lower.contains("cpu") || lower.contains("hardware") || lower.contains("nucleos") || lower.contains("núcleos")) {
            val msg = if (isEs) {
                "Tu dispositivo cuenta con $availableCores núcleos de CPU activos. El Segundo Cerebro de Firi utiliza estos núcleos junto con SQLite para responder al instante."
            } else {
                "Your device features $availableCores active CPU cores. Firi's Second Brain pairs these cores with SQLite for instant responses."
            }
            return FiriActionResult(spokenResponse = msg)
        }

        // 10. HÁBITOS Y SUGERENCIAS LOCALES DESDE SQL
        if (lower.contains("que hago") || lower.contains("sugerencia") || lower.contains("rutina") || lower.contains("apps uso")) {
            val topSql = sqlContext?.topAppsSummary?.takeIf { it.isNotBlank() }
                ?: availableApps.take(3).joinToString(", ") { it.label }
            val msg = if (isEs) {
                "Según tu base SQL de hábitos, tus aplicaciones principales son: $topSql. ¿Cuál te gustaría abrir?"
            } else {
                "Based on your SQL habit database, your top apps are: $topSql. Which one would you like to open?"
            }
            return FiriActionResult(
                spokenResponse = msg,
                sqlInsightsSummary = "Hábitos SQL",
                usedSqlData = true
            )
        }

        // 11. CHISTES Y PRODUCTIVIDAD ON-DEVICE
        if (lower.contains("chiste") || lower.contains("broma") || lower.contains("joke")) {
            val jokesEs = listOf(
                "¿Por qué el procesador no pasa frío? Porque siempre está ejecutando hilos calientes.",
                "Hay 10 tipos de personas: las que entienden binario y las que no.",
                "¿Qué le dice un bit a otro? Nos vemos en el bus de datos.",
                "¿Por qué los programadores prefieren el modo oscuro? Porque la luz atrae a los bugs.",
                "Un SELECT * sin WHERE entró a una base SQL... y se llevó toda la mesa."
            )
            val jokesEn = listOf(
                "Why do programmers prefer dark mode? Because light attracts bugs.",
                "There are 10 types of people: those who understand binary, and those who don't.",
                "A SQL query walks into a bar, walks up to two tables and asks: Can I join you?"
            )
            val joke = if (isEs) jokesEs.random() else jokesEn.random()
            return FiriActionResult(spokenResponse = "Segundo Cerebro: $joke")
        }

        if (lower.contains("consejo") || lower.contains("productividad") || lower.contains("tip")) {
            val tip = if (isEs) {
                "Consejo del Segundo Cerebro: Enfócate en una sola tarea durante los próximos 25 minutos. La multitarea fragmenta tu memoria de trabajo tanto como una CPU saturada."
            } else {
                "Second Brain Tip: Focus on one task for 25 minutes. Multitasking fragments working memory just like an overloaded CPU."
            }
            return FiriActionResult(spokenResponse = tip)
        }

        // 12. SALUDOS Y EMPATÍA CONVERSACIONAL
        if (lower.contains("hola") || lower.contains("buenos dias") || lower.contains("buenas tardes") || lower == "hi" || lower == "hello") {
            val greeting = if (isEs) {
                "¡Hola, $name! Segundo Cerebro activo en $availableCores núcleos con ${sqlContext?.totalSavedPlacesCount ?: 0} mapas y ${sqlContext?.totalMemoriesCount ?: 0} recuerdos SQL. Sin internet puedo abrir apps, consultar tus notas y mostrar mapas guardados."
            } else {
                "Hello, $name! Second Brain active on $availableCores cores with ${sqlContext?.totalSavedPlacesCount ?: 0} maps and ${sqlContext?.totalMemoriesCount ?: 0} SQL memories. Ready to launch apps, check notes, and show saved maps."
            }
            return FiriActionResult(spokenResponse = greeting, usedSqlData = sqlContext?.hasRichData == true)
        }

        if (lower.contains("gracias") || lower.contains("thank")) {
            val reply = if (isEs) "¡Un placer absoluto, $name! Mi Segundo Cerebro y memoria SQL siempre están listos."
            else "You're very welcome, $name! My on-device Second Brain and SQL memory are always ready."
            return FiriActionResult(spokenResponse = reply)
        }

        // 13. RESPUESTA INTELIGENTE POR DEFECTO DEL SEGUNDO CEREBRO
        val defaultMsg = if (isEs) {
            "Segundo Cerebro (Procesador Local): Operando sin conexión con tus $availableCores núcleos y base SQL. Pídeme abrir apps, consultar 'mis datos SQL', calcular operaciones o buscar un mapa guardado."
        } else {
            "Second Brain (On-Device CPU): Operating offline with $availableCores cores and local SQLite. Ask me to launch apps, check 'my SQL data', do math, or open a saved map."
        }
        return FiriActionResult(spokenResponse = defaultMsg)
    }

    private fun extractPlaceSearchTerm(rawQuery: String): String {
        return rawQuery.lowercase()
            .replace(Regex("(dame|muestra|muéstrame|busca|buscar|abre|ver|el|un)?\\s*mapa\\s*(de|del|para|hacia)?"), "")
            .replace(Regex("(donde esta|dónde está|como llegar a|cómo llegar a|ubicacion de|ubicación de)"), "")
            .replace(Regex("(show me a|find a|map of|where is|directions to)"), "")
            .trim()
    }

    @Suppress("DEPRECATION")
    private fun tryGeocodeOffline(placeQuery: String): FiriMapPlace? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = geocoder.getFromLocationName(placeQuery, 1)
            val addr = results?.firstOrNull() ?: return null
            val fullAddress = (0..addr.maxAddressLineIndex).joinToString(", ") { addr.getAddressLine(it) }
            FiriMapPlace(
                title = addr.featureName ?: placeQuery.replaceFirstChar { it.uppercase() },
                address = fullAddress,
                latitude = addr.latitude,
                longitude = addr.longitude,
                mapsUri = "https://www.google.com/maps/search/?api=1&query=${addr.latitude},${addr.longitude}"
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun findAppByName(query: String, apps: List<AppInfo>): AppInfo? {
        val q = query.lowercase().trim()
        return apps.firstOrNull { it.label.lowercase() == q }
            ?: apps.firstOrNull { it.label.lowercase().contains(q) }
            ?: apps.firstOrNull { it.packageName.lowercase().contains(q) }
    }

    private fun adjustVolume(increase: Boolean) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            am?.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        } catch (_: Exception) {}
    }

    private fun muteVolume() {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        } catch (_: Exception) {}
    }

    private fun tryEvaluateMath(text: String): String? {
        val cleaned = text
            .replace("cuanto es", "")
            .replace("cuánto es", "")
            .replace("calcula", "")
            .replace("calcular", "")
            .replace("what is", "")
            .replace("calculate", "")
            .trim()

        val percentRegex = Regex("""(\d+(?:\.\d+)?)\s*%\s*(?:de|of)\s*(\d+(?:\.\d+)?)""")
        val percentMatch = percentRegex.find(cleaned)
        if (percentMatch != null) {
            val (pStr, valStr) = percentMatch.destructured
            val p = pStr.toDoubleOrNull() ?: return null
            val total = valStr.toDoubleOrNull() ?: return null
            val res = (p / 100.0) * total
            return "$p% de $total = ${formatNumber(res)}"
        }

        val sqrtRegex = Regex("""(?:raiz|raíz|sqrt)\s*(?:cuadrada\s*de|de)?\s*(\d+(?:\.\d+)?)""")
        val sqrtMatch = sqrtRegex.find(cleaned)
        if (sqrtMatch != null) {
            val numStr = sqrtMatch.groupValues[1]
            val num = numStr.toDoubleOrNull() ?: return null
            if (num < 0) return "No se puede calcular la raíz de un número negativo."
            val res = sqrt(num)
            return "√$num = ${formatNumber(res)}"
        }

        val opPattern = Regex("""(-?\d+(?:\.\d+)?)\s*(\+|\-|\*|\/|x|por|entre|dividido|times|plus|minus)\s*(-?\d+(?:\.\d+)?)""")
        val opMatch = opPattern.find(cleaned)
        if (opMatch != null) {
            val (aStr, opStr, bStr) = opMatch.destructured
            val a = aStr.toDoubleOrNull() ?: return null
            val b = bStr.toDoubleOrNull() ?: return null
            val (symbol, result) = when (opStr) {
                "+", "plus" -> Pair("+", a + b)
                "-", "minus" -> Pair("-", a - b)
                "*", "x", "por", "times" -> Pair("×", a * b)
                "/", "entre", "dividido" -> {
                    if (b == 0.0) return "Indeterminación: no se puede dividir entre cero."
                    Pair("÷", a / b)
                }
                else -> return null
            }
            return "$a $symbol $b = ${formatNumber(result)}"
        }

        return null
    }

    private fun tryConvertUnits(text: String, isEs: Boolean): String? {
        val kmMilesRegex = Regex("""(\d+(?:\.\d+)?)\s*(km|kilometros|kilómetros|millas|miles)""")
        val kmMilesMatch = kmMilesRegex.find(text)
        if (kmMilesMatch != null && (text.contains("son") || text.contains("equivale") || text.contains("convert") || text.contains("en"))) {
            val (valStr, unit) = kmMilesMatch.destructured
            val v = valStr.toDoubleOrNull() ?: return null
            return if (unit.startsWith("m")) {
                val km = v * 1.60934
                if (isEs) "$v millas equivalen a ${formatNumber(km)} kilómetros." else "$v miles equals ${formatNumber(km)} km."
            } else {
                val miles = v / 1.60934
                if (isEs) "$v kilómetros equivalen a ${formatNumber(miles)} millas." else "$v km equals ${formatNumber(miles)} miles."
            }
        }

        val tempRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:grados)?\s*(celsius|centigrados|centígrados|fahrenheit)""")
        val tempMatch = tempRegex.find(text)
        if (tempMatch != null && (text.contains("son") || text.contains("equivale") || text.contains("convert") || text.contains("en"))) {
            val (valStr, unit) = tempMatch.destructured
            val v = valStr.toDoubleOrNull() ?: return null
            return if (unit.startsWith("c")) {
                val f = (v * 9.0 / 5.0) + 32.0
                if (isEs) "$v°C equivalen a ${formatNumber(f)}°F." else "$v°C equals ${formatNumber(f)}°F."
            } else {
                val c = (v - 32.0) * 5.0 / 9.0
                if (isEs) "$v°F equivalen a ${formatNumber(c)}°C." else "$v°F equals ${formatNumber(c)}°C."
            }
        }

        return null
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }
}
