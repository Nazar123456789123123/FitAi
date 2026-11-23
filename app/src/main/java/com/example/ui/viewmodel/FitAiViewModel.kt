package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AiCoachInsight
import com.example.data.model.AthleteProfile
import com.example.data.model.DistanceUnit
import com.example.data.model.EquipmentTier
import com.example.data.model.MuscleGroup
import com.example.data.model.PrimaryGoal
import com.example.data.model.TrainingExperience
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutType
import com.example.data.model.deriveRirFromRpe
import com.example.data.repository.AiCoachRepository
import com.example.data.repository.FitnessRepository
import com.example.data.science.NaturalHypertrophyScience
import com.example.data.science.WeeklyScienceSummary
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiActionState {
    data object Idle : AiActionState
    data object Loading : AiActionState
    data class Streaming(val text: String) : AiActionState
    data class Success(val text: String) : AiActionState
    data class Error(val message: String) : AiActionState
}

data class FitAiDashboardData(
    val profile: AthleteProfile,
    val workouts: List<WorkoutLog>,
    val insights: List<AiCoachInsight>,
    val scienceSummary: WeeklyScienceSummary
)

data class LiveSessionState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Int = 0,
    val completedSets: Int = 0,
    val restRemainingSeconds: Int = 0,
    val restTotalSeconds: Int = 0,
    val modality: WorkoutType = WorkoutType.STRENGTH
)

