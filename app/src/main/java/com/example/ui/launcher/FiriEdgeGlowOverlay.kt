package com.example.ui.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun FiriEdgeGlowOverlay(
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isListening,
        enter = fadeIn(animationSpec = tween(350)),
        exit = fadeOut(animationSpec = tween(400))
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "edge_glow_trail")

        // Rotating gradient animation along the perimeter
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "edge_rotation"
        )

        // Pulsing breathing intensity
        val pulseIntensity by infiniteTransition.animateFloat(
            initialValue = 0.75f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_intensity"
        )

        val neonColors = listOf(
            Color(0xFF2E5BFF), // Electric Blue
            Color(0xFF00F5D4), // Vibrant Cyan
            Color(0xFFFF007F), // Hot Magenta / Fuchsia
            Color(0xFF9D4EDD), // Deep Violet
            Color(0xFFFF9E00), // Warm Amber
            Color(0xFF00F5D4), // Cyan return
            Color(0xFF2E5BFF)  // Electric Blue return
        )

        Box(modifier = modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthInner = 14.dp.toPx()
                val strokeWidthCore = 6.dp.toPx()
                val strokeWidthGlow = 26.dp.toPx()

                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                val sweepBrush = Brush.sweepGradient(
                    colors = neonColors,
                    center = centerOffset
                )

                // 1. Soft wide ambient inner bloom (mimicking Apple Intelligence edge glow)
                drawRoundRect(
                    brush = sweepBrush,
                    topLeft = Offset(strokeWidthGlow / 4, strokeWidthGlow / 4),
                    size = Size(
                        size.width - strokeWidthGlow / 2,
                        size.height - strokeWidthGlow / 2
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(40.dp.toPx()),
                    style = Stroke(width = strokeWidthGlow, cap = StrokeCap.Round),
                    alpha = 0.45f * pulseIntensity
                )

                // 2. Focused vibrant mid ring
                drawRoundRect(
                    brush = sweepBrush,
                    topLeft = Offset(strokeWidthInner / 2, strokeWidthInner / 2),
                    size = Size(
                        size.width - strokeWidthInner,
                        size.height - strokeWidthInner
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(34.dp.toPx()),
                    style = Stroke(width = strokeWidthInner, cap = StrokeCap.Round),
                    alpha = 0.80f * pulseIntensity
                )

                // 3. Crisp sharp outer boundary line
                drawRoundRect(
                    brush = sweepBrush,
                    topLeft = Offset(strokeWidthCore / 2, strokeWidthCore / 2),
                    size = Size(
                        size.width - strokeWidthCore,
                        size.height - strokeWidthCore
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()),
                    style = Stroke(width = strokeWidthCore, cap = StrokeCap.Round),
                    alpha = 0.95f
                )
            }
        }
    }
}
