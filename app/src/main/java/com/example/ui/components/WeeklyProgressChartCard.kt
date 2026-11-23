package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.WorkoutType
import com.example.data.science.DailyProgressPoint
import com.example.data.science.WeeklyScienceSummary
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkObsidian
import com.example.ui.theme.RunningAccent
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.SwimmingAccent
import com.example.ui.theme.VoltLime
import com.example.ui.theme.VoltLimeDark
import com.example.ui.theme.VoltLimeSoft
import com.example.ui.theme.ZoneOptimalBg
import com.example.ui.theme.ZoneOptimalGreen
import kotlin.math.max
import kotlin.math.roundToInt

enum class ChartVolumeMetric(val labelUk: String, val unitUk: String) {
    SRPE("sRPE", "AU"),
    SETS("Підходи", "підх."),
    MINUTES("Час", "хв"),
    DISTANCE("Дистанція", "км")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeeklyProgressChartCard(
    summary: WeeklyScienceSummary,
    modifier: Modifier = Modifier
) {
    val points = summary.dailyPoints
    if (points.isEmpty()) return

    var selectedMetric by remember { mutableStateOf(ChartVolumeMetric.SRPE) }
    val defaultIndex = remember(points) {
        points.indexOfLast { it.workoutCount > 0 }.takeIf { it >= 0 } ?: points.lastIndex
    }
    var selectedDayIndex by remember(points) { mutableIntStateOf(defaultIndex) }
    val clampedIndex = selectedDayIndex.coerceIn(0, points.lastIndex)
    val selectedPoint = points[clampedIndex]

    val volumeValues = remember(points, selectedMetric) {
        points.map { point ->
            when (selectedMetric) {
                ChartVolumeMetric.SRPE -> point.totalSrpe.toFloat()
                ChartVolumeMetric.SETS -> point.totalSets.toFloat()
                ChartVolumeMetric.MINUTES -> point.totalMinutes.toFloat()
                ChartVolumeMetric.DISTANCE -> point.totalDistanceKm.toFloat()
            }
        }
    }

    val maxVolume = remember(volumeValues, selectedMetric, summary.recommendedSrpeBudget) {
        val peak = volumeValues.maxOrNull() ?: 0f
        val baseline = when (selectedMetric) {
            ChartVolumeMetric.SRPE -> (summary.recommendedSrpeBudget / 5f).coerceAtLeast(350f)
            ChartVolumeMetric.SETS -> 18f
            ChartVolumeMetric.MINUTES -> 60f
            ChartVolumeMetric.DISTANCE -> 10f
        }
        max(peak * 1.15f, baseline)
    }

    val targetPerDay = remember(selectedMetric, summary.recommendedSrpeBudget) {
        when (selectedMetric) {
            ChartVolumeMetric.SRPE -> summary.recommendedSrpeBudget / 7f
            ChartVolumeMetric.SETS -> 12f
            ChartVolumeMetric.MINUTES -> 45f
            ChartVolumeMetric.DISTANCE -> 5f
        }
    }

    val revealProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chartReveal"
    )

