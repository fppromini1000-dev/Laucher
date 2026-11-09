package com.example.ui.onboarding

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla de animación cinemática previa a la pre-configuración (Onboarding).
 *
 * Secuencia fluida y orgánica inspirada en el arranque de Fusion OS:
 * 1. Intro: Destello estelar geométrico (estrella de 4 puntas) con resplandor sutil y "Fusion estudio".
 * 2. Expansión metálica: Crecimiento y reflejos de cromo/platino con destellos de lente.
 * 3. Transición armónica: El destello se expande en un arco de luz luminiscente en trayectoria curva.
 * 4. Revelación: "hello" en caligrafía cursiva pura y luminosa (sin fondo ni artefactos),
 *    seguido de "Fusion OS" y el botón píldora interactivo con flecha para avanzar.
 */
@Composable
fun FusionBootAnimationScreen(
    onComplete: () -> Unit
) {
    // Manejo de retroceso para salir o saltar con gracia
    BackHandler {
        onComplete()
    }

    // Fases de animación para coordinar estados
    // -1: Intro de código cibernético de arranque de Fusion OS
    // 0: Intro con "Fusion estudio" y estrella
    // 1: Expansión metálica con reflejos y destellos de haz
    // 2: Transición en arco luminoso fluido
    // 3: Pantalla final "hello" + "Fusion OS" + botón de inicio
    var currentPhase by remember { mutableIntStateOf(-1) }

    // Estados de animación de la fase de código de arranque
    val codeAlpha = remember { Animatable(1f) }
    var visibleLineCount by remember { mutableIntStateOf(0) }
    val codeProgress = remember { Animatable(0f) }
    var skipCodeRequested by remember { mutableStateOf(false) }

    // Transiciones y animables continuos
    val screenAlpha = remember { Animatable(1f) }
    val starAlpha = remember { Animatable(0f) }
    val starScale = remember { Animatable(0.55f) }
    val starRotation = remember { Animatable(-8f) }
    val studioTextAlpha = remember { Animatable(0f) }

    val metallicFlare = remember { Animatable(0f) }
    val crossBeamScale = remember { Animatable(0f) }

    val arcProgress = remember { Animatable(0f) }
    val arcAlpha = remember { Animatable(0f) }

    val helloAlpha = remember { Animatable(0f) }
    val helloScale = remember { Animatable(0.9f) }
    val fusionOsAlpha = remember { Animatable(0f) }
    val buttonAlpha = remember { Animatable(0f) }
    val buttonOffsetY = remember { Animatable(24f) }

    // Shimmer y pulso ambiental infinito para la escena final
    val infiniteTransition = rememberInfiniteTransition(label = "fusion_ambient")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -120f,
        targetValue = 350f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_shimmer"
    )

    val buttonPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "button_pulse"
    )

    val hintBreath by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hint_breath"
    )

    // Controlador cronometrado de la animación cinemática
    LaunchedEffect(Unit) {
        // === ESCENA PREVIA: Animación de Código de Arranque (Antes de Fusion Estudio) ===
        currentPhase = -1
        for (i in 1..bootCodeLines.size) {
            if (skipCodeRequested) break
            visibleLineCount = i
            launch {
                codeProgress.animateTo(i.toFloat() / bootCodeLines.size, tween(90))
            }
            delay(if (i == bootCodeLines.size) 460L else 125L)
        }

        if (!skipCodeRequested) {
            delay(150)
            codeAlpha.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
        } else {
            codeAlpha.snapTo(0f)
        }

        // === ESCENA 1: Intro estelar "Fusion estudio" ===
        currentPhase = 0
        launch {
            starAlpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
        launch {
            starScale.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
        launch {
            starRotation.animateTo(0f, tween(1100, easing = FastOutSlowInEasing))
        }
        delay(250)
        studioTextAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing))

        delay(1100) // Pausa de lectura de "Fusion estudio"

        // === ESCENA 2: Expansión y destello metálico (1.8s - 3.0s) ===
        currentPhase = 1
        launch {
            studioTextAlpha.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
        }
        launch {
            starScale.animateTo(1.75f, tween(1100, easing = FastOutSlowInEasing))
        }
        launch {
            metallicFlare.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
        }
        launch {
            crossBeamScale.animateTo(1.2f, tween(900, easing = FastOutSlowInEasing))
        }

        delay(950)

        // === ESCENA 3: Transición suave en arco luminoso (3.0s - 3.9s) ===
        currentPhase = 2
        launch {
            // El destello se expande y se funde armónicamente
            starScale.animateTo(2.3f, tween(600, easing = FastOutSlowInEasing))
            starAlpha.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
        }
        launch {
            arcAlpha.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
            arcProgress.animateTo(1f, tween(850, easing = FastOutSlowInEasing))
            delay(150)
            arcAlpha.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        }

        delay(550)

        // === ESCENA 4: Revelación "hello" + "Fusion OS" + Botón (3.9s en adelante) ===
        currentPhase = 3
        launch {
            helloAlpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
        launch {
            helloScale.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
        delay(350)
        launch {
            fusionOsAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        }
        delay(250)
        launch {
            buttonOffsetY.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
            buttonAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        }
    }

    var isExiting by remember { mutableStateOf(false) }
    val handleFinish: () -> Unit = {
        if (!isExiting) {
            isExiting = true
            onComplete()
        }
    }

    val handleScreenTap: () -> Unit = {
        if (currentPhase == -1) {
            // Un toque durante el código salta inmediatamente a Fusion Estudio
            skipCodeRequested = true
        } else {
            handleFinish()
        }
    }

    // Familia tipográfica cursiva oficial
    val cursiveFontFamily = remember {
        FontFamily(Font(R.font.allura))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .alpha(screenAlpha.value)
            .testTag("fusion_boot_screen")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tocar la pantalla avanza con fluidez
                handleScreenTap()
            },
        contentAlignment = Alignment.Center
    ) {
        // Botón discreto de "Saltar" en la parte superior derecha
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Text(
                text = "Saltar",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { handleFinish() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("fusion_boot_skip_button")
            )
        }

        // ====================================================================
        // CAPA 0: TERMINAL DE CÓDIGO CIBERNÉTICO DE ARRANQUE (Antes de Fusion Estudio)
        // ====================================================================
        if (codeAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(codeAlpha.value)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                CinematicCodeTerminalView(
                    visibleCount = visibleLineCount,
                    progress = codeProgress.value
                )
            }
        }

        // ====================================================================
        // CAPA 1 & 2: ESTRELLA DE 4 PUNTAS + RESPLANDOR METÁLICO Y HACES DE LUZ
        // ====================================================================
        if (starAlpha.value > 0.01f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.alpha(starAlpha.value)
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(starScale.value),
                    contentAlignment = Alignment.Center
                ) {
                    CinematicStarCanvas(
                        isMetallic = currentPhase >= 1,
                        metallicProgress = metallicFlare.value,
                        crossBeamScale = crossBeamScale.value,
                        rotationAngle = starRotation.value,
                        shimmerX = shimmerOffset
                    )
                }

                if (studioTextAlpha.value > 0.01f) {
                    Spacer(modifier = Modifier.height(44.dp))
                    Text(
                        text = "Fusion estudio",
                        color = Color.White,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 2.sp,
                        modifier = Modifier.alpha(studioTextAlpha.value)
                    )
                }
            }
        }

        // ====================================================================
        // CAPA 3: ARCO LUMINOSO DINÁMICO (Transición en haz de luz curvo)
        // ====================================================================
        if (arcAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(arcAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                CinematicLuminousArc(progress = arcProgress.value)
            }
        }

        // ====================================================================
        // CAPA 4: "hello" EN CALIGRAFÍA PURA + "Fusion OS" + BOTÓN DE FLECHA
        // ====================================================================
        if (helloAlpha.value > 0.01f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // Caligrafía "hello" en caligrafía vectorizada nativa sin ningún fondo rectangular
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .graphicsLayer {
                            alpha = helloAlpha.value
                            scaleX = helloScale.value
                            scaleY = helloScale.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Resplandor difuso ambiental detrás de las letras
                    Text(
                        text = "hello",
                        fontFamily = cursiveFontFamily,
                        fontSize = 86.sp,
                        color = Color(0x5580D8FF),
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0x66FFFFFF),
                                offset = Offset(0f, 0f),
                                blurRadius = 36f
                            )
                        ),
                        modifier = Modifier.scale(1.02f)
                    )

                    // Texto principal en blanco puro y nítido
                    Text(
                        text = "hello",
                        fontFamily = cursiveFontFamily,
                        fontSize = 86.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0x99FFFFFF),
                                offset = Offset(0f, 0f),
                                blurRadius = 18f
                            )
                        )
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Texto: "Fusion OS" (reemplazo solicitado de Infiniti OS)
                Text(
                    text = "Fusion OS",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.4.sp,
                    modifier = Modifier.alpha(fusionOsAlpha.value)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Botón píldora con flecha -> para iniciar
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = buttonAlpha.value
                            translationY = buttonOffsetY.value
                            scaleX = buttonPulse
                            scaleY = buttonPulse
                        }
                        .size(width = 82.dp, height = 42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { handleFinish() }
                        .testTag("fusion_boot_next_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Continuar a la configuración",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ====================================================================
        // PIE DE PÁGINA: Indicador de toque sutil
        // ====================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = currentPhase == 3 && buttonAlpha.value > 0.5f,
                enter = fadeIn(tween(600)),
                exit = fadeOut(tween(300))
            ) {
                Text(
                    text = "Toca para comenzar",
                    color = Color.White.copy(alpha = hintBreath),
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

/**
 * Dibuja la estrella de cuatro puntas astroidal con geometría perfecta y curvas cóncavas.
 * Incluye resplandor estelar, reflejos metálicos cromados y rayos de lente cross-beam.
 */
@Composable
private fun CinematicStarCanvas(
    isMetallic: Boolean,
    metallicProgress: Float,
    crossBeamScale: Float,
    rotationAngle: Float,
    shimmerX: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = (minOf(w, h) / 2f) * 0.92f

        rotate(rotationAngle, pivot = Offset(cx, cy)) {
            // Halo de luz radial de fondo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x4480D8FF),
                        Color(0x1AFFFFFF),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = r * 1.5f
                ),
                radius = r * 1.5f,
                center = Offset(cx, cy)
            )

            // Geometría del astroid de 4 puntas
            val starPath = Path().apply {
                moveTo(cx, cy - r) // Punta superior
                quadraticTo(cx + r * 0.15f, cy - r * 0.15f, cx + r, cy) // Punta derecha
                quadraticTo(cx + r * 0.15f, cy + r * 0.15f, cx, cy + r) // Punta inferior
                quadraticTo(cx - r * 0.15f, cy + r * 0.15f, cx - r, cy) // Punta izquierda
                quadraticTo(cx - r * 0.15f, cy - r * 0.15f, cx, cy - r) // Cierre
                close()
            }

            if (!isMetallic) {
                // Modo blanco brillante uniforme con borde iluminado
                drawPath(path = starPath, color = Color.White)
            } else {
                // Modo platino/cromo metálico con reflejo oblicuo de alta gama
                val metallicBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE2E8F0),
                        Color(0xFFFFFFFF),
                        Color(0xFF94A3B8),
                        Color(0xFF475569),
                        Color(0xFFCBD5E1),
                        Color(0xFFFFFFFF),
                        Color(0xFF64748B)
                    ),
                    start = Offset(cx - r + shimmerX, cy - r),
                    end = Offset(cx + r + shimmerX, cy + r)
                )

                drawPath(path = starPath, brush = metallicBrush)

                // Delineado plateado de contorno
                drawPath(
                    path = starPath,
                    color = Color.White.copy(alpha = 0.9f),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Rayos estelares sutiles (haz horizontal y vertical)
                if (crossBeamScale > 0.05f) {
                    val beamLength = r * 1.45f * crossBeamScale
                    val beamAlpha = (0.6f * metallicProgress).coerceIn(0f, 1f)

                    // Rayo horizontal
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = beamAlpha),
                                Color.Transparent
                            ),
                            startX = cx - beamLength,
                            endX = cx + beamLength
                        ),
                        start = Offset(cx - beamLength, cy),
                        end = Offset(cx + beamLength, cy),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Rayo vertical
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = beamAlpha),
                                Color.Transparent
                            ),
                            startY = cy - beamLength,
                            endY = cy + beamLength
                        ),
                        start = Offset(cx, cy - beamLength),
                        end = Offset(cx, cy + beamLength),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * Trazo en arco luminoso fluido que simula el movimiento de luz curvada hacia la firma "hello".
 */
