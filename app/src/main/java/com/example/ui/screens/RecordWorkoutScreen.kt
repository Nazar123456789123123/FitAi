package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DistanceUnit
import com.example.data.model.MuscleGroup
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutType
import com.example.data.model.deriveRirFromRpe
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
import com.example.ui.theme.VoltLime
import com.example.ui.theme.VoltLimeDark
import com.example.ui.theme.VoltLimeSoft
import com.example.ui.theme.ZoneHighBg
import com.example.ui.theme.ZoneHighCrimson
import com.example.ui.theme.ZoneModerateAmber
import com.example.ui.theme.ZoneModerateBg
import com.example.ui.theme.ZoneOptimalBg
import com.example.ui.theme.ZoneOptimalGreen
import com.example.ui.theme.ZoneRecoveryBg
import com.example.ui.theme.ZoneRecoveryTeal
import kotlin.math.roundToInt

private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordWorkoutScreen(
    sessionKey: Int,
    initialType: WorkoutType = WorkoutType.STRENGTH,
    editingWorkout: WorkoutLog? = null,
    prefillTitle: String? = null,
    prefillDurationMinutes: Int? = null,
    prefillDistanceValue: Double? = null,
    prefillDistanceUnit: DistanceUnit? = null,
    prefillRpe: Int? = null,
    prefillMuscleGroup: MuscleGroup? = null,
    prefillWorkingSets: Int? = null,
    prefillNotes: String? = null,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSave: (
        type: WorkoutType,
        title: String,
        durationMinutes: Int,
        distanceValue: Double,
        distanceUnit: DistanceUnit,
        rpe: Int,
        muscleGroup: MuscleGroup,
        workingSets: Int,
        notes: String,
        performedAtMillis: Long
    ) -> Unit
) {
    BackHandler(onBack = onCancel)

    var selectedType by remember(sessionKey, editingWorkout?.id, initialType) {
        mutableStateOf(editingWorkout?.workoutTypeEnum() ?: initialType)
    }
    var title by remember(sessionKey, editingWorkout?.id, initialType, prefillTitle) {
        mutableStateOf(
            editingWorkout?.title
                ?: prefillTitle
                ?: when (initialType) {
                    WorkoutType.STRENGTH -> "Силове тренування (Гіпертрофія)"
                    WorkoutType.RUNNING -> "Аеробний біг"
                    WorkoutType.SWIMMING -> "Кроль у басейні"
                }
        )
    }
    var durationText by remember(sessionKey, editingWorkout?.id, prefillDurationMinutes) {
        mutableStateOf((editingWorkout?.durationMinutes ?: prefillDurationMinutes?.toLong() ?: 45L).toString())
    }
    var distanceText by remember(sessionKey, editingWorkout?.id, initialType, prefillDistanceValue) {
        mutableStateOf(
            if (editingWorkout != null) {
                if (editingWorkout.distanceValue > 0.0) editingWorkout.distanceValue.toString().removeSuffix(".0") else "0"
            } else if (prefillDistanceValue != null) {
                if (prefillDistanceValue > 0.0) prefillDistanceValue.toString().removeSuffix(".0") else "0"
            } else {
                when (initialType) {
                    WorkoutType.RUNNING -> "5.0"
                    WorkoutType.SWIMMING -> "1500"
                    WorkoutType.STRENGTH -> "0"
                }
            }
        )
    }
    var distanceUnit by remember(sessionKey, editingWorkout?.id, initialType, prefillDistanceUnit) {
        mutableStateOf(
            editingWorkout?.distanceUnitEnum()
                ?: prefillDistanceUnit
                ?: when (initialType) {
                    WorkoutType.SWIMMING -> DistanceUnit.M
                    else -> DistanceUnit.KM
                }
        )
    }
    var rpe by remember(sessionKey, editingWorkout?.id, prefillRpe) {
        mutableIntStateOf((editingWorkout?.rpe?.toInt() ?: prefillRpe ?: 8).coerceIn(1, 10))
    }
    var selectedMuscle by remember(sessionKey, editingWorkout?.id, prefillMuscleGroup) {
        mutableStateOf(
            editingWorkout?.muscleGroupEnum()?.takeIf { it != MuscleGroup.NONE }
                ?: prefillMuscleGroup?.takeIf { it != MuscleGroup.NONE }
                ?: MuscleGroup.CHEST
        )
    }
    var workingSets by remember(sessionKey, editingWorkout?.id, prefillWorkingSets) {
        mutableIntStateOf((editingWorkout?.workingSets?.toInt() ?: prefillWorkingSets ?: 10).coerceIn(1, 40))
    }
    var notes by remember(sessionKey, editingWorkout?.id, prefillNotes) {
        mutableStateOf(editingWorkout?.notes ?: prefillNotes ?: "")
    }
    var dayOffset by remember(sessionKey, editingWorkout?.id) {
        mutableIntStateOf(0)
    }
    var validationError by remember(sessionKey, editingWorkout?.id) {
        mutableStateOf<String?>(null)
    }

    val parsedDurationPreview = (durationText.toIntOrNull() ?: 0).coerceIn(0, 600)
    val previewSrpe = parsedDurationPreview * rpe
    val derivedRir = deriveRirFromRpe(rpe)

    val (rpeColor, rpeBg) = when (rpe) {
        in 1..4 -> ZoneRecoveryTeal to ZoneRecoveryBg
        in 5..6 -> ZoneModerateAmber to ZoneModerateBg
        in 7..8 -> ZoneOptimalGreen to ZoneOptimalBg
        else -> ZoneHighCrimson to ZoneHighBg
    }
    val rpeAdvice = when (rpe) {
        in 1..4 -> "Відновлення / легка циркуляція крові"
        in 5..6 -> "Аеробна база (Зона 2) або технічний розігрів"
        in 7..8 -> "Золота зона натуральної гіпертрофії ($derivedRir RIR) — максимум стимулу"
        9 -> "Високе напруження ($derivedRir RIR) — останній підхід ізоляції"
        else -> "Повна м'язова відмова (0 RIR) — дозовано для ЦНС"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("record_workout_screen"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Editorial Screen Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = SurfaceWhite,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onCancel)
                        .testTag("record_back_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Повернутися",
                            tint = InkObsidian
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (editingWorkout == null) "ЩОДЕННИЙ ЗАПИС СЕСІЇ" else "РЕДАГУВАННЯ ЗАПИСУ",
                        style = MaterialTheme.typography.labelSmall,
                        color = VoltLimeDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (editingWorkout == null) "Записати тренування" else "Оновити тренування",
                        style = MaterialTheme.typography.headlineLarge,
                        color = InkObsidian
                    )
                }
            }
        }

        // 1. Exercise Type (Силові, Біг, Плавання)
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
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.field_workout_type),
                        style = MaterialTheme.typography.titleMedium,
                        color = InkObsidian
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WorkoutType.entries.forEach { modality ->
                            val isSelected = selectedType == modality
                            val (accent, container, icon) = when (modality) {
                                WorkoutType.STRENGTH -> Triple(StrengthAccent, StrengthContainer, Icons.Filled.FitnessCenter)
                                WorkoutType.RUNNING -> Triple(RunningAccent, RunningContainer, Icons.AutoMirrored.Filled.DirectionsRun)
                                WorkoutType.SWIMMING -> Triple(SwimmingAccent, SwimmingContainer, Icons.Filled.Pool)
                            }
                            Surface(
                                color = if (isSelected) container else SurfaceMuted.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) accent else HairlineBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedType = modality
                                        if (editingWorkout == null) {
                                            when (modality) {
                                                WorkoutType.STRENGTH -> {
                                                    title = "Силове тренування (Гіпертрофія)"
                                                    distanceUnit = DistanceUnit.KM
                                                    if (distanceText == "1500" || distanceText == "5.0") distanceText = "0"
                                                }
                                                WorkoutType.RUNNING -> {
                                                    title = "Аеробний біг"
                                                    distanceUnit = DistanceUnit.KM
                                                    if (distanceText == "0" || distanceText == "1500") distanceText = "5.0"
                                                }
                                                WorkoutType.SWIMMING -> {
                                                    title = "Кроль у басейні"
                                                    distanceUnit = DistanceUnit.M
                                                    if (distanceText == "0" || distanceText == "5.0") distanceText = "1500"
                                                }
                                            }
                                        }
                                    }
                                    .testTag("type_option_${modality.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = modality.labelUk,
                                        tint = if (isSelected) accent else InkCharcoal,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Text(
                                        text = modality.labelUk,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = InkObsidian,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Title + Quick Presets
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 100) title = it },
                        label = { Text(stringResource(R.string.field_title)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("workout_title_input")
                    )

                    val presets = when (selectedType) {
                        WorkoutType.STRENGTH -> listOf("Верх (Груди + Спина)", "Низ (Квадрі + Сідниці)", "Full Body (База)")
                        WorkoutType.RUNNING -> listOf("Аеробний біг Зона 2", "Темповий біг", "Інтервали VO2max")
                        WorkoutType.SWIMMING -> listOf("Кроль 50м басейн", "Відновлювальне плавання", "Інтервали 100м")
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { preset ->
                            FilterChip(
                                selected = title == preset,
                                onClick = { title = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VoltLimeSoft,
                                    selectedLabelColor = InkObsidian
                                )
                            )
                        }
                    }

                    // Date selector when logging a daily session
                    if (editingWorkout == null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = InkMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            listOf(
                                0 to "Сьогодні",
                                1 to "Вчора",
                                2 to "2 дні тому"
                            ).forEach { (offset, label) ->
                                FilterChip(
                                    selected = dayOffset == offset,
                                    onClick = { dayOffset = offset },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = InkObsidian,
                                        selectedLabelColor = VoltLime
                                    ),
                                    modifier = Modifier.testTag("day_offset_$offset")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Duration (minutes) & Distance (km or m)
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
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Тривалість та дистанція",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkObsidian
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = durationText,
                            onValueChange = { input ->
                                if (input.length <= 3 && input.all { it.isDigit() }) {
                                    durationText = input
                                }
                            },
                            label = { Text(stringResource(R.string.field_duration)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("workout_duration_input")
                        )

                        OutlinedTextField(
                            value = distanceText,
                            onValueChange = { input ->
                                val cleaned = input.replace(',', '.')
                                if (cleaned.length <= 8 && cleaned.count { it == '.' } <= 1 &&
                                    cleaned.all { it.isDigit() || it == '.' }
                                ) {
                                    distanceText = cleaned
                                }
                            },
                            label = { Text("${stringResource(R.string.field_distance)} (${distanceUnit.labelUk})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("workout_distance_input")
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(20, 30, 45, 60, 75, 90).forEach { mins ->
                            FilterChip(
                                selected = durationText == mins.toString(),
                                onClick = { durationText = mins.toString() },
                                label = { Text("${mins} хв") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Одиниця дистанції:",
                            style = MaterialTheme.typography.labelLarge,
                            color = InkCharcoal
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DistanceUnit.entries.forEach { unit ->
                                FilterChip(
                                    selected = distanceUnit == unit,
                                    onClick = {
                                        if (distanceUnit != unit) {
                                            val currentVal = distanceText.toDoubleOrNull() ?: 0.0
                                            if (currentVal > 0.0) {
                                                distanceText = if (unit == DistanceUnit.M) {
                                                    (currentVal * 1000.0).roundToInt().toString()
                                                } else {
                                                    String.format("%.2f", currentVal / 1000.0)
                                                        .trimEnd('0')
                                                        .trimEnd('.')
                                                }
                                            }
                                            distanceUnit = unit
                                        }
                                    },
                                    label = { Text(unit.labelUk) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = InkObsidian,
                                        selectedLabelColor = VoltLime
                                    ),
                                    modifier = Modifier.testTag("distance_unit_${unit.name.lowercase()}")
                                )
                            }
                        }
                    }

                    if (selectedType != WorkoutType.STRENGTH) {
                        val quickDistances = if (selectedType == WorkoutType.RUNNING) {
                            listOf("3.0" to "3 км", "5.0" to "5 км", "10.0" to "10 км")
                        } else {
                            listOf("1000" to "1000 м", "1500" to "1500 м", "2000" to "2000 м")
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickDistances.forEach { (valStr, label) ->
                                FilterChip(
                                    selected = distanceText == valStr,
                                    onClick = {
                                        distanceUnit = if (selectedType == WorkoutType.RUNNING) DistanceUnit.KM else DistanceUnit.M
                                        distanceText = valStr
                                    },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VoltLimeSoft,
                                        selectedLabelColor = InkObsidian
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Subjective Intensity RPE (1-10) + Live sRPE Preview
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
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.field_rpe),
                            style = MaterialTheme.typography.titleMedium,
                            color = InkObsidian,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(color = rpeBg, shape = RoundedCornerShape(10.dp)) {
                            Text(
                                text = "RPE $rpe / 10",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = rpeColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (1..10).forEach { value ->
                            val selected = rpe == value
                            Surface(
                                color = if (selected) InkObsidian else SurfaceMuted,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clickable { rpe = value }
                                    .testTag("rpe_btn_$value")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = value.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) VoltLime else InkObsidian
                                    )
                                }
                            }
                        }
                    }

                    Slider(
                        value = rpe.toFloat(),
                        onValueChange = { rpe = it.roundToInt().coerceIn(1, 10) },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = InkObsidian,
                            activeTrackColor = rpeColor
                        ),
                        modifier = Modifier.testTag("rpe_slider")
                    )

                    Text(
                        text = rpeAdvice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = rpeColor
                    )

                    // Live sRPE Preview Bar
                    Surface(
                        color = InkObsidian,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ІНДЕКС НАВАНТАЖЕННЯ (sRPE)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VoltLime
                                )
                                Text(
                                    text = "$parsedDurationPreview хв × RPE $rpe",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SurfaceWhite
                                )
                            }
                            Text(
                                text = "$previewSrpe AU",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = VoltLime
                            )
                        }
                    }
                }
            }
        }

        // 4. Strength Hypertrophy Fields (Muscle Group & Working Sets)
        if (selectedType == WorkoutType.STRENGTH) {
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
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.field_muscle_group),
                            style = MaterialTheme.typography.titleMedium,
                            color = InkObsidian
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MuscleGroup.strengthTargets.forEach { mg ->
                                FilterChip(
                                    selected = selectedMuscle == mg,
                                    onClick = { selectedMuscle = mg },
                                    label = { Text(mg.labelUk, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = InkObsidian,
                                        selectedLabelColor = VoltLime
                                    ),
                                    modifier = Modifier.testTag("muscle_chip_${mg.name.lowercase()}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.field_working_sets),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = InkObsidian
                                )
                                Text(
                                    text = "Ціль MAV: ${selectedMuscle.mavMinSets}–${selectedMuscle.mavMaxSets} / тиждень",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkMuted
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { if (workingSets > 1) workingSets-- },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Зменшити підходи")
                                }
                                Text(
                                    text = workingSets.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = InkObsidian
                                )
                                IconButton(
                                    onClick = { if (workingSets < 40) workingSets++ },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Збільшити підходи")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Notes & Save Actions
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 500) notes = it },
                    label = { Text(stringResource(R.string.field_notes)) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workout_notes_input")
                )

                if (validationError != null) {
                    Surface(
                        color = ZoneHighBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError.orEmpty(),
                            color = ZoneHighCrimson,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("cancel_workout_button")
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = {
                            val cleanTitle = title.trim()
                            val parsedDuration = durationText.toIntOrNull()
                            val parsedDistance = distanceText.toDoubleOrNull() ?: 0.0

                            when {
                                cleanTitle.isEmpty() || cleanTitle.length > 100 ->
                                    validationError = "Вкажіть назву тренування (1–100 символів)."
                                parsedDuration == null || parsedDuration !in 1..600 ->
                                    validationError = "Тривалість має бути від 1 до 600 хвилин."
                                parsedDistance < 0.0 || parsedDistance > 500000.0 ->
                                    validationError = "Дистанція має бути від 0 до 500 000."
                                else -> {
                                    validationError = null
                                    val timestamp = editingWorkout?.performedAtMillis
                                        ?: (System.currentTimeMillis() - dayOffset * ONE_DAY_MILLIS)
                                    onSave(
                                        selectedType,
                                        cleanTitle,
                                        parsedDuration,
                                        parsedDistance,
                                        distanceUnit,
                                        rpe,
                                        if (selectedType == WorkoutType.STRENGTH) selectedMuscle else MuscleGroup.NONE,
                                        if (selectedType == WorkoutType.STRENGTH) workingSets else 0,
                                        notes.trim(),
                                        timestamp
                                    )
                                }
                            }
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InkObsidian,
                            contentColor = VoltLime
                        ),
                        modifier = Modifier
                            .weight(1.6f)
                            .height(54.dp)
                            .testTag("save_workout_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = VoltLime,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = if (editingWorkout == null) "Зберегти у Firestore" else stringResource(R.string.action_update),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
