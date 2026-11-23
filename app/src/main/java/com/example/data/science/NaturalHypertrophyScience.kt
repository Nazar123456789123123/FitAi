package com.example.data.science

import com.example.data.model.AthleteProfile
import com.example.data.model.MuscleGroup
import com.example.data.model.TrainingExperience
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutType
import java.util.Calendar
import kotlin.math.roundToInt

enum class VolumeZone(val labelUk: String) {
    BELOW_MEV("Нижче MEV — підтримання"),
    MEV_TO_MAV("MEV → MAV — прогрес"),
    MAV_OPTIMAL("MAV — золота зона натурала"),
    MRV_WARNING("Близько до MRV — ризик втоми")
}

data class DailyProgressPoint(
    val dayShortUk: String,
    val dayOfMonth: Int,
    val isToday: Boolean,
    val workoutCount: Int,
    val totalSrpe: Int,
    val totalMinutes: Int,
    val totalSets: Int,
    val totalDistanceKm: Double,
    val averageRpe: Double,
    val dominantType: WorkoutType?
)

data class MuscleVolumeMetric(
    val muscleGroup: MuscleGroup,
    val completedSets: Int,
    val mev: Int,
    val mavMax: Int,
    val mrv: Int,
    val zone: VolumeZone
)

data class WeeklyScienceSummary(
    val totalSessions: Int,
    val strengthSessions: Int,
    val runningSessions: Int,
    val swimmingSessions: Int,
    val totalMinutes: Int,
    val totalDistanceKm: Double,
    val totalSrpeLoad: Int,
    val previousWeekSrpe: Int,
    val averageRpe: Double,
    val cardioRatioPercent: Int,
    val interferenceRiskUk: String,
    val interferenceIsHigh: Boolean,
    val dailyProteinGramsMin: Int,
    val dailyProteinGramsMax: Int,
    val recommendedSrpeBudget: Int,
    val muscleVolumes: List<MuscleVolumeMetric>,
    val dailyPoints: List<DailyProgressPoint>,
    val deloadRecommended: Boolean,
    val deloadReasonUk: String,
    val laggingMuscleGroup: MuscleGroup?,
    val recommendedSplitDays: List<String>,
    val readinessVerdictUk: String
)

data class SciencePrinciple(
    val id: String,
    val badge: String,
    val title: String,
    val takeaway: String,
    val details: String,
    val evidenceRef: String
)

object NaturalHypertrophyScience {

    private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L
    private const val SEVEN_DAYS_MILLIS = 7L * ONE_DAY_MILLIS

