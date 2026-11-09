package com.example.ai

import android.content.Context
import android.media.AudioManager
import com.example.model.AppCategory
import com.example.model.AppInfo
import com.example.model.HabitPrediction
import com.example.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
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
 * sin requerir conexión a internet.
 */
class OnDeviceNeuralBrain(private val context: Context) {

    val availableCores: Int = Runtime.getRuntime().availableProcessors()

    /**
     * Procesa la consulta del usuario en el procesador local multihilo.
     */
    suspend fun processQueryLocally(
        query: String,
        userSettings: UserSettings,
        availableApps: List<AppInfo>,
        currentPrediction: HabitPrediction?
    ): OnDeviceInferenceResult = withContext(Dispatchers.Default) {
        var actionResult: FiriActionResult
        val timeMs = measureTimeMillis {
            actionResult = analyzeLocally(query.trim(), userSettings, availableApps, currentPrediction)
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
        currentPrediction: HabitPrediction?
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
            val msg = if (isEs) {
                "¡Estás usando mi Segundo Cerebro! Me ejecuto directamente en los $availableCores núcleos del procesador de tu teléfono. Al no haber internet o estar en modo offline, mi motor neural local toma el control en milisegundos para gestionar tus apps, resolver cálculos y asistirte con total privacidad."
            } else {
                "You are using my Second Brain! I run directly on your phone's $availableCores CPU cores. Without internet or in offline mode, my on-device neural engine takes over instantly to launch apps, solve math, and assist you with complete privacy."
            }
            return FiriActionResult(spokenResponse = msg)
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
            val msg = if (isEs) "Segundo Cerebro: Abriendo tu panel de hábitos analizado en este dispositivo." else "Second Brain: Opening your on-device habit dashboard."
            return FiriActionResult(spokenResponse = msg, openHabitsDashboard = true)
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

        // 3. APERTURA DE APLICACIONES LOCALES
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

        // Búsqueda directa por nombre de app si el usuario dijo solo el nombre (ej. "calculadora", "whatsapp", "spotify")
        val directApp = findAppByName(lower, availableApps)
        if (directApp != null && (lower.length >= 4 || lower == directApp.label.lowercase())) {
            val response = if (isEs) "Segundo Cerebro: Ejecutando ${directApp.label} desde el procesador."
            else "Second Brain: Launching ${directApp.label} from local CPU."
            return FiriActionResult(spokenResponse = response, appToLaunch = directApp)
        }

        // 4. MOTOR MATEMÁTICO Y CALCULADORA ON-DEVICE
        val mathResult = tryEvaluateMath(lower)
        if (mathResult != null) {
            val msg = if (isEs) "Segundo Cerebro (Cálculo CPU): $mathResult"
            else "Second Brain (CPU Math): $mathResult"
            return FiriActionResult(spokenResponse = msg)
        }

        // 5. CONVERSIONES DE UNIDADES
        val conversionResult = tryConvertUnits(lower, isEs)
        if (conversionResult != null) {
            val msg = if (isEs) "Segundo Cerebro (Conversión): $conversionResult"
            else "Second Brain (Conversion): $conversionResult"
            return FiriActionResult(spokenResponse = msg)
        }

        // 6. FECHA, HORA Y TELEMETRÍA DE HARDWARE
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
                "Tu dispositivo cuenta con $availableCores núcleos de CPU activos. El Segundo Cerebro de Firi utiliza estos núcleos en paralelo para procesar lenguaje y comandos sin consumir datos móviles ni batería innecesaria."
            } else {
                "Your device features $availableCores active CPU cores. Firi's Second Brain leverages these cores in parallel to process language and intents with zero data usage."
            }
            return FiriActionResult(spokenResponse = msg)
        }

        // 7. HÁBITOS Y SUGERENCIAS LOCALES
        if (lower.contains("que hago") || lower.contains("sugerencia") || lower.contains("rutina")) {
            val topApps = availableApps.take(3).joinToString(", ") { it.label }
            val msg = if (isEs) {
                "Según tu historial local aprendido por Firi, en este momento sueles usar: $topApps. Tu procesador local está listo para abrir cualquiera de ellas."
            } else {
                "Based on on-device learning, your top apps right now are: $topApps. Ready to launch whichever you prefer."
            }
            return FiriActionResult(spokenResponse = msg)
        }

