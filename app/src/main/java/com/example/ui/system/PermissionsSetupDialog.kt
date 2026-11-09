package com.example.ui.system

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.model.UserSettings
import com.example.system.SystemControlManager
import com.example.ui.theme.LauncherPalette

@Composable
fun PermissionsSetupDialog(
    systemControlManager: SystemControlManager,
    palette: LauncherPalette,
    userSettings: UserSettings,
    onTestLockScreen: () -> Unit,
    onTestControlCenter: () -> Unit,
    onOpenVolumeActivationGuide: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val isEs = userSettings.isSpanish
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permisos reactivos que se refrescan al volver de la pantalla de ajustes
    var hasOverlayPermission by remember { mutableStateOf(systemControlManager.canDrawOverlays()) }
    var hasWriteSettingsPermission by remember { mutableStateOf(systemControlManager.canWriteSettings()) }
    var hasDndPermission by remember { mutableStateOf(systemControlManager.hasNotificationPolicyAccess()) }
    var hasAccessibilityPermission by remember { mutableStateOf(systemControlManager.isAccessibilityServiceEnabled()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = systemControlManager.canDrawOverlays()
                hasWriteSettingsPermission = systemControlManager.canWriteSettings()
                hasDndPermission = systemControlManager.hasNotificationPolicyAccess()
                hasAccessibilityPermission = systemControlManager.isAccessibilityServiceEnabled()
                systemControlManager.refreshSettingsState()
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
                                .background(palette.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isEs) "Permisos y Bloqueo" else "Lock & Control Permissions",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.onBackground
                            )
                            Text(
                                text = if (isEs) "Pantalla de bloqueo y Centro de control" else "Lock Screen and Control Center setup",
                                fontSize = 11.sp,
                                color = palette.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = palette.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Paso 1: Quitar bloqueo de pantalla del sistema
                    item {
                        PermissionStepCard(
                            palette = palette,
                            icon = Icons.Default.Lock,
                            title = if (isEs) "1. Quitar Bloqueo del Sistema" else "1. Disable System Lock Screen",
                            description = if (isEs)
                                "Para evitar que tu teléfono te pida desbloquear dos veces (primero Android y luego Firi), ve a Seguridad -> Bloqueo de pantalla y selecciona 'Ninguno'."
                            else
                                "To prevent double unlocking, go to Security -> Screen lock and select 'None' or 'Swipe'.",
                            badgeText = if (isEs) "Recomendado" else "Recommended",
                            badgeColor = Color(0xFF3B82F6),
                            actionText = if (isEs) "Ir a Seguridad" else "Open Security",
                            onAction = { systemControlManager.openSecuritySettings() }
                        )
                    }

                    // Paso 2: Mostrar encima de otras apps (Overlay)
                    item {
                        PermissionStepCard(
                            palette = palette,
                            icon = Icons.Default.Layers,
                            title = if (isEs) "2. Superposición sobre otras apps" else "2. Display Over Other Apps",
                            description = if (isEs)
                                "Permite que la Barra de Volumen estilo Fusion OS aparezca sobre cualquier app (TikTok, YouTube, Instagram) al presionar los botones de volumen, y que la Pantalla de Bloqueo funcione globalmente."
                            else
                                "Allows the Fusion OS Volume Bar to show over any app (TikTok, YouTube, Instagram) on volume key presses, and enables global Lock Screen.",
                            badgeText = if (hasOverlayPermission) (if (isEs) "Concedido" else "Granted") else (if (isEs) "Requerido" else "Required"),
                            badgeColor = if (hasOverlayPermission) Color(0xFF10B981) else Color(0xFFF59E0B),
                            actionText = if (hasOverlayPermission) (if (isEs) "Configurado" else "Ready") else (if (isEs) "Conceder Permiso" else "Grant Permission"),
                            onAction = { systemControlManager.openOverlaySettings() }
                        )
                    }

                    // Paso 3: Reemplazo Global de Barra de Volumen (Accesibilidad)
                    item {
                        PermissionStepCard(
                            palette = palette,
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            title = if (isEs) "3. Reemplazo de Barra de Volumen (TikTok, YouTube, etc.)" else "3. Global Volume Bar Replacement",
                            description = if (isEs)
                                "Permite que al presionar los botones físicos de volumen en CUALQUIER app (TikTok, YouTube, Instagram), Firi intercepte las teclas y oculte la barra de sonido del sistema para poner la de Firi."
                            else
                                "Allows Firi to intercept hardware volume keys in ANY app (TikTok, YouTube, Instagram) and replace the system volume dialog with Firi's.",
                            badgeText = if (hasAccessibilityPermission) (if (isEs) "Activado" else "Active") else (if (isEs) "Recomendado" else "Recommended"),
                            badgeColor = if (hasAccessibilityPermission) Color(0xFF10B981) else Color(0xFF3B82F6),
                            actionText = if (hasAccessibilityPermission) (if (isEs) "Configurado" else "Ready") else (if (isEs) "Activar en Accesibilidad" else "Enable in Settings"),
                            onAction = {
                                onOpenVolumeActivationGuide()
                            }
                        )
                    }

                    // Paso 4: Modificar ajustes del sistema (Brillo y volumen)
                    item {
                        PermissionStepCard(
                            palette = palette,
                            icon = Icons.Default.Tune,
                            title = if (isEs) "4. Modificar Ajustes del Sistema" else "4. Modify System Settings",
                            description = if (isEs)
                                "Permite que los deslizadores del Centro de Control cambien el brillo de la pantalla, la rotación automática y los tonos de llamada."
                            else
                                "Allows the Control Center sliders to adjust screen brightness, volume and auto-rotation.",
                            badgeText = if (hasWriteSettingsPermission) (if (isEs) "Concedido" else "Granted") else (if (isEs) "Requerido" else "Required"),
                            badgeColor = if (hasWriteSettingsPermission) Color(0xFF10B981) else Color(0xFFF59E0B),
                            actionText = if (hasWriteSettingsPermission) (if (isEs) "Configurado" else "Ready") else (if (isEs) "Conceder Permiso" else "Grant Permission"),
                            onAction = { systemControlManager.openWriteSettings() }
                        )
                    }

                    // Paso 5: Modo No Molestar
                    item {
                        PermissionStepCard(
                            palette = palette,
                            icon = Icons.Default.NotificationsActive,
                            title = if (isEs) "5. Control de 'No Molestar'" else "5. Do Not Disturb Control",
                            description = if (isEs)
                                "Permite silenciar llamadas y notificaciones desde el botón del Centro de Control."
                            else
                                "Allows muting alerts and toggling Do Not Disturb from the Control Center.",
                            badgeText = if (hasDndPermission) (if (isEs) "Concedido" else "Granted") else (if (isEs) "Opcional" else "Optional"),
                            badgeColor = if (hasDndPermission) Color(0xFF10B981) else Color(0xFF6B7280),
                            actionText = if (hasDndPermission) (if (isEs) "Configurado" else "Ready") else (if (isEs) "Configurar" else "Setup"),
                            onAction = { systemControlManager.openNotificationPolicySettings() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botones rápidos de prueba
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onTestLockScreen()
                        },
                        modifier = Modifier.weight(1f).testTag("test_lockscreen_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, palette.cardBorder),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEs) "Bloqueo" else "Lock", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onTestControlCenter()
                        },
                        modifier = Modifier.weight(1f).testTag("test_control_center_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, palette.cardBorder),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEs) "Centro Control" else "Control", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            com.example.service.VolumeOverlayService.start(context)
                            val testIntent = android.content.Intent(context, com.example.service.VolumeOverlayService::class.java).apply {
                                action = com.example.service.VolumeOverlayService.ACTION_SHOW_TEST
                            }
                            context.startService(testIntent)
                        },
                        modifier = Modifier.weight(1f).testTag("test_volume_bar_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, palette.primary),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = palette.primary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEs) "Barra Volumen" else "Volume Bar", fontSize = 11.sp, color = palette.primary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag("permissions_done_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (isEs) "Guardar y Continuar" else "Save & Continue", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PermissionStepCard(
    palette: LauncherPalette,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    badgeText: String,
    badgeColor: Color,
    actionText: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = palette.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, palette.cardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.onBackground
                    )
                }

                // Badge estado
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                fontSize = 11.5.sp,
                color = palette.onSurface.copy(alpha = 0.7f),
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = actionText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
