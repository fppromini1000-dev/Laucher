package com.example.ui.launcher

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.common.WallpaperBackground
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import com.example.R
import com.example.model.AppCategory
import com.example.model.AppFolder
import com.example.model.AppInfo
import com.example.model.HabitPrediction
import com.example.model.LauncherHomeItem
import com.example.model.LauncherWidgetItem
import com.example.model.UserSettings
import com.example.model.WidgetType
import com.example.ui.theme.LauncherPalette
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherScreen(
    palette: LauncherPalette,
    userSettings: UserSettings,
    allApps: List<AppInfo>,
    homeItems: List<LauncherHomeItem>,
    filteredApps: List<AppInfo>,
    searchQuery: String,
    selectedCategory: AppCategory?,
    currentPrediction: HabitPrediction?,
    widgets: List<LauncherWidgetItem>,
    isEditMode: Boolean,
    isAssistantListeningActive: Boolean,
    onSearchChanged: (String) -> Unit,
    onSelectCategory: (AppCategory?) -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
    onSetEditMode: (Boolean) -> Unit,
    onRemoveApp: (AppInfo) -> Unit,
    onMoveApp: (AppInfo, Int) -> Unit,
    onOpenFolder: (AppFolder) -> Unit,
    onCreateFolderWithApps: (AppInfo, AppInfo) -> Unit,
    onAddAppToFolder: (folderId: String, AppInfo) -> Unit,
    onReorderApps: (AppInfo, AppInfo) -> Unit,
    onDissolveFolder: (folderId: String) -> Unit,
    onOpenAddWidget: () -> Unit,
    onRemoveWidget: (String) -> Unit,
    onUpdateWidgetData: (String, String, WidgetType, Int) -> Unit,
    onOpenVoiceAssistant: () -> Unit,
    onAssistantButtonHold: () -> Unit,
    onOpenHabitInsights: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLockScreen: () -> Unit,
    onOpenControlCenter: () -> Unit,
    onOpenFavoriteAppsPicker: () -> Unit,
    onOpenVolumeActivationGuide: () -> Unit = {},
    onOpenAppLibrary: () -> Unit = {}
) {
    val isEs = userSettings.isSpanish
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var isAccessibilityActive by remember {
        mutableStateOf(com.example.service.VolumeAccessibilityService.isAccessibilityEnabled(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isAccessibilityActive = com.example.service.VolumeAccessibilityService.isAccessibilityEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Reloj en tiempo real
    var currentTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    val locale = if (isEs) {
        when (userSettings.regionCode) {
            "MX" -> Locale.forLanguageTag("es-MX")
            "CO" -> Locale.forLanguageTag("es-CO")
            "AR" -> Locale.forLanguageTag("es-AR")
            else -> Locale.forLanguageTag("es-ES")
        }
    } else {
        if (userSettings.regionCode == "GB") Locale.UK else Locale.US
    }

    val timeFormat = SimpleDateFormat("HH:mm", locale)
    val dateFormat = SimpleDateFormat("M'月'd'日' EEEE", locale)
    val latinDateFormat = SimpleDateFormat("EEEE, d 'de' MMMM", locale)

    // Jiggle / Temblor al editar (Apps empiezan a temblar al entrar al modo edición)
    val infiniteTransition = rememberInfiniteTransition(label = "app_jiggle_shake")
    val jiggleRotation by infiniteTransition.animateFloat(
        initialValue = -3.2f,
        targetValue = 3.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(115, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jiggle_rotation"
    )
    val jiggleOffsetY by infiniteTransition.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(130, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jiggle_offset_y"
    )
    val jiggleOffsetX by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(105, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jiggle_offset_x"
    )

    // Estados para Drag & Drop y creación de carpetas
    var draggedApp by remember { mutableStateOf<AppInfo?>(null) }
    var dragAccumulatedOffset by remember { mutableStateOf(Offset.Zero) }
    var hoveredTargetIndex by remember { mutableStateOf<Int?>(null) }
    val itemBoundsMap = remember { mutableStateMapOf<Int, Rect>() }

    // Diálogo auxiliar para agrupar app en carpeta con un toque
    var appToGroupPicker by remember { mutableStateOf<AppInfo?>(null) }

    // Invocación de Firi con triple toque en espacio vacío
    var emptySpaceTapCount by remember { mutableStateOf(0) }
    var lastEmptySpaceTapTime by remember { mutableStateOf(0L) }

    val handleEmptySpaceTap: () -> Unit = {
        val now = System.currentTimeMillis()
        if (now - lastEmptySpaceTapTime <= 450L) {
            emptySpaceTapCount++
        } else {
            emptySpaceTapCount = 1
        }
        lastEmptySpaceTapTime = now

        if (emptySpaceTapCount >= 3) {
            emptySpaceTapCount = 0
            onOpenVoiceAssistant()
        } else if (isEditMode) {
            onSetEditMode(false)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            // Detección de triple toque o pulsación larga en espacio vacío
            .pointerInput(isEditMode) {
                detectTapGestures(
                    onLongPress = {
                        emptySpaceTapCount = 0
                        onSetEditMode(true)
                    },
                    onTap = {
                        handleEmptySpaceTap()
                    }
                )
            }
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        val columnsCount = when {
            screenWidth >= 800.dp -> 6
            screenWidth >= 600.dp -> 5
            screenWidth >= 360.dp -> 4
            else -> 4
        }

        // 1. Fondo de Pantalla (Predefinido o Fotografía del Usuario)
        WallpaperBackground(
            wallpaperId = userSettings.homeWallpaperId,
            customWallpaperUri = userSettings.homeCustomWallpaperUri,
            scrimBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.20f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.35f)
                )
            )
        )

        // Contenido Principal del Launcher
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ==========================================
            // SECCIÓN SUPERIOR: Top Bar & Reloj Central
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                // Barra de accesos directos
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge Inteligente de Asistente Firi AI (Encendida vs Apagada)
                    val isFiriEnabled = userSettings.isFiriEnabled
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isFiriEnabled) {
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF2563EB).copy(alpha = 0.40f),
                                            Color(0xFF7C3AED).copy(alpha = 0.40f)
                                        )
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF1E293B).copy(alpha = 0.60f),
                                            Color(0xFF334155).copy(alpha = 0.60f)
                                        )
                                    )
                                }
                            )
                            .border(
                                1.dp,
                                if (isFiriEnabled) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { onOpenVoiceAssistant() }
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                            .testTag("current_habit_badge")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.firi_app_icon),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Fit,
                                    alpha = if (isFiriEnabled) 1f else 0.45f
                                )
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isFiriEnabled) "Firi AI" else (if (isEs) "Firi Apagada" else "Firi Off"),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFiriEnabled) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFiriEnabled) {
                                    if (isEs) "• Hablar" else "• Talk"
                                } else {
                                    if (isEs) "• Inactiva" else "• Inactive"
                                },
                                fontSize = 10.sp,
                                color = if (isFiriEnabled) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.45f)
                            )
                        }
                    }

                    // ==========================================
                    // OPCIONES SUPERIORES DERECHAS:
                    // SOLO APARECEN CUANDO MANTIENES PRESIONADO EN UN ESPACIO VACÍO (MODO EDICIÓN)
                    // ==========================================
                    AnimatedVisibility(
                        visible = isEditMode,
                        enter = fadeIn() + slideInVertically { -20 },
                        exit = fadeOut() + slideOutVertically { -20 }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // Botón "Listo" para guardar/salir de modo edición
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF10B981))
                                    .clickable { onSetEditMode(false) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("done_edit_mode_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Listo",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isEs) "Listo" else "Done",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Botón Añadir Widget
                            IconButton(
                                onClick = onOpenAddWidget,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.20f))
                                    .testTag("add_widget_quick_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Añadir Widget",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Botón Bloqueo
                            IconButton(
                                onClick = onOpenLockScreen,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.20f))
                                    .testTag("lock_screen_quick_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Bloquear",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Botón Centro de Control
                            IconButton(
                                onClick = onOpenControlCenter,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.20f))
                                    .testTag("control_center_quick_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Centro de Control",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            // Botón Ajustes
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.20f))
                                    .testTag("launcher_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Ajustes",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                // Banner de Reemplazo Global de Barra de Volumen (TikTok, YouTube, etc.)
                if (!isAccessibilityActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.72f))
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                            .clickable { onOpenVolumeActivationGuide() }
                            .padding(horizontal = 11.dp, vertical = 7.dp)
                            .testTag("activate_volume_banner"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEs) "Barra de Volumen en TikTok y YouTube" else "Volume Bar in TikTok & YouTube",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isEs) "Toca aquí para reemplazar la barra de Android" else "Tap here to replace the default system slider",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Reloj Central (HarmonyOS NEXT: "08:08")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(isEditMode) {
                            detectTapGestures(
                                onLongPress = {
                                    emptySpaceTapCount = 0
                                    onSetEditMode(true)
                                },
                                onTap = { handleEmptySpaceTap() }
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = timeFormat.format(currentTime),
                        style = TextStyle(
                            fontSize = if (screenHeight < 680.dp) 48.sp else 62.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1.5).sp,
                            color = Color.White,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.35f),
                                offset = Offset(0f, 3f),
                                blurRadius = 8f
                            )
                        ),
                        modifier = Modifier.testTag("harmony_clock_display")
                    )

                    Spacer(modifier = Modifier.height(1.dp))

                    val dateFormatted = if (isEs) latinDateFormat.format(currentTime) else dateFormat.format(currentTime)
                    Text(
                        text = "${dateFormatted.replaceFirstChar { it.uppercase() }}  |  20°C ⛅",
                        style = TextStyle(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.90f),
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.40f),
                                offset = Offset(0f, 2f),
                                blurRadius = 6f
                            )
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // SECCIÓN DE APPS FAVORITAS / ACCESOS RÁPIDOS
                // ==========================================
                val favoriteApps = remember(userSettings.favoriteAppPackageNames, allApps) {
                    if (userSettings.favoriteAppPackageNames.isNotEmpty()) {
                        userSettings.favoriteAppPackageNames.mapNotNull { pkg ->
                            allApps.find { it.packageName == pkg }
                        }
                    } else {
                        allApps.take(4)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⭐", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isEs) "Apps Favoritas" else "Favorite Apps",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (isEditMode) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .clickable { onOpenFavoriteAppsPicker() }
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                                    .testTag("edit_favorites_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isEs) "Elegir" else "Edit",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (app in favoriteApps.take(4)) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(82.dp)
                                    .clickable { onLaunchApp(app) }
                                    .testTag("favorite_card_${app.packageName.replace('.', '_')}"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (app.iconDrawable != null) {
                                        val bitmap = remember(app.iconDrawable) {
                                            try { app.iconDrawable.toBitmap(80, 80) } catch (_: Exception) { null }
                                        }
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = app.label,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        } else {
                                            Icon(
                                                imageVector = app.category.icon,
                                                contentDescription = null,
                                                tint = Color(0xFF007AFF),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = app.category.icon,
                                            contentDescription = null,
                                            tint = Color(0xFF007AFF),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = app.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (favoriteApps.size < 4) {
                            for (i in 0 until (4 - favoriteApps.size)) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(82.dp)
                                        .clickable { onOpenFavoriteAppsPicker() },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.35f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Añadir",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = if (isEs) "Añadir" else "Add",
                                            fontSize = 9.5.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECCIÓN CENTRAL: Cuadrícula de Aplicaciones y Carpetas
            // ==========================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .pointerInput(isEditMode) {
                        detectTapGestures(
                            onLongPress = {
                                emptySpaceTapCount = 0
                                onSetEditMode(true)
                            },
                            onTap = { handleEmptySpaceTap() }
                        )
                    }
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnsCount),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("launcher_apps_grid"),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(homeItems, key = { _, item ->
                        when (item) {
                            is LauncherHomeItem.SingleApp -> "app_${item.app.packageName}"
                            is LauncherHomeItem.Folder -> "folder_${item.folder.id}"
                        }
                    }) { index, item ->
                        val isHovered = (hoveredTargetIndex == index)

                        Box(
                            modifier = Modifier
                                .onGloballyPositioned { coordinates ->
                                    itemBoundsMap[index] = coordinates.boundsInRoot()
                                }
                        ) {
                            when (item) {
                                is LauncherHomeItem.SingleApp -> {
                                    val isBeingDragged = (draggedApp?.packageName == item.app.packageName)

                                    HarmonySquircleAppIcon(
                                        app = item.app,
                                        index = index,
                                        isEditMode = isEditMode,
                                        isHoveredDropTarget = isHovered && !isBeingDragged,
                                        isBeingDragged = isBeingDragged,
                                        dragOffset = if (isBeingDragged) dragAccumulatedOffset else Offset.Zero,
                                        jiggleRotation = jiggleRotation,
                                        jiggleOffsetY = jiggleOffsetY,
                                        jiggleOffsetX = jiggleOffsetX,
                                        palette = palette,
                                        onClick = {
                                            if (isEditMode) onAppLongPress(item.app) else onLaunchApp(item.app)
                                        },
                                        onLongClick = {
                                            if (!isEditMode) onSetEditMode(true)
                                            onAppLongPress(item.app)
                                        },
                                        onDelete = { onRemoveApp(item.app) },
                                        onMoveLeft = { onMoveApp(item.app, -1) },
                                        onMoveRight = { onMoveApp(item.app, 1) },
                                        onOpenGroupPicker = { appToGroupPicker = item.app },
                                        onDragStart = {
                                            draggedApp = item.app
                                            dragAccumulatedOffset = Offset.Zero
                                        },
                                        onDragMove = { delta ->
                                            dragAccumulatedOffset += delta
                                            // Detectar sobre qué otro elemento se encuentra
                                            val currentCenter = itemBoundsMap[index]?.center?.plus(dragAccumulatedOffset)
                                            if (currentCenter != null) {
                                                val found = itemBoundsMap.entries.firstOrNull { (k, rect) ->
                                                    k != index && rect.contains(currentCenter)
                                                }?.key
                                                hoveredTargetIndex = found
                                            }
                                        },
                                        onDragEnd = {
                                            val targetIdx = hoveredTargetIndex
                                            if (targetIdx != null && targetIdx in homeItems.indices) {
                                                when (val targetItem = homeItems[targetIdx]) {
                                                    is LauncherHomeItem.SingleApp -> {
                                                        // Puso una app encima de otra: Se crea una carpeta de apps
                                                        onCreateFolderWithApps(item.app, targetItem.app)
                                                    }
                                                    is LauncherHomeItem.Folder -> {
                                                        // Puso una app encima de una carpeta existente: Se añade a la carpeta
                                                        onAddAppToFolder(targetItem.folder.id, item.app)
                                                    }
                                                }
                                            }
                                            draggedApp = null
                                            hoveredTargetIndex = null
                                            dragAccumulatedOffset = Offset.Zero
                                        }
                                    )
                                }

                                is LauncherHomeItem.Folder -> {
                                    HarmonySquircleFolderIcon(
                                        folder = item.folder,
                                        apps = item.apps,
                                        index = index,
                                        isEditMode = isEditMode,
                                        isHoveredDropTarget = isHovered,
                                        jiggleRotation = jiggleRotation,
                                        jiggleOffsetY = jiggleOffsetY,
                                        jiggleOffsetX = jiggleOffsetX,
                                        onClick = {
                                            onOpenFolder(item.folder)
                                        },
                                        onLongClick = {
                                            if (!isEditMode) onSetEditMode(true)
                                        },
                                        onDelete = {
                                            onDissolveFolder(item.folder.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECCIÓN INFERIOR: Puntos de página & Dock Flotante
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Botón Píldora de Búsqueda y Biblioteca de Apps estilo iOS
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (userSettings.isDarkMode) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.65f),
                    border = BorderStroke(0.5.dp, if (userSettings.isDarkMode) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenAppLibrary() }
                        .testTag("toggle_app_library_btn")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Biblioteca de apps",
                            tint = if (userSettings.isDarkMode) Color.White else Color(0xFF1C1C1E),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isEs) "Buscar" else "Search",
                            color = if (userSettings.isDarkMode) Color.White else Color(0xFF1C1C1E),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (userSettings.isDarkMode) Color.White else Color(0xFF1C1C1E)))
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background((if (userSettings.isDarkMode) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.4f)))
                    }
                }

                // Dock Flotante Translúcido (HarmonyOS NEXT)
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .height(76.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Teléfono
                        DockPillItem(
                            iconBg = Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF10B981))),
                            icon = Icons.Default.Phone,
                            contentDescription = "Teléfono",
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_DIAL).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                } catch (_: Exception) {}
                            }
                        )

                        // 2. Mensajes
                        DockPillItem(
                            iconBg = Brush.linearGradient(listOf(Color(0xFF60A5FA), Color(0xFF2563EB))),
                            icon = Icons.Default.Check,
                            contentDescription = "Mensajes",
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_MAIN).apply {
                                        addCategory(Intent.CATEGORY_APP_MESSAGING)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        )

                        // 3. Firi AI Asistente (Acceso Rápido en el Dock con ícono visual oficial de Firi)
                        DockPillImageItem(
                            imageRes = R.drawable.firi_app_icon,
                            contentDescription = "Firi AI",
                            onClick = { onOpenVoiceAssistant() }
                        )

                        // 4. Navegador
                        DockPillItem(
                            iconBg = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))),
                            icon = Icons.Default.Language,
                            contentDescription = "Navegador",
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                } catch (_: Exception) {}
                            }
                        )

                        // 5. Cámara
                        DockPillItem(
                            iconBg = Brush.linearGradient(listOf(Color(0xFF475569), Color(0xFF0F172A))),
                            icon = Icons.Default.CameraAlt,
                            contentDescription = "Cámara",
                            onClick = {
                                try {
                                    context.startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }

        // Diálogo para crear carpeta seleccionando otra app
        appToGroupPicker?.let { sourceApp ->
            GroupWithAppDialog(
                sourceApp = sourceApp,
                allApps = allApps.filter { it.packageName != sourceApp.packageName },
                onSelectTargetApp = { targetApp ->
                    onCreateFolderWithApps(sourceApp, targetApp)
                    appToGroupPicker = null
                },
                onDismiss = { appToGroupPicker = null }
            )
        }

        // Iluminación perimetral reactiva al asistente
        FiriEdgeGlowOverlay(
            isListening = isAssistantListeningActive,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Ícono de aplicación con forma squircle continua orgánica y animación de temblor en modo edición.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HarmonySquircleAppIcon(
    app: AppInfo,
    index: Int,
    isEditMode: Boolean,
    isHoveredDropTarget: Boolean,
    isBeingDragged: Boolean,
    dragOffset: Offset,
    jiggleRotation: Float,
    jiggleOffsetY: Float,
    jiggleOffsetX: Float,
    palette: LauncherPalette,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onOpenGroupPicker: () -> Unit,
    onDragStart: () -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    // Cálculo de rotación y traslación para el temblor alternado
    val rotationVal = if (isEditMode && !isBeingDragged) {
        if (index % 2 == 0) jiggleRotation else -jiggleRotation
    } else 0f

    val translationYVal = if (isEditMode && !isBeingDragged) {
        if (index % 3 == 0) jiggleOffsetY else -jiggleOffsetY
    } else 0f

    val translationXVal = if (isEditMode && !isBeingDragged) {
        if (index % 2 == 0) jiggleOffsetX else -jiggleOffsetX
    } else 0f

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isBeingDragged) 10f else 1f)
            .offset {
                if (isBeingDragged) IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
                else IntOffset.Zero
            }
            .graphicsLayer {
                rotationZ = rotationVal
                translationY = translationYVal
                translationX = translationXVal
                scaleX = if (isBeingDragged) 1.15f else if (isHoveredDropTarget) 1.08f else 1f
                scaleY = if (isBeingDragged) 1.15f else if (isHoveredDropTarget) 1.08f else 1f
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                // Habilitar arrastre físico cuando está en modo edición
                .then(
                    if (isEditMode) {
                        Modifier.pointerInput(app.packageName) {
                            detectDragGestures(
                                onDragStart = { onDragStart() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDragMove(dragAmount)
                                },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() }
                            )
                        }
                    } else Modifier
                )
                .padding(vertical = 2.dp, horizontal = 2.dp)
                .testTag("app_item_${app.packageName.replace('.', '_')}"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Contenedor Squircle continuo de HarmonyOS NEXT
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = if (isBeingDragged) 0.95f else 0.92f))
                    .border(
                        border = if (isHoveredDropTarget) {
                            BorderStroke(2.5.dp, Color(0xFF38BDF8))
                        } else {
                            BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                        },
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (app.packageName == "com.firi.ai" || app.label.contains("Firi", ignoreCase = true)) {
                    Image(
                        painter = painterResource(id = R.drawable.firi_app_icon),
                        contentDescription = app.label,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else if (app.iconDrawable != null) {
                    val bitmap = remember(app.iconDrawable) {
                        try {
                            app.iconDrawable.toBitmap(108, 108)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Icon(
                            imageVector = app.category.icon,
                            contentDescription = app.label,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = app.category.icon,
                        contentDescription = app.label,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Pin badge
                if (app.isPinned) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Anclada",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }

                // Indicador visual de crear carpeta al pasar por encima
                if (isHoveredDropTarget) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Crear Carpeta",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Etiqueta con sombra suave
            Text(
                text = if (isHoveredDropTarget) "Crear Carpeta" else app.label,
                style = TextStyle(
                    fontSize = 11.5.sp,
                    fontWeight = if (isHoveredDropTarget) FontWeight.Bold else FontWeight.Medium,
                    color = if (isHoveredDropTarget) Color(0xFF38BDF8) else Color.White,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.55f),
                        offset = Offset(0f, 2f),
                        blurRadius = 5f
                    )
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Controles de reordenar y crear carpeta en modo edición
            if (isEditMode) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    // Mover a la izquierda
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.4f))
                            .clickable { onMoveLeft() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Mover atrás",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(3.dp))

                    // Botón para crear carpeta directamente con otra app
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.6f))
                            .clickable { onOpenGroupPicker() }
                            .testTag("group_app_btn_${app.packageName.replace('.', '_')}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Agrupar en carpeta",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(3.dp))

                    // Mover a la derecha
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.4f))
                            .clickable { onMoveRight() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Mover adelante",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }

        // Botón de eliminar en modo edición
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .offset(x = 18.dp, y = (-2).dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE11D48))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Eliminar",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Ícono de carpeta de aplicaciones (estilo HarmonyOS NEXT / iOS) con cuadrícula 2x2 de iconos internos.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HarmonySquircleFolderIcon(
    folder: AppFolder,
    apps: List<AppInfo>,
    index: Int,
    isEditMode: Boolean,
    isHoveredDropTarget: Boolean,
    jiggleRotation: Float,
    jiggleOffsetY: Float,
    jiggleOffsetX: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val rotationVal = if (isEditMode) {
        if (index % 2 == 0) jiggleRotation else -jiggleRotation
    } else 0f

    val translationYVal = if (isEditMode) {
        if (index % 3 == 0) jiggleOffsetY else -jiggleOffsetY
    } else 0f

    val translationXVal = if (isEditMode) {
        if (index % 2 == 0) jiggleOffsetX else -jiggleOffsetX
    } else 0f

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationZ = rotationVal
                translationY = translationYVal
                translationX = translationXVal
                scaleX = if (isHoveredDropTarget) 1.08f else 1f
                scaleY = if (isHoveredDropTarget) 1.08f else 1f
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(vertical = 2.dp, horizontal = 2.dp)
                .testTag("folder_item_${folder.id}"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Contenedor Squircle de Carpeta Translúcida
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.88f))
                    .border(
                        border = if (isHoveredDropTarget) {
                            BorderStroke(2.5.dp, Color(0xFF38BDF8))
                        } else {
                            BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                // Mini cuadrícula 2x2 con iconos de las apps dentro de la carpeta
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MiniFolderIconItem(app = apps.getOrNull(0))
                        MiniFolderIconItem(app = apps.getOrNull(1))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MiniFolderIconItem(app = apps.getOrNull(2))
                        MiniFolderIconItem(app = apps.getOrNull(3))
                    }
                }

                // Overlay si se está arrastrando una app hacia esta carpeta
                if (isHoveredDropTarget) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.40f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Añadir a Carpeta",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Nombre de la Carpeta
            Text(
                text = folder.name,
                style = TextStyle(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.55f),
                        offset = Offset(0f, 2f),
                        blurRadius = 5f
                    )
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }

        // Botón de disolver carpeta en modo edición
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .offset(x = 18.dp, y = (-2).dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE11D48))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Disolver carpeta",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Mini icono dentro del squircle de la carpeta.
 */
