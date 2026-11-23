package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MuscleGroup
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutType
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkObsidian
import com.example.ui.theme.RunningAccent
import com.example.ui.theme.RunningContainer
import com.example.ui.theme.StrengthAccent
import com.example.ui.theme.StrengthContainer
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.SwimmingAccent
import com.example.ui.theme.SwimmingContainer
import com.example.ui.theme.ZoneHighBg
import com.example.ui.theme.ZoneHighCrimson
import com.example.ui.theme.ZoneModerateAmber
import com.example.ui.theme.ZoneModerateBg
import com.example.ui.theme.ZoneOptimalBg
import com.example.ui.theme.ZoneOptimalGreen
import com.example.ui.theme.ZoneRecoveryBg
import com.example.ui.theme.ZoneRecoveryTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutCard(
    workout: WorkoutLog,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRepeatProgressive: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val type = workout.workoutTypeEnum()
    val (accentColor, badgeBg, icon) = when (type) {
        WorkoutType.STRENGTH -> Triple(StrengthAccent, StrengthContainer, Icons.Filled.FitnessCenter)
        WorkoutType.RUNNING -> Triple(RunningAccent, RunningContainer, Icons.AutoMirrored.Filled.DirectionsRun)
        WorkoutType.SWIMMING -> Triple(SwimmingAccent, SwimmingContainer, Icons.Filled.Pool)
    }

    val rpeInt = workout.rpe.toInt()
    val (rpeColor, rpeBg) = when (rpeInt) {
        in 1..4 -> ZoneRecoveryTeal to ZoneRecoveryBg
        in 5..6 -> ZoneModerateAmber to ZoneModerateBg
        in 7..8 -> ZoneOptimalGreen to ZoneOptimalBg
        else -> ZoneHighCrimson to ZoneHighBg
    }

    val dateLabel = SimpleDateFormat("dd MMM, HH:mm", Locale.forLanguageTag("uk")).format(Date(workout.performedAtMillis))

    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("workout_card_${workout.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = type.labelUk,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "${type.labelUk.uppercase()} • $dateLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted
                        )
                        Text(
                            text = workout.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = InkObsidian
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("edit_workout_${workout.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Редагувати тренування",
                            tint = InkCharcoal
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_workout_${workout.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Видалити тренування",
                            tint = ZoneHighCrimson
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "${workout.durationMinutes} хв",
                    containerColor = SurfaceMuted,
                    textColor = InkObsidian
                )
                if (workout.distanceValue > 0.0) {
                    MetricPill(
                        label = workout.formattedDistance(),
                        containerColor = badgeBg,
                        textColor = accentColor
                    )
                }
                MetricPill(
                    label = "RPE ${workout.rpe}/10",
                    containerColor = rpeBg,
                    textColor = rpeColor
                )
                MetricPill(
                    label = "sRPE ${workout.sessionLoadSrpe}",
                    containerColor = SurfaceMuted,
                    textColor = InkCharcoal
                )
                if (type == WorkoutType.STRENGTH && workout.workingSets > 0) {
                    val mg = workout.muscleGroupEnum()
                    val mgText = if (mg != MuscleGroup.NONE) "${mg.labelUk} • " else ""
                    MetricPill(
                        label = "$mgText${workout.workingSets} підх. (${workout.rir} RIR)",
                        containerColor = StrengthContainer,
                        textColor = InkObsidian
                    )
                }
            }

            Text(
                text = workout.rpeZoneLabel(),
                style = MaterialTheme.typography.labelMedium,
                color = rpeColor
            )

            if (workout.notes.isNotBlank()) {
                Text(
                    text = workout.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkCharcoal
                )
            }

            if (onRepeatProgressive != null) {
                val repeatLabel = if (type == WorkoutType.STRENGTH) {
                    "Повторити (+1 підхід)"
                } else {
                    "Повторити (+5 хв)"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onRepeatProgressive,
                        modifier = Modifier.testTag("repeat_workout_${workout.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay,
                            contentDescription = repeatLabel,
                            tint = InkObsidian,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "  $repeatLabel",
                            style = MaterialTheme.typography.labelLarge,
                            color = InkObsidian
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricPill(
    label: String,
    containerColor: Color,
    textColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
