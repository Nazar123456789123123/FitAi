package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AthleteProfile
import com.example.data.model.PrimaryGoal
import com.example.data.science.NaturalHypertrophyScience
import com.example.data.science.SciencePrinciple
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkObsidian
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.VoltLime
import com.example.ui.theme.VoltLimeDark
import com.example.ui.theme.VoltLimeSoft
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScienceScreen(
    profile: AthleteProfile
) {
    val weight = profile.bodyWeightKg.coerceIn(40.0, 200.0)
    val proteinMin = (weight * 1.6).roundToInt()
    val proteinMax = (weight * 2.2).roundToInt()
    val leanBulkRateMin = String.format("%.2f", weight * 0.0025)
    val leanBulkRateMax = String.format("%.2f", weight * 0.005)
    val exp = profile.experienceEnum()

    // Interactive Calculator State initialized from AthleteProfile
    var calcWeightText by remember(profile) {
        mutableStateOf(profile.bodyWeightKg.toString().removeSuffix(".0"))
    }
    var calcHeightText by remember(profile) { mutableStateOf(profile.heightCm.toString()) }
    var calcAgeText by remember(profile) { mutableStateOf(profile.ageYears.toString()) }
    var calcGoal by remember(profile) { mutableStateOf(profile.goalEnum()) }
    var calcSessions by remember(profile) {
        mutableIntStateOf(profile.weeklyTargetSessions.toInt().coerceIn(2, 6))
    }

    val calcWeight = (calcWeightText.toDoubleOrNull() ?: profile.bodyWeightKg).coerceIn(35.0, 220.0)
    val calcHeight = (calcHeightText.toIntOrNull() ?: profile.heightCm.toInt()).coerceIn(130, 225)
    val calcAge = (calcAgeText.toIntOrNull() ?: profile.ageYears.toInt()).coerceIn(14, 90)
    val calcProfile = remember(calcWeight, calcHeight, calcAge, calcGoal, calcSessions) {
        AthleteProfile(
            bodyWeightKg = calcWeight,
            heightCm = calcHeight.toLong(),
            ageYears = calcAge.toLong(),
            primaryGoal = calcGoal.name,
            weeklyTargetSessions = calcSessions.toLong()
        )
    }

    val bmr = (10.0 * calcWeight + 6.25 * calcHeight - 5.0 * calcAge + 5.0).roundToInt()
    val tdee = calcProfile.estimatedTdeeKcal()
    val targetKcal = calcProfile.recommendedTargetKcal()
    val proteinG = (calcWeight * calcGoal.proteinPerKg).roundToInt()
    val fatG = (calcWeight * 0.85).roundToInt()
    val carbsG = ((targetKcal - proteinG * 4 - fatG * 9).coerceAtLeast(80)) / 4

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Наука натуральної гіпертрофії",
                    style = MaterialTheme.typography.headlineLarge,
                    color = InkObsidian
                )
                Text(
                    text = "Актуальні мета-аналізи (2022–2026) з набору м'язової маси без фармакології та поєднання силових із бігом і плаванням.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkCharcoal
                )
                Image(
                    painter = painterResource(id = R.drawable.img_science_banner),
                    contentDescription = "Наукові криві гіпертрофії та аеробних зон",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(22.dp))
                )
            }
        }

        // Personalized Natural Athlete Targets Card
        item {
            Surface(
                color = InkObsidian,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ПЕРСОНАЛЬНІ НОРМИ ДЛЯ ${profile.bodyWeightKg} КГ (${exp.labelUk.uppercase()})",
                        style = MaterialTheme.typography.labelSmall,
                        color = VoltLime
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Білок на добу", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                            Text(
                                "$proteinMin–$proteinMax г",
                                style = MaterialTheme.typography.titleLarge,
                                color = SurfaceWhite
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Чистий набір / тижд.", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                            Text(
                                "+$leanBulkRateMin–$leanBulkRateMax кг",
                                style = MaterialTheme.typography.titleLarge,
                                color = SurfaceWhite
                            )
                        }
                    }
                    Text(
                        text = "Рекомендований спліт: ${exp.recommendedFreqUk}. Профіцит: +${profile.goalEnum().caloriesDeltaKcal} ккал/день, робочий діапазон 1–3 RIR (RPE 7–9).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VoltLimeSoft
                    )
                }
            }
        }

        // Interactive Natural Athlete Calorie, Macro & BMI Calculator
        item {
            Surface(
                color = SurfaceWhite,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .testTag("science_macro_calculator_card")
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
                            imageVector = Icons.Filled.Calculate,
                            contentDescription = null,
                            tint = VoltLimeDark
                        )
                        Column {
                            Text(
                                text = "Калькулятор калорій, БЖВ та ІМТ",
                                style = MaterialTheme.typography.titleLarge,
                                color = InkObsidian
                            )
                            Text(
                                text = "Формула Mifflin-St Jeor + помірний натуральний профіцит",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkMuted
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = calcWeightText,
                            onValueChange = { input ->
                                val cleaned = input.replace(',', '.')
                                if (cleaned.length <= 6 && cleaned.count { it == '.' } <= 1 &&
                                    cleaned.all { it.isDigit() || it == '.' }
                                ) {
                                    calcWeightText = cleaned
                                }
                            },
                            label = { Text("Вага (кг)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calc_weight_input")
                        )
                        OutlinedTextField(
                            value = calcHeightText,
                            onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) calcHeightText = it },
                            label = { Text("Зріст (см)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calc_height_input")
                        )
                        OutlinedTextField(
                            value = calcAgeText,
                            onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) calcAgeText = it },
                            label = { Text("Вік") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("calc_age_input")
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PrimaryGoal.entries.forEach { goal ->
                            FilterChip(
                                selected = calcGoal == goal,
                                onClick = { calcGoal = goal },
                                label = { Text(goal.labelUk) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = InkObsidian,
                                    selectedLabelColor = VoltLime
                                )
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 3, 4, 5, 6).forEach { count ->
                            FilterChip(
                                selected = calcSessions == count,
                                onClick = { calcSessions = count },
                                label = { Text("$count×/тиж.") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VoltLimeSoft,
                                    selectedLabelColor = InkObsidian
                                )
                            )
                        }
                    }

                    Surface(
                        color = SurfaceSubtle,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MacroCol("Ціль ккал", "$targetKcal")
                                MacroCol("TDEE / BMR", "$tdee / $bmr")
                                MacroCol(
                                    "ІМТ",
                                    String.format(Locale.forLanguageTag("uk"), "%.1f", calcProfile.bmi())
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MacroCol("Білки", "$proteinG г")
                                MacroCol("Жири", "$fatG г")
                                MacroCol("Вуглеводи", "$carbsG г")
                            }
                            Text(
                                text = "Статус ІМТ: ${calcProfile.bmiStatusUk()}. Помірний профіцит мінімізує набір жиру.",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkCharcoal
                            )
                        }
                    }
                }
            }
        }

        items(NaturalHypertrophyScience.evidencePrinciples, key = { it.id }) { principle ->
            PrincipleCard(
                principle = principle,
                modifier = Modifier.widthIn(max = 720.dp)
            )
        }
    }
}

@Composable
private fun MacroCol(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = InkMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = InkObsidian
        )
    }
}

@Composable
private fun PrincipleCard(
    principle: SciencePrinciple,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = VoltLimeSoft,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = principle.badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = VoltLimeDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Згорнути" else "Розгорнути",
                    tint = InkCharcoal
                )
            }

            Text(
                text = principle.title,
                style = MaterialTheme.typography.titleLarge,
                color = InkObsidian
            )
            Text(
                text = principle.takeaway,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = InkObsidian
            )

            if (expanded) {
                Text(
                    text = principle.details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkCharcoal
                )
                Text(
                    text = "Джерело: ${principle.evidenceRef}",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted
                )
            }
        }
    }
}
