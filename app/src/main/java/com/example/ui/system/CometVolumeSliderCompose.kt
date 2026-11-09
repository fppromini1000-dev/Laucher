package com.example.ui.system

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CometVolumeSliderCompose(
    volumeFraction: Float,
    visible: Boolean,
    onVolumeChanged: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    var lastVibratedStep by remember { mutableIntStateOf(-1) }
    var localFraction by remember(volumeFraction) { mutableFloatStateOf(volumeFraction) }

    fun triggerTick() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10)
            }
        } catch (_: Exception) {}
    }

    // Auto dismiss and collapse timer
    LaunchedEffect(visible, localFraction, isDragging) {
        if (visible && !isDragging) {
            delay(1400)
            isExpanded = false
            delay(1200)
            onDismiss()
        }
    }

    // Slide animation (from right edge)
    val slideOffset = remember { Animatable(200f) }
    LaunchedEffect(visible) {
        if (visible) {
            slideOffset.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
        } else {
            slideOffset.animateTo(200f, tween(180, easing = FastOutSlowInEasing))
        }
    }

    val widthDp by animateDpAsState(
        targetValue = if (isExpanded) 46.dp else 14.dp,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "volume_width"
    )

    if (visible || slideOffset.value < 190f) {
        Box(
            modifier = modifier
                .fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            val totalHeight = 186.dp

            Box(
                modifier = Modifier
                    .padding(top = 170.dp)
                    .offset { IntOffset(slideOffset.value.roundToInt(), 0) }
                    .width(widthDp)
                    .height(totalHeight)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(topStart = 23.dp, bottomStart = 23.dp))
                    .clip(RoundedCornerShape(topStart = 23.dp, bottomStart = 23.dp))
                    .background(Color(0xFF141416)) // CometOS deep black
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.18f),
                        RoundedCornerShape(topStart = 23.dp, bottomStart = 23.dp)
                    )
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                isDragging = true
                                isExpanded = true
                                triggerTick()
                            },
                            onDragEnd = {
                                isDragging = false
                            },
                            onDragCancel = {
                                isDragging = false
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val totalPx = totalHeight.toPx()
                                val deltaFraction = -dragAmount / totalPx
                                val next = (localFraction + deltaFraction).coerceIn(0f, 1f)
                                localFraction = next
                                onVolumeChanged(next)

                                val step = (next * 20).toInt()
                                if (step != lastVibratedStep) {
                                    lastVibratedStep = step
                                    triggerTick()
                                }
                            }
                        )
                    }
                    .testTag("comet_volume_slider_container")
            ) {
                // Interior Track and Fill
                val isWide = widthDp > 28.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = if (isWide) 9.dp else 3.dp,
                            vertical = 10.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Percentage indicator when expanded
                    if (isWide) {
                        val isMuted = localFraction <= 0.01f
                        Text(
                            text = if (isMuted) "MUTE" else "${(localFraction * 100).toInt()}%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMuted) Color(0xFFEF4444) else Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Track Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2A2B32))
                    ) {
                        // Level Fill (Bottom to Top)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(localFraction)
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                        )

                        // Circular Knob (Only when expanded)
                        if (isWide) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxHeight(localFraction)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .size(24.dp)
                                        .shadow(4.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFE0E0E0), CircleShape)
                                )
                            }
                        }
                    }

                    // Bottom Icon (Only when expanded)
                    if (isWide) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val isMuted = localFraction <= 0.01f

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) Color(0x33EF4444) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isMuted -> Icons.AutoMirrored.Filled.VolumeOff
                                    localFraction < 0.35f -> Icons.AutoMirrored.Filled.VolumeMute
                                    localFraction < 0.70f -> Icons.AutoMirrored.Filled.VolumeDown
                                    else -> Icons.AutoMirrored.Filled.VolumeUp
                                },
                                contentDescription = if (isMuted) "Silenciado" else "Volumen",
                                tint = if (isMuted) Color(0xFFEF4444) else Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
