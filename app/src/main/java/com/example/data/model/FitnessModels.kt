package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlin.math.roundToInt

enum class WorkoutType(
    val labelUk: String,
    val shortDescUk: String
) {
    STRENGTH("Силові", "Гіпертрофія, RIR та робочі підходи"),
    RUNNING("Біг", "Аеробна витривалість, інтервали"),
    SWIMMING("Плавання", "Низькоударне кардіо та відновлення");

    companion object {
        fun fromRaw(raw: String?): WorkoutType =
            entries.firstOrNull { it.name == raw } ?: STRENGTH
    }
}

enum class DistanceUnit(val labelUk: String) {
    KM("км"),
    M("м");

    companion object {
        fun fromRaw(raw: String?): DistanceUnit =
            entries.firstOrNull { it.name == raw } ?: KM
    }
}

enum class MuscleGroup(
    val labelUk: String,
    val mevSets: Int,
    val mavMinSets: Int,
    val mavMaxSets: Int,
    val mrvSets: Int
) {
    NONE("Загальне / Кардіо", 0, 0, 0, 0),
    CHEST("Груди", 6, 10, 18, 22),
    BACK("Спина", 8, 12, 20, 24),
    QUADS("Квадрицепси", 6, 10, 16, 20),
    HAMSTRINGS("Біцепс стегна", 4, 8, 14, 18),
    SHOULDERS("Плечі", 6, 10, 18, 22),
    ARMS("Руки (біцепс/трицепс)", 4, 8, 16, 20),
    CORE("Кор / Прес", 4, 6, 14, 18),
    FULL_BODY("Усе тіло", 6, 10, 16, 20);

    companion object {
        fun fromRaw(raw: String?): MuscleGroup =
            entries.firstOrNull { it.name == raw } ?: NONE

        val strengthTargets: List<MuscleGroup> =
            listOf(CHEST, BACK, QUADS, HAMSTRINGS, SHOULDERS, ARMS, CORE, FULL_BODY)
    }
}

enum class TrainingExperience(
    val labelUk: String,
    val volumeMultiplier: Double,
    val recommendedFreqUk: String
) {
    BEGINNER("Новачок (< 1 року)", 0.85, "2–3 тренування / тиждень (Full Body)"),
    INTERMEDIATE("Середній (1–3 роки)", 1.0, "3–5 тренувань / тиждень (Upper/Lower)"),
    ADVANCED("Досвідчений (3+ роки)", 1.15, "4–6 тренувань / тиждень (PPL / Upper-Lower)");

    companion object {
        fun fromRaw(raw: String?): TrainingExperience =
            entries.firstOrNull { it.name == raw } ?: INTERMEDIATE
    }
}

enum class PrimaryGoal(
    val labelUk: String,
    val caloriesDeltaKcal: Int,
    val proteinPerKg: Double
) {
    HYPERTROPHY("Чистий набір маси", 250, 2.0),
    RECOMPOSITION("Рекомпозиція тіла", 0, 2.2),
    ENDURANCE_HYBRID("Гібридний атлет (сила + кардіо)", 150, 1.9);

    companion object {
        fun fromRaw(raw: String?): PrimaryGoal =
            entries.firstOrNull { it.name == raw } ?: HYPERTROPHY
    }
}

enum class EquipmentTier(
    val labelUk: String,
    val descriptionUk: String
) {
    BODYWEIGHT("Без обладнання", "Тренування з вагою власного тіла"),
    DUMBBELLS("Гантелі", "Вправи з гантелями та вагою тіла"),
    HOME_GYM("Гаражний зал", "Штанга, гантелі та базова лавка"),
    GYM("Тренажерний зал", "Повний набір тренажерів, штанги та блоків");

    companion object {
        fun fromRaw(raw: String?): EquipmentTier =
            entries.firstOrNull { it.name == raw } ?: GYM
    }
}

@Keep
@IgnoreExtraProperties
data class AthleteProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Атлет",
    val bodyWeightKg: Double = 75.0,
    val ageYears: Long = 26L,
    val heightCm: Long = 178L,
    val trainingExperience: String = TrainingExperience.INTERMEDIATE.name,
    val primaryGoal: String = PrimaryGoal.HYPERTROPHY.name,
    val equipment: String = EquipmentTier.GYM.name,
    val focusMuscles: List<String> = listOf("CHEST", "BACK", "QUADS"),
    val weeklyTargetSessions: Long = 4L,
    val onboardingDone: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun experienceEnum(): TrainingExperience = TrainingExperience.fromRaw(trainingExperience)
    fun goalEnum(): PrimaryGoal = PrimaryGoal.fromRaw(primaryGoal)
    fun equipmentEnum(): EquipmentTier = EquipmentTier.fromRaw(equipment)

    fun focusMuscleEnums(): List<MuscleGroup> =
        focusMuscles.mapNotNull { code -> MuscleGroup.entries.firstOrNull { it.name == code } }

    fun bmi(): Double {
        val hMeters = (heightCm.coerceIn(120L, 230L) / 100.0)
        return bodyWeightKg.coerceIn(30.0, 250.0) / (hMeters * hMeters)
    }

    fun bmiStatusUk(): String {
        val v = bmi()
        return when {
            v < 18.5 -> "Недостатня вага"
            v < 25.0 -> "Здорова норма"
            v < 30.0 -> "Підвищена маса (або м'язи)"
            else -> "Висока маса тіла"
        }
    }

    fun estimatedTdeeKcal(): Int {
        // Mifflin-St Jeor baseline + weekly training activity multiplier
        val bmr = 10.0 * bodyWeightKg + 6.25 * heightCm - 5.0 * ageYears + 5.0
        val factor = when (weeklyTargetSessions.toInt()) {
            in 1..2 -> 1.375
            in 3..4 -> 1.55
            else -> 1.65
        }
        return (bmr * factor).roundToInt()
    }

    fun recommendedTargetKcal(): Int =
        estimatedTdeeKcal() + goalEnum().caloriesDeltaKcal
}

