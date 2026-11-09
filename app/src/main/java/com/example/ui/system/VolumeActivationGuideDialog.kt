package com.example.ui.system

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.UserSettings
import com.example.service.VolumeAccessibilityService
import com.example.ui.theme.LauncherPalette

@Composable
fun VolumeActivationGuideDialog(
    palette: LauncherPalette,
    userSettings: UserSettings,
    onDismiss: () -> Unit
) {
    val isEs = userSettings.isSpanish
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isAccessibilityActive by remember {
        mutableStateOf(VolumeAccessibilityService.isAccessibilityEnabled(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityActive = VolumeAccessibilityService.isAccessibilityEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(26.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.cardBorder),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isEs) "Barra en Todas las Apps" else "Global Volume Bar",
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.onBackground
                            )
                            Text(
                                text = if (isEs) "TikTok, YouTube, Reels, Juegos y más" else "TikTok, YouTube, Reels, Games & more",
                                fontSize = 11.sp,
                                color = palette.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = palette.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Estado actual
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAccessibilityActive)
                            Color(0xFF10B981).copy(alpha = 0.14f)
                        else
                            Color(0xFFF59E0B).copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(
                        1.2.dp,
                        if (isAccessibilityActive) Color(0xFF10B981) else Color(0xFFF59E0B)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAccessibilityActive) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isAccessibilityActive) Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAccessibilityActive)
                                    (if (isEs) "¡Servicio Activado y Listo!" else "Service Activated & Ready!")
                                else
                                    (if (isEs) "Requiere activación en Ajustes" else "Requires activation in Settings"),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAccessibilityActive) Color(0xFF059669) else Color(0xFFD97706)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isAccessibilityActive)
                                    (if (isEs)
                                        "Al presionar volumen en cualquier app (TikTok, YouTube, etc.) se oculta la del sistema y sale la de Firi."
                                    else
                                        "Volume buttons in all apps will now hide the system dialog and show Firi's.")
                                else
                                    (if (isEs)
                                        "Android no permite interceptar botones de volumen fuera del launcher sin este permiso oficial."
                                    else
                                        "Android requires this official permission to intercept volume keys inside external apps."),
                                fontSize = 11.sp,
                                color = palette.onSurface.copy(alpha = 0.85f),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pasos de activación (cuando aún no está activo)
                if (!isAccessibilityActive) {
                    Text(
                        text = if (isEs) "Pasos sencillos para activarlo (30 segundos):" else "Simple steps to enable (30 seconds):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StepCard(
                            palette = palette,
                            number = "1",
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            title = if (isEs) "Toca 'Abrir Ajustes de Accesibilidad' abajo" else "Tap 'Open Accessibility Settings' below",
                            description = if (isEs) "Te llevará directo a la sección de Accesibilidad de tu teléfono." else "Directly opens device accessibility settings."
                        )

                        StepCard(
                            palette = palette,
                            number = "2",
                            icon = Icons.Default.Settings,
                            title = if (isEs) "Entra en 'Apps instaladas' o 'Servicios descargados'" else "Open 'Installed apps' or 'Downloaded services'",
                            description = if (isEs) "En Xiaomi/Samsung suele llamarse 'Servicios descargados' o 'Apps instaladas'." else "Usually named 'Downloaded services' or 'Installed apps'."
                        )

                        StepCard(
                            palette = palette,
                            number = "3",
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            title = if (isEs) "Selecciona 'Barra de Volumen Firi'" else "Select 'Firi Volume Bar'",
                            description = if (isEs) "Verás el icono y nombre de Firi Launcher." else "Look for the Firi Launcher service."
                        )

                        StepCard(
                            palette = palette,
                            number = "4",
                            icon = Icons.Default.TouchApp,
                            title = if (isEs) "Activa el interruptor y pulsa 'Permitir'" else "Turn ON the switch and tap 'Allow'",
                            description = if (isEs) "¡Y listo! Al volver aquí, verás la confirmación en verde." else "Done! Return here to see the confirmation."
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            VolumeAccessibilityService.openAccessibilitySettings(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("open_accessibility_guide_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEs) "Abrir Ajustes de Accesibilidad" else "Open Accessibility Settings",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                } else {
                    // Contenido cuando YA está activo
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isEs)
                                "La barra de volumen Fusion OS ya está configurada globalmente. Cada vez que abras TikTok, YouTube, Instagram o cualquier juego y toques los botones de volumen, aparecerá tu barra personalizada y la de fábrica quedará oculta."
                            else
                                "Fusion OS volume bar is globally enabled. When you open TikTok, YouTube, Instagram or games and press volume keys, Firi's slider will show and the system dialog is suppressed.",
                            fontSize = 12.sp,
                            color = palette.onSurface.copy(alpha = 0.8f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                VolumeAccessibilityService.showVolume(context, 0.70f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("test_volume_guide_btn"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEs) "Probar Barra de Volumen Ahora" else "Test Volume Bar Now",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("close_volume_guide_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isEs) "Cerrar" else "Close",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    palette: LauncherPalette,
    number: String,
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(palette.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = palette.onBackground
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = description,
                fontSize = 10.5.sp,
                color = palette.onSurface.copy(alpha = 0.65f),
                lineHeight = 14.sp
            )
        }
    }
}