    val deltaPercent = if (summary.previousWeekSrpe > 0) {
        (((summary.totalSrpeLoad - summary.previousWeekSrpe).toDouble() / summary.previousWeekSrpe) * 100.0).roundToInt()
    } else null

    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_progress_chart_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header + Week-over-Week Delta Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ДИНАМІКА ЗА 7 ДНІВ",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Обсяг та інтенсивність (RPE)",
                        style = MaterialTheme.typography.titleLarge,
                        color = InkObsidian
                    )
                }

                val badgeText = when {
                    deltaPercent != null && deltaPercent >= 0 -> "+$deltaPercent% до мин. тиж."
                    deltaPercent != null -> "$deltaPercent% до мин. тиж."
                    summary.totalSessions > 0 -> "Тиждень активний"
                    else -> "Старт тижня"
                }
                Surface(
                    color = VoltLimeSoft,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = VoltLimeDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Metric Selector Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ChartVolumeMetric.entries.forEach { metric ->
                    FilterChip(
                        selected = selectedMetric == metric,
                        onClick = { selectedMetric = metric },
                        label = { Text("${metric.labelUk} + RPE", style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = InkObsidian,
                            selectedLabelColor = VoltLime
                        ),
                        modifier = Modifier.testTag("chart_metric_${metric.name.lowercase()}")
                    )
                }
            }

            // Native Compose Canvas Chart (Volume Bars + Optimal RPE 7-9 Band + RPE Intensity Curve)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points.size) {
                            detectTapGestures { offset ->
                                val slotWidth = size.width.toFloat() / points.size.toFloat()
                                val tapped = (offset.x / slotWidth).toInt().coerceIn(0, points.lastIndex)
                                selectedDayIndex = tapped
                            }
                        }
                        .testTag("weekly_progress_canvas")
                ) {
                    val chartWidth = size.width
                    val chartHeight = size.height
                    val topPad = 14.dp.toPx()
                    val bottomPad = 12.dp.toPx()
                    val usableHeight = (chartHeight - topPad - bottomPad).coerceAtLeast(1f)
                    val slotWidth = chartWidth / points.size.toFloat()
                    val barWidth = (slotWidth * 0.44f).coerceAtMost(32.dp.toPx())

                    // 1. Optimal Hypertrophy Intensity Band (RPE 7..9 out of 10)
                    val rpe9Y = topPad + usableHeight * (1f - 9f / 10f)
                    val rpe7Y = topPad + usableHeight * (1f - 7f / 10f)
                    drawRoundRect(
                        color = ZoneOptimalBg.copy(alpha = 0.55f),
                        topLeft = Offset(0f, rpe9Y),
                        size = Size(chartWidth, rpe7Y - rpe9Y),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )

                    // 2. Subtle Horizontal Grid Lines (0%, 50%, 100%)
                    listOf(0f, 0.5f, 1f).forEach { frac ->
                        val y = topPad + usableHeight * (1f - frac)
                        drawLine(
                            color = SurfaceMuted,
                            start = Offset(0f, y),
                            end = Offset(chartWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // 3. Dashed Target Daily Volume Reference Line
                    val targetFrac = (targetPerDay / maxVolume).coerceIn(0f, 1f)
                    val targetY = topPad + usableHeight * (1f - targetFrac)
                    drawLine(
                        color = InkMuted.copy(alpha = 0.55f),
                        start = Offset(0f, targetY),
                        end = Offset(chartWidth, targetY),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )

                    // 4. Vertical Volume Bars per Day
                    points.forEachIndexed { idx, point ->
                        val centerX = slotWidth * idx + slotWidth / 2f
                        val value = volumeValues[idx]
                        val barFrac = (value / maxVolume).coerceIn(0f, 1f) * revealProgress
                        val barHeight = if (value > 0f) {
                            max(barFrac * usableHeight, 8.dp.toPx())
                        } else {
                            6.dp.toPx()
                        }
                        val barTop = topPad + usableHeight - barHeight
                        val barLeft = centerX - barWidth / 2f
                        val isSelected = idx == clampedIndex

                        val barColor = when {
                            value <= 0f -> if (isSelected) InkMuted.copy(alpha = 0.45f) else SurfaceSubtle
                            isSelected -> InkObsidian
                            point.dominantType == WorkoutType.RUNNING -> RunningAccent.copy(alpha = 0.78f)
                            point.dominantType == WorkoutType.SWIMMING -> SwimmingAccent.copy(alpha = 0.78f)
                            else -> InkCharcoal.copy(alpha = 0.82f)
                        }

                        if (isSelected) {
                            drawRoundRect(
                                color = VoltLimeSoft,
                                topLeft = Offset(barLeft - 4.dp.toPx(), topPad - 4.dp.toPx()),
                                size = Size(barWidth + 8.dp.toPx(), usableHeight + 8.dp.toPx()),
                                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                            )
                        }

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )

                        if (isSelected && value > 0f) {
                            drawRoundRect(
                                color = VoltLime,
                                topLeft = Offset(barLeft, barTop),
                                size = Size(barWidth, 5.dp.toPx().coerceAtMost(barHeight)),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }
                    }

                    // 5. RPE 1-10 Intensity Curve + Nodes across active days
                    val activeNodes = points.mapIndexedNotNull { idx, point ->
                        if (point.workoutCount > 0 && point.averageRpe > 0.0) {
                            val cx = slotWidth * idx + slotWidth / 2f
                            val rpeFrac = (point.averageRpe.toFloat() / 10f).coerceIn(0.05f, 1f)
                            val cy = topPad + usableHeight * (1f - rpeFrac * revealProgress)
                            idx to Offset(cx, cy)
                        } else null
                    }

                    if (activeNodes.size >= 2) {
                        val linePath = Path().apply {
                            moveTo(activeNodes.first().second.x, activeNodes.first().second.y)
                            for (i in 1 until activeNodes.size) {
                                val prev = activeNodes[i - 1].second
                                val curr = activeNodes[i].second
                                val midX = (prev.x + curr.x) / 2f
                                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                            }
                        }
                        drawPath(
                            path = linePath,
                            brush = Brush.horizontalGradient(listOf(ZoneOptimalGreen, RunningAccent)),
                            style = Stroke(
                                width = 2.8.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    activeNodes.forEach { (idx, offset) ->
                        val isSelected = idx == clampedIndex
                        drawCircle(
                            color = SurfaceWhite,
                            radius = if (isSelected) 7.dp.toPx() else 5.dp.toPx(),
                            center = offset
                        )
                        drawCircle(
                            color = if (isSelected) InkObsidian else ZoneOptimalGreen,
                            radius = if (isSelected) 4.8.dp.toPx() else 3.4.dp.toPx(),
                            center = offset
                        )
                    }
                }
            }

            // Accessible Day Selector Row (48dp touch targets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEachIndexed { idx, point ->
                    val isSelected = idx == clampedIndex
                    Surface(
                        color = if (isSelected) InkObsidian else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { selectedDayIndex = idx }
                            .testTag("chart_day_$idx")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = point.dayShortUk,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected || point.isToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) VoltLime else InkObsidian
                            )
                            Text(
                                text = point.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) SurfaceWhite else InkMuted
                            )
                        }
                    }
                }
            }

            // Selected Day Detail Callout
            SelectedDayInspector(
                point = selectedPoint,
                metric = selectedMetric
            )

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = InkObsidian, label = "Обсяг (${selectedMetric.labelUk})")
                LegendItem(color = ZoneOptimalGreen, label = "RPE крива")
                LegendItem(color = ZoneOptimalBg, label = "Оптимум 7–9 RPE")
            }
        }
    }
}

