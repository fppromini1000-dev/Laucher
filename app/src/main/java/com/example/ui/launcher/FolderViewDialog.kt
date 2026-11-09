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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.example.model.AppFolder
import com.example.model.AppInfo
import com.example.ui.theme.LauncherPalette

@Composable
fun FolderViewDialog(
    folder: AppFolder,
    apps: List<AppInfo>,
    palette: LauncherPalette,
    isEditMode: Boolean,
    onLaunchApp: (AppInfo) -> Unit,
    onRemoveAppFromFolder: (folderId: String, app: AppInfo) -> Unit,
    onRenameFolder: (folderId: String, newName: String) -> Unit,
    onDissolveFolder: (folderId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var isEditingTitle by remember { mutableStateOf(false) }
    var folderTitleText by remember(folder.name) { mutableStateOf(folder.name) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .testTag("folder_view_card"),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B).copy(alpha = 0.90f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Barra superior: Título editable de la carpeta y botón cerrar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditingTitle) {
                            OutlinedTextField(
                                value = folderTitleText,
                                onValueChange = { folderTitleText = it },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                                    .testTag("folder_title_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                                )
                            )
                            IconButton(
                                onClick = {
                                    onRenameFolder(folder.id, folderTitleText)
                                    isEditingTitle = false
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("✓", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { isEditingTitle = true }
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                            ) {
                                Text(
                                    text = folder.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar nombre",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .testTag("close_folder_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Cuadrícula de aplicaciones dentro de la carpeta
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(apps, key = { it.packageName }) { app ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        onDismiss()
                                        onLaunchApp(app)
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(54.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White.copy(alpha = 0.95f))
                                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (app.iconDrawable != null) {
                                            val bitmap = remember(app.iconDrawable) {
                                                try {
                                                    app.iconDrawable.toBitmap(96, 96)
                                                } catch (_: Exception) {
                                                    null
                                                }
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
                                                    contentDescription = app.label,
                                                    tint = Color(0xFF007AFF),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = app.category.icon,
                                                contentDescription = app.label,
                                                tint = Color(0xFF007AFF),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    // Botón para sacar app de la carpeta
                                    IconButton(
                                        onClick = { onRemoveAppFromFolder(folder.id, app) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE11D48))
                                            .testTag("remove_app_from_folder_${app.packageName.replace('.', '_')}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Sacar de carpeta",
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = app.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Opción para disolver carpeta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = {
                                onDissolveFolder(folder.id)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("dissolve_folder_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Disolver carpeta",
                                color = Color(0xFFF87171),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
