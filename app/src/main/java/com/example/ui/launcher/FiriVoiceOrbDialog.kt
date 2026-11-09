package com.example.ui.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.FiriChatMessage
import com.example.ai.FiriVoiceState
import com.example.model.UserSettings
import com.example.ui.theme.LauncherPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiriVoiceOrbDialog(
    voiceState: FiriVoiceState,
    transcription: String,
    rmsDb: Float,
    conversationHistory: List<FiriChatMessage> = emptyList(),
    brainTelemetry: com.example.ai.FiriBrainTelemetry = com.example.ai.FiriBrainTelemetry(),
    onToggleForceOfflineMode: ((Boolean) -> Unit)? = null,
    palette: LauncherPalette,
    userSettings: UserSettings,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    onSubmitTextQuery: (String) -> Unit,
    onClearConversation: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val isEs = userSettings.isSpanish
    var manualText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll hacia el mensaje más reciente
    LaunchedEffect(conversationHistory.size) {
        if (conversationHistory.isNotEmpty()) {
            listState.animateScrollToItem(conversationHistory.size - 1)
        }
    }

    // Animaciones del Orbe y Resplandor de Firi AI
    val infiniteTransition = rememberInfiniteTransition(label = "firi_voice_orb")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val waveRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_rotation"
    )

    // Escala del micrófono reactiva y suave
    val audioScale = (1f + (rmsDb / 14f)).coerceIn(1f, 1.38f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(28.dp),
            color = palette.surface,
            border = BorderStroke(1.5.dp, palette.primary.copy(alpha = 0.45f)),
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ========================================================
                // BARRA SUPERIOR: Marca, Estado y Controles
                // ========================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.firi_app_icon),
                            contentDescription = "Firi Icon",
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Firi AI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.onBackground
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (voiceState) {
                                        is FiriVoiceState.Listening -> Color(0xFF00F5D4)
                                        is FiriVoiceState.Thinking -> Color(0xFFFFD166)
                                        is FiriVoiceState.Speaking -> palette.primary
                                        else -> palette.onSurface.copy(alpha = 0.35f)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.primary.copy(alpha = 0.14f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isEs) "Pro Inteligente" else "Smart AI",
                                fontSize = 10.5.sp,
                                color = palette.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (conversationHistory.isNotEmpty()) {
                            IconButton(
                                onClick = onClearConversation,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("clear_firi_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Limpiar conversación",
                                    tint = palette.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("close_firi_dialog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = palette.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ========================================================
                // CINTA TELEMETRÍA DEL ASISTENTE: Doble Cerebro de Firi
                // ========================================================
                val isLocalBrainActive = brainTelemetry.activeBrain == com.example.ai.FiriBrainMode.ON_DEVICE_CPU
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isLocalBrainActive)
                                Color(0xFF10B981).copy(alpha = 0.12f)
                            else
                                Color(0xFF3B82F6).copy(alpha = 0.12f)
                        )
                        .border(
                            1.dp,
                            if (isLocalBrainActive) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFF3B82F6).copy(alpha = 0.35f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isLocalBrainActive) "⚡" else "☁️",
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (isLocalBrainActive)
                                    (if (isEs) "Segundo Cerebro (Procesador Local)" else "Second Brain (On-Device CPU)")
                                else
                                    (if (isEs) "Primer Cerebro (Nube Gemini)" else "First Brain (Gemini Cloud)"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLocalBrainActive) Color(0xFF10B981) else Color(0xFF3B82F6)
                            )
                            Text(
                                text = if (isLocalBrainActive)
                                    (if (isEs) "100% Sin Internet · ${brainTelemetry.processorCores} núcleos CPU · ${brainTelemetry.lastExecutionTimeMs}ms" else "100% Offline · ${brainTelemetry.processorCores} CPU cores · ${brainTelemetry.lastExecutionTimeMs}ms")
                                else
                                    (if (isEs) "En línea · Conexión a Nube Gemini" else "Online · Cloud reasoning"),
                                fontSize = 9.5.sp,
                                color = palette.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (onToggleForceOfflineMode != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (brainTelemetry.forceOfflineMode)
                                        Color(0xFF10B981).copy(alpha = 0.22f)
                                    else
                                        palette.surfaceVariant
                                )
                                .clickable {
                                    onToggleForceOfflineMode(!brainTelemetry.forceOfflineMode)
                                }
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                                .testTag("toggle_offline_brain_btn")
                        ) {
                            Text(
                                text = if (brainTelemetry.forceOfflineMode)
                                    (if (isEs) "Modo Offline Forzado" else "Offline Forced")
                                else
                                    (if (isEs) "Probar Sin Internet" else "Test Offline"),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (brainTelemetry.forceOfflineMode) Color(0xFF10B981) else palette.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ========================================================
                // ORBE INTERACTIVO DINÁMICO DE FIRI AI
                // ========================================================
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(130.dp)
                        .clickable {
                            when (voiceState) {
                                is FiriVoiceState.Listening -> onStopListening()
                                is FiriVoiceState.Speaking -> onStopSpeaking()
                                else -> onStartListening()
                            }
                        }
                        .testTag("firi_main_orb_clickable")
                ) {
                    // Resplandor exterior de energía
                    Box(
                        modifier = Modifier
                            .size(122.dp)
                            .scale(if (voiceState is FiriVoiceState.Listening) audioScale else pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        palette.primary.copy(alpha = 0.45f),
                                        palette.secondary.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Corona de luz rotativa multicolor
                    Box(
                        modifier = Modifier
                            .size(98.dp)
                            .scale(if (voiceState is FiriVoiceState.Speaking) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFF2E5BFF), // Azul eléctrico
                                        Color(0xFF00F5D4), // Cian
                                        Color(0xFFFF007F), // Magenta
                                        Color(0xFF9D4EDD), // Violeta
                                        Color(0xFFFFD166), // Dorado
                                        Color(0xFF2E5BFF)  // Cierre
                                    )
                                )
                            )
                    )

                    // Esfera interna táctil con gradiente refinado
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        palette.primary,
                                        palette.secondary,
                                        palette.surface
                                    )
                                )
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        when (voiceState) {
                            is FiriVoiceState.Listening -> {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Escuchando",
                                    tint = palette.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            is FiriVoiceState.Thinking -> {
                                CircularProgressIndicator(
                                    color = palette.onPrimary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            is FiriVoiceState.Speaking -> {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Detener habla",
                                    tint = palette.onPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.firi_app_icon),
                                        contentDescription = "Hablar con Firi",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }

                // Estado visual textual
                Text(
                    text = when (voiceState) {
                        is FiriVoiceState.Listening -> if (isEs) "Escuchando... Háblame con naturalidad" else "Listening... Speak naturally"
                        is FiriVoiceState.Thinking -> if (isEs) "Firi está razonando..." else "Firi is thinking..."
                        is FiriVoiceState.Speaking -> if (isEs) "Firi respondiendo (Toca el orbe para pausar)" else "Firi speaking (Tap to pause)"
                        is FiriVoiceState.Error -> voiceState.message
                        else -> if (isEs) "Toca el orbe para hablar o escribe abajo" else "Tap orb to speak or type below"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (voiceState is FiriVoiceState.Error) palette.secondary else palette.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )

                // Transcripción en tiempo real
                if (transcription.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.surfaceVariant.copy(alpha = 0.85f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "\"$transcription\"",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.onBackground
                        )
                    }
                }

                // ========================================================
                // HISTORIAL CONVERSACIONAL (Burbujas dinámicas)
                // ========================================================
                if (conversationHistory.isNotEmpty()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp, max = 220.dp)
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(conversationHistory, key = { it.id }) { msg ->
                            if (msg.isUser) {
                                // Mensaje del Usuario
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                                            .background(palette.primary)
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            color = palette.onPrimary,
                                            fontSize = 13.sp,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            } else {
                                // Respuesta de Firi
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                                            .background(palette.surfaceVariant)
                                            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "✨", fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Firi",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            val isCpu = msg.brainUsed == com.example.ai.FiriBrainMode.ON_DEVICE_CPU
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isCpu) Color(0xFF10B981).copy(alpha = 0.18f)
                                                        else Color(0xFF3B82F6).copy(alpha = 0.18f)
                                                    )
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = if (isCpu)
                                                        "⚡ CPU · ${msg.executionTimeMs}ms"
                                                    else
                                                        "☁️ Nube · ${msg.executionTimeMs}ms",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isCpu) Color(0xFF059669) else Color(0xFF2563EB)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = msg.text,
                                            color = palette.onBackground,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ========================================================
                // CHIPS DE SUGERENCIAS RÁPIDAS
                // ========================================================
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val suggestions = if (isEs) listOf(
                        "⚡ Segundo cerebro",
                        "🧮 25 * 4",
                        "📶 ¿Cómo funcionas sin internet?",
                        "💬 ¿Cómo estás?",
                        "🎨 Personalizar bloqueo",
                        "🔊 Subir volumen",
                        "💡 Consejo de enfoque",
                        "📱 Ajustes"
                    ) else listOf(
                        "⚡ Second brain",
                        "🧮 25 * 4",
                        "📶 How do you work offline?",
                        "💬 How are you?",
                        "🎨 Customize lock screen",
                        "🔊 Volume up",
                        "💡 Productivity tip",
                        "📱 Settings"
                    )

                    suggestions.forEach { chipText ->
                        val cleanQuery = chipText.substringAfter(" ")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(palette.surfaceVariant.copy(alpha = 0.8f))
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                                .clickable { onSubmitTextQuery(cleanQuery) }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .testTag("firi_chip_${chipText.take(5)}")
                        ) {
                            Text(
                                text = chipText,
                                fontSize = 11.sp,
                                color = palette.onSurface.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ========================================================
                // CAMPO DE TEXTO MANUAL Y BOTÓN DE ACCIÓN
                // ========================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it },
                        placeholder = {
                            Text(
                                if (isEs) "Pregúntale algo a Firi..." else "Ask Firi anything...",
                                fontSize = 12.sp,
                                color = palette.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("firi_manual_text_input"),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.primary,
                            unfocusedBorderColor = palette.cardBorder,
                            focusedTextColor = palette.onBackground,
                            unfocusedTextColor = palette.onBackground,
                            focusedContainerColor = palette.surfaceVariant,
                            unfocusedContainerColor = palette.surfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (manualText.isNotBlank()) {
                                onSubmitTextQuery(manualText)
                                manualText = ""
                            } else {
                                onStartListening()
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.primary)
                            .testTag("firi_send_manual_query_button")
                    ) {
                        if (manualText.isNotBlank()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar",
                                tint = palette.onPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.firi_app_icon),
                                    contentDescription = "Hablar con Firi",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