@Composable
private fun SelectedDayInspector(
    point: DailyProgressPoint,
    metric: ChartVolumeMetric
) {
    Surface(
        color = SurfaceSubtle,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chart_selected_day_summary")
    ) {
        if (point.workoutCount == 0) {
            Text(
                text = "${point.dayShortUk}, ${point.dayOfMonth}: день відновлення (0 тренувань — синтез білка та адаптація ЦНС).",
                style = MaterialTheme.typography.bodyMedium,
                color = InkCharcoal,
                modifier = Modifier.padding(12.dp)
            )
        } else {
            val primaryVal = when (metric) {
                ChartVolumeMetric.SRPE -> "${point.totalSrpe} AU"
                ChartVolumeMetric.SETS -> "${point.totalSets} підх."
                ChartVolumeMetric.MINUTES -> "${point.totalMinutes} хв"
                ChartVolumeMetric.DISTANCE -> String.format("%.2f км", point.totalDistanceKm)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${point.dayShortUk}, ${point.dayOfMonth} • ${point.workoutCount} трен.",
                        style = MaterialTheme.typography.labelLarge,
                        color = InkObsidian
                    )
                    Text(
                        text = "${point.totalMinutes} хв • ${point.totalSets} підх. • ${String.format("%.1f", point.totalDistanceKm)} км",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkCharcoal
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = primaryVal,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = InkObsidian
                    )
                    Text(
                        text = "Сер. RPE ${String.format("%.1f", point.averageRpe)}/10",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ZoneOptimalGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = color,
            shape = CircleShape,
            modifier = Modifier.size(10.dp)
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = InkCharcoal
        )
    }
}
