package com.example.ui.system

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Teclado numérico estilo iOS / Cristal translúcido para introducir el PIN de seguridad.
 */
@Composable
fun PinKeypadView(
    correctPin: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    title: String? = null,
    isSettingUp: Boolean = false,
    onPinDefined: (String) -> Unit = {}
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val targetLength = if (isSettingUp) 4 else correctPin.ifEmpty { "1234" }.length

    fun triggerError() {
        isError = true
        coroutineScope.launch {
            repeat(3) {
                shakeOffset.animateTo(-16f, tween(50, easing = FastOutSlowInEasing))
                shakeOffset.animateTo(16f, tween(50, easing = FastOutSlowInEasing))
            }
            shakeOffset.animateTo(0f, tween(50, easing = FastOutSlowInEasing))
            delay(200)
            enteredPin = ""
            isError = false
        }
    }

    fun onDigitPress(digit: String) {
        if (enteredPin.length < targetLength) {
            val updated = enteredPin + digit
            enteredPin = updated
            if (updated.length == targetLength) {
                if (isSettingUp) {
                    onPinDefined(updated)
                    onSuccess()
                } else {
                    val expected = correctPin.ifEmpty { "1234" }
                    if (updated == expected) {
                        onSuccess()
                    } else {
                        triggerError()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ícono de Candado
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title ?: if (isSettingUp) "Define tu PIN de 4 dígitos" else "Introduce el PIN",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Indicadores de puntos del PIN (con animación de sacudida en error)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
        ) {
            for (i in 0 until targetLength) {
                val isFilled = i < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (isError) Color(0xFFEF4444)
                            else if (isFilled) Color.White
                            else Color.Transparent
                        )
                        .border(
                            1.5.dp,
                            if (isError) Color(0xFFEF4444) else Color.White.copy(alpha = 0.6f),
                            CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Teclado numérico 3x4
        val digits = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("CANCEL", "0", "BACK")
        )

        for (row in digits) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (key in row) {
                    when (key) {
                        "CANCEL" -> {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clickable { onCancel() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cancelar",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                        "BACK" -> {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clickable {
                                        if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Borrar",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                    .clickable { onDigitPress(key) }
                                    .testTag("pin_key_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Light,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

/**
 * Lienzo interactivo de 3x3 puntos para dibujar el patrón de desbloqueo.
 */
@Composable
fun PatternDrawView(
    correctPattern: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    title: String? = null,
    isSettingUp: Boolean = false,
    onPatternDefined: (String) -> Unit = {}
) {
    val selectedNodes = remember { mutableStateListOf<Int>() }
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }
    var isError by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun verifyPattern() {
        val patternStr = selectedNodes.joinToString(",")
        if (isSettingUp) {
            if (selectedNodes.size >= 4) {
                onPatternDefined(patternStr)
                onSuccess()
            } else {
                isError = true
                coroutineScope.launch {
                    delay(600)
                    selectedNodes.clear()
                    isError = false
                }
            }
        } else {
            val expectedPattern = correctPattern.ifEmpty { "0,1,2,5" }
            if (patternStr == expectedPattern) {
                onSuccess()
            } else {
                isError = true
                coroutineScope.launch {
                    delay(600)
                    selectedNodes.clear()
                    isError = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title ?: if (isSettingUp) "Traza tu nuevo patrón (mínimo 4 puntos)" else "Dibuja el patrón para desbloquear",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            if (isError) {
                Text(
                    text = if (isSettingUp) "El patrón debe unir al menos 4 puntos" else "Patrón incorrecto. Inténtalo de nuevo.",
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Rejilla de 3x3 puntos
        Box(
            modifier = Modifier
                .size(300.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedNodes.clear()
                            isError = false
                            currentTouchPoint = offset
                            val node = findClosestNode(offset, size.width.toFloat(), size.height.toFloat())
                            if (node != null && !selectedNodes.contains(node)) {
                                selectedNodes.add(node)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentTouchPoint = change.position
                            val node = findClosestNode(change.position, size.width.toFloat(), size.height.toFloat())
                            if (node != null && !selectedNodes.contains(node)) {
                                selectedNodes.add(node)
                            }
                        },
                        onDragEnd = {
                            currentTouchPoint = null
                            verifyPattern()
                        },
                        onDragCancel = {
                            currentTouchPoint = null
                            selectedNodes.clear()
                        }
                    )
                }
                .testTag("pattern_draw_surface")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val nodePositions = calculateNodePositions(size.width, size.height)

                // 1. Dibujar líneas conectoras entre nodos seleccionados
                val lineColor = if (isError) Color(0xFFEF4444) else Color(0xFF38BDF8)
                if (selectedNodes.size > 1) {
                    for (i in 0 until selectedNodes.size - 1) {
                        val p1 = nodePositions[selectedNodes[i]]
                        val p2 = nodePositions[selectedNodes[i + 1]]
                        drawLine(
                            color = lineColor,
                            start = p1,
                            end = p2,
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // 2. Línea guía al punto de toque actual
                if (selectedNodes.isNotEmpty() && currentTouchPoint != null) {
                    val lastNodePos = nodePositions[selectedNodes.last()]
                    drawLine(
                        color = lineColor.copy(alpha = 0.6f),
                        start = lastNodePos,
                        end = currentTouchPoint!!,
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 3. Dibujar los 9 puntos
                for (i in 0 until 9) {
                    val pos = nodePositions[i]
                    val isSelected = selectedNodes.contains(i)

                    // Círculo exterior
                    drawCircle(
                        color = if (isSelected) lineColor else Color.White.copy(alpha = 0.35f),
                        radius = if (isSelected) 22.dp.toPx() else 14.dp.toPx(),
                        center = pos
                    )
                    // Núcleo central
                    drawCircle(
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                        radius = 6.dp.toPx(),
                        center = pos
                    )
                }
            }
        }

        // Botón Cancelar
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .clickable { onCancel() }
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text(text = "Cancelar", fontSize = 14.sp, color = Color.White)
        }
    }
}

private fun calculateNodePositions(width: Float, height: Float): List<Offset> {
    val stepX = width / 4f
    val stepY = height / 4f
    val positions = mutableListOf<Offset>()
    for (row in 1..3) {
        for (col in 1..3) {
            positions.add(Offset(col * stepX, row * stepY))
        }
    }
    return positions
}

private fun findClosestNode(touch: Offset, width: Float, height: Float): Int? {
    val positions = calculateNodePositions(width, height)
    val maxDistance = width / 7f
    for (i in positions.indices) {
        val dist = hypot(touch.x - positions[i].x, touch.y - positions[i].y)
        if (dist <= maxDistance) return i
    }
    return null
}
