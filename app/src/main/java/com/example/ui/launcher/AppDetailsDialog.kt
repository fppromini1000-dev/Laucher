package com.example.ui.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.AppCategory
import com.example.model.AppInfo
import com.example.model.UserSettings
import com.example.ui.theme.LauncherPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppDetailsDialog(
    app: AppInfo,
    palette: LauncherPalette,
    userSettings: UserSettings,
    onUpdateCategory: (AppCategory) -> Unit,
    onTogglePin: () -> Unit,
    onStartEditMode: () -> Unit,
    onDeleteOrHide: () -> Unit,
    onUninstall: () -> Unit,
    onLaunch: () -> Unit,
    onDismiss: () -> Unit
) {
    val isEs = userSettings.isSpanish

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (app.packageName == "com.firi.ai" || app.label.contains("Firi", ignoreCase = true)) Color.White else palette.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (app.packageName == "com.firi.ai" || app.label.contains("Firi", ignoreCase = true)) {
                                Image(
                                    painter = painterResource(id = R.drawable.firi_app_icon),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(
                                    imageVector = app.category.icon,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = app.label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.onBackground
                            )
                            Text(
                                text = app.category.getDisplayName(isEs),
                                fontSize = 12.sp,
                                color = palette.primary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = palette.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Pin and Launch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onTogglePin,
                        modifier = Modifier.weight(1f).testTag("toggle_pin_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, palette.cardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (app.isPinned) palette.primary else palette.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (app.isPinned) {
                                if (isEs) "Desanclar" else "Unpin"
                            } else {
                                if (isEs) "Anclar" else "Pin"
                            },
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onLaunch()
                        },
                        modifier = Modifier.weight(1f).testTag("dialog_launch_app_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Launch,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEs) "Abrir" else "Open", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Move and Delete actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mover / Organizar (triggers jiggle mode)
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onStartEditMode()
                        },
                        modifier = Modifier.weight(1f).testTag("move_apps_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = palette.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenWith,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEs) "Mover" else "Move",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Eliminar / Ocultar
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onDeleteOrHide()
                        },
                        modifier = Modifier.weight(1f).testTag("delete_or_hide_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, palette.secondary.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = palette.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEs) "Eliminar" else "Remove",
                            fontSize = 12.sp
                        )
                    }

                    // Desinstalar
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onUninstall()
                        },
                        modifier = Modifier.weight(1.1f).testTag("uninstall_app_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, palette.cardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = palette.onSurface.copy(alpha = 0.8f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEs) "Desinstalar" else "Uninstall",
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = if (isEs) "Cambiar categoría organizada:" else "Change organized category:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.onSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category selection chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppCategory.entries.forEach { category ->
                        val isSelected = category == app.category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) palette.primary else palette.surfaceVariant)
                                .clickable {
                                    onUpdateCategory(category)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("cat_option_${category.id}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) palette.onPrimary else palette.onSurface,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = category.getDisplayName(isEs),
                                    fontSize = 11.sp,
                                    color = if (isSelected) palette.onPrimary else palette.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
