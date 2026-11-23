package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.WorkoutType
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkObsidian
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.VoltLime
import com.example.ui.theme.VoltLimeDark
import com.example.ui.theme.VoltLimeSoft
import com.example.ui.viewmodel.LiveSessionState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveWorkoutTimerBar(
    liveSession: LiveSessionState,
    onStartSession: (WorkoutType) -> Unit,
    onTogglePause: () -> Unit,
    onLogSetWithRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onCancelSession: () -> Unit,
    onFinishAndSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!liveSession.isActive) {
        Surface(
            color = SurfaceWhite,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = modifier
                .fillMaxWidth()
                .testTag("live_timer_launcher_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = VoltLimeSoft,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = VoltLimeDark,
                            modifier = Modifier.padding(9.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Живий таймер тренування",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkObsidian
                        )
                        Text(
                            text = "Секундомір + паузи відпочинку 90с / 2хв / 3хв",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted
                        )
                    }
                }
                Button(
                    onClick = { onStartSession(WorkoutType.STRENGTH) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InkObsidian,
                        contentColor = VoltLime
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("start_live_timer_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Старт")
                }
            }
        }
        return
    }

    val elapsedMins = liveSession.elapsedSeconds / 60
    val elapsedSecs = liveSession.elapsedSeconds % 60
    val formattedElapsed = String.format("%02d:%02d", elapsedMins, elapsedSecs)

    Surface(
        color = InkObsidian,
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_timer_active_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (liveSession.isPaused) "ТРЕНАЖЕРНИЙ ТАЙМЕР • ПАУЗА" else "ЖИВА СЕСІЯ • ${liveSession.modality.labelUk.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = VoltLime
                    )
                    Text(
                        text = "$formattedElapsed • ${liveSession.completedSets} підх.",
                        style = MaterialTheme.typography.headlineLarge,
                        color = SurfaceWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTogglePause,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("live_timer_pause_button")
                    ) {
                        Icon(
                            imageVector = if (liveSession.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (liveSession.isPaused) "Продовжити" else "Пауза",
                            tint = VoltLime
                        )
                    }
                    IconButton(
                        onClick = onCancelSession,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("live_timer_cancel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Скасувати таймер",
                            tint = SurfaceWhite
                        )
                    }
                }
            }

            if (liveSession.restRemainingSeconds > 0 && liveSession.restTotalSeconds > 0) {
                val restMins = liveSession.restRemainingSeconds / 60
                val restSecs = liveSession.restRemainingSeconds % 60
                val progress = liveSession.restRemainingSeconds.toFloat() / liveSession.restTotalSeconds.toFloat()
                Surface(
                    color = InkCharcoal,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Відпочинок між підходами: ${String.format("%d:%02d", restMins, restSecs)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = VoltLime
                            )
                            TextButton(onClick = onSkipRest) {
                                Text("Пропустити", color = SurfaceWhite)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            color = VoltLime,
                            trackColor = InkObsidian,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                        )
                    }
                }
            }

            // Science-backed rest presets (+1 working set & start rest)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "+1 підхід і запуск відпочинку (відновлення АТФ):",
                    style = MaterialTheme.typography.labelSmall,
                    color = SurfaceMuted
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        90 to "90с Ізоляція",
                        120 to "2:00 Гіпертрофія",
                        180 to "3:00 База"
                    ).forEach { (secs, label) ->
                        FilterChip(
                            selected = liveSession.restTotalSeconds == secs && liveSession.restRemainingSeconds > 0,
                            onClick = { onLogSetWithRest(secs) },
                            leadingIcon = {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = InkCharcoal,
                                labelColor = SurfaceWhite,
                                iconColor = VoltLime,
                                selectedContainerColor = VoltLime,
                                selectedLabelColor = InkObsidian,
                                selectedLeadingIconColor = InkObsidian
                            ),
                            modifier = Modifier.testTag("live_rest_preset_$secs")
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onLogSetWithRest(0) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, VoltLime),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("live_add_set_only_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = VoltLime, modifier = Modifier.size(18.dp))
                    Text(" +1 підхід", color = VoltLime)
                }
                Button(
                    onClick = onFinishAndSave,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VoltLime,
                        contentColor = InkObsidian
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("live_finish_and_save_button")
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Завершити і записати", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
