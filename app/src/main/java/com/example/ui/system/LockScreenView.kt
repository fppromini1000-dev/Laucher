package com.example.ui.system

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.ui.common.WallpaperBackground
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.UserSettings
import com.example.system.SystemControlManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun LockScreenView(
    systemControlManager: SystemControlManager,
    userSettings: UserSettings,
    onUnlock: () -> Unit,
    onOpenCustomizer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isEs = userSettings.isSpanish
    val coroutineScope = rememberCoroutineScope()

    // Fecha y hora
    var currentTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    val locale = if (isEs) {
        when (userSettings.regionCode) {
            "MX" -> Locale.forLanguageTag("es-MX")
            "CO" -> Locale.forLanguageTag("es-CO")
            "AR" -> Locale.forLanguageTag("es-AR")
            else -> Locale.forLanguageTag("es-ES")
        }
    } else {
        if (userSettings.regionCode == "GB") Locale.UK else Locale.US
    }

    val timeFormat = SimpleDateFormat("HH:mm", locale)
    val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM", locale)

    // Desplazamiento vertical para desbloquear
    val offsetY = remember { Animatable(0f) }
    var isPinKeypadOpen by remember { mutableStateOf(false) }
    var isPatternDrawOpen by remember { mutableStateOf(false) }

    // Pulso animado de "Desliza para desbloquear"
    val infiniteTransition = rememberInfiniteTransition(label = "unlock_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Color y tipografía del reloj personalizados
    val clockColor = remember(userSettings.lockClockColor) {
        try {
            Color(android.graphics.Color.parseColor(userSettings.lockClockColor))
        } catch (_: Exception) {
            Color.White
        }
    }

    val (clockFontFamily, clockFontWeight) = remember(userSettings.lockClockFont) {
        when (userSettings.lockClockFont) {
            "SERIF" -> Pair(FontFamily.Serif, FontWeight.Normal)
            "TECH" -> Pair(FontFamily.Monospace, FontWeight.SemiBold)
            "ROUNDED" -> Pair(FontFamily.Default, FontWeight.Bold)
            "THIN" -> Pair(FontFamily.Default, FontWeight.Light)
            else -> Pair(FontFamily.Default, FontWeight.ExtraBold) // BOLD
        }
    }

    var pendingAuthAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var authDialogTitle by remember { mutableStateOf<String?>(null) }

    fun requestProtectedAction(actionTitle: String, action: () -> Unit) {
        when (userSettings.lockSecurityType) {
            "PIN" -> {
                coroutineScope.launch { offsetY.animateTo(0f, tween(150)) }
                authDialogTitle = actionTitle
                pendingAuthAction = action
                isPinKeypadOpen = true
            }
            "PATTERN" -> {
                coroutineScope.launch { offsetY.animateTo(0f, tween(150)) }
                authDialogTitle = actionTitle
                pendingAuthAction = action
                isPatternDrawOpen = true
            }
            else -> {
                action()
            }
        }
    }

    val onOpenCustomizerProtected = {
        val title = if (isEs) "Introduce tu método de seguridad para editar" else "Enter your security code to edit"
        requestProtectedAction(title) {
            onOpenCustomizer()
        }
    }

    fun initiateUnlock() {
        when (userSettings.lockSecurityType) {
            "PIN" -> {
                coroutineScope.launch {
                    offsetY.animateTo(0f, tween(150))
                }
                authDialogTitle = if (isEs) "Introduce el PIN para desbloquear" else "Enter PIN to unlock"
                pendingAuthAction = { onUnlock() }
                isPinKeypadOpen = true
            }
            "PATTERN" -> {
                coroutineScope.launch {
                    offsetY.animateTo(0f, tween(150))
                }
                authDialogTitle = if (isEs) "Dibuja el patrón para desbloquear" else "Draw pattern to unlock"
                pendingAuthAction = { onUnlock() }
                isPatternDrawOpen = true
            }
            else -> {
                coroutineScope.launch {
                    offsetY.animateTo(-2000f, tween(250, easing = FastOutSlowInEasing))
                    onUnlock()
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .draggable(
                state = rememberDraggableState { delta ->
                    coroutineScope.launch {
                        val newOffset = (offsetY.value + delta).coerceAtMost(0f)
                        offsetY.snapTo(newOffset)
                    }
                },
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    coroutineScope.launch {
                        if (offsetY.value < -240f || velocity < -1000f) {
                            initiateUnlock()
                        } else {
                            offsetY.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
                        }
                    }
                }
            )
            .testTag("lockscreen_container")
    ) {
        val screenHeight = maxHeight

        // Fondo de pantalla elegido (Predefinido o Fotografía del Usuario) con gesto de pulsación prolongada estilo iOS 16
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            onOpenCustomizerProtected()
                        }
                    )
                }
        ) {
            WallpaperBackground(
                wallpaperId = userSettings.lockWallpaperId,
                customWallpaperUri = userSettings.lockCustomWallpaperUri,
                scrimBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.32f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.52f)
                    )
                )
            )
        }

        // Contenido Principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ==========================================
            // SECCIÓN SUPERIOR: Candado, Fecha y Reloj
            // ==========================================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                // Barra superior con candado y botón de Personalizar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp)) // Espaciador

                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Pantalla bloqueada",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )

                    // Botón Personalizar estilo iOS 16
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .clickable { onOpenCustomizerProtected() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("lockscreen_customize_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isEs) "Editar" else "Edit", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fecha
                Text(
                    text = dateFormat.format(currentTime).replaceFirstChar { it.uppercase() },
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.95f),
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 2f), blurRadius = 6f)
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Reloj con estilo y color personalizados
                Text(
                    text = timeFormat.format(currentTime),
                    style = TextStyle(
                        fontSize = if (screenHeight < 680.dp) 72.sp else 86.sp,
                        fontWeight = clockFontWeight,
                        fontFamily = clockFontFamily,
                        letterSpacing = (-2).sp,
                        color = clockColor,
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(0f, 4f), blurRadius = 10f)
                    ),
                    modifier = Modifier
                        .clickable { onOpenCustomizerProtected() }
                        .testTag("lockscreen_big_clock")
                )

                // ==========================================
                // BANDEJA DE MINI WIDGETS BAJO EL RELOJ
                // ==========================================
                if (userSettings.lockMiniWidgets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .clickable { onOpenCustomizerProtected() }
                            .testTag("lockscreen_mini_widgets_row")
                    ) {
                        items(userSettings.lockMiniWidgets) { widgetKey ->
                            LockMiniWidgetPill(widgetKey = widgetKey, isEs = isEs)
                        }
                    }
                }
            }

            // ==========================================
            // SECCIÓN INFERIOR: Linterna, Cámara & Desbloqueo
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp, start = 36.dp, end = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Linterna y Cámara
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (systemControlManager.isTorchOn) Color.White else Color.Black.copy(alpha = 0.45f))
                            .border(1.dp, Color.White.copy(alpha = if (systemControlManager.isTorchOn) 0.8f else 0.25f), CircleShape)
                            .clickable { systemControlManager.toggleTorch() }
                            .testTag("lockscreen_flashlight_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (systemControlManager.isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Linterna",
                            tint = if (systemControlManager.isTorchOn) Color.Black else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                            .clickable { systemControlManager.openCamera() }
                            .testTag("lockscreen_camera_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Cámara",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Texto "Desliza hacia arriba para desbloquear"
                Text(
                    text = when (userSettings.lockSecurityType) {
                        "PIN" -> if (isEs) "Desliza para introducir PIN" else "Swipe for PIN"
                        "PATTERN" -> if (isEs) "Desliza para trazar patrón" else "Swipe for Pattern"
                        else -> if (isEs) "Desliza hacia arriba para desbloquear" else "Swipe up to unlock"
                    },
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = pulseAlpha),
                    modifier = Modifier
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            initiateUnlock()
                        }
                        .testTag("unlock_text_hint")
                )
            }
        }

        // Overlay de Seguridad con Teclado PIN
        if (isPinKeypadOpen) {
            PinKeypadView(
                correctPin = userSettings.lockPinCode.ifEmpty { "1234" },
                title = authDialogTitle,
                onSuccess = {
                    isPinKeypadOpen = false
                    val action = pendingAuthAction
                    pendingAuthAction = null
                    authDialogTitle = null
                    if (action != null) {
                        action()
                    } else {
                        onUnlock()
                    }
                },
                onCancel = {
                    isPinKeypadOpen = false
                    pendingAuthAction = null
                    authDialogTitle = null
                }
            )
        }

        // Overlay de Seguridad con Patrón de Puntos
        if (isPatternDrawOpen) {
            PatternDrawView(
                correctPattern = userSettings.lockPatternCode.ifEmpty { "0,1,2,5" },
                title = authDialogTitle,
                onSuccess = {
                    isPatternDrawOpen = false
                    val action = pendingAuthAction
                    pendingAuthAction = null
                    authDialogTitle = null
                    if (action != null) {
                        action()
                    } else {
                        onUnlock()
                    }
                },
                onCancel = {
                    isPatternDrawOpen = false
                    pendingAuthAction = null
                    authDialogTitle = null
                }
            )
        }
    }
}

/**
 * Píldora individual de Mini Widget bajo el reloj estilo iOS 16.
 */
@Composable
private fun LockMiniWidgetPill(
    widgetKey: String,
    isEs: Boolean
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (widgetKey) {
                "WEATHER_MAIN" -> {
                    Icon(imageVector = Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFFACC15), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "22° Mayormente nublado", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                "BATTERY" -> {
                    Icon(imageVector = Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = "85%", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                "CALENDAR" -> {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = if (isEs) "14:00 Reunión" else "14:00 Meeting", fontSize = 11.5.sp, color = Color.White)
                }
                "STEPS" -> {
                    Icon(imageVector = Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = Color(0xFFFB923C), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = "7,436", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                "ALARM" -> {
                    Icon(imageVector = Icons.Default.Alarm, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = "07:00", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}
