package com.example.ui.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.SupportedLocales
import com.example.model.UserSettings
import com.example.system.UpdateManager
import com.example.ui.common.WallpaperSelectorSection
import com.example.ui.system.UpdatesCenterContent
import com.example.ui.theme.LauncherPalette
import com.example.ui.theme.PaletteManager

/**
 * Secciones dedicadas de la configuración del Launcher.
 */
enum class SettingsSection {
    PERSONALIZATION,
    SYSTEM,
    UPDATES
}

@Composable
fun SettingsDialog(
    userSettings: UserSettings,
    activePalette: LauncherPalette,
    updateManager: UpdateManager? = null,
    firiAssistant: com.example.ai.FiriAssistant? = null,
    initialSection: SettingsSection = SettingsSection.PERSONALIZATION,
    onUpdateSettings: (UserSettings) -> Unit,
    onOpenPermissionsSetup: () -> Unit,
    onOpenLockCustomizer: () -> Unit = {},
    onLockScreenNow: () -> Unit = {},
    onOpenVolumeActivationGuide: () -> Unit = {},
    onPreviewBootAnimation: () -> Unit = {},
    onOpenUpdatesCenter: () -> Unit = {},
    onResetOnboarding: () -> Unit,
    onDismiss: () -> Unit
) {
    val isEs = userSettings.isSpanish
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val resolvedUpdateManager = remember(updateManager) {
        updateManager ?: UpdateManager(context.applicationContext, coroutineScope)
    }
    val brainTelemetry by firiAssistant?.brainTelemetry?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) }
    var currentSection by remember { mutableStateOf(initialSection) }

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
            color = activePalette.surface,
            border = BorderStroke(1.dp, activePalette.cardBorder),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEs) "Configuración de Launcher" else "Launcher Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = activePalette.onBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = activePalette.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Selector de Apartados Dedicados (Pestañas Segmentadas)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(activePalette.surfaceVariant.copy(alpha = 0.55f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SettingsSection.entries.forEach { section ->
                        val isSelected = currentSection == section
                        val (icon, label) = when (section) {
                            SettingsSection.PERSONALIZATION -> Icons.Default.Palette to (if (isEs) "Estilo" else "Style")
                            SettingsSection.SYSTEM -> Icons.Default.Tune to (if (isEs) "Sistema" else "System")
                            SettingsSection.UPDATES -> Icons.Default.SystemUpdate to (if (isEs) "Actualizar" else "Updates")
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) activePalette.primary else Color.Transparent)
                                .clickable { currentSection = section }
                                .padding(vertical = 8.dp)
                                .testTag("settings_tab_${section.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) activePalette.onPrimary else activePalette.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) activePalette.onPrimary else activePalette.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (currentSection) {
                    SettingsSection.PERSONALIZATION -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // ========================================================
                            // ASPECTO ESTILO IOS: MODO CLARO / OSCURO & TAMAÑO ICONOS (Image 1)
                            // ========================================================
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = activePalette.surfaceVariant),
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.dp, activePalette.cardBorder),
                                    modifier = Modifier.fillMaxWidth().testTag("settings_aspect_card")
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (userSettings.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                                contentDescription = null,
                                                tint = activePalette.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = if (isEs) "Aspecto del Sistema" else "System Appearance",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = activePalette.onBackground
                                                )
                                                Text(
                                                    text = if (isEs) "Alterna entre Modo Claro y Modo Oscuro" else "Toggle Light & Dark theme",
                                                    fontSize = 12.sp,
                                                    color = activePalette.onBackground.copy(alpha = 0.65f)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Switch Modo Claro / Modo Oscuro con preview visual
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Botón Modo Claro
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        onUpdateSettings(userSettings.copy(isDarkMode = false))
                                                    }
                                                    .testTag("settings_theme_light"),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7)),
                                                border = BorderStroke(
                                                    width = if (!userSettings.isDarkMode) 2.dp else 1.dp,
                                                    color = if (!userSettings.isDarkMode) Color(0xFF007AFF) else Color(0x30000000)
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.LightMode,
                                                        contentDescription = null,
                                                        tint = Color(0xFFFF9500),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = if (isEs) "Modo Claro" else "Light Mode",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1C1C1E)
                                                    )
                                                }
                                            }

                                            // Botón Modo Oscuro
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        onUpdateSettings(userSettings.copy(isDarkMode = true))
                                                    }
                                                    .testTag("settings_theme_dark"),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                                                border = BorderStroke(
                                                    width = if (userSettings.isDarkMode) 2.dp else 1.dp,
                                                    color = if (userSettings.isDarkMode) Color(0xFF0A84FF) else Color(0x30FFFFFF)
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DarkMode,
                                                        contentDescription = null,
                                                        tint = Color(0xFF0A84FF),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = if (isEs) "Modo Oscuro" else "Dark Mode",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Tamaño de Iconos y Texto (Image 1)
                                        Text(
                                            text = if (isEs) "Tamaño de Iconos y Texto" else "Icon & Text Size",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = activePalette.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(
                                                "NORMAL" to if (isEs) "Normal" else "Default",
                                                "MEDIUM" to if (isEs) "Mediano" else "Medium",
                                                "LARGE" to if (isEs) "Grande" else "Large"
                                            ).forEach { (sizeCode, label) ->
                                                val isSelected = userSettings.appIconSize == sizeCode
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            onUpdateSettings(userSettings.copy(appIconSize = sizeCode))
                                                        }
                                                        .testTag("settings_icon_size_${sizeCode.lowercase()}"),
                                                    color = if (isSelected) activePalette.primary else activePalette.surface,
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, if (isSelected) activePalette.primary else activePalette.cardBorder)
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) activePalette.onPrimary else activePalette.onBackground,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                        modifier = Modifier.padding(vertical = 8.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Palettes Section
                            item {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ColorLens,
                                    contentDescription = null,
                                    tint = activePalette.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEs) "Paleta Minimalista" else "Minimalist Palette",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activePalette.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(PaletteManager.allPalettes) { pal ->
                                    val isSelected = pal.id == userSettings.paletteId
                                    Card(
                                        modifier = Modifier
                                            .width(130.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                onUpdateSettings(userSettings.copy(paletteId = pal.id))
                                            }
                                            .testTag("settings_palette_${pal.id}"),
                                        colors = CardDefaults.cardColors(containerColor = pal.surface),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) pal.primary else pal.cardBorder
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(pal.primary))
                                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(pal.secondary))
                                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(pal.tertiary))
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = if (isEs) pal.nameEs else pal.nameEn,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = pal.onBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Language & Region Section
                    item {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = activePalette.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEs) "Idioma y Región" else "Language & Region",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activePalette.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // Language switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(activePalette.surfaceVariant)
                                    .padding(4.dp)
                            ) {
                                listOf("es" to "Español", "en" to "English").forEach { (code, label) ->
                                    val isSelected = userSettings.languageCode == code
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) activePalette.primary else activePalette.surfaceVariant)
                                            .clickable {
                                                onUpdateSettings(userSettings.copy(languageCode = code))
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) activePalette.onPrimary else activePalette.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Region selector row
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(SupportedLocales.regions) { region ->
                                    val isSelected = region.code == userSettings.regionCode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) activePalette.surfaceVariant else activePalette.surface)
                                            .border(
                                                1.dp,
                                                if (isSelected) activePalette.primary else activePalette.cardBorder,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                onUpdateSettings(
                                                    userSettings.copy(
                                                        regionCode = region.code,
                                                        languageCode = region.defaultLanguage
                                                    )
                                                )
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${region.flag} ${if (isEs) region.nameEs else region.nameEn}",
                                            fontSize = 12.sp,
                                            color = activePalette.onBackground
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Selector de Fondos de Pantalla de Inicio (Home)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = activePalette.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                WallpaperSelectorSection(
                                    title = if (isEs) "Fondo de Pantalla de Inicio" else "Home Screen Wallpaper",
                                    subtitle = if (isEs) "Elige tu fondo favorito o usa una imagen propia" else "Select wallpaper preset or pick custom photo",
                                    selectedWallpaperId = userSettings.homeWallpaperId,
                                    customWallpaperUri = userSettings.homeCustomWallpaperUri,
                                    palette = activePalette,
                                    isSpanish = isEs,
                                    onSelectPreset = {
                                        onUpdateSettings(userSettings.copy(homeWallpaperId = it, homeCustomWallpaperUri = ""))
                                    },
                                    onSelectCustomUri = {
                                        onUpdateSettings(userSettings.copy(homeCustomWallpaperUri = it))
                                    }
                                )
                            }
                        }
                    }

                    // Selector de Fondos de Pantalla de Bloqueo (Lock Screen)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = activePalette.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                WallpaperSelectorSection(
                                    title = if (isEs) "Fondo de Pantalla de Bloqueo" else "Lock Screen Wallpaper",
                                    subtitle = if (isEs) "Personaliza la imagen que ves al encender el móvil" else "Customize the image shown on lock screen",
                                    selectedWallpaperId = userSettings.lockWallpaperId,
                                    customWallpaperUri = userSettings.lockCustomWallpaperUri,
                                    palette = activePalette,
                                    isSpanish = isEs,
                                    onSelectPreset = {
                                        onUpdateSettings(userSettings.copy(lockWallpaperId = it, lockCustomWallpaperUri = ""))
                                    },
                                    onSelectCustomUri = {
                                        onUpdateSettings(userSettings.copy(lockCustomWallpaperUri = it))
                                    }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Botón dedicado para abrir Personalizador de Bloqueo (Reloj, Tipografía, Mini Widgets, Seguridad)
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onOpenLockCustomizer()
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("settings_open_lock_customizer_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = activePalette.primary,
                                        contentColor = activePalette.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEs) "Personalizar Reloj, Widgets y PIN de Bloqueo" else "Customize Lock Clock, Widgets & PIN",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onLockScreenNow()
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("settings_test_lock_screen_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEs) "Probar Pantalla de Bloqueo Ahora" else "Test Lock Screen Now",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SettingsSection.SYSTEM -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ========================================================
                    // INTERRUPTOR PRINCIPAL: APAGAR / ENCENDER A FIRI
                    // ========================================================
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (userSettings.isFiriEnabled)
                                    activePalette.surfaceVariant
                                else
                                    Color(0xFFEF4444).copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (userSettings.isFiriEnabled) activePalette.primary.copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (userSettings.isFiriEnabled)
                                                        activePalette.primary.copy(alpha = 0.2f)
                                                    else
                                                        Color(0xFFEF4444).copy(alpha = 0.2f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.firi_app_icon),
                                                contentDescription = "Firi Icon",
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape),
                                                alpha = if (userSettings.isFiriEnabled) 1f else 0.45f
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = if (isEs) "Asistente Firi AI" else "Firi AI Assistant",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = activePalette.onBackground
                                            )
                                            Text(
                                                text = if (userSettings.isFiriEnabled) {
                                                    if (isEs) "Encendida (Activa y lista)" else "Turned On (Active & ready)"
                                                } else {
                                                    if (isEs) "Apagada (Desactivada)" else "Turned Off (Disabled)"
                                                },
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (userSettings.isFiriEnabled) Color(0xFF10B981) else Color(0xFFEF4444)
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = userSettings.isFiriEnabled,
                                        onCheckedChange = { isEnabled ->
                                            onUpdateSettings(userSettings.copy(isFiriEnabled = isEnabled))
                                        },
                                        modifier = Modifier.testTag("firi_assistant_enable_switch"),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = activePalette.onPrimary,
                                            checkedTrackColor = activePalette.primary,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = Color(0xFFEF4444).copy(alpha = 0.6f)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (userSettings.isFiriEnabled) {
                                        if (isEs)
                                            "Firi responderá a tus preguntas por voz, texto y ejecutará acciones del sistema desde el orbe y el dock."
                                        else
                                            "Firi will respond to voice, text queries, and system commands via orb and dock."
                                    } else {
                                        if (isEs)
                                            "Firi está completamente apagada. No escuchará audio, no responderá en el dock ni consumirá recursos en segundo plano."
                                        else
                                            "Firi is completely turned off. It will not listen to audio, respond in the dock, or consume background resources."
                                    },
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = activePalette.onSurface.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }

                    // Pantalla de Bloqueo y Centro de Control (Permisos y Setup)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = activePalette.surfaceVariant),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = activePalette.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isEs) "Bloqueo y Centro de Control" else "Lock & Control Center",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = activePalette.onBackground
                                        )
                                        Text(
                                            text = if (isEs) "Quitar bloqueo del sistema y permisos de brillo/volumen" else "Remove system lock & permissions setup",
                                            fontSize = 11.sp,
                                            color = activePalette.onSurface.copy(alpha = 0.65f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        onDismiss()
                                        onOpenPermissionsSetup()
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("open_permissions_setup_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = activePalette.primary,
                                        contentColor = activePalette.onPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEs) "Configurar Permisos y Bloqueo" else "Setup Permissions & Lock",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Spacer(modifier = Modifier.height(10.dp))

                                val isAccessibilityActive = com.example.service.VolumeAccessibilityService.isAccessibilityEnabled(context)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isAccessibilityActive) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    ),
                                    border = BorderStroke(1.dp, if (isAccessibilityActive) Color(0xFF10B981) else Color(0xFFF59E0B)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = null,
                                                    tint = if (isAccessibilityActive) Color(0xFF10B981) else Color(0xFFF59E0B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isEs) "Barra Global (TikTok, YouTube)" else "Global Volume (All Apps)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = activePalette.onBackground
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isAccessibilityActive) Color(0xFF10B981) else Color(0xFFF59E0B))
                                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = if (isAccessibilityActive) (if (isEs) "Activo" else "Active") else (if (isEs) "Inactivo" else "Inactive"),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isAccessibilityActive)
                                                (if (isEs) "La barra de volumen de Firi reemplaza la del sistema en cualquier aplicación." else "Firi's volume bar replaces the system sound bar in all apps.")
                                            else
                                                (if (isEs) "Para que la barra aparezca sobre TikTok, YouTube y todas las apps, activa el servicio de Firi en Accesibilidad." else "To show the volume bar in TikTok, YouTube and other apps, enable Firi in Accessibility settings."),
                                            fontSize = 10.5.sp,
                                            color = activePalette.onSurface.copy(alpha = 0.8f)
                                        )

                                        if (!isAccessibilityActive) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    onOpenVolumeActivationGuide()
                                                },
                                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isEs) "Ver Guía y Activar en Accesibilidad" else "View Steps & Enable in Settings",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        val am = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
                                        val cur = am?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: 5
                                        val max = am?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 15
                                        val frac = if (max > 0) cur.toFloat() / max.toFloat() else 0.5f
                                        com.example.service.VolumeAccessibilityService.showVolume(context, frac)
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("test_volume_bar_btn"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEs) "Probar Barra de Volumen Fusion OS" else "Test Fusion OS Volume Bar",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Voice Output Switch
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = activePalette.surfaceVariant),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = activePalette.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isEs) "Respuesta por voz de Firi" else "Firi Voice Response",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = activePalette.onBackground
                                        )
                                        Text(
                                            text = if (isEs) "Lee en voz alta las respuestas" else "Speaks responses aloud",
                                            fontSize = 11.sp,
                                            color = activePalette.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Switch(
                                    checked = userSettings.voiceOutputEnabled,
                                    onCheckedChange = {
                                        onUpdateSettings(userSettings.copy(voiceOutputEnabled = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = activePalette.onPrimary,
                                        checkedTrackColor = activePalette.primary,
                                        uncheckedThumbColor = activePalette.onSurface.copy(alpha = 0.6f),
                                        uncheckedTrackColor = activePalette.surface
                                    )
                                )
                            }
                        }
                    }

                    // ========================================================
                    // SEGUNDO CEREBRO DE FIRI (PROCESADOR LOCAL ON-DEVICE)
                    // ========================================================
                    if (firiAssistant != null && brainTelemetry != null) {
                        val telemetry = brainTelemetry!!
                        val isLocal = telemetry.activeBrain == com.example.ai.FiriBrainMode.ON_DEVICE_CPU

                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isLocal)
                                        Color(0xFF10B981).copy(alpha = 0.12f)
                                    else
                                        activePalette.surfaceVariant
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isLocal) Color(0xFF10B981).copy(alpha = 0.4f) else activePalette.cardBorder
                                ),
                                shape = RoundedCornerShape(14.dp)
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
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isLocal) Color(0xFF10B981).copy(alpha = 0.2f) else activePalette.primary.copy(alpha = 0.2f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = if (isLocal) Color(0xFF10B981) else activePalette.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = if (isEs) "Segundo Cerebro (Procesador)" else "Second Brain (On-Device CPU)",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = activePalette.onBackground
                                                )
                                                Text(
                                                    text = if (isEs)
                                                        "Funciona 100% sin internet usando tu CPU"
                                                    else
                                                        "Runs 100% offline using your CPU",
                                                    fontSize = 10.5.sp,
                                                    color = if (isLocal) Color(0xFF10B981) else activePalette.onSurface.copy(alpha = 0.65f)
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isLocal) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF3B82F6).copy(alpha = 0.2f)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (isLocal)
                                                    (if (isEs) "Offline Activo" else "Offline Active")
                                                else
                                                    (if (isEs) "Nube Online" else "Cloud Online"),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isLocal) Color(0xFF10B981) else Color(0xFF3B82F6)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = if (isEs)
                                            "Firi cuenta con un cerebro secundario embebido en los ${telemetry.processorCores} núcleos del procesador de tu móvil. Al no haber internet o activar el modo avión, Firi responde al instante (latencia ~${telemetry.lastExecutionTimeMs}ms) para abrir apps, resolver cálculos y mantener tu privacidad sin enviar datos a la red."
                                        else
                                            "Firi has a secondary brain running on your phone's ${telemetry.processorCores} CPU cores. When offline or in airplane mode, Firi answers instantly (~${telemetry.lastExecutionTimeMs}ms) to launch apps, solve math, and protect your privacy.",
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = activePalette.onSurface.copy(alpha = 0.8f)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isEs) "Forzar modo sin internet (Simular CPU)" else "Force offline mode (Simulate CPU)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = activePalette.onBackground
                                        )

                                        Switch(
                                            checked = telemetry.forceOfflineMode,
                                            onCheckedChange = { firiAssistant.toggleForceOfflineMode(it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color(0xFF10B981)
                                            ),
                                            modifier = Modifier.testTag("force_offline_brain_switch")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Acceso directo a la pestaña dedicada de Actualizaciones
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = activePalette.surfaceVariant),
                            border = BorderStroke(1.dp, activePalette.primary.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(activePalette.primary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdate,
                                            contentDescription = null,
                                            tint = activePalette.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isEs) "Apartado de Actualizaciones" else "Updates Section",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = activePalette.onBackground
                                        )
                                        Text(
                                            text = if (isEs)
                                                "Soporte 5 años Android 12 (2026-2031) · Auto-actualización"
                                            else
                                                "5-Year Android 12 support (2026-2031)",
                                            fontSize = 10.5.sp,
                                            color = activePalette.onSurface.copy(alpha = 0.65f)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { currentSection = SettingsSection.UPDATES },
                                    modifier = Modifier.testTag("switch_to_updates_tab_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = activePalette.primary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (isEs) "Abrir" else "Open",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activePalette.onPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Ver Animación de Inicio (Código + Fusion Estudio)
                    item {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onPreviewBootAnimation()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("preview_boot_animation_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, activePalette.primary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = activePalette.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEs) "Ver Animación de Inicio (Código + Fusion)" else "Preview Boot Animation (Code + Fusion)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Re-run Onboarding
                    item {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onResetOnboarding()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("restart_onboarding_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, activePalette.cardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = activePalette.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEs) "Repetir Asistente de Configuración" else "Restart Setup Wizard",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // APARTADO DEDICADO DE ACTUALIZACIONES EN LA CONFIGURACIÓN DEL LAUNCHER
            SettingsSection.UPDATES -> {
                UpdatesCenterContent(
                    updateManager = resolvedUpdateManager,
                    palette = activePalette,
                    userSettings = userSettings,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
}
}