@Keep
@IgnoreExtraProperties
data class WorkoutLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = WorkoutType.STRENGTH.name,
    val title: String = "",
    val durationMinutes: Long = 45L,
    val distanceValue: Double = 0.0,
    val distanceUnit: String = DistanceUnit.KM.name,
    val rpe: Long = 8L,
    val muscleGroup: String = MuscleGroup.CHEST.name,
    val workingSets: Long = 10L,
    val rir: Long = 2L,
    val sessionLoadSrpe: Long = 360L,
    val notes: String = "",
    val performedAtMillis: Long = System.currentTimeMillis(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun workoutTypeEnum(): WorkoutType = WorkoutType.fromRaw(type)
    fun distanceUnitEnum(): DistanceUnit = DistanceUnit.fromRaw(distanceUnit)
    fun muscleGroupEnum(): MuscleGroup = MuscleGroup.fromRaw(muscleGroup)

    fun distanceInKm(): Double {
        return when (distanceUnitEnum()) {
            DistanceUnit.KM -> distanceValue
            DistanceUnit.M -> distanceValue / 1000.0
        }
    }

    fun formattedDistance(): String {
        if (distanceValue <= 0.0) return "—"
        return when (distanceUnitEnum()) {
            DistanceUnit.KM -> String.format("%.2f км", distanceValue).replace(".00", "")
            DistanceUnit.M -> "${distanceValue.roundToInt()} м"
        }
    }

    fun rpeZoneLabel(): String {
        return when (rpe.toInt()) {
            in 1..4 -> "Відновлення / Зона 1–2"
            in 5..6 -> "Аеробна / Помірна база"
            in 7..8 -> "Оптимум гіпертрофії (2–3 RIR)"
            9 -> "Високе напруження (1 RIR)"
            else -> "Максимальна відмова (0 RIR)"
        }
    }
}

@Keep
@IgnoreExtraProperties
data class AiCoachInsight(
    @DocumentId val id: String = "",
    val userId: String = "",
    val summary: String = "",
    val volumeStatus: String = "",
    val recoveryAdvice: String = "",
    val nextWorkoutPlan: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

fun deriveRirFromRpe(rpe: Int): Int {
    return when {
        rpe >= 10 -> 0
        rpe == 9 -> 1
        rpe == 8 -> 2
        rpe == 7 -> 3
        rpe == 6 -> 4
        else -> 5
    }
}

fun buildWorkoutCreateMap(
    userId: String,
    type: WorkoutType,
    title: String,
    durationMinutes: Int,
    distanceValue: Double,
    distanceUnit: DistanceUnit,
    rpe: Int,
    muscleGroup: MuscleGroup,
    workingSets: Int,
    rir: Int,
    notes: String,
    performedAtMillis: Long
): Map<String, Any> {
    val safeDuration = durationMinutes.coerceIn(1, 600)
    val safeRpe = rpe.coerceIn(1, 10)
    val srpe = (safeDuration * safeRpe).coerceIn(1, 6000)
    return mapOf(
        "userId" to userId,
        "type" to type.name,
        "title" to title.trim().take(100),
        "durationMinutes" to safeDuration,
        "distanceValue" to distanceValue.coerceIn(0.0, 500000.0),
        "distanceUnit" to distanceUnit.name,
        "rpe" to safeRpe,
        "muscleGroup" to muscleGroup.name,
        "workingSets" to workingSets.coerceIn(0, 60),
        "rir" to rir.coerceIn(0, 5),
        "sessionLoadSrpe" to srpe,
        "notes" to notes.trim().take(500),
        "performedAtMillis" to performedAtMillis.coerceAtLeast(1L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

fun buildWorkoutUpdateMap(
    type: WorkoutType,
    title: String,
    durationMinutes: Int,
    distanceValue: Double,
    distanceUnit: DistanceUnit,
    rpe: Int,
    muscleGroup: MuscleGroup,
    workingSets: Int,
    rir: Int,
    notes: String,
    performedAtMillis: Long
): Map<String, Any> {
    val safeDuration = durationMinutes.coerceIn(1, 600)
    val safeRpe = rpe.coerceIn(1, 10)
    val srpe = (safeDuration * safeRpe).coerceIn(1, 6000)
    return mapOf(
        "type" to type.name,
        "title" to title.trim().take(100),
        "durationMinutes" to safeDuration,
        "distanceValue" to distanceValue.coerceIn(0.0, 500000.0),
        "distanceUnit" to distanceUnit.name,
        "rpe" to safeRpe,
        "muscleGroup" to muscleGroup.name,
        "workingSets" to workingSets.coerceIn(0, 60),
        "rir" to rir.coerceIn(0, 5),
        "sessionLoadSrpe" to srpe,
        "notes" to notes.trim().take(500),
        "performedAtMillis" to performedAtMillis.coerceAtLeast(1L),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
