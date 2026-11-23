package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.example.ui.theme.InkObsidian
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.VoltLime
import com.example.ui.theme.ZoneHighCrimson
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileSettingsSheet(
    profile: AthleteProfile,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
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
    onSignOut: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(profile) { mutableStateOf(profile.displayName) }
    var weightText by remember(profile) { mutableStateOf(profile.bodyWeightKg.toString().removeSuffix(".0")) }
    var ageText by remember(profile) { mutableStateOf(profile.ageYears.toString()) }
    var heightText by remember(profile) { mutableStateOf(profile.heightCm.toString()) }
    var experience by remember(profile) { mutableStateOf(profile.experienceEnum()) }
    var goal by remember(profile) { mutableStateOf(profile.goalEnum()) }
    var equipment by remember(profile) { mutableStateOf(profile.equipmentEnum()) }
    var focusMuscles by remember(profile) {
        mutableStateOf(
            profile.focusMuscleEnums()
                .filter { it != MuscleGroup.NONE && it != MuscleGroup.FULL_BODY }
                .ifEmpty { listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADS) }
        )
    }
    var weeklySessions by remember(profile) { mutableIntStateOf(profile.weeklyTargetSessions.toInt().coerceIn(1, 14)) }

    val parsedWeight = (weightText.toDoubleOrNull() ?: profile.bodyWeightKg).coerceIn(30.0, 250.0)
    val parsedAge = (ageText.toIntOrNull() ?: profile.ageYears.toInt()).coerceIn(14, 90)
    val parsedHeight = (heightText.toIntOrNull() ?: profile.heightCm.toInt()).coerceIn(120, 230)
    val previewProfile = remember(parsedWeight, parsedAge, parsedHeight, experience, goal, weeklySessions) {
        AthleteProfile(
            bodyWeightKg = parsedWeight,
            ageYears = parsedAge.toLong(),
            heightCm = parsedHeight.toLong(),
            trainingExperience = experience.name,
            primaryGoal = goal.name,
            weeklyTargetSessions = weeklySessions.toLong()
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CanvasStone
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Профіль натурального атлета",
                style = MaterialTheme.typography.headlineMedium,
                color = InkObsidian
            )

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 80) name = it },
                label = { Text("Ім'я атлета") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_name_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                    label = { Text("Вага (кг)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_weight_input")
                )

                OutlinedTextField(
                    value = ageText,
                    onValueChange = { input ->
                        if (input.length <= 2 && input.all { it.isDigit() }) {
                            ageText = input
                        }
                    },
                    label = { Text("Вік") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("profile_age_input")
                )

                OutlinedTextField(
                    value = heightText,
                    onValueChange = { input ->
                        if (input.length <= 3 && input.all { it.isDigit() }) {
                            heightText = input
                        }
                    },
                    label = { Text("Зріст (см)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_height_input")
                )
            }

            Surface(
                color = SurfaceWhite,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ІМТ: ${String.format(Locale.forLanguageTag("uk"), "%.1f", previewProfile.bmi())} (${previewProfile.bmiStatusUk()})",
                        style = MaterialTheme.typography.labelLarge,
                        color = InkCharcoal
                    )
                    Text(
                        text = "TDEE ${previewProfile.estimatedTdeeKcal()} ккал",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = InkObsidian
                    )
                }
            }

            Text("Тренувальний стаж", style = MaterialTheme.typography.labelLarge, color = InkObsidian)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TrainingExperience.entries.forEach { exp ->
                    FilterChip(
                        selected = experience == exp,
                        onClick = { experience = exp },
                        label = { Text(exp.labelUk) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = InkObsidian,
                            selectedLabelColor = VoltLime
                        )
                    )
                }
            }

            Text("Головна ціль", style = MaterialTheme.typography.labelLarge, color = InkObsidian)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PrimaryGoal.entries.forEach { g ->
                    FilterChip(
                        selected = goal == g,
                        onClick = { goal = g },
                        label = { Text(g.labelUk) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = InkObsidian,
                            selectedLabelColor = VoltLime
                        )
                    )
                }
            }

            Text("Обладнання", style = MaterialTheme.typography.labelLarge, color = InkObsidian)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EquipmentTier.entries.forEach { eq ->
                    FilterChip(
                        selected = equipment == eq,
                        onClick = { equipment = eq },
                        label = { Text(eq.labelUk) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = InkObsidian,
                            selectedLabelColor = VoltLime
                        )
                    )
                }
            }

            Text("Пріоритетні м'язи", style = MaterialTheme.typography.labelLarge, color = InkObsidian)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MuscleGroup.strengthTargets.filter { it != MuscleGroup.FULL_BODY }.forEach { mg ->
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
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = InkObsidian,
                            selectedLabelColor = VoltLime,
                            selectedLeadingIconColor = VoltLime
                        )
                    )
                }
            }

            Text("Ціль тренувань на тиждень: $weeklySessions", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 3, 4, 5, 6).forEach { count ->
                    FilterChip(
                        selected = weeklySessions == count,
                        onClick = { weeklySessions = count },
                        label = { Text("$count×") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = InkObsidian,
                            selectedLabelColor = VoltLime
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Закрити")
                }
                Button(
                    onClick = {
                        onSave(
                            name,
                            parsedWeight,
                            parsedAge,
                            parsedHeight,
                            experience,
                            goal,
                            equipment,
                            focusMuscles,
                            weeklySessions
                        )
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InkObsidian,
                        contentColor = VoltLime
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp)
                        .testTag("save_profile_button")
                ) {
                    Text("Зберегти профіль")
                }
            }

            TextButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = ZoneHighCrimson,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "  Вийти з акаунту Google",
                    color = ZoneHighCrimson,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
