package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AiCoachInsight
import com.example.data.model.AthleteProfile
import com.example.data.model.DistanceUnit
import com.example.data.model.EquipmentTier
import com.example.data.model.MuscleGroup
import com.example.data.model.PrimaryGoal
import com.example.data.model.TrainingExperience
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutType
import com.example.data.model.buildWorkoutCreateMap
import com.example.data.model.buildWorkoutUpdateMap
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FitnessRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Користувач повинен увійти через Google перед доступом до Firestore.")
    }

    fun observeProfile(userId: String): Flow<AthleteProfile> = flow {
        val uid = requireUserId()
        check(uid == userId) { "UID mismatch" }
        val docRef = db.collection("users").document(uid)
        val path = docRef.path
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        AthleteProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: AthleteProfile(
                        userId = uid,
                        displayName = auth.currentUser?.displayName?.take(80)?.ifBlank { "Атлет" } ?: "Атлет"
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeWorkouts(userId: String): Flow<List<WorkoutLog>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "UID mismatch" }
        val colRef = db.collection("users").document(uid).collection("workouts")
        val path = colRef.path
        emitAll(
            colRef
                .orderBy("performedAtMillis", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WorkoutLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAiInsights(userId: String): Flow<List<AiCoachInsight>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "UID mismatch" }
        val colRef = db.collection("users").document(uid).collection("ai_insights")
        val path = colRef.path
        emitAll(
            colRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(5)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AiCoachInsight::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveProfile(
        displayName: String,
        bodyWeightKg: Double,
        ageYears: Int,
        heightCm: Int,
        experience: TrainingExperience,
        goal: PrimaryGoal,
        equipment: EquipmentTier,
        focusMuscles: List<MuscleGroup>,
        weeklyTargetSessions: Int,
        onboardingDone: Boolean = true
    ): Result<Unit> {
        var docPath = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().ifBlank { "Атлет" }.take(80)
            val safeWeight = bodyWeightKg.coerceIn(30.0, 250.0)
            val safeAge = ageYears.coerceIn(14, 90)
            val safeHeight = heightCm.coerceIn(120, 230)
            val safeTarget = weeklyTargetSessions.coerceIn(1, 14)
            val safeFocus = focusMuscles
                .filter { it != MuscleGroup.NONE }
                .distinct()
                .take(8)
                .map { it.name }

            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "bodyWeightKg" to safeWeight,
                        "ageYears" to safeAge,
                        "heightCm" to safeHeight,
                        "trainingExperience" to experience.name,
                        "primaryGoal" to goal.name,
                        "equipment" to equipment.name,
                        "focusMuscles" to safeFocus,
                        "weeklyTargetSessions" to safeTarget,
                        "onboardingDone" to onboardingDone,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "bodyWeightKg" to safeWeight,
                        "ageYears" to safeAge,
                        "heightCm" to safeHeight,
                        "trainingExperience" to experience.name,
                        "primaryGoal" to goal.name,
                        "equipment" to equipment.name,
                        "focusMuscles" to safeFocus,
                        "weeklyTargetSessions" to safeTarget,
                        "onboardingDone" to onboardingDone,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addWorkout(
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
    ): Result<Unit> {
        var docPath = "users/unauthenticated/workouts"
        return try {
            val uid = requireUserId()
            require(title.trim().isNotEmpty()) { "Назва тренування не може бути порожньою" }
            require(durationMinutes in 1..600) { "Тривалість має бути від 1 до 600 хв" }
            require(distanceValue in 0.0..500000.0) { "Дистанція має бути від 0 до 500 000" }
            require(rpe in 1..10) { "Інтенсивність (RPE) має бути від 1 до 10" }

            val docRef = db.collection("users").document(uid).collection("workouts").document()
            docPath = docRef.path
            val payload = buildWorkoutCreateMap(
                userId = uid,
                type = type,
                title = title,
                durationMinutes = durationMinutes,
                distanceValue = distanceValue,
                distanceUnit = distanceUnit,
                rpe = rpe,
                muscleGroup = muscleGroup,
                workingSets = workingSets,
                rir = rir,
                notes = notes,
                performedAtMillis = performedAtMillis
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateWorkout(
        workoutId: String,
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
    ): Result<Unit> {
        var docPath = "users/unauthenticated/workouts/$workoutId"
        return try {
            val uid = requireUserId()
            require(workoutId.isNotBlank()) { "ID тренування порожній" }
            require(title.trim().isNotEmpty()) { "Назва тренування не може бути порожньою" }
            require(durationMinutes in 1..600) { "Тривалість має бути від 1 до 600 хв" }
            require(distanceValue in 0.0..500000.0) { "Дистанція має бути від 0 до 500 000" }
            require(rpe in 1..10) { "Інтенсивність (RPE) має бути від 1 до 10" }

            val docRef = db.collection("users").document(uid)
                .collection("workouts").document(workoutId)
            docPath = docRef.path
            val payload = buildWorkoutUpdateMap(
                type = type,
                title = title,
                durationMinutes = durationMinutes,
                distanceValue = distanceValue,
                distanceUnit = distanceUnit,
                rpe = rpe,
                muscleGroup = muscleGroup,
                workingSets = workingSets,
                rir = rir,
                notes = notes,
                performedAtMillis = performedAtMillis
            )
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteWorkout(workoutId: String): Result<Unit> {
        var docPath = "users/unauthenticated/workouts/$workoutId"
        return try {
            val uid = requireUserId()
            require(workoutId.isNotBlank()) { "ID тренування порожній" }
            val docRef = db.collection("users").document(uid)
                .collection("workouts").document(workoutId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveAiInsight(
        summary: String,
        volumeStatus: String,
        recoveryAdvice: String,
        nextWorkoutPlan: String
    ): Result<Unit> {
        var docPath = "users/unauthenticated/ai_insights"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("ai_insights").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "summary" to summary.trim().take(2000).ifBlank { "Аналіз сформовано." },
                "volumeStatus" to volumeStatus.trim().take(160).ifBlank { "MAV — Контроль обсягу" },
                "recoveryAdvice" to recoveryAdvice.trim().take(1200).ifBlank { "Підтримуйте 1–3 RIR і якісний сон." },
                "nextWorkoutPlan" to nextWorkoutPlan.trim().take(1200).ifBlank { "Наступна сесія: помірний обсяг RPE 7–8." },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }
}