@Composable
private fun CinematicLuminousArc(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(320.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val startX = cx - 80.dp.toPx()
        val startY = cy + 60.dp.toPx()
        val ctrlX = cx + 50.dp.toPx()
        val ctrlY = cy + 80.dp.toPx()
        val endX = cx + (100.dp.toPx() * progress)
        val endY = cy - (70.dp.toPx() * progress)

        val arcPath = Path().apply {
            moveTo(startX, startY)
            quadraticTo(ctrlX, ctrlY, endX, endY)
        }

        // Resplandor exterior difuso
        drawPath(
            path = arcPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x6680D8FF),
                    Color(0x99FFFFFF)
                )
            ),
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
        )

        // Núcleo brillante concentrado
        drawPath(
            path = arcPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFE0F7FA),
                    Color.White
                )
            ),
            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
        )

        // Chispa en la punta del arco
        drawCircle(
            color = Color.White,
            radius = 3.5.dp.toPx(),
            center = Offset(endX, endY)
        )
    }
}

/**
 * Representa una línea sintáctica de arranque del kernel Fusion OS.
 */
private data class BootCodeLine(
    val lineNumber: String,
    val prefix: String,
    val text: String,
    val suffix: String = "",
    val prefixColor: Color = Color(0xFF64748B),
    val textColor: Color = Color(0xFFE2E8F0),
    val suffixColor: Color = Color(0xFF34D399)
)