    private fun startOfDayMillis(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun weekdayShortUk(dayOfWeek: Int): String = when (dayOfWeek) {
        Calendar.MONDAY -> "Пн"
        Calendar.TUESDAY -> "Вт"
        Calendar.WEDNESDAY -> "Ср"
        Calendar.THURSDAY -> "Чт"
        Calendar.FRIDAY -> "Пт"
        Calendar.SATURDAY -> "Сб"
        Calendar.SUNDAY -> "Нд"
        else -> "—"
    }

    fun analyzeLastSevenDays(
        workouts: List<WorkoutLog>,
        profile: AthleteProfile,
        nowMillis: Long = System.currentTimeMillis()
    ): WeeklyScienceSummary {
        val todayStart = startOfDayMillis(nowMillis)
        val sevenDayStart = todayStart - 6L * ONE_DAY_MILLIS
        val prevWeekStart = sevenDayStart - SEVEN_DAYS_MILLIS

        val weekLogs = workouts.filter { it.performedAtMillis >= sevenDayStart }
        val prevWeekLogs = workouts.filter {
            it.performedAtMillis in prevWeekStart until sevenDayStart
        }

        val dailyPoints = (0..6).map { offset ->
            val dayStart = sevenDayStart + offset * ONE_DAY_MILLIS
            val dayEnd = dayStart + ONE_DAY_MILLIS
            val cal = Calendar.getInstance().apply { timeInMillis = dayStart }
            val dayLogs = weekLogs.filter { it.performedAtMillis in dayStart until dayEnd }
            val dominant = dayLogs
                .groupingBy { it.workoutTypeEnum() }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key

            DailyProgressPoint(
                dayShortUk = weekdayShortUk(cal.get(Calendar.DAY_OF_WEEK)),
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                isToday = offset == 6,
                workoutCount = dayLogs.size,
                totalSrpe = dayLogs.sumOf { it.sessionLoadSrpe.toInt() },
                totalMinutes = dayLogs.sumOf { it.durationMinutes.toInt() },
                totalSets = dayLogs.sumOf { it.workingSets.toInt() },
                totalDistanceKm = dayLogs.sumOf { it.distanceInKm() },
                averageRpe = if (dayLogs.isEmpty()) 0.0 else dayLogs.map { it.rpe.toDouble() }.average(),
                dominantType = dominant
            )
        }

        val totalSessions = weekLogs.size
        val strengthLogs = weekLogs.filter { it.workoutTypeEnum() == WorkoutType.STRENGTH }
        val runningLogs = weekLogs.filter { it.workoutTypeEnum() == WorkoutType.RUNNING }
        val swimmingLogs = weekLogs.filter { it.workoutTypeEnum() == WorkoutType.SWIMMING }

        val totalMinutes = weekLogs.sumOf { it.durationMinutes.toInt() }
        val totalDistanceKm = weekLogs.sumOf { it.distanceInKm() }
        val totalSrpeLoad = weekLogs.sumOf { it.sessionLoadSrpe.toInt() }
        val previousWeekSrpe = prevWeekLogs.sumOf { it.sessionLoadSrpe.toInt() }
        val averageRpe = if (weekLogs.isEmpty()) 0.0 else weekLogs.map { it.rpe.toDouble() }.average()

        val cardioMinutes = runningLogs.sumOf { it.durationMinutes.toInt() } +
            swimmingLogs.sumOf { it.durationMinutes.toInt() }
        val cardioRatioPercent = if (totalMinutes > 0) {
            ((cardioMinutes.toDouble() / totalMinutes) * 100.0).roundToInt()
        } else 0

        val exp = profile.experienceEnum()
        val multiplier = exp.volumeMultiplier

        val trackedGroups = listOf(
            MuscleGroup.CHEST,
            MuscleGroup.BACK,
            MuscleGroup.QUADS,
            MuscleGroup.HAMSTRINGS,
            MuscleGroup.SHOULDERS,
            MuscleGroup.ARMS
        )

        val muscleVolumes = trackedGroups.map { group ->
            val directSets = strengthLogs
                .filter { it.muscleGroupEnum() == group }
                .sumOf { it.workingSets.toInt() }
            val fullBodyFraction = strengthLogs
                .filter { it.muscleGroupEnum() == MuscleGroup.FULL_BODY }
                .sumOf { (it.workingSets.toInt() / 4).coerceAtLeast(1) }
            val totalSets = directSets + fullBodyFraction

            val mev = (group.mevSets * multiplier).roundToInt().coerceAtLeast(4)
            val mavMin = (group.mavMinSets * multiplier).roundToInt().coerceAtLeast(6)
            val mavMax = (group.mavMaxSets * multiplier).roundToInt().coerceAtLeast(12)
            val mrv = (group.mrvSets * multiplier).roundToInt().coerceAtLeast(16)

            val zone = when {
                totalSets < mev -> VolumeZone.BELOW_MEV
                totalSets < mavMin -> VolumeZone.MEV_TO_MAV
                totalSets <= mavMax -> VolumeZone.MAV_OPTIMAL
                else -> VolumeZone.MRV_WARNING
            }
            MuscleVolumeMetric(
                muscleGroup = group,
                completedSets = totalSets,
                mev = mev,
                mavMax = mavMax,
                mrv = mrv,
                zone = zone
            )
        }

        val highIntensityRunMinutes = runningLogs
            .filter { it.rpe >= 8 }
            .sumOf { it.durationMinutes.toInt() }

        val (interferenceRiskUk, interferenceIsHigh) = when {
            weekLogs.isEmpty() ->
                "Запишіть перше тренування для оцінки кардіо-інтерференції" to false
            highIntensityRunMinutes > 90 && strengthLogs.isNotEmpty() ->
                "Увага: багато важкого бігу (RPE ≥ 8). Розводьте біг та силові на ноги мінімум на 6–24 год або замініть частину на плавання." to true
            swimmingLogs.isNotEmpty() && runningLogs.isEmpty() ->
                "Мінімальна інтерференція: плавання не створює ударного ексцентричного навантаження на ноги і прискорює відновлення." to false
            cardioRatioPercent > 55 && profile.goalEnum().name == "HYPERTROPHY" ->
                "Висока частка кардіо ($cardioRatioPercent%) при цілі набору маси — компенсуйте калоражем (+250–350 ккал)." to true
            else ->
                "Оптимальний баланс: аеробна робота підтримує капіляризацію без пригнічення сигналу mTOR." to false
        }

        val recommendedSrpeBudget = when (exp) {
            TrainingExperience.BEGINNER -> 1400
            TrainingExperience.INTERMEDIATE -> 2100
            TrainingExperience.ADVANCED -> 2700
        }

        val readinessVerdictUk = when {
            totalSessions == 0 -> "Готовий до старту тижневого циклу"
            totalSrpeLoad > recommendedSrpeBudget ->
                "Високе накопичене навантаження (${totalSrpeLoad} AU). Заплануйте легкий день або Deload (RPE 5–6)."
            averageRpe > 8.8 && totalSessions >= 4 ->
                "Висока середня інтенсивність (RPE ${String.format("%.1f", averageRpe)}). Тримайте 2 RIR, щоб уникнути вигорання ЦНС."
            else ->
                "Стабільна прогресія натурального атлета — оптимальна зона адаптації."
        }

        val highRpeStreak = weekLogs.count { it.rpe >= 9 }
        val deloadRecommended = totalSrpeLoad > recommendedSrpeBudget || (highRpeStreak >= 3 && totalSessions >= 4)
        val deloadReasonUk = when {
            totalSrpeLoad > recommendedSrpeBudget ->
                "Тижневе навантаження ($totalSrpeLoad AU) перевищує бюджет ($recommendedSrpeBudget AU). Знизьте підходи на 40–50% і тримайте RPE 5–6."
            highRpeStreak >= 3 && totalSessions >= 4 ->
                "Зафіксовано $highRpeStreak важких сесій (RPE ≥ 9) за тиждень. Рекомендовано легку відновну сесію (2–3 RIR) для розвантаження ЦНС."
            else ->
                "Втома під контролем — продовжуйте подвійну прогресію в межах MAV (1–3 RIR)."
        }

        val focusSet = profile.focusMuscleEnums().toSet()
        val laggingMuscleGroup = muscleVolumes
            .filter { mv -> focusSet.isEmpty() || mv.muscleGroup in focusSet }
            .filter { mv -> mv.zone == VolumeZone.BELOW_MEV }
            .minByOrNull { it.completedSets }
            ?.muscleGroup
            ?: muscleVolumes.filter { it.zone == VolumeZone.BELOW_MEV }.minByOrNull { it.completedSets }?.muscleGroup

        val recommendedSplitDays = when (profile.weeklyTargetSessions.toInt()) {
            1, 2 -> listOf("Пн: Full Body (Силові)", "Чт: Full Body + Плавання")
            3 -> listOf("Пн: Full Body A", "Ср: Full Body B + Кардіо Зона 2", "Пт: Full Body C")
            4 -> listOf("Пн: Верх (Сила)", "Вт: Низ (Гіпертрофія)", "Чт: Верх (Обсяг) + Плав.", "Пт: Низ + Біг (розведено)")
            5 -> listOf("Пн: Push", "Вт: Pull", "Ср: Ноги", "Пт: Верх (MAV)", "Сб: Кардіо + Слабкі зони")
            else -> listOf("Пн: Push", "Вт: Pull", "Ср: Ноги", "Чт: Плавання (віднов.)", "Пт: Верх", "Сб: Низ + Біг")
        }

        val weight = profile.bodyWeightKg.coerceIn(40.0, 200.0)
        return WeeklyScienceSummary(
            totalSessions = totalSessions,
            strengthSessions = strengthLogs.size,
            runningSessions = runningLogs.size,
            swimmingSessions = swimmingLogs.size,
            totalMinutes = totalMinutes,
            totalDistanceKm = totalDistanceKm,
            totalSrpeLoad = totalSrpeLoad,
            previousWeekSrpe = previousWeekSrpe,
            averageRpe = averageRpe,
            cardioRatioPercent = cardioRatioPercent,
            interferenceRiskUk = interferenceRiskUk,
            interferenceIsHigh = interferenceIsHigh,
            dailyProteinGramsMin = (weight * 1.6).roundToInt(),
            dailyProteinGramsMax = (weight * 2.2).roundToInt(),
            recommendedSrpeBudget = recommendedSrpeBudget,
            muscleVolumes = muscleVolumes,
            dailyPoints = dailyPoints,
            deloadRecommended = deloadRecommended,
            deloadReasonUk = deloadReasonUk,
            laggingMuscleGroup = laggingMuscleGroup,
            recommendedSplitDays = recommendedSplitDays,
            readinessVerdictUk = readinessVerdictUk
        )
    }

    val evidencePrinciples: List<SciencePrinciple> = listOf(
        SciencePrinciple(
            id = "volume",
            badge = "ОБСЯГ • 10–20 ПІДХОДІВ",
            title = "MEV / MAV / MRV для натуральних атлетів",
            takeaway = "Оптимум росту: 10–18 якісних робочих підходів на м'язову групу за тиждень.",
            details = "Без екзогенних гормонів відновлювальний ресурс організму обмежений. Дослідження показують логарифмічну залежність: 6–8 підходів (MEV) запускають ріст, 10–18 підходів (MAV) дають максимум гіпертрофії, а понад 20–22 підходи (MRV) різко підвищують пошкодження тканин і втому ЦНС.",
            evidenceRef = "Pelland et al. (2024); Schoenfeld & Israetel Volume Landmarks"
        ),
        SciencePrinciple(
            id = "rir_rpe",
            badge = "ІНТЕНСИВНІСТЬ • RPE 7–9",
            title = "Близькість до відмови (1–3 RIR) замість виснаження",
            takeaway = "Робота на RPE 7–9 (1–3 повторення в запасі) стимулює ріст так само ефективно, як RPE 10, але з удвічі меншою втомою.",
            details = "Мета-аналізи (Robinson et al., 2024) доводять, що рекрутування високопорогових рухових одиниць відбувається вже за 2–3 повторення до відмови. Постійна робота до абсолютної відмови (RPE 10) у базових вправах погіршує відновлення між сесіями у натуралів. Залишайте 2 RIR у базі та 0–1 RIR в ізоляції.",
            evidenceRef = "Robinson et al. (2024) Sports Medicine Meta-Analysis"
        ),
        SciencePrinciple(
            id = "frequency",
            badge = "ЧАСТОТА • 2–3× НА ТИЖДЕНЬ",
            title = "Кінетика синтезу м'язового білка (MPS)",
            takeaway = "Тренуйте кожну м'язову групу мінімум двічі на тиждень (Upper/Lower або Full Body).",
            details = "У натуральних атлетів пік синтезу м'язового білка (MPS) триває 24–48 годин після тренування. Розбиття 12 тижневих підходів на 2–3 сесії по 4–6 підходів забезпечує вищу якість повторень, менше «сміттєвого обсягу» та частіші анаболічні імпульси, ніж 1 тренування раз на 7 днів.",
            evidenceRef = "Schoenfeld et al. Resistance Training Frequency; Damas et al."
        ),
        SciencePrinciple(
            id = "interference",
            badge = "ГІБРИД • БІГ І ПЛАВАННЯ",
            title = "Ефект інтерференції: як поєднувати кардіо та масу",
            takeaway = "Плавання та помірний біг (RPE 5–7) не заважають гіпертрофії при розведенні на 6+ годин від силових.",
            details = "Сучасні дані (Schumann et al., 2022) спростовують міф, що кардіо «спалює м'язи». Критичними є два фактори: механічне ударне пошкодження (біг по асфальту перед днем ніг) і дефіцит калорій. Плавання дає потужну кардіо-адаптацію та активне відновлення спини/плечей без ексцентричного стресу для ніг.",
            evidenceRef = "Schumann et al. (2022) Concurrent Training Meta-Analysis"
        ),
        SciencePrinciple(
            id = "nutrition_deload",
            badge = "ВІДНОВЛЕННЯ • БІЛОК & DELOAD",
            title = "Білок 1.6–2.2 г/кг, помірний профіцит і Deload",
            takeaway = "Розвантаження кожні 5–8 тижнів (зниження обсягу на 40–50%) відновлює чутливість м'язів до навантаження.",
            details = "Для натурального набору м'язової маси оптимальний темп ваги — 0.25–0.5% маси тіла на тиждень (+200–350 ккал/день). Надлишковий профіцит призводить лише до жиру. Білок 1.6–2.2 г/кг, 7.5–9 годин сну та контроль тижневого sRPE гарантують стабільний прогрес без плато.",
            evidenceRef = "Morton et al. (2018); Helms Natural Bodybuilding Guidelines"
        )
    )
}
