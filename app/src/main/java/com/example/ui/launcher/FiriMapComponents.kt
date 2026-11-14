package com.example.ui.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.R
import com.example.ai.FiriMapPlace
import com.example.ai.FiriWebSource
import com.example.ui.theme.LauncherPalette
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import java.util.Locale

private fun hasValidMapsSdkKey(): Boolean {
    val key = BuildConfig.MAPS_API_KEY
    return key.isNotBlank() && key != "MY_MAPS_API_KEY"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiriGroundingBadgesRow(
    usedSqlData: Boolean,
    usedGoogleSearch: Boolean,
    usedGoogleMaps: Boolean,
    sqlSummary: String?,
    palette: LauncherPalette
) {
    if (!usedSqlData && !usedGoogleSearch && !usedGoogleMaps) return

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (usedSqlData) {
            SourceMiniBadge(
                icon = Icons.Default.Storage,
                label = sqlSummary?.takeIf { it.length <= 28 } ?: stringResource(R.string.firi_sql_badge),
                tint = Color(0xFF8B5CF6),
                palette = palette
            )
        }
        if (usedGoogleSearch) {
            SourceMiniBadge(
                icon = Icons.Default.Public,
                label = stringResource(R.string.firi_search_badge),
                tint = Color(0xFF0EA5E9),
                palette = palette
            )
        }
        if (usedGoogleMaps) {
            SourceMiniBadge(
                icon = Icons.Default.LocationOn,
                label = stringResource(R.string.firi_maps_attribution),
                tint = Color(0xFF10B981),
                palette = palette
            )
        }
    }
}

