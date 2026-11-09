package com.example.ui.common

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.WallpaperManager

@Composable
fun WallpaperBackground(
    wallpaperId: String,
    customWallpaperUri: String,
    modifier: Modifier = Modifier,
    scrimBrush: Brush? = null
) {
    val context = LocalContext.current
    val hasCustom = remember(customWallpaperUri) {
        customWallpaperUri.isNotBlank()
    }

    Box(modifier = modifier.fillMaxSize()) {
        val preset = remember(wallpaperId) {
            WallpaperManager.getPreset(wallpaperId)
        }

        if (hasCustom) {
            val imageRequest = remember(customWallpaperUri) {
                ImageRequest.Builder(context)
                    .data(com.example.util.WallpaperStorageHelper.toCoilModel(customWallpaperUri))
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = "Fondo de pantalla personalizado",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(id = preset.drawableRes)
            )
        } else {
            Image(
                painter = painterResource(id = preset.drawableRes),
                contentDescription = preset.nameEs,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (scrimBrush != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimBrush)
            )
        }
    }
}
