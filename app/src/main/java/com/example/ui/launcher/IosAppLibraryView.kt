package com.example.ui.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.R
import com.example.model.AppCategory
import com.example.model.AppInfo
import com.example.model.UserSettings
import com.example.ui.theme.LauncherPalette

/**
 * Biblioteca de Apps oficial con diseño idéntico a iOS 17 / iOS 18 (Image 2).
 * Incluye cuadrícula de 2 columnas de carpetas translúcidas con 3 iconos grandes directos
 * y 1 clúster 2x2 para ver el resto de aplicaciones de la categoría.
 */
@Composable
fun IosAppLibraryView(
    allApps: List<AppInfo>,
    palette: LauncherPalette,
    userSettings: UserSettings,
    onLaunchApp: (AppInfo) -> Unit,
    onOpenCategoryFolder: (AppCategory, List<AppInfo>) -> Unit,
    onCloseLibrary: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val isEs = userSettings.isSpanish
    val isDark = userSettings.isDarkMode

    val folderCardBg = if (isDark) Color(0x35FFFFFF) else Color(0x40FFFFFF)
    val folderBorderColor = if (isDark) Color(0x28FFFFFF) else Color(0x35FFFFFF)
    val textColor = Color.White
    val textShadow = Shadow(color = Color.Black.copy(alpha = 0.75f), blurRadius = 8f)
    val searchBarBg = if (isDark) Color(0x601C1C1E) else Color(0x55000000)

    // Agrupación de apps por categoría estilo iOS
    val appsByCategory = remember(allApps) {
        val grouped = allApps.groupBy { it.category }
        // Aseguramos categorías sugeridas y recientes al inicio
        val result = mutableListOf<Pair<String, List<AppInfo>>>()

        // Sugeridas (las más usadas o prioritarias)
        val suggestedApps = allApps.take(6)
        if (suggestedApps.isNotEmpty()) {
            result.add((if (isEs) "Sugeridas" else "Suggestions") to suggestedApps)
        }

        // Recientes
        val recentApps = allApps.takeLast(6).reversed()
        if (recentApps.isNotEmpty()) {
            result.add((if (isEs) "Recientes" else "Recently Added") to recentApps)
        }

        // Categorías estándar
        AppCategory.entries.forEach { cat ->
            val list = grouped[cat]
            if (!list.isNullOrEmpty()) {
                val catLabel = cat.getDisplayName(isEs)
                result.add(catLabel to list)
            }
        }
        result
    }

    // Filtrado alfabético estilo iOS cuando se escribe en el buscador
    val searchResults = remember(searchQuery, allApps) {
        if (searchQuery.isBlank()) emptyList()
        else allApps.filter {
            it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
        }.sortedBy { it.label.lowercase() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("ios_app_library_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Barra superior de búsqueda estilo iOS (Image 2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (isEs) "Biblioteca de apps" else "App Library",
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Borrar",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = searchBarBg,
                        unfocusedContainerColor = searchBarBg,
                        focusedBorderColor = Color(0x25FFFFFF),
                        unfocusedBorderColor = Color(0x18FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("app_library_search_input")
                )

                if (searchQuery.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEs) "Cancelar" else "Cancel",
                        fontSize = 15.sp,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { searchQuery = "" }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (searchQuery.isNotBlank()) {
                // Vista alfabética de resultados de búsqueda (lista vertical A-Z estilo iOS)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDark) Color(0x651C1C1E) else Color(0x60000000))
                                .border(0.5.dp, Color(0x30FFFFFF), RoundedCornerShape(14.dp))
                                .clickable { onLaunchApp(app) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (app.iconDrawable != null) {
                                    Image(
                                        bitmap = app.iconDrawable.toBitmap().asImageBitmap(),
                                        contentDescription = app.label,
                                        modifier = Modifier.size(40.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = app.category.icon,
                                        contentDescription = app.label,
                                        tint = Color(0xFF007AFF),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = app.label,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = app.category.getDisplayName(isEs),
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.70f)
                                )
                            }
                        }
                    }
                }
            } else {
                // CUADRÍCULA 2 COLUMNAS ESTILO IOS 17 / 18 (Image 2)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("ios_app_library_grid"),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(appsByCategory, key = { it.first }) { (categoryTitle, apps) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Tarjeta cuadrada translúcida con 4 cuadrantes estilo Image 2
                            Card(
                                modifier = Modifier
                                    .size(width = 164.dp, height = 164.dp)
                                    .clip(RoundedCornerShape(26.dp)),
                                colors = CardDefaults.cardColors(containerColor = folderCardBg),
                                border = BorderStroke(1.dp, folderBorderColor)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Fila Superior (2 Apps grandes)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Cuadrante 1 (Top-Left)
                                            if (apps.isNotEmpty()) {
                                                IosLibraryLargeAppItem(app = apps[0], onClick = { onLaunchApp(apps[0]) })
                                            } else {
                                                Spacer(modifier = Modifier.size(62.dp))
                                            }

                                            // Cuadrante 2 (Top-Right)
                                            if (apps.size > 1) {
                                                IosLibraryLargeAppItem(app = apps[1], onClick = { onLaunchApp(apps[1]) })
                                            } else {
                                                Spacer(modifier = Modifier.size(62.dp))
                                            }
                                        }

                                        // Fila Inferior (App grande + Clúster mini)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Cuadrante 3 (Bottom-Left)
                                            if (apps.size > 2) {
                                                IosLibraryLargeAppItem(app = apps[2], onClick = { onLaunchApp(apps[2]) })
                                            } else {
                                                Spacer(modifier = Modifier.size(62.dp))
                                            }

                                            // Cuadrante 4 (Bottom-Right): Si son 4 exactas, app grande. Si hay > 4, Clúster mini 2x2 estilo Image 2!
                                            if (apps.size == 4) {
                                                IosLibraryLargeAppItem(app = apps[3], onClick = { onLaunchApp(apps[3]) })
                                            } else if (apps.size > 4) {
                                                // Clúster 2x2 de iconos mini que expande la categoría al tocarlo
                                                val remainingApps = apps.drop(3)
                                                Box(
                                                    modifier = Modifier
                                                        .size(62.dp)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(if (isDark) Color(0x28FFFFFF) else Color(0x35FFFFFF))
                                                        .border(0.5.dp, Color(0x25FFFFFF), RoundedCornerShape(16.dp))
                                                        .clickable {
                                                            val cat = apps.firstOrNull()?.category ?: AppCategory.UTILITIES
                                                            onOpenCategoryFolder(cat, apps)
                                                        }
                                                        .padding(5.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        modifier = Modifier.fillMaxSize(),
                                                        verticalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            IosMiniClusterIcon(app = remainingApps.getOrNull(0))
                                                            IosMiniClusterIcon(app = remainingApps.getOrNull(1))
                                                        }
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            IosMiniClusterIcon(app = remainingApps.getOrNull(2))
                                                            IosMiniClusterIcon(app = remainingApps.getOrNull(3))
                                                        }
                                                    }
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.size(62.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Nombre de la categoría centrado abajo con sombra legible sobre cualquier fondo
                            Text(
                                text = categoryTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                style = TextStyle(
                                    shadow = textShadow
                                ),
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
 * Icono grande dentro de una carpeta de la Biblioteca de Apps (toque directo para abrir).
 */
@Composable
private fun IosLibraryLargeAppItem(
    app: AppInfo,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (app.iconDrawable != null) {
            Image(
                bitmap = app.iconDrawable.toBitmap().asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(13.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFF007AFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = app.category.icon,
                    contentDescription = app.label,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Icono mini (24dp) para el cuadrante de clúster 2x2.
 */
@Composable
private fun IosMiniClusterIcon(app: AppInfo?) {
    if (app == null) {
        Spacer(modifier = Modifier.size(22.dp))
        return
    }
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (app.iconDrawable != null) {
            Image(
                bitmap = app.iconDrawable.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(Color(0xFF38BDF8)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.label.take(1),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
