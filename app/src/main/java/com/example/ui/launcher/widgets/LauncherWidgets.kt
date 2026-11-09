package com.example.ui.launcher.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppInfo
import com.example.model.HabitPrediction
import com.example.model.LauncherWidgetItem
import com.example.model.TimeOfDaySlot
import com.example.model.UserSettings
import com.example.model.WidgetType
import com.example.ui.theme.LauncherPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WidgetCardContainer(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    palette: LauncherPalette,
    onRemove: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = palette.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = BorderStroke(1.dp, palette.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Widget Title Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.onBackground
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp).testTag("remove_widget_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Eliminar widget",
                        tint = palette.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            content()
        }
    }
}

@Composable
fun WeatherWidget(
    palette: LauncherPalette,
    userSettings: UserSettings,
    onRemove: () -> Unit
) {
    val isEs = userSettings.isSpanish
    WidgetCardContainer(
        title = if (isEs) "Clima y Pronóstico" else "Weather & Forecast",
        icon = Icons.Default.Cloud,
        palette = palette,
        onRemove = onRemove
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "22°",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Light,
                        color = palette.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isEs) "Despejado" else "Clear Sky",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.primary
                        )
                        Text(
                            text = if (isEs) "Máx 25° • Mín 16°" else "H: 25° • L: 16°",
                            fontSize = 11.sp,
                            color = palette.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Text(
                    text = "${userSettings.regionCode} • ${if (isEs) "Viento 10 km/h • Humedad 48%" else "Wind 10 km/h • Humidity 48%"}",
                    fontSize = 11.sp,
                    color = palette.onSurface.copy(alpha = 0.5f)
                )
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(palette.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun FiriHabitsWidget(
    prediction: HabitPrediction?,
    palette: LauncherPalette,
    userSettings: UserSettings,
    onTriggerVoice: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onRemove: () -> Unit
) {
    val isEs = userSettings.isSpanish
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val slot = TimeOfDaySlot.current(hour)

    WidgetCardContainer(
        title = if (isEs) "Firi Smart Habits" else "Firi Smart Habits",
        icon = Icons.Default.Psychology,
        palette = palette,
        onRemove = onRemove
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = slot.iconEmoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEs) slot.titleEs else slot.titleEn,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val topApp = prediction?.topApps?.firstOrNull()
                if (topApp != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surfaceVariant)
                            .clickable { onLaunchApp(topApp) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEs) "Recomendada: " else "Suggested: ",
                            fontSize = 11.sp,
                            color = palette.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = topApp.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                    }
                } else {
                    Text(
                        text = if (isEs) "Aprendiendo tus horarios de hoy" else "Learning your schedules today",
                        fontSize = 11.sp,
                        color = palette.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Quick Voice Trigger Orb inside widget with Firi icon (reemplazando el micrófono)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.5.dp, palette.primary.copy(alpha = 0.5f), CircleShape)
                    .clickable { onTriggerVoice() }
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.firi_app_icon),
                    contentDescription = "Hablar con Firi",
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun NotesTasksWidget(
    widgetItem: LauncherWidgetItem,
    palette: LauncherPalette,
    userSettings: UserSettings,
    onUpdateData: (String) -> Unit,
    onRemove: () -> Unit
) {
    val isEs = userSettings.isSpanish
    var newTaskText by remember { mutableStateOf("") }

    val rawTasks = remember(widgetItem.customData) {
        if (widgetItem.customData.isBlank()) {
            listOf("Revisar correo", "Preparar presentación")
        } else {
            widgetItem.customData.split(";").filter { it.isNotBlank() }
        }
    }

    var tasks by remember(rawTasks) { mutableStateOf(rawTasks) }

    WidgetCardContainer(
        title = if (isEs) "Tareas y Notas Rápidas" else "Tasks & Quick Notes",
        icon = Icons.Default.CheckCircle,
        palette = palette,
        onRemove = onRemove
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tasks.take(4).forEachIndexed { idx, task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val mutable = tasks.toMutableList()
                            mutable.removeAt(idx)
                            tasks = mutable
                            onUpdateData(mutable.joinToString(";"))
                        }
                        .padding(vertical = 3.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task,
                        fontSize = 12.sp,
                        color = palette.onBackground
                    )
                }
            }

            // Quick add row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    placeholder = {
                        Text(
                            if (isEs) "Nueva tarea..." else "New task...",
                            fontSize = 11.sp,
                            color = palette.onSurface.copy(alpha = 0.4f)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primary,
                        unfocusedBorderColor = palette.cardBorder,
                        focusedTextColor = palette.onBackground,
                        unfocusedTextColor = palette.onBackground,
                        focusedContainerColor = palette.surface.copy(alpha = 0.5f),
                        unfocusedContainerColor = palette.surface.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        if (newTaskText.isNotBlank()) {
                            val updated = tasks + newTaskText.trim()
                            tasks = updated
                            onUpdateData(updated.joinToString(";"))
                            newTaskText = ""
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Añadir",
                        tint = palette.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarWidget(
    palette: LauncherPalette,
    userSettings: UserSettings,
    onRemove: () -> Unit
) {
    val isEs = userSettings.isSpanish
    val date = Date()
    val dayFormat = SimpleDateFormat("d", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
    val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())

    WidgetCardContainer(
        title = if (isEs) "Agenda de Hoy" else "Today's Agenda",
        icon = Icons.Default.CalendarMonth,
        palette = palette,
        onRemove = onRemove
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.primary),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dayFormat.format(date),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.onPrimary
                    )
                    Text(
                        text = monthFormat.format(date).take(3).uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = dayOfWeekFormat.format(date).replaceFirstChar { it.uppercase() },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.onBackground
                )
                Text(
                    text = if (isEs) "Sin eventos pendientes para hoy" else "No pending events scheduled",
                    fontSize = 11.sp,
                    color = palette.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun MusicPlayerWidget(
    palette: LauncherPalette,
    userSettings: UserSettings,
    onRemove: () -> Unit
) {
    val isEs = userSettings.isSpanish
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0.42f) }

    WidgetCardContainer(
        title = if (isEs) "Reproductor de Música" else "Media Player",
        icon = Icons.Default.MusicNote,
        palette = palette,
        onRemove = onRemove
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Firi Minimal Focus",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.onBackground
                        )
                        Text(
                            text = "Lo-Fi Instrumental Beats",
                            fontSize = 11.sp,
                            color = palette.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { progress = (progress - 0.1f).coerceAtLeast(0f) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Atrás",
                            tint = palette.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(palette.primary)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = palette.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { progress = (progress + 0.1f).coerceAtMost(1f) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Adelante",
                            tint = palette.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = palette.primary,
                trackColor = palette.surfaceVariant
            )
        }
    }
}