class FitAiViewModel(
    private val repository: FitnessRepository,
    private val aiCoachRepository: AiCoachRepository,
    private val currentUserId: String
) : ViewModel() {

    val dashboardState: StateFlow<UiState<FitAiDashboardData>> = combine(
        repository.observeProfile(currentUserId),
        repository.observeWorkouts(currentUserId),
        repository.observeAiInsights(currentUserId)
    ) { profile, workouts, insights ->
        val summary = NaturalHypertrophyScience.analyzeLastSevenDays(workouts, profile)
        FitAiDashboardData(
            profile = profile,
            workouts = workouts,
            insights = insights,
            scienceSummary = summary
        )
    }
        .combine(MutableStateFlow(Unit)) { data, _ ->
            UiState.Success(data) as UiState<FitAiDashboardData>
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing dashboard streams", rootError)
            emit(UiState.Error(rootError.message ?: "Помилка завантаження даних із хмари."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _selectedFilter = MutableStateFlow<WorkoutType?>(null)
    val selectedFilter: StateFlow<WorkoutType?> = _selectedFilter.asStateFlow()

    private val _mutationStatus = MutableStateFlow<String?>(null)
    val mutationStatus: StateFlow<String?> = _mutationStatus.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _aiAnalysisState = MutableStateFlow<AiActionState>(AiActionState.Idle)
    val aiAnalysisState: StateFlow<AiActionState> = _aiAnalysisState.asStateFlow()

    private val _aiChatState = MutableStateFlow<AiActionState>(AiActionState.Idle)
    val aiChatState: StateFlow<AiActionState> = _aiChatState.asStateFlow()

    private val _liveSession = MutableStateFlow(LiveSessionState())
    val liveSession: StateFlow<LiveSessionState> = _liveSession.asStateFlow()

    private var timerJob: Job? = null

    fun setFilter(type: WorkoutType?) {
        _selectedFilter.value = type
    }

    fun clearMutationStatus() {
        _mutationStatus.value = null
    }

    fun startLiveSession(modality: WorkoutType = WorkoutType.STRENGTH) {
        _liveSession.value = LiveSessionState(
            isActive = true,
            isPaused = false,
            elapsedSeconds = 0,
            completedSets = 0,
            restRemainingSeconds = 0,
            restTotalSeconds = 0,
            modality = modality
        )
        ensureTimerRunning()
    }

    fun toggleLivePause() {
        _liveSession.update { it.copy(isPaused = !it.isPaused) }
    }

    fun logLiveSetAndStartRest(restSeconds: Int) {
        _liveSession.update {
            it.copy(
                completedSets = (it.completedSets + 1).coerceAtMost(60),
                restRemainingSeconds = restSeconds,
                restTotalSeconds = restSeconds
            )
        }
    }

    fun startRestCountdown(restSeconds: Int) {
        _liveSession.update {
            it.copy(
                restRemainingSeconds = restSeconds,
                restTotalSeconds = restSeconds
            )
        }
    }

    fun skipRestCountdown() {
        _liveSession.update { it.copy(restRemainingSeconds = 0, restTotalSeconds = 0) }
    }

    fun stopLiveSession() {
        timerJob?.cancel()
        timerJob = null
        _liveSession.value = LiveSessionState()
    }

    private fun ensureTimerRunning() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _liveSession.update { state ->
                    if (!state.isActive) return@update state
                    val nextElapsed = if (!state.isPaused) state.elapsedSeconds + 1 else state.elapsedSeconds
                    val nextRest = if (state.restRemainingSeconds > 0) state.restRemainingSeconds - 1 else 0
                    state.copy(elapsedSeconds = nextElapsed, restRemainingSeconds = nextRest)
                }
            }
        }
    }

    fun saveWorkout(
        editingWorkoutId: String?,
        type: WorkoutType,
        title: String,
        durationMinutes: Int,
        distanceValue: Double,
        distanceUnit: DistanceUnit,
        rpe: Int,
        muscleGroup: MuscleGroup,
        workingSets: Int,
        notes: String,
        performedAtMillis: Long = System.currentTimeMillis(),
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val derivedRir = if (type == WorkoutType.STRENGTH) deriveRirFromRpe(rpe) else 3
            val resolvedMuscle = if (type == WorkoutType.STRENGTH) {
                if (muscleGroup == MuscleGroup.NONE) MuscleGroup.FULL_BODY else muscleGroup
            } else {
                muscleGroup
            }
            val resolvedSets = if (type == WorkoutType.STRENGTH) workingSets.coerceIn(1, 60) else 0

            val result = if (editingWorkoutId.isNullOrBlank()) {
                repository.addWorkout(
                    type = type,
                    title = title,
                    durationMinutes = durationMinutes,
                    distanceValue = distanceValue,
                    distanceUnit = distanceUnit,
                    rpe = rpe,
                    muscleGroup = resolvedMuscle,
                    workingSets = resolvedSets,
                    rir = derivedRir,
                    notes = notes,
                    performedAtMillis = performedAtMillis
                )
            } else {
                repository.updateWorkout(
                    workoutId = editingWorkoutId,
                    type = type,
                    title = title,
                    durationMinutes = durationMinutes,
                    distanceValue = distanceValue,
                    distanceUnit = distanceUnit,
                    rpe = rpe,
                    muscleGroup = resolvedMuscle,
                    workingSets = resolvedSets,
                    rir = derivedRir,
                    notes = notes,
                    performedAtMillis = performedAtMillis
                )
            }
            _isSaving.value = false
            result.onSuccess {
                _mutationStatus.value = if (editingWorkoutId.isNullOrBlank()) {
                    "Тренування збережено в журналі"
                } else {
                    "Тренування оновлено"
                }
                onDone()
            }.onFailure { err ->
                _mutationStatus.value = err.localizedMessage ?: "Не вдалося зберегти тренування"
            }
        }
    }

    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            val result = repository.deleteWorkout(workoutId)
            result.onSuccess {
                _mutationStatus.value = "Запис видалено"
            }.onFailure { err ->
                _mutationStatus.value = err.localizedMessage ?: "Не вдалося видалити запис"
            }
        }
    }

    fun saveAthleteProfile(
        displayName: String,
        bodyWeightKg: Double,
        ageYears: Int,
        heightCm: Int,
        experience: TrainingExperience,
        goal: PrimaryGoal,
        equipment: EquipmentTier,
        focusMuscles: List<MuscleGroup>,
        weeklyTargetSessions: Int,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveProfile(
                displayName = displayName,
                bodyWeightKg = bodyWeightKg,
                ageYears = ageYears,
                heightCm = heightCm,
                experience = experience,
                goal = goal,
                equipment = equipment,
                focusMuscles = focusMuscles,
                weeklyTargetSessions = weeklyTargetSessions,
                onboardingDone = true
            )
            _isSaving.value = false
            result.onSuccess {
                _mutationStatus.value = "Профіль атлета збережено"
                onDone()
            }.onFailure { err ->
                _mutationStatus.value = err.localizedMessage ?: "Помилка оновлення профілю"
            }
        }
    }

    fun runWeeklyAiAnalysis() {
        val current = (dashboardState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _aiAnalysisState.value = AiActionState.Loading
            try {
                val insight = aiCoachRepository.analyzeTrainingWeek(
                    profile = current.profile,
                    workouts = current.workouts,
                    scienceSummary = current.scienceSummary
                )
                repository.saveAiInsight(
                    summary = insight.summary,
                    volumeStatus = insight.volumeStatus,
                    recoveryAdvice = insight.recoveryAdvice,
                    nextWorkoutPlan = insight.nextWorkoutPlan
                )
                _aiAnalysisState.value = AiActionState.Success(insight.summary)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI analysis timed out", e)
                _aiAnalysisState.value = AiActionState.Error("Перевищено час очікування AI (60 с). Спробуйте ще раз.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI analysis failed", e)
                _aiAnalysisState.value = AiActionState.Error(e.localizedMessage ?: "Помилка AI-аналізу.")
            }
        }
    }

    fun askAiCoach(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return
        val current = (dashboardState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _aiChatState.value = AiActionState.Loading
            var lastText = ""
            try {
                aiCoachRepository.streamCoachReply(
                    question = trimmed,
                    profile = current.profile,
                    workouts = current.workouts,
                    scienceSummary = current.scienceSummary
                ).collect { streamed ->
                    lastText = streamed
                    _aiChatState.value = AiActionState.Streaming(streamed)
                }
                if (lastText.isNotBlank()) {
                    _aiChatState.value = AiActionState.Success(lastText)
                } else {
                    _aiChatState.value = AiActionState.Error("Порожня відповідь від AI-тренера.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI chat timed out", e)
                _aiChatState.value = AiActionState.Error("Перевищено час очікування відповіді AI.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI chat failed", e)
                _aiChatState.value = AiActionState.Error(e.localizedMessage ?: "Не вдалося отримати відповідь AI.")
            }
        }
    }

    private companion object {
        const val TAG = "FitAiVM"
    }
}
