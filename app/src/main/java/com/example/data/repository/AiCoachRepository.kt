package com.example.data.repository

import android.util.Log
import com.example.data.model.AthleteProfile
import com.example.data.model.WorkoutLog
import com.example.data.science.WeeklyScienceSummary
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class GeneratedAiInsight(
    val summary: String,
    val volumeStatus: String,
    val recoveryAdvice: String,
    val nextWorkoutPlan: String
)

class AiCoachRepository {

    private val insightSchema = Schema.obj(
        properties = mapOf(
            "summary" to Schema.string(
                description = "Concise Ukrainian assessment of the natural athlete's last 7 days across strength, running, and swimming."
            ),
            "volumeStatus" to Schema.string(
                description = "Short status badge in Ukrainian evaluating MEV/MAV/MRV weekly volume and sRPE load."
            ),
            "recoveryAdvice" to Schema.string(
                description = "Evidence-based Ukrainian guidance on RIR/RPE 7-9, MPS kinetics (24-48h), and cardio interference management."
            ),
            "nextWorkoutPlan" to Schema.string(
                description = "Specific next workout recommendation in Ukrainian (exercises/sets/RIR or cardio zone/duration/distance)."
            )
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = insightSchema
            }
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    suspend fun analyzeTrainingWeek(
        profile: AthleteProfile,
        workouts: List<WorkoutLog>,
        scienceSummary: WeeklyScienceSummary
    ): GeneratedAiInsight {
        val recentLines = workouts.take(12).joinToString("\n") { w ->
            "- ${w.workoutTypeEnum().labelUk} «${w.title}»: ${w.durationMinutes} хв, дистанція ${w.formattedDistance()}, RPE ${w.rpe}/10 (RIR ${w.rir}), sRPE=${w.sessionLoadSrpe}, м'язи: ${w.muscleGroupEnum().labelUk}, підходів: ${w.workingSets}. Нотатка: ${w.notes.ifBlank { "немає" }}"
        }.ifBlank { "Тренування ще не записані." }

        val muscleLines = scienceSummary.muscleVolumes.joinToString(", ") { mv ->
            "${mv.muscleGroup.labelUk}: ${mv.completedSets} підх. (MEV ${mv.mev}, MAV до ${mv.mavMax}, MRV ${mv.mrv})"
        }

        val prompt = """
            Ти — елітний спортивний науковець та тренер із натурального бодібілдингу й гібридного тренінгу.
            Відповідай виключно українською мовою. Базуйся на сучасних мета-аналізах (2022–2026) щодо натуральної гіпертрофії:
            - Оптимальний тижневий обсяг MAV: 10–18 робочих підходів на м'язову групу (>20 MRV підвищує ризик системної втоми натурала).
            - Оптимум близькості до відмови: 1–3 RIR (RPE 7–9) у базових вправах; уникати постійного RPE 10.
            - Частота стимуляції м'язів 2+ рази/тиждень через кінетику MPS (24–48 год у натуралів).
            - Інтерференція кардіо (біг/плавання): розводити важкий біг і силові на ноги (6–24 год), плавання цінне як низькоударне кардіо.
            - Білок: 1.6–2.2 г/кг, помірний профіцит +200–350 ккал для чистого набору.

            Дані атлета:
            - Ім'я: ${profile.displayName}, вік: ${profile.ageYears} р., зріст: ${profile.heightCm} см, вага: ${profile.bodyWeightKg} кг (BMI ${String.format("%.1f", profile.bmi())}, TDEE ~${profile.estimatedTdeeKcal()} ккал)
            - Досвід: ${profile.experienceEnum().labelUk}, ціль: ${profile.goalEnum().labelUk}, обладнання: ${profile.equipmentEnum().labelUk}
            - Пріоритетні м'язи: ${profile.focusMuscleEnums().joinToString(", ") { it.labelUk }.ifBlank { "Усе тіло" }}
            - Ціль тренувань на тиждень: ${profile.weeklyTargetSessions}. Deload рекомендовано: ${if (scienceSummary.deloadRecommended) "ТАК" else "НІ"}
            - Зведення за 7 днів: сесій ${scienceSummary.totalSessions} (силові: ${scienceSummary.strengthSessions}, біг: ${scienceSummary.runningSessions}, плавання: ${scienceSummary.swimmingSessions}), час ${scienceSummary.totalMinutes} хв, дистанція ${String.format("%.1f", scienceSummary.totalDistanceKm)} км, сумарний sRPE ${scienceSummary.totalSrpeLoad} (бюджет ${scienceSummary.recommendedSrpeBudget}), середній RPE ${String.format("%.1f", scienceSummary.averageRpe)}.
            - Обсяг по групах м'язів: $muscleLines
            - Останні тренування:
            $recentLines

            Сформуй точний, персоналізований та практичний розбір у JSON за заданою схемою.
        """.trimIndent()

        val response = withTimeout(60_000L) {
            structuredModel.generateContent(prompt)
        }
        val jsonText = response.text ?: throw IllegalStateException("Модель повернула порожню відповідь.")
        val json = JSONObject(jsonText)
        return GeneratedAiInsight(
            summary = json.optString("summary", "Аналіз тижня виконано."),
            volumeStatus = json.optString("volumeStatus", "MAV — Контроль тижневого обсягу"),
            recoveryAdvice = json.optString("recoveryAdvice", "Зберігайте 1–3 RIR та контролюйте навантаження sRPE."),
            nextWorkoutPlan = json.optString("nextWorkoutPlan", "Заплануйте збалансоване силове тренування на RPE 8.")
        )
    }

    fun streamCoachReply(
        question: String,
        profile: AthleteProfile,
        workouts: List<WorkoutLog>,
        scienceSummary: WeeklyScienceSummary
    ): Flow<String> = flow {
        val recentSummary = workouts.take(6).joinToString("; ") {
            "${it.workoutTypeEnum().labelUk} ${it.durationMinutes}хв RPE ${it.rpe}"
        }.ifBlank { "немає записів" }

        val prompt = """
            Ти — FitAI Coach, науковий AI-тренер для натуральних атлетів. Відповідай українською, чітко й структуровано, спираючись на сучасні дослідження натуральної гіпертрофії (MEV/MAV/MRV, 1–3 RIR / RPE 7–9, MPS 24–48 год, інтерференція бігу та плавання).
            Профіль атлета: ${profile.bodyWeightKg} кг, ${profile.experienceEnum().labelUk}, ціль: ${profile.goalEnum().labelUk}.
            Тиждень: ${scienceSummary.totalSessions} тренувань, sRPE ${scienceSummary.totalSrpeLoad}/${scienceSummary.recommendedSrpeBudget}, білок ${scienceSummary.dailyProteinGramsMin}–${scienceSummary.dailyProteinGramsMax} г/день.
            Останні сесії: $recentSummary.

            Запитання спортсмена: $question
        """.trimIndent()

        val accumulator = StringBuilder()
        try {
            withTimeout(60_000L) {
                chatModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulator.append(piece)
                        emit(accumulator.toString())
                    }
                }
            }
            if (accumulator.isBlank()) {
                throw IllegalStateException("AI-тренер не надав відповіді.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AiCoachRepo", "Streaming failed", e)
            throw e
        }
    }
}
