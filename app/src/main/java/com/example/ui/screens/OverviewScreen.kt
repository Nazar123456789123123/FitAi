package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MuscleGroup
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutType
import com.example.data.science.VolumeZone
import com.example.ui.components.LiveWorkoutTimerBar
import com.example.ui.components.WeeklyProgressChartCard
import com.example.ui.components.WorkoutCard
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkObsidian
import com.example.ui.theme.RunningAccent
import com.example.ui.theme.RunningContainer
import com.example.ui.theme.StrengthAccent
import com.example.ui.theme.StrengthContainer
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.SwimmingAccent
import com.example.ui.theme.SwimmingContainer
import com.example.ui.theme.VoltLime
import com.example.ui.theme.VoltLimeDark
import com.example.ui.theme.VoltLimeSoft
import com.example.ui.theme.ZoneModerateAmber
import com.example.ui.theme.ZoneModerateBg
import com.example.ui.theme.ZoneOptimalBg
import com.example.ui.theme.ZoneOptimalGreen
import com.example.ui.viewmodel.FitAiDashboardData
import com.example.ui.viewmodel.LiveSessionState
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverviewScreen(
    data: FitAiDashboardData,
    liveSession: LiveSessionState,
    onStartLiveSession: (WorkoutType) -> Unit,
    onToggleLivePause: () -> Unit,
    onLogLiveSetWithRest: (Int) -> Unit,
    onSkipLiveRest: () -> Unit,
    onCancelLiveSession: () -> Unit,
    onFinishLiveAndSave: () -> Unit,
    onQuickAdd: (WorkoutType) -> Unit,
    onEditWorkout: (WorkoutLog) -> Unit,
    onDeleteWorkout: (String) -> Unit,
    onRepeatProgressive: (WorkoutLog) -> Unit,
    onStartDeload: () -> Unit,
    onStartLaggingMuscle: (MuscleGroup) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenJournal: () -> Unit
) {
    val summary = data.scienceSummary
    val profile = data.profile

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Editorial Athlete Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FITAI • ЩОДЕННИК АТЛЕТА",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Привіт, ${profile.displayName}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = InkObsidian
                    )
                    Text(
                        text = "${profile.experienceEnum().labelUk} • ${profile.bodyWeightKg} кг • ІМТ ${String.format(Locale.forLanguageTag("uk"), "%.1f", profile.bmi())}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkCharcoal
                    )
                }

                Surface(
                    color = SurfaceWhite,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onOpenProfile)
                        .testTag("open_profile_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Профіль атлета",
                            tint = InkObsidian
                        )
                    }
                }
            }
        }

        // Quick Action Discipline Buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickLogTile(
                    title = "Силові",
                    subtitle = "Підходи & RIR",
                    icon = Icons.Filled.FitnessCenter,
                    accent = StrengthAccent,
                    bg = StrengthContainer,
                    onClick = { onQuickAdd(WorkoutType.STRENGTH) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_strength")
                )
                QuickLogTile(
                    title = "Біг",
                    subtitle = "км • хв • RPE",
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    accent = RunningAccent,
                    bg = RunningContainer,
                    onClick = { onQuickAdd(WorkoutType.RUNNING) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_running")
                )
                QuickLogTile(
                    title = "Плавання",
                    subtitle = "м • хв • RPE",
                    icon = Icons.Filled.Pool,
                    accent = SwimmingAccent,
                    bg = SwimmingContainer,
                    onClick = { onQuickAdd(WorkoutType.SWIMMING) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_swimming")
                )
            }
        }

        // Live Workout Stopwatch & Science-Backed Rest Timer
        item {
            LiveWorkoutTimerBar(
                liveSession = liveSession,
                onStartSession = onStartLiveSession,
                onTogglePause = onToggleLivePause,
                onLogSetWithRest = onLogLiveSetWithRest,
                onSkipRest = onSkipLiveRest,
                onCancelSession = onCancelLiveSession,
                onFinishAndSave = onFinishLiveAndSave,
                modifier = Modifier.widthIn(max = 720.dp)
            )
        }

        // Weekly Readiness & sRPE Hero Card (Obsidian High-Contrast Editorial Card)
        item {
            val budgetProgress = if (summary.recommendedSrpeBudget > 0) {
                (summary.totalSrpeLoad.toFloat() / summary.recommendedSrpeBudget.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Surface(
                color = InkObsidian,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "НАВАНТАЖЕННЯ ЗА 7 ДНІВ",
                                style = MaterialTheme.typography.labelSmall,
                                color = VoltLime
                            )
                            Text(
                                text = "${summary.totalSrpeLoad} AU (sRPE)",
                                style = MaterialTheme.typography.headlineLarge,
                                color = SurfaceWhite
                            )
                        }
                        Surface(
                            color = VoltLime,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${summary.totalSessions}/${profile.weeklyTargetSessions} трен.",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = InkObsidian,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { budgetProgress },
                        color = VoltLime,
                        trackColor = InkCharcoal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HeroMetricItem("Час", "${summary.totalMinutes} хв")
                        HeroMetricItem(
                            "Дистанція",
                            String.format("%.1f км", summary.totalDistanceKm)
                        )
                        HeroMetricItem(
                            "Сер. RPE",
                            if (summary.totalSessions > 0) String.format("%.1f/10", summary.averageRpe) else "—"
                        )
                        HeroMetricItem(
                            "Білок",
                            "${summary.dailyProteinGramsMin}–${summary.dailyProteinGramsMax}г"
                        )
                    }

                    Text(
                        text = summary.readinessVerdictUk,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD6DAE0)
                    )
                }
            }
        }

        // Deload Readiness & Recommended Weekly Split Card
        item {
            Surface(
                color = if (summary.deloadRecommended) ZoneModerateBg else SurfaceWhite,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(
                    1.dp,
                    if (summary.deloadRecommended) ZoneModerateAmber else HairlineBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .testTag("deload_split_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (summary.deloadRecommended) Icons.Filled.BatteryChargingFull else Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = if (summary.deloadRecommended) ZoneModerateAmber else VoltLimeDark,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (summary.deloadRecommended) {
                                "Сигнал Deload (розвантаження ЦНС)"
                            } else {
                                "Оптимальний спліт • ${profile.weeklyTargetSessions}×/тиждень"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = InkObsidian
                        )
                    }

                    Text(
                        text = summary.deloadReasonUk,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkCharcoal
                    )

                    if (summary.deloadRecommended) {
                        Button(
                            onClick = onStartDeload,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InkObsidian,
                                contentColor = VoltLime
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_deload_button")
                        ) {
                            Text("Запланувати Deload-сесію (RPE 6 • 5 підходів)")
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        summary.recommendedSplitDays.forEach { dayItem ->
                            Surface(
                                color = if (summary.deloadRecommended) SurfaceWhite else SurfaceSubtle,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, HairlineBorder)
                            ) {
                                Text(
                                    text = dayItem,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = InkObsidian,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Lagging Muscle Quick-Start Banner (when a tracked / focus muscle is below MEV)
        val lagging = summary.laggingMuscleGroup
        if (lagging != null) {
            item {
                Surface(
                    color = VoltLimeSoft,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, InkObsidian),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                        .testTag("lagging_muscle_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = InkObsidian
                            )
                            Column {
                                Text(
                                    text = "Фокус на відстаючу зону: ${lagging.labelUk}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkObsidian
                                )
                                Text(
                                    text = "Нижче MEV (${lagging.mevSets} підх.). Додайте сесію в діапазоні 1–3 RIR.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkCharcoal
                                )
                            }
                        }
                        Button(
                            onClick = { onStartLaggingMuscle(lagging) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InkObsidian,
                                contentColor = VoltLime
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("quick_start_lagging_button")
                        ) {
                            Text("Старт")
                        }
                    }
                }
            }
        }

        // Weekly Progress Chart (Volume Bars + RPE Intensity Curve)
        item {
            WeeklyProgressChartCard(
                summary = summary,
                modifier = Modifier.widthIn(max = 720.dp)
            )
        }

        // Natural Hypertrophy MEV / MAV / MRV Weekly Volume Card
        item {
            Surface(
                color = SurfaceWhite,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Science,
                            contentDescription = null,
                            tint = VoltLimeDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Тижневий обсяг гіпертрофії (MEV → MAV → MRV)",
                                style = MaterialTheme.typography.titleMedium,
                                color = InkObsidian
                            )
                            Text(
                                text = "Оптимум для натурала: 10–18 якісних підходів / тиждень",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkMuted
                            )
                        }
                    }

                    summary.muscleVolumes.forEach { metric ->
                        val ratio = (metric.completedSets.toFloat() / metric.mrv.toFloat()).coerceIn(0f, 1f)
                        val barColor = when (metric.zone) {
                            VolumeZone.BELOW_MEV -> InkMuted
                            VolumeZone.MEV_TO_MAV -> ZoneModerateAmber
                            VolumeZone.MAV_OPTIMAL -> ZoneOptimalGreen
                            VolumeZone.MRV_WARNING -> com.example.ui.theme.ZoneHighCrimson
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = metric.muscleGroup.labelUk,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = InkObsidian
                                )
                                Text(
                                    text = "${metric.completedSets} / ${metric.mavMax} підх. (${metric.zone.labelUk})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = barColor
                                )
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                color = barColor,
                                trackColor = SurfaceMuted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(999.dp))
                            )
                        }
                    }

                    Surface(
                        color = if (summary.interferenceIsHigh) ZoneModerateBg else ZoneOptimalBg,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Інтерференція кардіо: ${summary.interferenceRiskUk}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (summary.interferenceIsHigh) ZoneModerateAmber else ZoneOptimalGreen,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Recent Workouts Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Останні тренування",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkObsidian
                )
                if (data.workouts.isNotEmpty()) {
                    Text(
                        text = "Увесь журнал →",
                        style = MaterialTheme.typography.labelLarge,
                        color = VoltLimeDark,
                        modifier = Modifier
                            .clickable(onClick = onOpenJournal)
                            .padding(8.dp)
                    )
                }
            }
        }

        if (data.workouts.isEmpty()) {
            item {
                Surface(
                    color = SurfaceWhite,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = null,
                            tint = InkMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Журнал тренувань поки порожній",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkObsidian
                        )
                        Text(
                            text = "Запиши силове тренування, пробіжку чи заплив у басейні, щоб відстежувати sRPE, дистанцію та тижневий обсяг MAV.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkCharcoal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { onQuickAdd(WorkoutType.STRENGTH) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InkObsidian,
                                contentColor = VoltLime
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("empty_add_workout_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Записати перше тренування")
                        }
                    }
                }
            }
        } else {
            items(data.workouts.take(5), key = { it.id }) { workout ->
                WorkoutCard(
                    workout = workout,
                    onEdit = { onEditWorkout(workout) },
                    onDelete = { onDeleteWorkout(workout.id) },
                    onRepeatProgressive = { onRepeatProgressive(workout) },
                    modifier = Modifier.widthIn(max = 720.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickLogTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    bg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = bg,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "+ $title",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkObsidian
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted
                )
            }
        }
    }
}

@Composable
private fun HeroMetricItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFA0A8B0)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = SurfaceWhite,
            fontWeight = FontWeight.Bold
        )
    }
}