private val bootCodeLines = listOf(
    BootCodeLine("01", "$", "init fusion_kernel.sys --reactive", "[CORE OK]", Color(0xFF38BDF8), Color(0xFFF1F5F9), Color(0xFF34D399)),
    BootCodeLine("02", "→", "import fusion.runtime.CometEngine", "LOADED", Color(0xFFA78BFA), Color(0xFFE2E8F0), Color(0xFF38BDF8)),
    BootCodeLine("03", "$", "mem_alloc(0x7FFF4A20, 2048MB)", "[OK]", Color(0xFF38BDF8), Color(0xFF94A3B8), Color(0xFF34D399)),
    BootCodeLine("04", "→", "mount_layer(/dev/fusion_gpu)", "READY", Color(0xFFA78BFA), Color(0xFFE2E8F0), Color(0xFF38BDF8)),
    BootCodeLine("05", "$", "val studio = FusionStudio.compile()", "", Color(0xFFF472B6), Color(0xFFFDE047), Color.Transparent),
    BootCodeLine("06", "→", "studio.preloadCinematicStar()", "[100%]", Color(0xFFA78BFA), Color(0xFFE2E8F0), Color(0xFF34D399)),
    BootCodeLine("07", "$", "firi_ai.connectNeuralBridge()", "[ACTIVE]", Color(0xFF38BDF8), Color(0xFF93C5FD), Color(0xFF34D399)),
    BootCodeLine("08", "→", "audio_subsystem.bindZeroLatency()", "[SYNC]", Color(0xFFA78BFA), Color(0xFFE2E8F0), Color(0xFF38BDF8)),
    BootCodeLine("09", "★", "Integrity checksum: SHA256 VERIFIED", "[PASS]", Color(0xFFFBBF24), Color(0xFFE2E8F0), Color(0xFF34D399)),
    BootCodeLine("10", ">>", "LAUNCHING FUSION ESTUDIO...", "[SUCCESS]", Color(0xFF34D399), Color(0xFF67E8F9), Color(0xFF34D399))
)

