package com.example.ui.launcher

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.ai.FiriImageEngine
import com.example.ui.theme.LauncherPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FiriImageGeneratingCard(
    isEditing: Boolean,
    prompt: String,
    isSpanish: Boolean,
    palette: LauncherPalette
) {
    val transition = rememberInfiniteTransition(label = "firi_img_shimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("firi_image_generating_card"),
        shape = RoundedCornerShape(18.dp),
        color = palette.surfaceVariant,
        border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.45f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            palette.primary.copy(alpha = 0.08f),
                            palette.secondary.copy(alpha = 0.18f * pulse),
                            palette.primary.copy(alpha = 0.08f)
                        ),
                        start = Offset(shimmerOffset, 0f),
                        end = Offset(shimmerOffset + 320f, 180f)
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = palette.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEditing) {
                            stringResource(R.string.firi_image_editing)
                        } else {
                            stringResource(R.string.firi_image_generating)
                        },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.onBackground
                    )
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        color = palette.onSurface.copy(alpha = 0.78f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = if (isSpanish) "Procesando renderizado multimodal (puede tardar 15-50s)…"
                        else "Synthesizing multimodal render (may take 15-50s)…",
                        fontSize = 10.sp,
                        color = palette.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FiriGeneratedImageCard(
    imagePath: String,
    modelUsed: String?,
    isSpanish: Boolean,
    palette: LauncherPalette,
    onEditThisImage: (String) -> Unit,
    onSetWallpaper: (String, Boolean) -> Unit,
    onOpenFullImage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showWallpaperMenu by remember { mutableStateOf(false) }

    val modelBadge = remember(modelUsed) {
        when {
            modelUsed.isNullOrBlank() || modelUsed.contains("nano-banana", ignoreCase = true) ->
                "✨ Nano Banana 2.1"
            else -> "✨ ${modelUsed.removePrefix("models/")}"
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("firi_generated_image_card"),
        shape = RoundedCornerShape(16.dp),
        color = palette.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, palette.cardBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceVariant)
                    .clickable { onOpenFullImage(imagePath) }
                    .testTag("firi_generated_image_preview")
            ) {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = if (isSpanish) "Imagen generada por Firi" else "Firi generated image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )

                // Badge del modelo en esquina superior izquierda
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = modelBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // Botón pantalla completa en esquina superior derecha
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.58f))
                        .clickable { onOpenFullImage(imagePath) }
                        .padding(6.dp)
                        .testTag("firi_expand_image_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = if (isSpanish) "Ampliar imagen" else "Expand image",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fila de acciones: Editar / Guardar / Fondo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onEditThisImage(imagePath) },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("firi_edit_generated_image_btn"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = stringResource(R.string.firi_image_edit),
                        tint = palette.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.firi_image_edit),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.primary,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val saved = withContext(Dispatchers.IO) {
                                FiriImageEngine.saveToDeviceGallery(context, imagePath)
                            }
                            Toast.makeText(
                                context,
                                if (saved) {
                                    if (isSpanish) "Imagen guardada en tu galería (Pictures/Firi AI)"
                                    else "Image saved to gallery (Pictures/Firi AI)"
                                } else {
                                    if (isSpanish) "No se pudo guardar la imagen"
                                    else "Could not save image"
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("firi_save_generated_image_btn"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.cardBorder),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = stringResource(R.string.firi_image_save),
                        tint = palette.onBackground,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.firi_image_save),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.onBackground,
                        maxLines = 1
                    )
                }

                Box(modifier = Modifier.weight(1.1f)) {
                    Button(
                        onClick = { showWallpaperMenu = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("firi_wallpaper_image_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = stringResource(R.string.firi_image_wallpaper),
                            tint = palette.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSpanish) "Fondo" else "Wallpaper",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.onPrimary,
                            maxLines = 1
                        )
                    }

                    DropdownMenu(
                        expanded = showWallpaperMenu,
                        onDismissRequest = { showWallpaperMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isSpanish) "Fondo de Inicio" else "Home Wallpaper",
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                showWallpaperMenu = false
                                onSetWallpaper(imagePath, false)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isSpanish) "Fondo de Bloqueo" else "Lock Wallpaper",
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                showWallpaperMenu = false
                                onSetWallpaper(imagePath, true)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FiriFullImagePreviewDialog(
    imagePath: String,
    isSpanish: Boolean,
    palette: LauncherPalette,
    onEditThisImage: (String) -> Unit,
    onSetWallpaper: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showWallpaperMenu by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(26.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.cardBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isSpanish) "Estudio Visual Firi" else "Firi Visual Studio",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.onBackground
                        )
                        Text(
                            text = "Nano Banana 2.1",
                            fontSize = 11.5.sp,
                            color = palette.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("firi_full_image_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isSpanish) "Cerrar vista previa" else "Close preview",
                            tint = palette.onBackground
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = File(imagePath),
                        contentDescription = if (isSpanish) "Vista completa de imagen" else "Full image preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onEditThisImage(imagePath)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("firi_full_edit_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.firi_image_edit),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val saved = withContext(Dispatchers.IO) {
                                    FiriImageEngine.saveToDeviceGallery(context, imagePath)
                                }
                                Toast.makeText(
                                    context,
                                    if (saved) {
                                        if (isSpanish) "Guardada en Pictures/Firi AI" else "Saved to Pictures/Firi AI"
                                    } else {
                                        if (isSpanish) "Error al guardar" else "Save failed"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("firi_full_save_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.firi_image_save),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    Box(modifier = Modifier.weight(1.1f)) {
                        Button(
                            onClick = { showWallpaperMenu = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("firi_full_wallpaper_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wallpaper,
                                contentDescription = null,
                                tint = palette.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isSpanish) "Fondo" else "Wallpaper",
                                fontSize = 12.sp,
                                color = palette.onPrimary,
                                maxLines = 1
                            )
                        }
                        DropdownMenu(
                            expanded = showWallpaperMenu,
                            onDismissRequest = { showWallpaperMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isSpanish) "Fondo de Inicio" else "Home Wallpaper") },
                                onClick = {
                                    showWallpaperMenu = false
                                    onSetWallpaper(imagePath, false)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isSpanish) "Fondo de Bloqueo" else "Lock Wallpaper") },
                                onClick = {
                                    showWallpaperMenu = false
                                    onSetWallpaper(imagePath, true)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
