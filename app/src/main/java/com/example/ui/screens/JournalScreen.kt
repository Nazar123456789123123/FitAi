package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutType
import com.example.ui.components.WorkoutCard
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkObsidian
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.VoltLime
import com.example.ui.theme.VoltLimeSoft
import com.example.ui.theme.ZoneHighCrimson

enum class JournalSortMode(val labelUk: String) {
    NEWEST("Новіші"),
    HIGHEST_RPE("Вищий RPE"),
    HIGHEST_SRPE("Більший sRPE")
}

@Composable
fun JournalScreen(
    workouts: List<WorkoutLog>,
    selectedFilter: WorkoutType?,
    onSelectFilter: (WorkoutType?) -> Unit,
    onAddWorkout: () -> Unit,
    onEditWorkout: (WorkoutLog) -> Unit,
    onDeleteWorkout: (String) -> Unit,
    onRepeatProgressive: (WorkoutLog) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<WorkoutLog?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(JournalSortMode.NEWEST) }

    val filtered = remember(workouts, selectedFilter, searchQuery, sortMode) {
        val byType = if (selectedFilter == null) workouts
        else workouts.filter { it.workoutTypeEnum() == selectedFilter }

        val query = searchQuery.trim().lowercase()
        val byQuery = if (query.isEmpty()) byType else {
            byType.filter { w ->
                w.title.lowercase().contains(query) ||
                    w.notes.lowercase().contains(query) ||
                    w.muscleGroupEnum().labelUk.lowercase().contains(query)
            }
        }

        when (sortMode) {
            JournalSortMode.NEWEST -> byQuery.sortedByDescending { it.performedAtMillis }
            JournalSortMode.HIGHEST_RPE -> byQuery.sortedByDescending { it.rpe }
            JournalSortMode.HIGHEST_SRPE -> byQuery.sortedByDescending { it.sessionLoadSrpe }
        }
    }

    val totalDuration = filtered.sumOf { it.durationMinutes.toInt() }
    val totalDistanceKm = filtered.sumOf { it.distanceInKm() }
    val avgRpe = if (filtered.isEmpty()) 0.0 else filtered.map { it.rpe.toDouble() }.average()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Журнал тренувань",
                    style = MaterialTheme.typography.headlineLarge,
                    color = InkObsidian
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { if (it.length <= 60) searchQuery = it },
                    placeholder = { Text("Пошук за назвою, м'язом чи нотатками…") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = InkMuted)
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Очистити пошук")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { onSelectFilter(null) },
                        label = { Text("Усі (${workouts.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = InkObsidian,
                            selectedLabelColor = VoltLime
                        ),
                        modifier = Modifier.testTag("filter_all")
                    )
                    WorkoutType.entries.forEach { type ->
                        val count = workouts.count { it.workoutTypeEnum() == type }
                        FilterChip(
                            selected = selectedFilter == type,
                            onClick = { onSelectFilter(type) },
                            label = { Text("${type.labelUk} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = InkObsidian,
                                selectedLabelColor = VoltLime
                            ),
                            modifier = Modifier.testTag("filter_${type.name.lowercase()}")
                        )
                    }
                }

                // Sort Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    JournalSortMode.entries.forEach { mode ->
                        FilterChip(
                            selected = sortMode == mode,
                            onClick = { sortMode = mode },
                            label = { Text(mode.labelUk) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VoltLimeSoft,
                                selectedLabelColor = InkObsidian
                            ),
                            modifier = Modifier.testTag("sort_${mode.name.lowercase()}")
                        )
                    }
                }

                Surface(
                    color = SurfaceWhite,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        JournalSummaryCol("Сесій", "${filtered.size}")
                        JournalSummaryCol("Тривалість", "$totalDuration хв")
                        JournalSummaryCol("Дистанція", String.format("%.2f км", totalDistanceKm))
                        JournalSummaryCol(
                            "Сер. RPE",
                            if (filtered.isNotEmpty()) String.format("%.1f", avgRpe) else "—"
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
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
                        Text(
                            text = "Немає тренувань за цим запитом",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkObsidian
                        )
                        Text(
                            text = "Змініть пошуковий запит або додайте нове тренування (Силові, Біг або Плавання) з RPE 1–10.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkCharcoal
                        )
                        Button(
                            onClick = onAddWorkout,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InkObsidian,
                                contentColor = VoltLime
                            )
                        ) {
                            Text("Додати тренування")
                        }
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { workout ->
                WorkoutCard(
                    workout = workout,
                    onEdit = { onEditWorkout(workout) },
                    onDelete = { pendingDelete = workout },
                    onRepeatProgressive = { onRepeatProgressive(workout) },
                    modifier = Modifier.widthIn(max = 720.dp)
                )
            }
        }
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Видалити запис?") },
            text = { Text("Тренування «${toDelete.title}» буде видалено з журналу.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteWorkout(toDelete.id)
                        pendingDelete = null
                    }
                ) {
                    Text("Видалити", color = ZoneHighCrimson)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@Composable
private fun JournalSummaryCol(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = InkMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = InkObsidian
        )
    }
}
