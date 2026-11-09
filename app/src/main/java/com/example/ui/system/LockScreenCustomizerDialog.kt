package com.example.ui.system

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwipeVertical
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import android.widget.Toast
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.UserSettings
import com.example.ui.common.WallpaperBackground
import com.example.ui.common.WallpaperSelectorSection
import com.example.ui.theme.LauncherPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LockScreenCustomizerDialog(
    userSettings: UserSettings,
    palette: LauncherPalette,
    onSaveSettings: (UserSettings) -> Unit,
    onSaveAndLock: ((UserSettings) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEs = userSettings.isSpanish

    var selectedFont by remember(userSettings.lockClockFont) { mutableStateOf(userSettings.lockClockFont) }
    var selectedColor by remember(userSettings.lockClockColor) { mutableStateOf(userSettings.lockClockColor) }
    val selectedMiniWidgets = remember(userSettings.lockMiniWidgets) {
        mutableStateListOf<String>().apply { addAll(userSettings.lockMiniWidgets) }
    }
    var selectedSecurityType by remember(userSettings.lockSecurityType) { mutableStateOf(userSettings.lockSecurityType) }
    var pinCode by remember(userSettings.lockPinCode) { mutableStateOf(userSettings.lockPinCode) }
    var patternCode by remember(userSettings.lockPatternCode) { mutableStateOf(userSettings.lockPatternCode) }
    var selectedWallpaper by remember(userSettings.lockWallpaperId) { mutableStateOf(userSettings.lockWallpaperId) }
    var customWallpaperUri by remember(userSettings.lockCustomWallpaperUri) { mutableStateOf(userSettings.lockCustomWallpaperUri) }

    var isSettingUpPin by remember { mutableStateOf(false) }
    var isSettingUpPattern by remember { mutableStateOf(false) }

    var isVerifyingCurrentSecurity by remember { mutableStateOf(false) }
    var pendingSecurityAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var currentSecurityVerifyTitle by remember { mutableStateOf("") }

    fun requireCurrentSecurityIfConfigured(title: String, onVerified: () -> Unit) {
        val hasActiveCurrentSecurity = (userSettings.lockSecurityType == "PIN" && userSettings.lockPinCode.isNotEmpty()) ||
                (userSettings.lockSecurityType == "PATTERN" && userSettings.lockPatternCode.isNotEmpty())
        if (hasActiveCurrentSecurity) {
            currentSecurityVerifyTitle = title
            pendingSecurityAction = onVerified
            isVerifyingCurrentSecurity = true
        } else {
            onVerified()
        }
    }

    // Parse color and font for live preview
    val parsedClockColor = remember(selectedColor) {
        try {
            Color(android.graphics.Color.parseColor(selectedColor))
        } catch (_: Exception) {
            Color.White
        }
    }

    val (previewFontFamily, previewFontWeight) = remember(selectedFont) {
        when (selectedFont) {
            "SERIF" -> Pair(FontFamily.Serif, FontWeight.Normal)
            "TECH" -> Pair(FontFamily.Monospace, FontWeight.SemiBold)
            "ROUNDED" -> Pair(FontFamily.Default, FontWeight.Bold)
            "THIN" -> Pair(FontFamily.Default, FontWeight.Light)
            else -> Pair(FontFamily.Default, FontWeight.ExtraBold) // BOLD
        }
    }

    val hasActiveSecurity = userSettings.lockSecurityType in listOf("PIN", "PATTERN") &&
            (userSettings.lockPinCode.isNotEmpty() || userSettings.lockPatternCode.isNotEmpty())
    var isAuthenticated by remember { mutableStateOf(!hasActiveSecurity) }

    if (!isAuthenticated) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
            ) {
                if (userSettings.lockSecurityType == "PIN") {
                    PinKeypadView(
                        correctPin = userSettings.lockPinCode.ifEmpty { "1234" },
                        title = if (isEs) "Introduce tu PIN para editar" else "Enter your PIN to edit",
                        onSuccess = { isAuthenticated = true },
                        onCancel = onDismiss
                    )
                } else {
                    PatternDrawView(
                        correctPattern = userSettings.lockPatternCode.ifEmpty { "0,1,2,5" },
                        title = if (isEs) "Dibuja tu patrón para editar" else "Draw pattern to edit",
                        onSuccess = { isAuthenticated = true },
                        onCancel = onDismiss
                    )
                }
            }
        }
        return
    }

    Dialog(
        onDismissRequest = {
            if (isSettingUpPin) {
                isSettingUpPin = false
            } else if (isSettingUpPattern) {
                isSettingUpPattern = false
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(26.dp),
            color = palette.surface,
            border = BorderStroke(1.5.dp, palette.cardBorder),
            tonalElevation = 10.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(palette.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isEs) "Personalizar Bloqueo" else "Customize Lock Screen",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.onBackground
                                )
                                Text(
                                    text = if (isEs) "Vista previa en vivo de reloj, widgets y seguridad" else "Live preview of clock, widgets & security",
                                    fontSize = 11.sp,
                                    color = palette.onSurface.copy(alpha = 0.65f)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_lock_customizer_btn")) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = palette.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ========================================================
                    // VISTA PREVIA EN VIVO (LIVE PREVIEW CARD)
                    // ========================================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .border(2.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                            .testTag("lock_live_preview_container")
                    ) {
                        // Fondo dinámico seleccionado
                        WallpaperBackground(
                            wallpaperId = selectedWallpaper,
                            customWallpaperUri = customWallpaperUri,
                            scrimBrush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f)
                                )
                            )
                        )

                        // Contenido de la vista previa
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Candado y etiqueta de Vista Previa
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (isEs) "● Vista Previa en Vivo" else "● Live Preview",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF4ADE80)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Reloj y fecha en vivo
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = SimpleDateFormat("EEEE, d MMMM", if (isEs) Locale.forLanguageTag("es-ES") else Locale.US).format(Date()).replaceFirstChar { it.uppercase() },
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.9f),
                                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(0f, 1f), blurRadius = 4f)
                                    )
                                )
                                Text(
                                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                                    style = TextStyle(
                                        fontSize = 48.sp,
                                        fontWeight = previewFontWeight,
                                        fontFamily = previewFontFamily,
                                        letterSpacing = (-1.5).sp,
                                        color = parsedClockColor,
                                        shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), offset = Offset(0f, 2f), blurRadius = 6f)
                                    )
                                )

                                // Mini widgets activos en la vista previa
                                if (selectedMiniWidgets.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (widgetKey in selectedMiniWidgets.take(3)) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color.Black.copy(alpha = 0.45f))
                                                    .border(0.8.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    when (widgetKey) {
                                                        "WEATHER_MAIN" -> {
                                                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFFACC15), modifier = Modifier.size(11.dp))
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text("22°", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                        }
                                                        "BATTERY" -> {
                                                            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(11.dp))
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text("85%", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                        }
                                                        "CALENDAR" -> {
                                                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(10.dp))
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text("14:00", fontSize = 9.sp, color = Color.White)
                                                        }
                                                        "STEPS" -> {
                                                            Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = Color(0xFFFB923C), modifier = Modifier.size(10.dp))
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text("7.4k", fontSize = 9.sp, color = Color.White)
                                                        }
                                                        "ALARM" -> {
                                                            Icon(Icons.Default.Alarm, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(10.dp))
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text("07:00", fontSize = 9.sp, color = Color.White)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Controles inferiores de la vista previa
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.FlashlightOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                }

                                Text(
                                    text = when (selectedSecurityType) {
                                        "PIN" -> if (isEs) "PIN activo" else "PIN active"
                                        "PATTERN" -> if (isEs) "Patrón activo" else "Pattern active"
                                        else -> if (isEs) "Deslizar" else "Swipe"
                                    },
                                    fontSize = 9.5.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )

                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // ========================================================
                    // SECCIÓN 1: TIPO DE LETRA DEL RELOJ
                    // ========================================================
                    Text(
                        text = if (isEs) "1. Tipo de Letra del Reloj" else "1. Clock Font Style",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val fontStyles = listOf(
                        Triple("BOLD", "Bold", FontWeight.ExtraBold),
                        Triple("SERIF", "Serif", FontWeight.Normal),
                        Triple("ROUNDED", "Rounded", FontWeight.Bold),
                        Triple("TECH", "Tech", FontWeight.SemiBold),
                        Triple("THIN", "Thin", FontWeight.Light)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(fontStyles) { (fontKey, label, weight) ->
                            val isSelected = selectedFont == fontKey
                            Box(
                                modifier = Modifier
                                    .size(width = 68.dp, height = 52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) palette.primary.copy(alpha = 0.2f) else palette.surfaceVariant)
                                    .border(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) palette.primary else palette.cardBorder,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedFont = fontKey }
                                    .testTag("lock_font_$fontKey"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "12:34",
                                        fontSize = 15.sp,
                                        fontWeight = weight,
                                        fontFamily = when (fontKey) {
                                            "SERIF" -> FontFamily.Serif
                                            "TECH" -> FontFamily.Monospace
                                            else -> FontFamily.Default
                                        },
                                        color = if (isSelected) palette.primary else palette.onBackground
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        color = palette.onSurface.copy(alpha = 0.65f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ========================================================
                    // SECCIÓN 2: COLOR DEL RELOJ
                    // ========================================================
                    Text(
                        text = if (isEs) "2. Color del Reloj" else "2. Clock Color",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val colors = listOf(
                        "#FFFFFF", "#BAE6FD", "#38BDF8", "#DDD6FE", "#FDA4AF", "#FEF08A", "#A7F3D0", "#FED7AA", "#F43F5E", "#10B981"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(colors) { hexColor ->
                            val parsed = Color(android.graphics.Color.parseColor(hexColor))
                            val isSelected = selectedColor.equals(hexColor, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(parsed)
                                    .border(
                                        if (isSelected) 3.dp else 1.dp,
                                        if (isSelected) palette.primary else Color.Black.copy(alpha = 0.25f),
                                        CircleShape
                                    )
                                    .clickable { selectedColor = hexColor }
                                    .testTag("lock_color_${hexColor.removePrefix("#")}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (hexColor == "#FFFFFF" || hexColor == "#FEF08A") Color.Black else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // ========================================================
                    // SECCIÓN 3: MINI WIDGETS BAJO EL RELOJ
                    // ========================================================
                    Text(
                        text = if (isEs) "3. Mini Widgets bajo el Reloj" else "3. Mini Widgets under Clock",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isEs) "Toca para activar o desactivar cada widget" else "Tap to toggle widgets on lock screen",
                        fontSize = 11.sp,
                        color = palette.onSurface.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val availableMiniWidgets = listOf(
                        Pair("WEATHER_MAIN", if (isEs) "⛅ Clima (22° Mayormente nublado)" else "⛅ Weather (22° Mostly cloudy)"),
                        Pair("BATTERY", if (isEs) "⚡ Batería (85%)" else "⚡ Battery (85%)"),
                        Pair("CALENDAR", if (isEs) "📅 Evento / Calendario" else "📅 Event / Calendar"),
                        Pair("STEPS", if (isEs) "👟 Pasos y Actividad" else "👟 Steps & Fitness"),
                        Pair("ALARM", if (isEs) "⏰ Próxima Alarma (07:00)" else "⏰ Next Alarm (07:00)")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for ((key, label) in availableMiniWidgets) {
                            val isChecked = selectedMiniWidgets.contains(key)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) {
                                            selectedMiniWidgets.remove(key)
                                        } else {
                                            selectedMiniWidgets.add(key)
                                        }
                                    }
                                    .testTag("lock_widget_toggle_$key"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isChecked) palette.primary.copy(alpha = 0.14f) else palette.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                border = BorderStroke(1.dp, if (isChecked) palette.primary else palette.cardBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                        color = palette.onBackground
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(if (isChecked) palette.primary else Color.Transparent)
                                            .border(1.5.dp, if (isChecked) palette.primary else palette.cardBorder, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isChecked) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = palette.onPrimary, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // ========================================================
                    // SECCIÓN 4: SEGURIDAD (SWIPE / PIN / PATRÓN)
                    // ========================================================
                    Text(
                        text = if (isEs) "4. Tipo de Seguridad" else "4. Lock Security",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val securityOptions = listOf(
                        Triple("SWIPE", if (isEs) "Deslizar" else "Swipe", Icons.Default.SwipeVertical),
                        Triple("PIN", if (isEs) "PIN Numérico" else "PIN Code", Icons.Default.Pin),
                        Triple("PATTERN", if (isEs) "Patrón" else "Pattern", Icons.Default.Pattern)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for ((secType, label, icon) in securityOptions) {
                            val isSelected = selectedSecurityType == secType
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (selectedSecurityType != secType) {
                                            val promptTitle = when (secType) {
                                                "SWIPE" -> if (isEs) "Introduce tu ${if (userSettings.lockSecurityType == "PIN") "PIN" else "patrón"} actual para desactivar la seguridad" else "Enter current ${if (userSettings.lockSecurityType == "PIN") "PIN" else "pattern"} to remove security"
                                                "PIN" -> if (isEs) "Introduce tu patrón actual para cambiar a PIN" else "Enter current pattern to switch to PIN"
                                                "PATTERN" -> if (isEs) "Introduce tu PIN actual para cambiar a Patrón" else "Enter current PIN to switch to Pattern"
                                                else -> if (isEs) "Confirma tu método de seguridad actual" else "Confirm current security"
                                            }
                                            requireCurrentSecurityIfConfigured(promptTitle) {
                                                selectedSecurityType = secType
                                                if (secType == "PIN" && pinCode.isEmpty()) {
                                                    isSettingUpPin = true
                                                } else if (secType == "PATTERN" && patternCode.isEmpty()) {
                                                    isSettingUpPattern = true
                                                }
                                            }
                                        }
                                    }
                                    .testTag("lock_security_option_$secType"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) palette.primary.copy(alpha = 0.16f) else palette.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                border = BorderStroke(1.dp, if (isSelected) palette.primary else palette.cardBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) palette.primary else palette.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = palette.onBackground
                                    )
                                }
                            }
                        }
                    }

                    // Botón para redefinir PIN / Patrón
                    if (selectedSecurityType == "PIN") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.primary.copy(alpha = 0.1f))
                                .clickable {
                                    val promptTitle = if (isEs) "Introduce tu PIN actual antes de cambiarlo" else "Enter your current PIN before changing"
                                    requireCurrentSecurityIfConfigured(promptTitle) {
                                        isSettingUpPin = true
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (pinCode.isNotEmpty()) "PIN configurado: • • • •" else "⚠️ Sin PIN asignado aún",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.onBackground
                            )
                            Text(
                                text = if (pinCode.isNotEmpty()) (if (isEs) "Cambiar" else "Change") else (if (isEs) "Crear PIN" else "Set PIN"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                        }
                    } else if (selectedSecurityType == "PATTERN") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.primary.copy(alpha = 0.1f))
                                .clickable {
                                    val promptTitle = if (isEs) "Dibuja tu patrón actual antes de cambiarlo" else "Draw your current pattern before changing"
                                    requireCurrentSecurityIfConfigured(promptTitle) {
                                        isSettingUpPattern = true
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (patternCode.isNotEmpty()) "Patrón configurado correctamente" else "⚠️ Sin patrón trazado aún",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.onBackground
                            )
                            Text(
                                text = if (patternCode.isNotEmpty()) (if (isEs) "Cambiar" else "Change") else (if (isEs) "Trazar" else "Draw"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // ========================================================
                    // SECCIÓN 5: FONDO DE BLOQUEO (PRESETS + FOTO PROPIA)
                    // ========================================================
                    WallpaperSelectorSection(
                        title = if (isEs) "5. Fondo de Bloqueo" else "5. Lock Wallpaper",
                        subtitle = if (isEs) "Elige un fondo estético o sube tu propia foto" else "Choose preset wallpaper or pick custom photo",
                        selectedWallpaperId = selectedWallpaper,
                        customWallpaperUri = customWallpaperUri,
                        palette = palette,
                        isSpanish = isEs,
                        onSelectPreset = { presetId ->
                            customWallpaperUri = "" // Desactivar URI custom para que el preset se muestre
                            selectedWallpaper = presetId
                        },
                        onSelectCustomUri = { uri ->
                            customWallpaperUri = uri
                        }
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // ========================================================
                    // BOTONES DE GUARDADO Y PRUEBA EN VIVO
                    // ========================================================
                    val updatedState = userSettings.copy(
                        lockClockFont = selectedFont,
                        lockClockColor = selectedColor,
                        lockMiniWidgets = selectedMiniWidgets.toList(),
                        lockSecurityType = selectedSecurityType,
                        lockPinCode = pinCode,
                        lockPatternCode = patternCode,
                        lockWallpaperId = selectedWallpaper,
                        lockCustomWallpaperUri = customWallpaperUri
                    )

                    fun validateAndProceed(onValid: () -> Unit) {
                        if (selectedSecurityType == "PIN" && pinCode.isEmpty()) {
                            Toast.makeText(
                                context,
                                if (isEs) "Debes definir un PIN de 4 dígitos antes de guardar" else "Please set a 4-digit PIN first",
                                Toast.LENGTH_LONG
                            ).show()
                            isSettingUpPin = true
                            return
                        }
                        if (selectedSecurityType == "PATTERN" && patternCode.isEmpty()) {
                            Toast.makeText(
                                context,
                                if (isEs) "Debes trazar un patrón antes de guardar" else "Please draw a pattern first",
                                Toast.LENGTH_LONG
                            ).show()
                            isSettingUpPattern = true
                            return
                        }
                        onValid()
                    }

                    // 1. Guardar y Bloquear Inmediatamente para probar el resultado
                    Button(
                        onClick = {
                            validateAndProceed {
                                onSaveSettings(updatedState)
                                Toast.makeText(
                                    context,
                                    if (isEs) "¡Bloqueo actualizado! Probando pantalla ahora..." else "Lock screen updated! Testing now...",
                                    Toast.LENGTH_SHORT
                                ).show()
                                if (onSaveAndLock != null) {
                                    onSaveAndLock(updatedState)
                                } else {
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_and_lock_screen_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEs) "Guardar y Bloquear Pantalla (Ver Resultado)" else "Save & Lock Screen (Test Now)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Solo Guardar Cambios sin bloquear
                    OutlinedButton(
                        onClick = {
                            validateAndProceed {
                                onSaveSettings(updatedState)
                                Toast.makeText(
                                    context,
                                    if (isEs) "Cambios de pantalla de bloqueo guardados" else "Lock screen changes saved",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("save_lock_customization_btn"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (isEs) "Solo Guardar Cambios" else "Save Changes Only",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // ========================================================
    // DIÁLOGO DEDICADO DE DEFINICIÓN DE PIN
    // ========================================================
    if (isSettingUpPin) {
        Dialog(
            onDismissRequest = {
                isSettingUpPin = false
                if (pinCode.isEmpty()) selectedSecurityType = "SWIPE"
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
            ) {
                PinKeypadView(
                    correctPin = "",
                    isSettingUp = true,
                    onPinDefined = { newPin ->
                        pinCode = newPin
                        selectedSecurityType = "PIN"
                        isSettingUpPin = false
                        Toast.makeText(
                            context,
                            if (isEs) "PIN configurado correctamente" else "PIN code configured",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onSuccess = { isSettingUpPin = false },
                    onCancel = {
                        isSettingUpPin = false
                        if (pinCode.isEmpty()) selectedSecurityType = "SWIPE"
                    }
                )
            }
        }
    }

    // ========================================================
    // DIÁLOGO DEDICADO DE DEFINICIÓN DE PATRÓN
    // ========================================================
    if (isSettingUpPattern) {
        Dialog(
            onDismissRequest = {
                isSettingUpPattern = false
                if (patternCode.isEmpty()) selectedSecurityType = "SWIPE"
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
            ) {
                PatternDrawView(
                    correctPattern = "",
                    isSettingUp = true,
                    onPatternDefined = { newPattern ->
                        patternCode = newPattern
                        selectedSecurityType = "PATTERN"
                        isSettingUpPattern = false
                        Toast.makeText(
                            context,
                            if (isEs) "Patrón configurado correctamente" else "Pattern configured",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onSuccess = { isSettingUpPattern = false },
                    onCancel = {
                        isSettingUpPattern = false
                        if (patternCode.isEmpty()) selectedSecurityType = "SWIPE"
                    }
                )
            }
        }
    }

    // ========================================================
    // DIÁLOGO DE CONFIRMACIÓN DE SEGURIDAD ACTUAL (PIN / PATRÓN)
    // ========================================================
    if (isVerifyingCurrentSecurity) {
        Dialog(
            onDismissRequest = {
                isVerifyingCurrentSecurity = false
                pendingSecurityAction = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
            ) {
                if (userSettings.lockSecurityType == "PIN") {
                    PinKeypadView(
                        correctPin = userSettings.lockPinCode,
                        title = currentSecurityVerifyTitle.ifEmpty {
                            if (isEs) "Introduce tu PIN actual para modificar la seguridad" else "Enter current PIN to modify security"
                        },
                        onSuccess = {
                            isVerifyingCurrentSecurity = false
                            val action = pendingSecurityAction
                            pendingSecurityAction = null
                            action?.invoke()
                        },
                        onCancel = {
                            isVerifyingCurrentSecurity = false
                            pendingSecurityAction = null
                        }
                    )
                } else if (userSettings.lockSecurityType == "PATTERN") {
                    PatternDrawView(
                        correctPattern = userSettings.lockPatternCode,
                        title = currentSecurityVerifyTitle.ifEmpty {
                            if (isEs) "Dibuja tu patrón actual para modificar la seguridad" else "Draw current pattern to modify security"
                        },
                        onSuccess = {
                            isVerifyingCurrentSecurity = false
                            val action = pendingSecurityAction
                            pendingSecurityAction = null
                            action?.invoke()
                        },
                        onCancel = {
                            isVerifyingCurrentSecurity = false
                            pendingSecurityAction = null
                        }
                    )
                }
            }
        }
    }
}
