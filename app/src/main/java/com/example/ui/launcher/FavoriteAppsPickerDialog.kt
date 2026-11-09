package com.example.ui.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.example.model.AppInfo
import com.example.model.UserSettings
import com.example.ui.theme.LauncherPalette

@Composable
fun FavoriteAppsPickerDialog(
    allApps: List<AppInfo>,
    userSettings: UserSettings,
    palette: LauncherPalette,
    onSaveFavorites: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val isEs = userSettings.isSpanish
    val selectedPackages = remember {
        mutableStateListOf<String>().apply {
            addAll(userSettings.favoriteAppPackageNames)
        }
    }

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
            color = palette.surface,
            border = BorderStroke(1.dp, palette.cardBorder),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(palette.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEs) "Elegir Apps Favoritas" else "Choose Favorite Apps",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.onBackground
                            )
                            Text(
                                text = if (isEs) "Selecciona hasta 4 aplicaciones destacadas" else "Select up to 4 featured apps",
                                fontSize = 11.sp,
                                color = palette.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = palette.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Lista de aplicaciones
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allApps) { app ->
                        val isSelected = selectedPackages.contains(app.packageName)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) {
                                        selectedPackages.remove(app.packageName)
                                    } else {
                                        if (selectedPackages.size < 4) {
                                            selectedPackages.add(app.packageName)
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) palette.primary.copy(alpha = 0.12f) else palette.surfaceVariant.copy(alpha = 0.6f)
                            ),
                            border = BorderStroke(1.dp, if (isSelected) palette.primary else palette.cardBorder),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (app.iconDrawable != null) {
                                        val bitmap = remember(app.iconDrawable) {
                                            try { app.iconDrawable.toBitmap(64, 64) } catch (_: Exception) { null }
                                        }
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = app.label,
                                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                            )
                                        } else {
                                            Icon(imageVector = app.category.icon, contentDescription = null, tint = palette.primary, modifier = Modifier.size(24.dp))
                                        }
                                    } else {
                                        Icon(imageVector = app.category.icon, contentDescription = null, tint = palette.primary, modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = app.label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = palette.onBackground
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) palette.primary else Color.Transparent)
                                        .border(1.5.dp, if (isSelected) palette.primary else palette.cardBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = palette.onPrimary, modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        onSaveFavorites(selectedPackages.toList())
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_favorite_apps_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = if (isEs) "Guardar Selección (${selectedPackages.size}/4)" else "Save Selection (${selectedPackages.size}/4)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