@Composable
private fun SourceMiniBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    palette: LauncherPalette
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.14f))
            .border(0.75.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.onBackground,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FiriMapPlaceCard(
    places: List<FiriMapPlace>,
    palette: LauncherPalette,
    isSpanish: Boolean,
    onExpandMap: (List<FiriMapPlace>, Int) -> Unit
) {
    if (places.isEmpty()) return
    val context = LocalContext.current
    var selectedIdx by remember(places) { mutableIntStateOf(0) }
    val selectedPlace = places.getOrElse(selectedIdx) { places.first() }
    val useLiveSdk = hasValidMapsSdkKey() &&
        selectedPlace.latitude != null &&
        selectedPlace.longitude != null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("firi_map_card"),
        shape = RoundedCornerShape(16.dp),
        color = palette.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, palette.cardBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Vista previa de mapa (Google Maps SDK vivo si hay clave o Canvas interactivo de coordenadas)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onExpandMap(places, selectedIdx) }
                    .testTag("firi_map_preview_box")
            ) {
                if (useLiveSdk) {
                    val lat = selectedPlace.latitude ?: 0.0
                    val lng = selectedPlace.longitude ?: 0.0
                    val target = LatLng(lat, lng)
                    val cameraState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(target, 14.5f)
                    }
                    LaunchedEffect(lat, lng) {
                        cameraState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 14.5f)
                    }
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            compassEnabled = false,
                            mapToolbarEnabled = false
                        )
                    ) {
                        places.forEachIndexed { idx, p ->
                            if (p.latitude != null && p.longitude != null) {
                                Marker(
                                    state = rememberUpdatedMarkerState(position = LatLng(p.latitude, p.longitude)),
                                    title = p.title,
                                    snippet = p.address.takeIf { it.isNotBlank() },
                                    alpha = if (idx == selectedIdx) 1f else 0.72f
                                )
                            }
                        }
                    }
                } else {
                    FiriStylizedVectorMapPreview(
                        places = places,
                        selectedIdx = selectedIdx,
                        palette = palette,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Botón expandir mapa en esquina superior derecha
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { onExpandMap(places, selectedIdx) }
                        .padding(6.dp)
                        .testTag("firi_expand_map_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = stringResource(R.string.firi_expand_map),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Chip flotante con coordenadas o estado SQL
                val coordLabel = if (selectedPlace.latitude != null && selectedPlace.longitude != null) {
                    String.format(Locale.US, "%.4f, %.4f", selectedPlace.latitude, selectedPlace.longitude)
                } else if (selectedPlace.fromSqlCache) {
                    if (isSpanish) "Guardado en SQL" else "Saved in SQL"
                } else {
                    if (isSpanish) "Toca para abrir mapa" else "Tap to view map"
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = coordLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            // Selector de múltiples lugares si Gemini Maps devolvió más de 1
            if (places.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    places.forEachIndexed { idx, p ->
                        val isSelected = idx == selectedIdx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) palette.primary.copy(alpha = 0.18f)
                                    else palette.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) palette.primary else palette.cardBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedIdx = idx }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("firi_place_chip_$idx")
                        ) {
                            Text(
                                text = p.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) palette.primary else palette.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Nombre y dirección del lugar seleccionado
            Text(
                text = selectedPlace.title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (selectedPlace.address.isNotBlank()) {
                Text(
                    text = selectedPlace.address,
                    fontSize = 11.5.sp,
                    color = palette.onSurface.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botones de acción: Ver mapa / Cómo llegar / Google Maps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onExpandMap(places, selectedIdx) },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("firi_open_full_map_btn"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.5f)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.firi_expand_map),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.primary,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = { openInGoogleMaps(context, selectedPlace, navigate = true) },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("firi_directions_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = null,
                        tint = palette.onPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.firi_navigate),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.onPrimary,
                        maxLines = 1
                    )
                }
            }

            // Atribución obligatoria exacta "Google Maps" en una línea, SansSerif >=12sp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clickable { openInGoogleMaps(context, selectedPlace, navigate = false) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.firi_maps_attribution),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    color = palette.onSurface.copy(alpha = 0.8f),
                    maxLines = 1,
                    softWrap = false
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.firi_open_in_maps),
                        fontSize = 11.sp,
                        color = palette.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.firi_open_in_maps),
                        tint = palette.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FiriStylizedVectorMapPreview(
    places: List<FiriMapPlace>,
    selectedIdx: Int,
    palette: LauncherPalette,
    modifier: Modifier = Modifier
) {
    val bgTop = if (palette.isDark) Color(0xFF132238) else Color(0xFFE6F0FA)
    val bgBottom = if (palette.isDark) Color(0xFF0E1726) else Color(0xFFD5E6F7)
    val roadColor = if (palette.isDark) Color(0xFF283E5E) else Color(0xFFFFFFFF)
    val parkColor = if (palette.isDark) Color(0xFF14362B) else Color(0xFFC5ECD6)
    val waterColor = if (palette.isDark) Color(0xFF0D2D4F) else Color(0xFFAED7FA)
    val pinPrimary = Color(0xFFEA4335)

    Box(modifier = modifier.background(Brush.verticalGradient(listOf(bgTop, bgBottom)))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Área verde / parque estilizado
            drawCircle(
                color = parkColor,
                radius = h * 0.34f,
                center = Offset(w * 0.22f, h * 0.72f)
            )
            // Canal / río estilizado
            val riverPath = Path().apply {
                moveTo(0f, h * 0.25f)
                cubicTo(w * 0.3f, h * 0.1f, w * 0.65f, h * 0.48f, w, h * 0.32f)
            }
            drawPath(
                path = riverPath,
                color = waterColor,
                style = Stroke(width = h * 0.08f, cap = StrokeCap.Round)
            )

            // Retícula vial limpia
            for (i in 1..4) {
                val y = h * (i / 5f)
                drawLine(
                    color = roadColor.copy(alpha = 0.75f),
                    start = Offset(0f, y),
                    end = Offset(w, y + (if (i % 2 == 0) 8f else -8f)),
                    strokeWidth = if (i == 2) 5f else 2.5f
                )
            }
            for (j in 1..5) {
                val x = w * (j / 6f)
                drawLine(
                    color = roadColor.copy(alpha = 0.75f),
                    start = Offset(x, 0f),
                    end = Offset(x + (if (j % 2 == 0) -10f else 10f), h),
                    strokeWidth = if (j == 3) 5f else 2.5f
                )
            }

            // Marcadores posicionados proporcionalmente
            val count = places.size.coerceAtLeast(1)
            places.forEachIndexed { idx, _ ->
                val isSel = idx == selectedIdx
                val px = if (count == 1) w * 0.5f else w * (0.2f + 0.6f * (idx.toFloat() / (count - 1).coerceAtLeast(1)))
                val py = if (isSel) h * 0.46f else h * (0.35f + (idx % 2) * 0.22f)

                if (isSel) {
                    drawCircle(
                        color = pinPrimary.copy(alpha = 0.22f),
                        radius = 26f,
                        center = Offset(px, py)
                    )
                }
                drawCircle(
                    color = Color.White,
                    radius = if (isSel) 13f else 9f,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = if (isSel) pinPrimary else palette.primary,
                    radius = if (isSel) 9.5f else 6.5f,
                    center = Offset(px, py)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiriWebSourcesBar(
    sources: List<FiriWebSource>,
    palette: LauncherPalette
) {
    if (sources.isEmpty()) return
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 7.dp)
    ) {
        Text(
            text = stringResource(R.string.firi_search_badge),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = palette.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            sources.take(4).forEachIndexed { idx, source ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.surface.copy(alpha = 0.85f))
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                        .clickable { openWebUrl(context, source.uri) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("firi_web_source_$idx"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = Color(0xFF0EA5E9),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = source.title.take(28),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun FiriFullMapDialog(
    places: List<FiriMapPlace>,
    initialIndex: Int = 0,
    palette: LauncherPalette,
    isSpanish: Boolean,
    onDismiss: () -> Unit
) {
    if (places.isEmpty()) return
    val context = LocalContext.current
    var selectedIdx by remember(places, initialIndex) {
        mutableIntStateOf(initialIndex.coerceIn(0, places.lastIndex))
    }
    val selectedPlace = places.getOrElse(selectedIdx) { places.first() }
    val useLiveSdk = hasValidMapsSdkKey() &&
        selectedPlace.latitude != null &&
        selectedPlace.longitude != null

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
                // Encabezado
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSpanish) "Mapa interactivo de Firi" else "Firi Interactive Map",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.onBackground
                        )
                        Text(
                            text = stringResource(R.string.firi_maps_attribution),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = palette.onSurface.copy(alpha = 0.75f),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("firi_full_map_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar mapa",
                            tint = palette.onBackground
                        )
                    }
                }

                // Área principal del mapa
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.55f)
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                ) {
                    if (useLiveSdk) {
                        val lat = selectedPlace.latitude ?: 0.0
                        val lng = selectedPlace.longitude ?: 0.0
                        val cameraState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 15f)
                        }
                        LaunchedEffect(lat, lng) {
                            cameraState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 15f)
                        }
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraState
                        ) {
                            places.forEachIndexed { idx, p ->
                                if (p.latitude != null && p.longitude != null) {
                                    Marker(
                                        state = rememberUpdatedMarkerState(position = LatLng(p.latitude, p.longitude)),
                                        title = p.title,
                                        snippet = p.address.takeIf { it.isNotBlank() },
                                        alpha = if (idx == selectedIdx) 1f else 0.75f
                                    )
                                }
                            }
                        }
                    } else {
                        FiriStylizedVectorMapPreview(
                            places = places,
                            selectedIdx = selectedIdx,
                            palette = palette,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.68f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            val hint = if (selectedPlace.latitude != null && selectedPlace.longitude != null) {
                                String.format(
                                    Locale.US,
                                    "Lat %.5f, Lng %.5f · %s",
                                    selectedPlace.latitude,
                                    selectedPlace.longitude,
                                    if (isSpanish) "Usa 'Cómo llegar' para abrir GPS" else "Tap 'Directions' for turn-by-turn GPS"
                                )
                            } else {
                                if (isSpanish) "Toca 'Abrir en Google Maps' para vista satelital y rutas"
                                else "Tap 'Open in Google Maps' for live satellite & routes"
                            }
                            Text(
                                text = hint,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // Lista de lugares y controles
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.45f)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(places) { idx, place ->
                        val isSelected = idx == selectedIdx
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedIdx = idx },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) palette.primary.copy(alpha = 0.12f) else palette.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) palette.primary else palette.cardBorder
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = place.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.onBackground,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (place.fromSqlCache) {
                                        Text(
                                            text = "SQL",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8B5CF6)
                                        )
                                    }
                                }
                                if (place.address.isNotBlank()) {
                                    Text(
                                        text = place.address,
                                        fontSize = 12.sp,
                                        color = palette.onSurface.copy(alpha = 0.78f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { openInGoogleMaps(context, place, navigate = false) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.firi_open_in_maps),
                                            fontSize = 11.5.sp,
                                            maxLines = 1
                                        )
                                    }
                                    Button(
                                        onClick = { openInGoogleMaps(context, place, navigate = true) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.firi_navigate),
                                            fontSize = 11.5.sp,
                                            color = palette.onPrimary,
                                            maxLines = 1
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
}

fun openInGoogleMaps(context: Context, place: FiriMapPlace, navigate: Boolean) {
    val targetUri = when {
        navigate && place.latitude != null && place.longitude != null ->
            Uri.parse("google.navigation:q=${place.latitude},${place.longitude}")
        navigate ->
            Uri.parse("google.navigation:q=${Uri.encode(place.address.ifBlank { place.title })}")
        place.mapsUri.isNotBlank() ->
            Uri.parse(place.mapsUri)
        place.latitude != null && place.longitude != null ->
            Uri.parse("geo:${place.latitude},${place.longitude}?q=${place.latitude},${place.longitude}(${Uri.encode(place.title)})")
        else ->
            Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(place.title)}")
    }

    try {
        val mapsIntent = Intent(Intent.ACTION_VIEW, targetUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(mapsIntent)
    } catch (_: Exception) {
        val fallbackWebUri = if (place.mapsUri.startsWith("http")) {
            Uri.parse(place.mapsUri)
        } else {
            Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(place.address.ifBlank { place.title })}")
        }
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, fallbackWebUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "No hay aplicación de mapas disponible", Toast.LENGTH_SHORT).show()
        }
    }
}

fun openWebUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
    }
}