/**
 * Terminal cinemático de código cibernético que se ejecuta antes de la animación de "Fusion estudio".
 */
@Composable
private fun CinematicCodeTerminalView(
    visibleCount: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF090D16).copy(alpha = 0.94f))
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF06B6D4).copy(alpha = 0.65f),
                        Color(0xFF8B5CF6).copy(alpha = 0.45f),
                        Color(0xFF3B82F6).copy(alpha = 0.25f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        // Barra superior de la ventana del terminal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
            }

            Text(
                text = "fusion_boot.sh",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.65f)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF06B6D4).copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "BOOT v4.12",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF06B6D4)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Líneas de código
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val count = visibleCount.coerceIn(0, bootCodeLines.size)
            for (idx in 0 until count) {
                val line = bootCodeLines[idx]
                val isLast = idx == count - 1

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = line.lineNumber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        color = Color(0xFF475569),
                        modifier = Modifier.width(20.dp)
                    )
                    Text(
                        text = line.prefix,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = line.prefixColor,
                        modifier = Modifier.width(18.dp)
                    )
                    Text(
                        text = line.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = if (idx == 9) FontWeight.Bold else FontWeight.Normal,
                        color = line.textColor,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    if (line.suffix.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = line.suffix,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = line.suffixColor
                        )
                    }
                    if (isLast) {
                        Text(
                            text = "▋",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = Color(0xFF06B6D4).copy(alpha = cursorAlpha),
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Barra de progreso inferior del terminal
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (progress >= 1f) "SISTEMA COMPILADO" else "COMPILANDO FUSION OS...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (progress >= 1f) Color(0xFF34D399) else Color(0xFF38BDF8)
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF06B6D4),
                                    Color(0xFF3B82F6),
                                    Color(0xFF8B5CF6),
                                    Color(0xFF34D399)
                                )
                            )
                        )
                )
            }
        }
    }
}
