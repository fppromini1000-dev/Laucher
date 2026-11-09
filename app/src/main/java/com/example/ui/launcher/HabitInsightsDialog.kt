package com.example.ui.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AppInfo
import com.example.model.HabitPrediction
import com.example.model.TimeOfDaySlot
import com.example.model.UserSettings
import com.example.ui.theme.LauncherPalette

@Composable
fun HabitInsightsDialog(
    prediction: HabitPrediction?,
    summaryText: String,
    palette: LauncherPalette,
    userSettings: UserSettings,
    onLaunchApp: (AppInfo) -> Unit,
    onClearHabits: () -> Unit,
    onDismiss: () -> Unit
) {
    val isEs = userSettings.isSpanish

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
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
                                .background(palette.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEs) "Hábitos de Firi" else "Firi Habit Engine",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.onBackground
                            )
                            Text(
                                text = if (isEs) "Aprendizaje activo de uso" else "Active usage learning",
                                fontSize = 11.sp,
                                color = palette.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_habit_insights_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = palette.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Summary Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = palette.surfaceVariant
                            ),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🧠", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEs) "Diagnóstico de Firi" else "Firi's Diagnosis",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = summaryText.ifEmpty {
                                        if (isEs) "Firi analiza continuamente cada vez que abres una app para mapear tus horarios de trabajo, ocio y descanso."
                                        else "Firi analyzes app usage to learn your exact work, entertainment, and rest schedules."
                                    },
                                    fontSize = 13.sp,
                                    color = palette.onBackground,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // Current Slot Predictions
                    item {
                        if (prediction != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = palette.surface
                                ),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, palette.cardBorder)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = prediction.slot.iconEmoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = if (isEs) prediction.slot.titleEs else prediction.slot.titleEn,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = palette.onBackground
                                                )
                                                Text(
                                                    text = "${prediction.slot.startHour}:00 - ${prediction.slot.endHour}:00",
                                                    fontSize = 11.sp,
                                                    color = palette.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${(prediction.confidenceScore * 100).toInt()}% Confianza",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    LinearProgressIndicator(
                                        progress = { prediction.confidenceScore },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = palette.primary,
                                        trackColor = palette.surfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (isEs) prediction.explanationEs else prediction.explanationEn,
                                        fontSize = 12.sp,
                                        color = palette.onSurface.copy(alpha = 0.7f)
                                    )

                                    if (prediction.topApps.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = if (isEs) "Apps recomendadas para este horario:" else "Apps recommended for this slot:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(prediction.topApps) { app ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(palette.surfaceVariant)
                                                        .clickable {
                                                            onDismiss()
                                                            onLaunchApp(app)
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = app.category.icon,
                                                            contentDescription = null,
                                                            tint = palette.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = app.label,
                                                            fontSize = 12.sp,
                                                            color = palette.onBackground,
                                                            fontWeight = FontWeight.Medium
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

                    // Time Slots Map
                    item {
                        Text(
                            text = if (isEs) "Ciclos diarios aprendidos:" else "Daily learning cycles:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            TimeOfDaySlot.entries.forEach { slot ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(palette.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = slot.iconEmoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isEs) slot.titleEs else slot.titleEn,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = palette.onBackground
                                        )
                                    }
                                    Text(
                                        text = "${slot.startHour}:00 - ${slot.endHour}:00",
                                        fontSize = 11.sp,
                                        color = palette.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reset habits button
                OutlinedButton(
                    onClick = onClearHabits,
                    modifier = Modifier.fillMaxWidth().testTag("clear_habits_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.secondary),
                    border = BorderStroke(1.dp, palette.cardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEs) "Reiniciar calibración de hábitos" else "Reset habit calibration",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
