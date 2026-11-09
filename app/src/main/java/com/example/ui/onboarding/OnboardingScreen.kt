package com.example.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ai.FiriBrainMode
import com.example.ai.FiriChatMessage
import com.example.model.FusionDeviceBackup
import com.example.model.SupportedLocales
import com.example.ui.theme.LauncherPalette
import com.example.ui.theme.PaletteManager
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    initialPaletteId: String,
    initialLanguage: String,
    initialRegion: String,
    initialHomeWallpaperId: String = "HARMONY_NEXT",
    initialLockWallpaperId: String = "TITANIUM_WAVES",
    initialDarkMode: Boolean = true,
    onComplete: (
        paletteId: String,
        languageCode: String,
        regionCode: String,
        homeWallpaperId: String,
        homeCustomUri: String,
        lockWallpaperId: String,
        lockCustomUri: String,
        isDarkMode: Boolean,
        appIconSize: String
    ) -> Unit,
    onImportFusionBackup: (FusionDeviceBackup) -> Unit = {}
) {
    val context = LocalContext.current

    // Pasos del Asistente estilo iOS:
    // 0: Inicio Rápido con otro dispositivo Fusion (Image 4)
    // 1: Idioma y Región
    // 2: Aspecto (Modo Claro / Modo Oscuro & Tamaño - Image 1)
    // 3: Fondos de Pantalla y Bloqueo (Image 3)
    // 4: Asistente Firi AI
    // 5: Te damos la bienvenida a Fusion
    var currentStep by remember { mutableIntStateOf(0) }

    var selectedRegion by remember { mutableStateOf(SupportedLocales.getRegion(initialRegion)) }
    var selectedLanguage by remember { mutableStateOf(initialLanguage) }
    var selectedPaletteId by remember { mutableStateOf(if (initialPaletteId.isBlank()) "ios" else initialPaletteId) }
    var isDarkMode by remember { mutableStateOf(initialDarkMode) }
    var appIconSize by remember { mutableStateOf("NORMAL") } // NORMAL, MEDIUM, LARGE (Image 1)

    var selectedHomeWallpaperId by remember { mutableStateOf(initialHomeWallpaperId) }
    var homeCustomUri by remember { mutableStateOf("") }
    var selectedLockWallpaperId by remember { mutableStateOf(initialLockWallpaperId) }
    var lockCustomUri by remember { mutableStateOf("") }
    var isFiriEnabledSetting by remember { mutableStateOf(true) }

    // Estado para migración / transferencia con otro Fusion (Image 4)
    var isTransferring by remember { mutableStateOf(false) }
    var transferProgress by remember { mutableFloatStateOf(0f) }
    var transferStatusText by remember { mutableStateOf("Buscando dispositivos cercanos...") }
    var isTransferComplete by remember { mutableStateOf(false) }

    val activePalette = remember(selectedPaletteId, isDarkMode) {
        PaletteManager.getPalette(selectedPaletteId, isDarkMode)
    }
    val isEs = selectedLanguage == "es"

    // Animación de halo pulsante para búsqueda de dispositivos
    val infiniteTransition = rememberInfiniteTransition(label = "quick_start_radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_radar"
    )
    val rotateRadar by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_rot"
    )

    // Lógica de simulación de migración entre dos dispositivos Fusion
    LaunchedEffect(isTransferring) {
        if (isTransferring) {
            transferStatusText = if (isEs) "Conectando con Fusion OS de Juan..." else "Connecting to Juan's Fusion OS..."
            delay(1000)
            transferProgress = 0.25f
            transferStatusText = if (isEs) "Transfiriendo 24 chats y recuerdos de Firi AI..." else "Transferring 24 Firi AI chats & memories..."
            delay(1200)
            transferProgress = 0.60f
            transferStatusText = if (isEs) "Copiando estilo de pantalla de bloqueo, widgets y reloj..." else "Copying lock screen style, widgets & clock..."
            delay(1100)
            transferProgress = 0.88f
            transferStatusText = if (isEs) "Aplicando fondos de pantalla y personalización..." else "Applying wallpapers and preferences..."
            delay(900)
            transferProgress = 1.0f
            transferStatusText = if (isEs) "¡Transferencia completada con éxito!" else "Transfer completed successfully!"
            isTransferComplete = true
        }
    }

    // Fondo del asistente estilo iOS (Modo Claro vs Modo Oscuro)
    val bgColor = if (isDarkMode) Color(0xFF000000) else Color(0xFFF2F2F7)
    val cardBg = if (isDarkMode) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) Color(0xFFFFFFFF) else Color(0xFF000000)
    val subtextColor = if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF6C6C70)
    val iosBlue = Color(0xFF007AFF)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ========================================================
            // BARRA SUPERIOR ESTILO IOS (Botón Atrás en círculo + Accesibilidad)
            // ========================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0 && !isTransferring) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                            .clickable { currentStep-- },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = iosBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(38.dp))
                }

                // Indicador de Progreso Discreto tipo iOS
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(6) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (currentStep == index) 16.dp else 6.dp, 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (currentStep == index) iosBlue
                                    else if (isDarkMode) Color(0xFF38383A)
                                    else Color(0xFFD1D1D6)
                                )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessibilityNew,
                        contentDescription = "Accesibilidad",
                        tint = iosBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ========================================================
            // CONTENIDO PRINCIPAL POR PASOS
            // ========================================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        }
                    },
                    label = "ios_setup_step_transition"
                ) { step ->
                    when (step) {
                        // ----------------------------------------------------
                        // PASO 0: CONFIGURA TU FUSION RÁPIDAMENTE (Image 4)
                        // ----------------------------------------------------
                        0 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Ilustración de transferencia entre 2 dispositivos
                                Box(
                                    modifier = Modifier
                                        .size(200.dp)
                                        .padding(top = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Ondas de halo pulsantes
                                    Box(
                                        modifier = Modifier
                                            .size(190.dp)
                                            .scale(pulseScale)
                                            .clip(CircleShape)
                                            .background(iosBlue.copy(alpha = 0.12f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(150.dp)
                                            .scale(pulseScale * 0.95f)
                                            .clip(CircleShape)
                                            .background(iosBlue.copy(alpha = 0.20f))
                                    )

                                    // Dos teléfonos fusionándose estilo Image 4
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy((-28).dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Teléfono 1 (Anterior con fondo y hola)
                                        Box(
                                            modifier = Modifier
                                                .size(width = 95.dp, height = 150.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color(0xFF60A5FA), Color(0xFF38BDF8), Color(0xFF34D399))
                                                    )
                                                )
                                                .border(2.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(20.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "hola",
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp
                                            )
                                        }

                                        // Teléfono 2 (Nuevo recibiendo cuadrícula)
                                        Box(
                                            modifier = Modifier
                                                .size(width = 88.dp, height = 140.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color(0xFFFDE68A), Color(0xFFFCA5A5), Color(0xFFA7F3D0))
                                                    )
                                                )
                                                .border(2.5.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(18.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.9f)))
                                                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF38BDF8)))
                                                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFF43F5E)))
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF10B981)))
                                                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFF59E0B)))
                                                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.9f)))
                                                }
                                            }
                                        }
                                    }
                                }

                                // Textos principales estilo Image 4
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isEs) "Configura el dispositivo rápidamente" else "Set Up Quickly",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (isEs)
                                            "Acerca tu Fusion actual a tu nuevo dispositivo para pasar tus chats con Firi, fondo de pantalla, estilos de bloqueo y preferencias al instante."
                                        else
                                            "Bring your current Fusion device near your new device to transfer your Firi chats, wallpapers, lock screens, and setup instantly.",
                                        fontSize = 14.5.sp,
                                        color = subtextColor,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 21.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Indicador o Tarjeta de Dispositivo Detectado
                                    if (!isTransferring && !isTransferComplete) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(18.dp))
                                                .clickable { isTransferring = true },
                                            colors = CardDefaults.cardColors(containerColor = cardBg),
                                            border = BorderStroke(1.dp, iosBlue.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape)
                                                        .background(iosBlue.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PhoneIphone,
                                                        contentDescription = null,
                                                        tint = iosBlue,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Fusion Phone de Juan",
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = textColor
                                                    )
                                                    Text(
                                                        text = if (isEs) "Fusion OS 3.0.9 · Toca para transferir todo" else "Fusion OS 3.0.9 · Tap to transfer all",
                                                        fontSize = 12.sp,
                                                        color = iosBlue
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.Sync,
                                                    contentDescription = null,
                                                    tint = iosBlue,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = subtextColor,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .rotate(rotateRadar)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isEs) "Buscando otros dispositivos cercanos..." else "Looking for nearby devices...",
                                                fontSize = 12.5.sp,
                                                color = subtextColor
                                            )
                                        }
                                    } else {
                                        // Estado de Progreso de Transferencia
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = cardBg),
                                            shape = RoundedCornerShape(18.dp),
                                            border = BorderStroke(1.dp, if (isTransferComplete) Color(0xFF34C759) else iosBlue)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                if (isTransferComplete) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = Color(0xFF34C759),
                                                        modifier = Modifier.size(44.dp)
                                                    )
                                                } else {
                                                    CircularProgressIndicator(
                                                        progress = { transferProgress },
                                                        color = iosBlue,
                                                        strokeWidth = 4.dp,
                                                        modifier = Modifier.size(44.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = transferStatusText,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = textColor,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                LinearProgressIndicator(
                                                    progress = { transferProgress },
                                                    color = if (isTransferComplete) Color(0xFF34C759) else iosBlue,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                )
                                            }
                                        }
                                    }
                                }

                                // Botones inferiores estilo Image 4
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (isTransferComplete) {
                                        Button(
                                            onClick = {
                                                // Crear Backup completo transferido y finalizar al sistema
                                                val backup = FusionDeviceBackup(
                                                    deviceName = "Fusion Phone de Juan",
                                                    systemVersion = "Fusion OS 3.0.9",
                                                    wallpaperId = "IOS_ASTRONOMY",
                                                    lockWallpaperId = "IOS_EARTH",
                                                    lockClockFont = "ROUNDED",
                                                    lockClockColor = "#FFFFFF",
                                                    lockMiniWidgets = listOf("WEATHER_MAIN", "BATTERY", "CALENDAR"),
                                                    isDarkMode = isDarkMode,
                                                    isFiriEnabled = true,
                                                    sampleFiriChats = listOf(
                                                        FiriChatMessage(
                                                            text = "¿Cómo funciona mi nuevo teléfono Fusion?",
                                                            isUser = true
                                                        ),
                                                        FiriChatMessage(
                                                            text = "¡Hola Juan! Tu Fusion OS está sincronizado y listo. Cuentas con 5 años de actualizaciones hasta 2031 y un segundo cerebro CPU para trabajar sin internet.",
                                                            isUser = false,
                                                            brainUsed = FiriBrainMode.ON_DEVICE_CPU,
                                                            executionTimeMs = 42L
                                                        ),
                                                        FiriChatMessage(
                                                            text = "¿Se transfirieron todos mis chats y fondos?",
                                                            isUser = true
                                                        ),
                                                        FiriChatMessage(
                                                            text = "¡Exacto! He recuperado tu historial, tus widgets de pantalla de bloqueo y tus fondos favoritos de Fusion.",
                                                            isUser = false,
                                                            brainUsed = FiriBrainMode.ON_DEVICE_CPU,
                                                            executionTimeMs = 36L
                                                        )
                                                    )
                                                )
                                                onImportFusionBackup(backup)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .testTag("finish_transfer_continue_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = iosBlue),
                                            shape = RoundedCornerShape(26.dp)
                                        ) {
                                            Text(
                                                text = if (isEs) "Continuar al Sistema Fusion" else "Continue to Fusion OS",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = { currentStep = 1 },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .testTag("skip_transfer_manual_setup_btn"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isDarkMode) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
                                                contentColor = textColor
                                            ),
                                            border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFD1D1D6)),
                                            shape = RoundedCornerShape(26.dp)
                                        ) {
                                            Text(
                                                text = if (isEs) "Configurar sin otro dispositivo" else "Set Up Without Another Device",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // PASO 1: IDIOMA Y REGIÓN (Lista Inset Grouped iOS)
                        // ----------------------------------------------------
                        1 -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = if (isEs) "Idioma y Región" else "Language & Region",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isEs)
                                            "Selecciona tu país para ajustar la fecha, hora y formato de Fusion OS."
                                        else
                                            "Select your region to format date, time and system settings.",
                                        fontSize = 14.sp,
                                        color = subtextColor
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Inset Grouped Card
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = cardBg),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                                    ) {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 340.dp)
                                        ) {
                                            items(SupportedLocales.regions) { region ->
                                                val isSelected = region.code == selectedRegion.code
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedRegion = region
                                                            selectedLanguage = region.defaultLanguage
                                                        }
                                                        .padding(horizontal = 16.dp, vertical = 13.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(text = region.flag, fontSize = 22.sp)
                                                        Spacer(modifier = Modifier.width(14.dp))
                                                        Text(
                                                            text = if (isEs) region.nameEs else region.nameEn,
                                                            fontSize = 16.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = textColor
                                                        )
                                                    }
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Seleccionado",
                                                            tint = iosBlue,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = { currentStep = 2 },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("onboarding_lang_continue_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = iosBlue),
                                    shape = RoundedCornerShape(26.dp)
                                ) {
                                    Text(
                                        text = if (isEs) "Continuar" else "Continue",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // PASO 2: ASPECTO (Modo Claro / Modo Oscuro & Tamaño - Image 1)
                        // ----------------------------------------------------
                        2 -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isEs) "Aspecto" else "Appearance",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isEs)
                                            "Selecciona el aspecto claro u oscuro y qué tamaño quieres que tengan los iconos y el texto en el teléfono."
                                        else
                                            "Choose Light or Dark mode and the size of icons and text.",
                                        fontSize = 14.sp,
                                        color = subtextColor,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Selector Modo Claro vs Modo Oscuro
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDarkMode) Color(0xFF1C1C1E) else Color(0xFFE5E5EA))
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Opción Claro
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (!isDarkMode) Color.White else Color.Transparent)
                                                .clickable { isDarkMode = false }
                                                .padding(horizontal = 22.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.LightMode,
                                                    contentDescription = null,
                                                    tint = if (!isDarkMode) Color(0xFFF59E0B) else subtextColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isEs) "Modo Claro" else "Light Mode",
                                                    fontSize = 13.sp,
                                                    fontWeight = if (!isDarkMode) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (!isDarkMode) Color.Black else subtextColor
                                                )
                                            }
                                        }

                                        // Opción Oscuro
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isDarkMode) Color(0xFF2C2C2E) else Color.Transparent)
                                                .clickable { isDarkMode = true }
                                                .padding(horizontal = 22.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.DarkMode,
                                                    contentDescription = null,
                                                    tint = if (isDarkMode) Color(0xFF60A5FA) else subtextColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isEs) "Modo Oscuro" else "Dark Mode",
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isDarkMode) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isDarkMode) Color.White else subtextColor
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Mockup de Teléfono con Preview en vivo estilo Image 1
                                    Box(
                                        modifier = Modifier
                                            .size(width = 160.dp, height = 250.dp)
                                            .clip(RoundedCornerShape(32.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    if (isDarkMode)
                                                        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
                                                    else
                                                        listOf(Color(0xFFBAE6FD), Color(0xFF7DD3FC), Color(0xFF38BDF8))
                                                )
                                            )
                                            .border(3.5.dp, if (isDarkMode) Color(0xFF334155) else Color.White, RoundedCornerShape(32.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Dynamic Island pequeña
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 46.dp, height = 12.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.Black)
                                            )

                                            // Reloj Preview 9:41 estilo Image 1
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "9:41",
                                                    fontSize = 32.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                                )
                                            }

                                            // Tarjetas de notificaciones translúcidas estilo Image 1
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(26.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.65f))
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(26.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.65f))
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Selector de tamaño de iconos y texto (Normal / Mediano / Grande - Image 1)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf(
                                            "NORMAL" to (if (isEs) "Normal" else "Normal"),
                                            "MEDIUM" to (if (isEs) "Mediano" else "Medium"),
                                            "LARGE" to (if (isEs) "Grande" else "Large")
                                        ).forEach { (sizeKey, label) ->
                                            val isSelected = appIconSize == sizeKey
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { appIconSize = sizeKey }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) iosBlue else Color.Transparent)
                                                        .border(
                                                            width = if (isSelected) 0.dp else 2.dp,
                                                            color = if (isSelected) iosBlue else subtextColor,
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = label,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) textColor else subtextColor
                                                )
                                            }
                                        }
                                    }
                                }

                                // Botón Continuar azul estilo Image 1
                                Button(
                                    onClick = { currentStep = 3 },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("onboarding_appearance_continue_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = iosBlue),
                                    shape = RoundedCornerShape(26.dp)
                                ) {
                                    Text(
                                        text = if (isEs) "Continuar" else "Continue",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // PASO 3: FONDOS DE PANTALLA ESTILO IOS 16/17 (Image 3)
                        // ----------------------------------------------------
                        3 -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = if (isEs) "Fondos de Pantalla" else "Wallpapers",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isEs)
                                            "Elige la colección de fondo para tu pantalla de inicio y bloqueo."
                                        else
                                            "Choose a wallpaper collection for your home and lock screen.",
                                        fontSize = 14.sp,
                                        color = subtextColor
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Colecciones de Wallpapers estilo iOS
                                    val presets = listOf(
                                        Triple("IOS_ASTRONOMY", "Astronomía", listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF38BDF8))),
                                        Triple("HARMONY_NEXT", "Colecciones", listOf(Color(0xFF2563EB), Color(0xFF7C3AED), Color(0xFFEC4899))),
                                        Triple("TITANIUM_WAVES", "Titanio", listOf(Color(0xFF334155), Color(0xFF64748B), Color(0xFF94A3B8))),
                                        Triple("GRADIENT_SUNSET", "Atardecer", listOf(Color(0xFF9A3412), Color(0xFFEA580C), Color(0xFFFACC15))),
                                        Triple("DARK_NEBULA", "Nebulosa", listOf(Color(0xFF581C87), Color(0xFF3B0764), Color(0xFF1E1B4B)))
                                    )

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    ) {
                                        items(presets) { (id, name, colors) ->
                                            val isSelected = selectedHomeWallpaperId == id
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.clickable {
                                                    selectedHomeWallpaperId = id
                                                    selectedLockWallpaperId = id
                                                }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 120.dp, height = 210.dp)
                                                        .clip(RoundedCornerShape(22.dp))
                                                        .background(Brush.verticalGradient(colors))
                                                        .border(
                                                            width = if (isSelected) 3.dp else 1.dp,
                                                            color = if (isSelected) iosBlue else Color.White.copy(alpha = 0.3f),
                                                            shape = RoundedCornerShape(22.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(10.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "9:41",
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                        if (isSelected) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(28.dp)
                                                                    .clip(CircleShape)
                                                                    .background(iosBlue),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(1.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = name,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) textColor else subtextColor
                                                )
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = { currentStep = 4 },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("onboarding_wallpaper_continue_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = iosBlue),
                                    shape = RoundedCornerShape(26.dp)
                                ) {
                                    Text(
                                        text = if (isEs) "Continuar" else "Continue",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // PASO 4: ASISTENTE FIRI AI & CONTROL
                        // ----------------------------------------------------
                        4 -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = if (isEs) "Asistente Firi AI" else "Firi AI Assistant",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isEs)
                                            "Firi es tu asistente inteligente integrado en Fusion OS. Puede hablarte, abrir apps y razonar sin internet con su segundo cerebro CPU."
                                        else
                                            "Firi is your intelligent assistant built into Fusion OS. Works offline with on-device CPU reasoning.",
                                        fontSize = 14.sp,
                                        color = subtextColor
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Tarjeta de activación de Firi
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = cardBg),
                                        shape = RoundedCornerShape(18.dp),
                                        border = BorderStroke(1.dp, if (isFiriEnabledSetting) iosBlue else Color(0xFFEF4444))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.firi_app_icon),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = if (isEs) "Firi AI Encendida" else "Firi AI Enabled",
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = textColor
                                                        )
                                                        Text(
                                                            text = if (isFiriEnabledSetting)
                                                                (if (isEs) "Activa para voz, texto y dock" else "Active for voice & dock")
                                                            else
                                                                (if (isEs) "Firi estará apagada" else "Firi will be turned off"),
                                                            fontSize = 12.sp,
                                                            color = if (isFiriEnabledSetting) Color(0xFF34C759) else Color(0xFFEF4444)
                                                        )
                                                    }
                                                }

                                                androidx.compose.material3.Switch(
                                                    checked = isFiriEnabledSetting,
                                                    onCheckedChange = { isFiriEnabledSetting = it },
                                                    colors = androidx.compose.material3.SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = iosBlue
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = { currentStep = 5 },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("onboarding_firi_continue_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = iosBlue),
                                    shape = RoundedCornerShape(26.dp)
                                ) {
                                    Text(
                                        text = if (isEs) "Continuar" else "Continue",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // PASO 5: TE DAMOS LA BIENVENIDA A FUSION OS
                        // ----------------------------------------------------
                        5 -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Spacer(modifier = Modifier.height(20.dp))

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF007AFF), Color(0xFF5856D6))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.firi_app_icon),
                                            contentDescription = null,
                                            modifier = Modifier.size(62.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Text(
                                        text = if (isEs) "Te damos la bienvenida a Fusion" else "Welcome to Fusion",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textColor,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (isEs)
                                            "Desliza hacia arriba para empezar a usar tu nuevo lanzador con estética de iOS, Biblioteca de Apps y modo ${if (isDarkMode) "Oscuro" else "Claro"}."
                                        else
                                            "Swipe up to start using your iOS-styled launcher with App Library and ${if (isDarkMode) "Dark" else "Light"} mode.",
                                        fontSize = 14.5.sp,
                                        color = subtextColor,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 21.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        onComplete(
                                            selectedPaletteId,
                                            selectedLanguage,
                                            selectedRegion.code,
                                            selectedHomeWallpaperId,
                                            homeCustomUri,
                                            selectedLockWallpaperId,
                                            lockCustomUri,
                                            isDarkMode,
                                            appIconSize
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("onboarding_finish_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = iosBlue),
                                    shape = RoundedCornerShape(27.dp)
                                ) {
                                    Text(
                                        text = if (isEs) "Empezar" else "Get Started",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
