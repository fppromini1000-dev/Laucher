package com.example.ui.system

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.UserSettings
import com.example.system.UpdateManager
import com.example.ui.theme.LauncherPalette

/**
 * Apartado oficial de actualizaciones de Firi Launcher (CometOS Edition).
 *
 * Implementa:
 * - Ciclo de vida de 5 años de actualizaciones para el estándar Android 12 (2026 a 2031).
 * - Bloqueo de actualizaciones y aviso prominente al llegar al año 2031 para Android 12.
 * - Motor de auto-actualización silenciosa que se actualiza por sí solo en segundo plano.
 * - Controles de simulación interactiva para probar el año 2031 y el SO Android 12 en vivo.
 */
@Composable
fun UpdatesCenterDialog(
    updateManager: UpdateManager,
    palette: LauncherPalette,
    userSettings: UserSettings,
    onDismiss: () -> Unit
) {
    val isEs = userSettings.isSpanish
    val state by updateManager.state.collectAsStateWithLifecycle()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp)
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
                // Cabecera del Centro de Actualizaciones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF0284C7), Color(0xFF6366F1))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isEs) "Actualizaciones del Sistema" else "System Updates",
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.onBackground
                            )
                            Text(
                                text = "Firi Launcher · Fusion OS OTA v3.0.9",
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

                UpdatesCenterContent(
                    updateManager = updateManager,
                    palette = palette,
                    userSettings = userSettings,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("close_updates_center_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isEs) "Entendido" else "Close",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Apartado dedicado de actualizaciones embebible directamente en la Configuración del Launcher o en diálogo.
 */
@Composable
fun UpdatesCenterContent(
    updateManager: UpdateManager,
    palette: LauncherPalette,
    userSettings: UserSettings,
    modifier: Modifier = Modifier
) {
    val isEs = userSettings.isSpanish
    val context = LocalContext.current
    val state by updateManager.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ========================================================
        // 1. AVISO CRÍTICO: FIN DE SOPORTE ANDROID 12 EN EL AÑO 2031
        // ========================================================
                    AnimatedVisibility(
                        visible = state.isSupportExpired,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("support_expired_warning_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFDC2626).copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isEs) "Dispositivo Ya No Compatible" else "Device No Longer Compatible",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444)
                                        )
                                        Text(
                                            text = if (isEs) "Ciclo de 5 años finalizado (Año 2031)" else "5-Year Lifecycle Ended (Year 2031)",
                                            fontSize = 10.sp,
                                            color = palette.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (isEs)
                                        "Aviso: Tu dispositivo (Android 12) ya no es compatible con las próximas actualizaciones. El ciclo oficial de 5 años de soporte finalizó en el año 2031. Esta versión de Firi Launcher permanecerá activa y estable, pero no recibirá nuevas funciones ni parches futuros."
                                    else
                                        "Notice: Your device (Android 12) is no longer compatible with upcoming updates. The official 5-year support lifecycle concluded in the year 2031. Your installed version will remain stable but will not receive new features or patches.",
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    color = palette.onBackground
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isEs) "Requiere Android 13+ para futuras versiones" else "Requires Android 13+ for future versions",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }

                    // ========================================================
                    // 2. CARD DE TELEMETRÍA Y CICLO DE 5 AÑOS DE SOPORTE
                    // ========================================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = palette.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, palette.cardBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Smartphone,
                                        contentDescription = null,
                                        tint = palette.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEs) "Dispositivo y Soporte" else "Device & Support Tier",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.onBackground
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (state.isSupportExpired)
                                                Color(0xFFEF4444).copy(alpha = 0.2f)
                                            else
                                                Color(0xFF10B981).copy(alpha = 0.2f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (state.isSupportExpired)
                                            (if (isEs) "Soporte Vencido" else "Expired")
                                        else
                                            (if (isEs) "Soporte Activo" else "Active Support"),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.isSupportExpired) Color(0xFFEF4444) else Color(0xFF10B981)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            TelemetryRow(
                                label = if (isEs) "Versión de Launcher" else "Launcher Version",
                                value = state.currentVersion,
                                valueColor = palette.primary
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            TelemetryRow(
                                label = if (isEs) "Sistema Operativo" else "Operating System",
                                value = state.deviceOsName,
                                valueColor = palette.onBackground
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            TelemetryRow(
                                label = if (isEs) "Ciclo Garantizado" else "Lifecycle Window",
                                value = "5 años (2026 — 2031)",
                                valueColor = palette.onSurface.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            TelemetryRow(
                                label = if (isEs) "Año en el Sistema" else "System Year",
                                value = "${state.currentYear} ${if (state.isSimulatingYear2031) "(Simulado)" else ""}",
                                valueColor = if (state.currentYear >= 2031) Color(0xFFEF4444) else palette.onSurface
                            )
                        }
                    }

                    // ========================================================
                    // 3. CARD DE AUTO-ACTUALIZACIÓN ("que se actualice solo")
                    // ========================================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = palette.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, palette.cardBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isEs) "Actualizar Automáticamente" else "Auto-Update Automatically",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.onBackground
                                        )
                                        Text(
                                            text = if (isEs) "El launcher se actualiza solo en segundo plano" else "Launcher updates itself silently in background",
                                            fontSize = 10.5.sp,
                                            color = palette.onSurface.copy(alpha = 0.65f)
                                        )
                                    }
                                }

                                Switch(
                                    checked = state.autoUpdateEnabled,
                                    onCheckedChange = { updateManager.setAutoUpdateEnabled(it) },
                                    enabled = !state.isSupportExpired,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF10B981)
                                    ),
                                    modifier = Modifier.testTag("auto_update_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = palette.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isEs)
                                        "Última comprobación: ${state.lastCheckedTimestamp} (Vía Wi-Fi)"
                                    else
                                        "Last checked: ${state.lastCheckedTimestamp} (Via Wi-Fi)",
                                    fontSize = 10.sp,
                                    color = palette.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // ========================================================
                    // REPOSITORIO OFICIAL GOOGLE DRIVE (firi_launcher.apk v3.0.9)
                    // ========================================================
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gdrive_repo_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = palette.surfaceVariant.copy(alpha = 0.55f)
                        ),
                        border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isEs) "Repositorio Oficial Fusion OS" else "Official Fusion OS Repo",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.onBackground
                                    )
                                    Text(
                                        text = "Google Drive · firi_launcher.apk (v3.0.9)",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (isEs)
                                    "El launcher descarga la nueva versión v3.0.9 desde Google Drive (firi_launcher.apk), ejecuta el instalador del paquete y tú solo confirmas el permiso en pantalla."
                                else
                                    "Fetches version v3.0.9 from Google Drive (firi_launcher.apk), executes package installer, and you simply grant permission on screen.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = palette.onSurface.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            val hasInstallPerm = updateManager.canInstallApks(context)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { updateManager.openGoogleDriveFolder(context) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_gdrive_folder_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInBrowser,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = palette.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEs) "Abrir Drive" else "Open Drive",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.primary
                                    )
                                }

                                OutlinedButton(
                                    onClick = { updateManager.openInstallPermissionSettings(context) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("request_install_perm_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (hasInstallPerm) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFF59E0B)
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (hasInstallPerm) Icons.Default.CheckCircle else Icons.Default.Security,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (hasInstallPerm) Color(0xFF10B981) else Color(0xFFF59E0B)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (hasInstallPerm)
                                            (if (isEs) "Permiso Activo" else "Perm Active")
                                        else
                                            (if (isEs) "Dar Permiso" else "Grant Perm"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (hasInstallPerm) Color(0xFF10B981) else Color(0xFFF59E0B)
                                    )
                                }
                            }
                        }
                    }

                    // ========================================================
                    // GUÍA: SOLUCIÓN A "ARCHIVO FALSO" / PLAY PROTECT / PERMISOS
                    // ========================================================
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apk_install_help_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = palette.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, palette.cardBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(palette.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = palette.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isEs) "Guía de Instalación Segura APK" else "Safe APK Install Guide",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.onBackground
                                    )
                                    Text(
                                        text = if (isEs) "Solución a avisos de seguridad en Android" else "Fix system security prompts",
                                        fontSize = 10.sp,
                                        color = palette.onSurface.copy(alpha = 0.65f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (isEs)
                                    "1. Permiso de orígenes: Toca 'Dar Permiso' para autorizar a Firi Launcher a instalar paquetes.\n" +
                                    "2. Paquete auténtico: El archivo firi_launcher.apk (v3.0.9) cuenta con firma oficial de Fusion OS y estructura binaria verificada.\n" +
                                    "3. Si Google Play Protect muestra 'Desarrollador desconocido', toca 'Más detalles' e 'Instalar de todas formas'."
                                else
                                    "1. Unknown sources: Tap 'Grant Perm' to allow Firi Launcher to install updates.\n" +
                                    "2. Authentic package: firi_launcher.apk (v3.0.9) is signed with Fusion OS key and binary integrity.\n" +
                                    "3. If Play Protect flags unknown developer, tap 'More details' and 'Install anyway'.",
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = palette.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }

                    // ========================================================
                    // 4. ACCIÓN DE ACTUALIZACIÓN & PROGRESO DE INSTALACIÓN
                    // ========================================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.isUpdateInstalled)
                                Color(0xFF10B981).copy(alpha = 0.12f)
                            else
                                palette.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (state.isUpdateInstalled) Color(0xFF10B981) else palette.cardBorder
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Mensaje de estado dinámico
                            if (state.statusMessage.isNotEmpty()) {
                                Text(
                                    text = state.statusMessage,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (state.isSupportExpired)
                                        Color(0xFFEF4444)
                                    else if (state.isUpdateInstalled)
                                        Color(0xFF059669)
                                    else
                                        palette.onBackground,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Barra de progreso si está descargando o instalando
                            if (state.isDownloading || state.isInstalling) {
                                LinearProgressIndicator(
                                    progress = { state.downloadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = Color(0xFF0284C7),
                                    trackColor = palette.surfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (!state.isSupportExpired) {
                                Button(
                                    onClick = {
                                        if (state.isUpdateInstalled) {
                                            updateManager.executeApkInstall(context)
                                        } else {
                                            updateManager.checkAndInstallUpdates(manual = true)
                                        }
                                    },
                                    enabled = !state.isChecking && !state.isDownloading && !state.isInstalling,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("check_updates_now_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (state.isUpdateInstalled) Color(0xFF10B981) else Color(0xFF0284C7)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (state.isChecking || state.isDownloading || state.isInstalling) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (state.isDownloading)
                                                (if (isEs) "Descargando firi_launcher.apk (v3.0.9)..." else "Downloading firi_launcher.apk (v3.0.9)...")
                                            else if (state.isInstalling)
                                                (if (isEs) "Ejecutando Instalador de Paquete..." else "Launching Package Installer...")
                                            else
                                                (if (isEs) "Buscando en Google Drive..." else "Searching in Google Drive..."),
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (state.isUpdateInstalled) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (state.isUpdateInstalled)
                                                (if (isEs) "¡Instalar APK v3.0.9 Ahora! (Abrir Instalador)" else "Install APK v3.0.9 Now (Launch Installer)")
                                            else
                                                (if (isEs) "Descargar y Ejecutar firi_launcher.apk (v3.0.9)" else "Download & Install firi_launcher.apk (v3.0.9)"),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isEs)
                                            "Actualizaciones bloqueadas permanentemente para Android 12 (Año 2031)"
                                        else
                                            "Updates permanently locked for Android 12 (Year 2031)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }

                    // ========================================================
                    // 5. BANEL DE SIMULACIÓN Y PRUEBA (Para verificar el año 2031)
                    // ========================================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF3B82F6).copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEs) "Simulación de Políticas y Pruebas" else "Policy Simulation & Testing",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3B82F6)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isEs)
                                    "Activa estos conmutadores para verificar en tiempo real las reglas de 5 años y el aviso del año 2031 en Android 12:"
                                else
                                    "Toggle these switches to test the 5-year lifecycle and 2031 Android 12 notice immediately:",
                                fontSize = 10.5.sp,
                                color = palette.onSurface.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Switch 1: Simular Año 2031
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEs) "Simular Año 2031 (Fin de Soporte)" else "Simulate Year 2031",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.onBackground
                                    )
                                    Text(
                                        text = if (isEs) "Activa el límite de 5 años para Android 12" else "Enforces 5-year limit for Android 12",
                                        fontSize = 9.5.sp,
                                        color = palette.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Switch(
                                    checked = state.isSimulatingYear2031,
                                    onCheckedChange = { updateManager.toggleSimulateYear2031(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFEF4444)
                                    ),
                                    modifier = Modifier.testTag("simulate_year_2031_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Switch 2: Simular Dispositivo Android 12
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEs) "Simular Android 12 (API 31)" else "Simulate Android 12 (API 31)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.onBackground
                                    )
                                    Text(
                                        text = if (isEs) "Fuerza la detección del SO como Android 12" else "Forces OS telemetry detection as Android 12",
                                        fontSize = 9.5.sp,
                                        color = palette.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Switch(
                                    checked = state.isSimulatingAndroid12,
                                    onCheckedChange = { updateManager.toggleSimulateAndroid12(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF3B82F6)
                                    ),
                                    modifier = Modifier.testTag("simulate_android_12_switch")
                                )
                            }
                        }
                    }

                    // ========================================================
                    // 6. HISTORIAL DE NOVEDADES (CHANGELOG)
                    // ========================================================
                    Text(
                        text = if (isEs) "Historial de Novedades" else "Release Notes",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primary
                    )

                    updateManager.changelog.forEach { release ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = palette.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = BorderStroke(1.dp, palette.cardBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = release.version,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primary
                                    )
                                    Text(
                                        text = release.releaseDate,
                                        fontSize = 10.sp,
                                        color = palette.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = release.title,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                release.highlights.forEach { bullet ->
                                    Text(
                                        text = "• $bullet",
                                        fontSize = 10.sp,
                                        color = palette.onSurface.copy(alpha = 0.75f),
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
    }

@Composable
private fun TelemetryRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.65f)
        )
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}
