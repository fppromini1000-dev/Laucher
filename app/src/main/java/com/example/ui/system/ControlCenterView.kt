package com.example.ui.system

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserSettings
import com.example.system.SystemControlManager
import com.example.ui.theme.LauncherPalette
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ControlCenterView(
    systemControlManager: SystemControlManager,
    palette: LauncherPalette,
    userSettings: UserSettings,
    onOpenPermissionsDialog: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEs = userSettings.isSpanish
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }

    var isMusicPlaying by remember { mutableStateOf(false) }

    // Colores frosted glass estilo HarmonyOS / iOS
    val glassCardColor = Color(0x38FFFFFF)
    val glassCardBorder = Color(0x35FFFFFF)
    val activePillColor = Color(0xFF007AFF)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
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
                        if (offsetY.value < -200f || velocity < -1000f) {
                            offsetY.animateTo(-1500f, tween(200, easing = FastOutSlowInEasing))
                            onClose()
                        } else {
                            offsetY.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
                        }
                    }
                }
            )
            .testTag("control_center_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Barra Superior de Control Center
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isEs) "Centro de Control" else "Control Center",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Botón para revisar permisos
                    IconButton(
                        onClick = onOpenPermissionsDialog,
                        modifier = Modifier.size(36.dp).testTag("control_center_permissions_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Permisos",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Botón de cerrar
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp).testTag("control_center_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sección 1: Reproductor Multimedia (Izquierda) + Conectividad (Derecha)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(154.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tarjeta Reproductor Musical (Estilo Imagen 2)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = glassCardColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, glassCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isMusicPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "High Five",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "HarmonyOS Beats",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Controles de música
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Anterior",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { isMusicPlaying = !isMusicPlaying },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = if (isMusicPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Siguiente",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Tarjeta de Conectividad (Wi-Fi y Bluetooth)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Píldora Wi-Fi
                    ConnectivityPill(
                        icon = Icons.Default.Wifi,
                        title = "Wi-Fi",
                        subtitle = if (isEs) "Conectado" else "Connected",
                        isActive = systemControlManager.isWifiEnabled,
                        activeColor = activePillColor,
                        glassColor = glassCardColor,
                        glassBorder = glassCardBorder,
                        onClick = { systemControlManager.openWifiSettings() }
                    )

                    // Píldora Bluetooth
                    ConnectivityPill(
                        icon = Icons.Default.Bluetooth,
                        title = "Bluetooth",
                        subtitle = if (isEs) "Disponible" else "Available",
                        isActive = systemControlManager.isBluetoothEnabled,
                        activeColor = activePillColor,
                        glassColor = glassCardColor,
                        glassBorder = glassCardBorder,
                        onClick = { systemControlManager.openBluetoothSettings() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sección 2: Deslizadores Interactivos (Brillo de pantalla y Volumen de audio)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Deslizador de Brillo de Pantalla
                VerticalControlSlider(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.WbSunny,
                    label = if (isEs) "Brillo" else "Brightness",
                    value = systemControlManager.brightnessFloat,
                    glassColor = glassCardColor,
                    glassBorder = glassCardBorder,
                    activeColor = Color.White,
                    onValueChange = { newVal ->
                        systemControlManager.setBrightness(newVal, context as? Activity)
                    }
                )

                // Deslizador de Volumen Multimedia
                VerticalControlSlider(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    label = if (isEs) "Volumen" else "Volume",
                    value = systemControlManager.musicVolumeFloat,
                    glassColor = glassCardColor,
                    glassBorder = glassCardBorder,
                    activeColor = Color.White,
                    onValueChange = { newVal ->
                        systemControlManager.setMusicVolume(newVal)
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sección 3: Matriz de Toggles Rápidos (Linterna, No Molestar, Rotación, Cámara, etc.)
            Text(
                text = if (isEs) "Acciones Rápidas" else "Quick Actions",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )

            val toggleActions = listOf(
                QuickActionItem(
                    icon = if (systemControlManager.isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    label = if (isEs) "Linterna" else "Torch",
                    isActive = systemControlManager.isTorchOn,
                    onClick = { systemControlManager.toggleTorch() }
                ),
                QuickActionItem(
                    icon = Icons.Default.DoNotDisturbOn,
                    label = if (isEs) "No Molestar" else "DND",
                    isActive = systemControlManager.isDndEnabled,
                    onClick = { systemControlManager.toggleDnd() }
                ),
                QuickActionItem(
                    icon = Icons.Default.ScreenRotation,
                    label = if (isEs) "Rotación" else "Rotate",
                    isActive = systemControlManager.isAutoRotateOn,
                    onClick = { systemControlManager.toggleAutoRotate() }
                ),
                QuickActionItem(
                    icon = Icons.Default.CameraAlt,
                    label = if (isEs) "Cámara" else "Camera",
                    isActive = false,
                    onClick = {
                        systemControlManager.openCamera()
                        onClose()
                    }
                ),
                QuickActionItem(
                    icon = Icons.Default.AirplanemodeActive,
                    label = if (isEs) "Modo Avión" else "Airplane",
                    isActive = false,
                    onClick = { systemControlManager.openWifiSettings() }
                ),
                QuickActionItem(
                    icon = Icons.Default.WifiTethering,
                    label = if (isEs) "Punto Acceso" else "Hotspot",
                    isActive = false,
                    onClick = { systemControlManager.openWifiSettings() }
                ),
                QuickActionItem(
                    icon = Icons.Default.Calculate,
                    label = if (isEs) "Calculadora" else "Calculator",
                    isActive = false,
                    onClick = {
                        try {
                            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.calculator")
                            if (intent != null) context.startActivity(intent)
                        } catch (_: Exception) {}
                        onClose()
                    }
                ),
                QuickActionItem(
                    icon = Icons.Default.Settings,
                    label = if (isEs) "Ajustes" else "Settings",
                    isActive = false,
                    onClick = {
                        systemControlManager.openSecuritySettings()
                        onClose()
                    }
                )
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(toggleActions) { action ->
                    QuickToggleIcon(
                        action = action,
                        glassColor = glassCardColor,
                        glassBorder = glassCardBorder,
                        activeColor = activePillColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barra inferior para cerrar deslizando
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(4.5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.5f))
                        .clickable { onClose() }
                )
            }
        }
    }
}

@Composable
private fun ConnectivityPill(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    activeColor: Color,
    glassColor: Color,
    glassBorder: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) activeColor.copy(alpha = 0.85f) else glassColor
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun VerticalControlSlider(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: Float,
    glassColor: Color,
    glassBorder: Color,
    activeColor: Color,
    onValueChange: (Float) -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = glassColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "${(value * 100).toInt()}%",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Slider(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                )
            )

            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

private data class QuickActionItem(
    val icon: ImageVector,
    val label: String,
    val isActive: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun QuickToggleIcon(
    action: QuickActionItem,
    glassColor: Color,
    glassBorder: Color,
    activeColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { action.onClick() }
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (action.isActive) activeColor else glassColor
                )
                .border(
                    1.dp,
                    if (action.isActive) activeColor else glassBorder,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = action.label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