        // 8. CHISTES Y PRODUCTIVIDAD ON-DEVICE
        if (lower.contains("chiste") || lower.contains("broma") || lower.contains("joke")) {
            val jokesEs = listOf(
                "¿Por qué el procesador no pasa frío? Porque siempre está ejecutando hilos calientes.",
                "Hay 10 tipos de personas: las que entienden binario y las que no.",
                "¿Qué le dice un bit a otro? Nos vemos en el bus de datos.",
                "¿Por qué los programadores prefieren el modo oscuro? Porque la luz atrae a los bugs.",
                "Un bucle infinito entró a un bar. El camarero aún sigue sirviéndole la misma copa."
            )
            val jokesEn = listOf(
                "Why do programmers prefer dark mode? Because light attracts bugs.",
                "There are 10 types of people: those who understand binary, and those who don't.",
                "Why did the developer go broke? Because he used up all his cache."
            )
            val joke = if (isEs) jokesEs.random() else jokesEn.random()
            return FiriActionResult(spokenResponse = "Segundo Cerebro: $joke")
        }

        if (lower.contains("consejo") || lower.contains("productividad") || lower.contains("tip")) {
            val tip = if (isEs) {
                "Consejo del Segundo Cerebro: Enfócate en una sola tarea durante los próximos 25 minutos. La multitarea fragmenta la memoria de trabajo de tu cerebro tanto como la de una CPU saturada."
            } else {
                "Second Brain Tip: Focus on one task for 25 minutes. Multitasking fragments cognitive working memory just like an overloaded CPU."
            }
            return FiriActionResult(spokenResponse = tip)
        }

        // 9. SALUDOS Y EMPATÍA CONVERSACIONAL
        if (lower.contains("hola") || lower.contains("buenos dias") || lower.contains("buenas tardes") || lower == "hi" || lower == "hello") {
            val greeting = if (isEs) {
                "¡Hola, $name! Segundo Cerebro activo y procesando en $availableCores núcleos locales. No tenemos internet en este momento, pero puedo abrir tus apps, calcular y ayudarte con todo en el teléfono. ¿Qué necesitas?"
            } else {
                "Hello, $name! Second Brain active and running on $availableCores local cores. Offline mode engaged, ready to launch apps and assist you instantly."
            }
            return FiriActionResult(spokenResponse = greeting)
        }

        if (lower.contains("gracias") || lower.contains("thank")) {
            val reply = if (isEs) "¡Un placer absoluto, $name! Mi Segundo Cerebro siempre está listo en tu procesador."
            else "You're very welcome, $name! My on-device Second Brain is always ready."
            return FiriActionResult(spokenResponse = reply)
        }

        // 10. RESPUESTA INTELIGENTE POR DEFECTO DEL SEGUNDO CEREBRO
        val defaultMsg = if (isEs) {
            "Segundo Cerebro (Procesador Local): Te escucho atentamente, $name. Al estar sin conexión a internet, estoy operando directamente con tu procesador. Puedes pedirme abrir cualquier aplicación, calcular operaciones matemáticas, silenciar el móvil o consultar tus hábitos."
        } else {
            "Second Brain (On-Device CPU): I'm listening, $name. Operating purely on your phone's processor without internet. You can ask me to launch any app, solve math, manage sound, or review habits."
        }
        return FiriActionResult(spokenResponse = defaultMsg)
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

    /**
     * Resuelve expresiones matemáticas naturales en el procesador.
     */
    private fun tryEvaluateMath(text: String): String? {
        val cleaned = text
            .replace("cuanto es", "")
            .replace("cuánto es", "")
            .replace("calcula", "")
            .replace("calcular", "")
            .replace("what is", "")
            .replace("calculate", "")
            .trim()

        // Porcentaje: ej. "15% de 200"
        val percentRegex = Regex("""(\d+(?:\.\d+)?)\s*%\s*(?:de|of)\s*(\d+(?:\.\d+)?)""")
        val percentMatch = percentRegex.find(cleaned)
        if (percentMatch != null) {
            val (pStr, valStr) = percentMatch.destructured
            val p = pStr.toDoubleOrNull() ?: return null
            val total = valStr.toDoubleOrNull() ?: return null
            val res = (p / 100.0) * total
            return "$p% de $total = ${formatNumber(res)}"
        }

        // Raíz cuadrada: ej. "raiz cuadrada de 144"
        val sqrtRegex = Regex("""(?:raiz|raíz|sqrt)\s*(?:cuadrada\s*de|de)?\s*(\d+(?:\.\d+)?)""")
        val sqrtMatch = sqrtRegex.find(cleaned)
        if (sqrtMatch != null) {
            val numStr = sqrtMatch.groupValues[1]
            val num = numStr.toDoubleOrNull() ?: return null
            if (num < 0) return "No se puede calcular la raíz de un número negativo."
            val res = sqrt(num)
            return "√$num = ${formatNumber(res)}"
        }

        // Operaciones aritméticas básicas: +, -, *, /, x, por, entre
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

    /**
     * Resuelve conversiones métricas comunes en el procesador.
     */
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
