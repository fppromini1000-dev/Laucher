package com.example.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.PresetWallpaper
import com.example.model.WallpaperManager
import com.example.ui.theme.LauncherPalette
import com.example.util.WallpaperStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DeviceSamplePhoto(
    val id: String,
    val nameEs: String,
    val nameEn: String,
    @field:DrawableRes val resId: Int
)

@Composable
fun WallpaperSelectorSection(
    title: String,
    subtitle: String,
    selectedWallpaperId: String,
    customWallpaperUri: String,
    palette: LauncherPalette,
    isSpanish: Boolean,
    onSelectPreset: (String) -> Unit,
    onSelectCustomUri: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showDevicePhotoDialog by remember { mutableStateOf(false) }

    // Helper to process and permanently store selected URI
    fun handleSelectedUri(uri: Uri?) {
        if (uri == null) return
        coroutineScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                WallpaperStorageHelper.saveUriToInternalStorage(context, uri, "wallpaper")
            }
            val finalTarget = savedPath ?: uri.toString()
            onSelectCustomUri(finalTarget)
            showDevicePhotoDialog = false
            Toast.makeText(
                context,
                if (isSpanish) "¡Foto del dispositivo establecida como fondo!" else "Device photo set as wallpaper!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Helper to process captured camera bitmap
    fun handleCapturedBitmap(bitmap: Bitmap?) {
        if (bitmap == null) return
        coroutineScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                WallpaperStorageHelper.saveBitmapToInternalStorage(context, bitmap, "camera_wallpaper")
            }
            if (savedPath != null) {
                onSelectCustomUri(savedPath)
                showDevicePhotoDialog = false
                Toast.makeText(
                    context,
                    if (isSpanish) "¡Foto tomada establecida como fondo!" else "Captured photo set as wallpaper!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Contract 1: Standard Universal GetContent (supported on 100% of Android devices & emulators)
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedUri(uri)
        } else {
            Toast.makeText(
                context,
                if (isSpanish) "No se seleccionó imagen. Puedes elegir una de las fotos de muestra abajo." else "No image selected. You can choose a sample photo below.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Contract 2: Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedUri(uri)
        } else {
            Toast.makeText(
                context,
                if (isSpanish) "No se seleccionó imagen. Puedes elegir una de las fotos de muestra abajo." else "No image selected. You can choose a sample photo below.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Contract 3: Camera Capture
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            handleCapturedBitmap(bitmap)
        } else {
            Toast.makeText(
                context,
                if (isSpanish) "Foto no capturada" else "Photo not captured",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Robust open gallery function with universal fallback
    val launchDeviceGallery = {
        try {
            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                try {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                } catch (_: Exception) {
                    getContentLauncher.launch("image/*")
                }
            } else {
                getContentLauncher.launch("image/*")
            }
        } catch (_: Exception) {
            try {
                getContentLauncher.launch("image/*")
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (isSpanish) "No se encontró aplicación de galería en el dispositivo" else "No gallery app found on device",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(palette.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wallpaper,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.onBackground
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = palette.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Botón destacado: "Foto del Dispositivo" / "Device Photo"
            OutlinedButton(
                onClick = {
                    showDevicePhotoDialog = true
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.2.dp, palette.primary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("pick_custom_wallpaper_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isSpanish) "Foto Dispositivo" else "Device Photo",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Si el usuario tiene una imagen personalizada seleccionada
        if (customWallpaperUri.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                colors = CardDefaults.cardColors(containerColor = palette.primary.copy(alpha = 0.12f)),
                border = BorderStroke(1.5.dp, palette.primary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp, 60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(WallpaperStorageHelper.toCoilModel(customWallpaperUri))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Fondo personalizado activo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.wallpaper_ios_mud)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isSpanish) "✓ Foto del celular activa" else "✓ Device photo active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                            Text(
                                text = if (isSpanish) "Tu fotografía se muestra como fondo" else "Your photo is set as wallpaper",
                                fontSize = 10.5.sp,
                                color = palette.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.primary.copy(alpha = 0.18f))
                                .clickable { showDevicePhotoDialog = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isSpanish) "Cambiar" else "Change",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.primary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                .clickable { onSelectCustomUri("") }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isSpanish) "Quitar" else "Remove",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }

        // Carrusel horizontal de Fondos Predefinidos (Ondas Titanio, Cristal, Turquesa, Deep Blue, etc.)
        Text(
            text = if (isSpanish) "O elige un fondo predeterminado:" else "Or choose a preset wallpaper:",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.onBackground.copy(alpha = 0.85f),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Primer item: Añadir o Cambiar Foto Propia / Galería
            item {
                val hasCustom = customWallpaperUri.isNotBlank()
                Card(
                    modifier = Modifier
                        .width(96.dp)
                        .height(134.dp)
                        .clickable { showDevicePhotoDialog = true }
                        .testTag("add_custom_photo_carousel_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasCustom) palette.primary.copy(alpha = 0.18f) else palette.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (hasCustom) palette.primary else palette.cardBorder
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (hasCustom) Icons.Default.Check else Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (hasCustom) (if (isSpanish) "Foto Activa" else "Active Photo") else (if (isSpanish) "+ Tu Foto" else "+ Your Photo"),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.onBackground
                        )
                        Text(
                            text = if (isSpanish) "Galería" else "Gallery",
                            fontSize = 9.5.sp,
                            color = palette.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            items(WallpaperManager.PRESETS) { preset ->
                val isSelected = (customWallpaperUri.isBlank() && selectedWallpaperId == preset.id)
                PresetWallpaperThumbCard(
                    preset = preset,
                    isSelected = isSelected,
                    palette = palette,
                    isSpanish = isSpanish,
                    onClick = {
                        onSelectCustomUri("") // Desactivar URI custom al elegir preset
                        onSelectPreset(preset.id)
                    }
                )
            }
        }
    }

    // Diálogo completo de selección de Foto del Dispositivo
    if (showDevicePhotoDialog) {
        DevicePhotoPickerDialog(
            palette = palette,
            isSpanish = isSpanish,
            onOpenGallery = {
                launchDeviceGallery()
            },
            onTakePhoto = {
                try {
                    takePictureLauncher.launch(null)
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        if (isSpanish) "Cámara no disponible" else "Camera unavailable",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onSelectSample = { sampleResId ->
                coroutineScope.launch {
                    val savedPath = withContext(Dispatchers.IO) {
                        WallpaperStorageHelper.saveDrawableToInternalStorage(context, sampleResId, "device_sample")
                    }
                    if (savedPath != null) {
                        onSelectCustomUri(savedPath)
                        showDevicePhotoDialog = false
                        Toast.makeText(
                            context,
                            if (isSpanish) "¡Foto del dispositivo guardada y aplicada!" else "Device photo saved and applied!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onSetUrl = { url ->
                if (url.isNotBlank()) {
                    onSelectCustomUri(url.trim())
                    showDevicePhotoDialog = false
                    Toast.makeText(
                        context,
                        if (isSpanish) "¡Fondo web configurado!" else "Web wallpaper set!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDismiss = { showDevicePhotoDialog = false }
        )
    }
}

@Composable
fun DevicePhotoPickerDialog(
    palette: LauncherPalette,
    isSpanish: Boolean,
    onOpenGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onSelectSample: (Int) -> Unit,
    onSetUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var webUrlInput by remember { mutableStateOf("") }
    var showUrlField by remember { mutableStateOf(false) }

    val samplePhotos = remember {
        listOf(
            DeviceSamplePhoto("alpino", "Reflejo Alpino", "Alpine Reflection", R.drawable.img_device_nature_photo),
            DeviceSamplePhoto("neon", "Metrópolis Neón", "Neon Metropolis", R.drawable.img_device_city_neon),
            DeviceSamplePhoto("aurora", "Nebulosa Aurora", "Aurora Nebula", R.drawable.lockscreen_wallpaper),
            DeviceSamplePhoto("cristal", "Prisma de Cristal", "Crystal Prism", R.drawable.wallpaper_ios_glass),
            DeviceSamplePhoto("titanio", "Ondas Titanio", "Titanium Curves", R.drawable.wallpaper_ios_mud),
            DeviceSamplePhoto("oasis", "Oasis Profundo", "Deep Oasis", R.drawable.wallpaper_geom_dark)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.cardBorder),
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(palette.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isSpanish) "Foto del Dispositivo" else "Device Photo Wallpaper",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.onBackground
                            )
                            Text(
                                text = if (isSpanish) "Elige o sube una fotografía del celular" else "Pick or load a phone photo",
                                fontSize = 11.5.sp,
                                color = palette.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = palette.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // BOTÓN PRINCIPAL 1: ABRIR GALERÍA DEL TELÉFONO
                Button(
                    onClick = {
                        onOpenGallery()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_phone_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSpanish) "Abrir Galería / Archivos del Teléfono" else "Open Phone Gallery / Files",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // BOTÓN 2: TOMAR FOTO CON LA CÁMARA
                OutlinedButton(
                    onClick = {
                        onTakePhoto()
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, palette.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("take_camera_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSpanish) "Tomar Foto con Cámara" else "Take Photo with Camera",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECCIÓN: FOTOS DE MUESTRA DEL CELULAR (HD)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSpanish) "Fotos de Galería del Dispositivo (HD):" else "Device Gallery Photos (HD):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.onBackground
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSpanish) "Listas para usar" else "Ready to use",
                        fontSize = 10.5.sp,
                        color = palette.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(samplePhotos) { sample ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(88.dp)
                                .clickable { onSelectSample(sample.resId) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 88.dp, height = 136.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.2.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = sample.resId),
                                    contentDescription = sample.nameEs,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isSpanish) sample.nameEs else sample.nameEn,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.onBackground,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ENLACE WEB OPCIONAL
                if (!showUrlField) {
                    Text(
                        text = if (isSpanish) "o usar un enlace de imagen web" else "or enter an image URL",
                        fontSize = 11.sp,
                        color = palette.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { showUrlField = true }
                            .padding(vertical = 4.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = if (isSpanish) "Enlace de Imagen Web (HTTPS):" else "Web Image Link (HTTPS):",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = webUrlInput,
                                onValueChange = { webUrlInput = it },
                                placeholder = {
                                    Text(
                                        text = "https://ejemplo.com/foto.jpg",
                                        fontSize = 11.sp
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = palette.primary,
                                    unfocusedBorderColor = palette.cardBorder
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { onSetUrl(webUrlInput) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(if (isSpanish) "Aplicar" else "Apply", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PresetWallpaperThumbCard(
    preset: PresetWallpaper,
    isSelected: Boolean,
    palette: LauncherPalette,
    isSpanish: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(82.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(width = 82.dp, height = 130.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    if (isSelected) 2.5.dp else 1.dp,
                    if (isSelected) palette.primary else palette.cardBorder,
                    RoundedCornerShape(14.dp)
                )
        ) {
            Image(
                painter = painterResource(id = preset.drawableRes),
                contentDescription = preset.nameEs,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Indicador de Check si está seleccionado
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(palette.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = palette.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isSpanish) preset.nameEs else preset.nameEn,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) palette.primary else palette.onBackground,
            maxLines = 1
        )
    }
}
