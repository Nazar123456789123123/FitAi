package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AthleteProfile
import com.example.data.model.EquipmentTier
import com.example.data.model.MuscleGroup
import com.example.data.model.PrimaryGoal
import com.example.data.model.TrainingExperience
import com.example.ui.theme.CanvasStone
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkObsidian
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.VoltLime
import com.example.ui.theme.VoltLimeDark
import com.example.ui.theme.VoltLimeSoft
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initialProfile: AthleteProfile,
    isSaving: Boolean,
    onComplete: (
        displayName: String,
        bodyWeightKg: Double,
        ageYears: Int,
        heightCm: Int,
        experience: TrainingExperience,
        goal: PrimaryGoal,
        equipment: EquipmentTier,
        focusMuscles: List<MuscleGroup>,
        weeklyTargetSessions: Int
    ) -> Unit,
    onSkip: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var displayName by remember(initialProfile) { mutableStateOf(initialProfile.displayName) }
    var ageYears by remember(initialProfile) { mutableIntStateOf(initialProfile.ageYears.toInt().coerceIn(14, 90)) }
    var heightCm by remember(initialProfile) { mutableIntStateOf(initialProfile.heightCm.toInt().coerceIn(120, 230)) }
    var weightText by remember(initialProfile) {
        mutableStateOf(initialProfile.bodyWeightKg.toString().removeSuffix(".0"))
    }
    var experience by remember(initialProfile) { mutableStateOf(initialProfile.experienceEnum()) }
    var goal by remember(initialProfile) { mutableStateOf(initialProfile.goalEnum()) }
    var equipment by remember(initialProfile) { mutableStateOf(initialProfile.equipmentEnum()) }
    var weeklySessions by remember(initialProfile) {
        mutableIntStateOf(initialProfile.weeklyTargetSessions.toInt().coerceIn(2, 6))
    }
    var focusMuscles by remember(initialProfile) {
        mutableStateOf(
            initialProfile.focusMuscleEnums()
                .filter { it != MuscleGroup.NONE && it != MuscleGroup.FULL_BODY }
                .ifEmpty { listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADS) }
        )
    }

    if (step > 0) {
        BackHandler { step-- }
    }

    val parsedWeight = (weightText.toDoubleOrNull() ?: 75.0).coerceIn(30.0, 250.0)
    val previewProfile = remember(displayName, parsedWeight, ageYears, heightCm, experience, goal, equipment, weeklySessions) {
        AthleteProfile(
            displayName = displayName.ifBlank { "Атлет" },
            bodyWeightKg = parsedWeight,
            ageYears = ageYears.toLong(),
            heightCm = heightCm.toLong(),
            trainingExperience = experience.name,
            primaryGoal = goal.name,
            equipment = equipment.name,
            weeklyTargetSessions = weeklySessions.toLong()
        )
    }

    Surface(
        color = CanvasStone,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Top Progress Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0) {
                        IconButton(
                            onClick = { step-- },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("onboarding_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = InkObsidian
                            )
                        }
                    } else {
                        Surface(
                            color = VoltLimeSoft,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "FITAI ПРОФІЛЬ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = VoltLimeDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Text(
                        text = "Крок ${step + 1} з 3",
                        style = MaterialTheme.typography.labelLarge,
                        color = InkCharcoal
                    )

                    TextButton(
                        onClick = onSkip,
                        enabled = !isSaving,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("onboarding_skip_button")
                    ) {
                        Text("Пропустити", color = InkMuted)
                    }
                }

                LinearProgressIndicator(
                    progress = { (step + 1) / 3f },
                    color = InkObsidian,
                    trackColor = SurfaceMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                )

                when (step) {
                    0 -> {
                        Text(
                            text = "Біометрія та орієнтири атлета",
                            style = MaterialTheme.typography.headlineLarge,
                            color = InkObsidian
                        )
                        Text(
                            text = "Ці параметри допомагають точно розрахувати TDEE, добову норму білка (1.6–2.2 г/кг) та темп чистого набору.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkCharcoal
                        )

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { if (it.length <= 80) displayName = it },
                            label = { Text("Як до тебе звертатися?") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_name_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StepperCard(
                                label = "Вік (років)",
                                valueText = ageYears.toString(),
                                onDecrement = { if (ageYears > 14) ageYears-- },
                                onIncrement = { if (ageYears < 90) ageYears++ },
                                testTagPrefix = "onboarding_age",
                                modifier = Modifier.weight(1f)
                            )
                            StepperCard(
                                label = "Зріст (см)",
                                valueText = heightCm.toString(),
                                onDecrement = { if (heightCm > 120) heightCm-- },
                                onIncrement = { if (heightCm < 230) heightCm++ },
                                testTagPrefix = "onboarding_height",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { input ->
                                val cleaned = input.replace(',', '.')
                                if (cleaned.length <= 6 && cleaned.count { it == '.' } <= 1 &&
                                    cleaned.all { it.isDigit() || it == '.' }
                                ) {
                                    weightText = cleaned
                                }
                            },
                            label = { Text("Вага тіла (кг)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_weight_input")
                        )

                        // Live BMI & TDEE Preview Card
                        Surface(
                            color = InkObsidian,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "ІМТ: ${String.format(Locale.forLanguageTag("uk"), "%.1f", previewProfile.bmi())}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = VoltLime
                                    )
                                    Text(
                                        text = previewProfile.bmiStatusUk(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SurfaceWhite
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Орієнтовний TDEE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkMuted
                                    )
                                    Text(
                                        text = "${previewProfile.estimatedTdeeKcal()} ккал",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = SurfaceWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        Text(
                            text = "Досвід, ціль та обладнання",
                            style = MaterialTheme.typography.headlineLarge,
                            color = InkObsidian
                        )
                        Text(
                            text = "Адаптуємо тижневий бюджет sRPE, пороги MEV/MAV та частоту стимуляції синтезу білка (MPS).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkCharcoal
                        )

                        Text("Тренувальний стаж", style = MaterialTheme.typography.titleMedium, color = InkObsidian)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TrainingExperience.entries.forEach { exp ->
                                SelectableOptionCard(
                                    title = exp.labelUk,
                                    subtitle = exp.recommendedFreqUk,
                                    selected = experience == exp,
                                    onClick = { experience = exp },
                                    modifier = Modifier.testTag("onboarding_exp_${exp.name.lowercase()}")
                                )
                            }
                        }

                        Text("Головна ціль", style = MaterialTheme.typography.titleMedium, color = InkObsidian)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryGoal.entries.forEach { g ->
                                val deltaText = if (g.caloriesDeltaKcal > 0) "+${g.caloriesDeltaKcal} ккал" else "підтримка (0 ккал)"
                                SelectableOptionCard(
                                    title = g.labelUk,
                                    subtitle = "Білок ${g.proteinPerKg} г/кг • $deltaText",
                                    selected = goal == g,
                                    onClick = { goal = g },
                                    modifier = Modifier.testTag("onboarding_goal_${g.name.lowercase()}")
                                )
                            }
                        }

                        Text("Доступне обладнання", style = MaterialTheme.typography.titleMedium, color = InkObsidian)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EquipmentTier.entries.forEach { eq ->
                                FilterChip(
                                    selected = equipment == eq,
                                    onClick = { equipment = eq },
                                    label = { Text(eq.labelUk) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = InkObsidian,
                                        selectedLabelColor = VoltLime
                                    ),
                                    modifier = Modifier.testTag("onboarding_eq_${eq.name.lowercase()}")
                                )
                            }
                        }

                        Text("Тренувань на тиждень: $weeklySessions×", style = MaterialTheme.typography.titleMedium, color = InkObsidian)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(2, 3, 4, 5, 6).forEach { count ->
                                FilterChip(
                                    selected = weeklySessions == count,
                                    onClick = { weeklySessions = count },
                                    label = { Text("$count×") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = InkObsidian,
                                        selectedLabelColor = VoltLime
                                    ),
                                    modifier = Modifier.testTag("onboarding_freq_$count")
                                )
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = "Пріоритетні м'язи та спліт",
                            style = MaterialTheme.typography.headlineLarge,
                            color = InkObsidian
                        )
                        Text(
                            text = "Обери 1–5 груп м'язів для фокусу. Додаток підкаже, коли обсяг відстає від MEV.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkCharcoal
                        )

                        val selectableMuscles = remember {
                            MuscleGroup.strengthTargets.filter { it != MuscleGroup.FULL_BODY }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectableMuscles.forEach { mg ->
                                val isSelected = mg in focusMuscles
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        focusMuscles = if (isSelected && focusMuscles.size > 1) {
                                            focusMuscles - mg
                                        } else if (!isSelected && focusMuscles.size < 6) {
                                            focusMuscles + mg
                                        } else {
                                            focusMuscles
                                        }
                                    },
                                    label = { Text(mg.labelUk) },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = InkObsidian,
                                        selectedLabelColor = VoltLime,
                                        selectedLeadingIconColor = VoltLime
                                    ),
                                    modifier = Modifier.testTag("onboarding_muscle_${mg.name.lowercase()}")
                                )
                            }
                        }

                        val splitSummary = when (weeklySessions) {
                            2 -> "Full Body 2×/тиждень + відновлювальне плавання"
                            3 -> "Full Body 3×/тиждень (оптимальна MPS частота)"
                            4 -> "Upper / Lower 4×/тиждень (розведене кардіо)"
                            5 -> "Push / Pull / Legs + Upper/Cardio"
                            else -> "Push / Pull / Legs 2× (високий MAV обсяг)"
                        }

                        val proteinGrams = (parsedWeight * goal.proteinPerKg).roundToInt()

                        Surface(
                            color = SurfaceWhite,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, HairlineBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "ПЕРСОНАЛЬНИЙ ПЛАН СТАРТУ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VoltLimeDark,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = splitSummary,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = InkObsidian
                                )
                                Text(
                                    text = "• Цільовий калораж: ${previewProfile.recommendedTargetKcal()} ккал/день\n" +
                                        "• Добовий білок: ~$proteinGrams г (${goal.proteinPerKg} г/кг)\n" +
                                        "• Обладнання: ${equipment.labelUk}\n" +
                                        "• Фокус: ${focusMuscles.joinToString(", ") { it.labelUk }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkCharcoal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (step > 0) {
                        OutlinedButton(
                            onClick = { step-- },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        ) {
                            Text("Назад")
                        }
                    }
                    Button(
                        onClick = {
                            if (step < 2) {
                                step++
                            } else {
                                onComplete(
                                    displayName.trim().ifEmpty { "Атлет" },
                                    parsedWeight,
                                    ageYears,
                                    heightCm,
                                    experience,
                                    goal,
                                    equipment,
                                    focusMuscles,
                                    weeklySessions
                                )
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
                            .testTag(if (step < 2) "onboarding_next_button" else "onboarding_finish_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = VoltLime,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = if (step < 2) "Далі" else "Завершити налаштування",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StepperCard(
    label: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = InkMuted)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("${testTagPrefix}_dec")
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Зменшити $label")
                }
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleLarge,
                    color = InkObsidian,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("${testTagPrefix}_inc")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Збільшити $label")
                }
            }
        }
    }
}

@Composable
private fun SelectableOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) VoltLimeSoft else SurfaceWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) InkObsidian else HairlineBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = InkObsidian
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkCharcoal
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Обрано",
                    tint = InkObsidian
                )
            }
        }
    }
}