@Composable
private fun MiniFolderIconItem(app: AppInfo?) {
    if (app != null) {
        if (app.packageName == "com.firi.ai" || app.label.contains("Firi", ignoreCase = true)) {
            Image(
                painter = painterResource(id = R.drawable.firi_app_icon),
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Fit
            )
        } else if (app.iconDrawable != null) {
            val bitmap = remember(app.iconDrawable) {
                try { app.iconDrawable.toBitmap(48, 48) } catch (_: Exception) { null }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF007AFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.label.take(1),
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF007AFF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.label.take(1),
                    fontSize = 9.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        Spacer(modifier = Modifier.size(18.dp))
    }
}

/**
 * Diálogo rápido para crear carpeta agrupando dos aplicaciones.
 */
@Composable
fun GroupWithAppDialog(
    sourceApp: AppInfo,
    allApps: List<AppInfo>,
    onSelectTargetApp: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crear carpeta con ${sourceApp.label}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Selecciona la otra app para crear la carpeta:",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(14.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(allApps) { _, app ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { onSelectTargetApp(app) }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = app.category.icon,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = app.label,
                                fontSize = 10.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Píldora individual del Dock flotante de HarmonyOS NEXT.
 */
@Composable
private fun DockPillItem(
    iconBg: Brush,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(iconBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

/**
 * Píldora con imagen/ícono personalizado para Firi en el Dock.
 */
@Composable
private fun DockPillImageItem(
    imageRes: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(13.dp)),
            contentScale = ContentScale.Fit
        )
    }
}
