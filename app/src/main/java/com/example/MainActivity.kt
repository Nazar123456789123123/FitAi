package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.data.firebase.signOut
import com.example.data.model.DistanceUnit
import com.example.data.model.MuscleGroup
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutType
import com.example.data.repository.AiCoachRepository
import com.example.data.repository.FitnessRepository
import com.example.ui.components.ProfileSettingsSheet
import com.example.ui.screens.AiCoachScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.RecordWorkoutScreen
import com.example.ui.screens.ScienceScreen
import com.example.ui.theme.CanvasStone
import com.example.ui.theme.FitAiTheme
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkObsidian
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.VoltLime
import com.example.ui.theme.VoltLimeSoft
import com.example.ui.theme.ZoneHighCrimson
import com.example.ui.viewmodel.FitAiViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_OVERVIEW = "overview"
const val ROUTE_RECORD = "record_workout"
const val ROUTE_JOURNAL = "journal"
const val ROUTE_SCIENCE = "science"
const val ROUTE_COACH = "coach"

private data class NavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

private data class WorkoutFormPrefill(
    val type: WorkoutType = WorkoutType.STRENGTH,
    val title: String? = null,
    val durationMinutes: Int? = null,
    val distanceValue: Double? = null,
    val distanceUnit: DistanceUnit? = null,
    val rpe: Int? = null,
    val muscleGroup: MuscleGroup? = null,
    val workingSets: Int? = null,
    val notes: String? = null,
    val clearLiveTimerOnSave: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase ops
        configureAppCheck(this, intent)

        // STEP 2: Edge-to-Edge & Compose UI
        enableEdgeToEdge()
        setContent {
            FitAiTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            FitAiAuthenticatedRoot(currentUserId = user.uid)
        }
    }
}

@Composable
private fun FitAiAuthenticatedRoot(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: FitAiViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FitAiViewModel(
                    repository = FitnessRepository(db),
                    aiCoachRepository = AiCoachRepository(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val mutationStatus by viewModel.mutationStatus.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val aiAnalysisState by viewModel.aiAnalysisState.collectAsStateWithLifecycle()
    val aiChatState by viewModel.aiChatState.collectAsStateWithLifecycle()
    val liveSession by viewModel.liveSession.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(mutationStatus) {
        val msg = mutationStatus
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMutationStatus()
        }
    }

    var formSessionKey by remember { mutableIntStateOf(0) }
    var editingWorkout by remember { mutableStateOf<WorkoutLog?>(null) }
    var formPrefill by remember { mutableStateOf(WorkoutFormPrefill()) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var onboardingDismissedLocally by remember { mutableStateOf(false) }

    if (showProfileSheet) {
        BackHandler { showProfileSheet = false }
    }

    val tabs = listOf(
        NavTab(ROUTE_OVERVIEW, context.getString(R.string.nav_overview), Icons.Filled.Home, "tab_overview"),
        NavTab(ROUTE_RECORD, context.getString(R.string.nav_record), Icons.Filled.AddCircleOutline, "tab_record"),
        NavTab(ROUTE_JOURNAL, context.getString(R.string.nav_journal), Icons.AutoMirrored.Filled.ListAlt, "tab_journal"),
        NavTab(ROUTE_SCIENCE, context.getString(R.string.nav_science), Icons.Filled.Science, "tab_science"),
        NavTab(ROUTE_COACH, context.getString(R.string.nav_coach), Icons.Filled.AutoAwesome, "tab_coach")
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_OVERVIEW

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openRecordScreen(prefill: WorkoutFormPrefill, workoutToEdit: WorkoutLog? = null) {
        editingWorkout = workoutToEdit
        formPrefill = prefill
        formSessionKey++
        navigateToTab(ROUTE_RECORD)
    }

    fun openProgressiveRepeat(workout: WorkoutLog) {
        val type = workout.workoutTypeEnum()
        val nextSets = if (type == WorkoutType.STRENGTH) (workout.workingSets.toInt() + 1).coerceAtMost(40) else 0
        val nextDuration = if (type == WorkoutType.STRENGTH) workout.durationMinutes.toInt() else (workout.durationMinutes.toInt() + 5).coerceAtMost(600)
        openRecordScreen(
            WorkoutFormPrefill(
                type = type,
                title = workout.title,
                durationMinutes = nextDuration,
                distanceValue = workout.distanceValue,
                distanceUnit = workout.distanceUnitEnum(),
                rpe = workout.rpe.toInt(),
                muscleGroup = workout.muscleGroupEnum(),
                workingSets = nextSets,
                notes = "Прогресія від попередньої сесії"
            )
        )
    }

    val successData = (dashboardState as? UiState.Success)?.data
    val needsOnboarding = successData != null && !successData.profile.onboardingDone && !onboardingDismissedLocally

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = CanvasStone,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (!needsOnboarding && (currentRoute == ROUTE_OVERVIEW || currentRoute == ROUTE_JOURNAL)) {
                    FloatingActionButton(
                        onClick = { openRecordScreen(WorkoutFormPrefill(type = WorkoutType.STRENGTH)) },
                        containerColor = InkObsidian,
                        contentColor = VoltLime,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.testTag("fab_add_workout")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Записати тренування"
                        )
                    }
                }
            },
            bottomBar = {
                if (!needsOnboarding && !useRail) {
                    NavigationBar(
                        containerColor = SurfaceWhite,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (tab.route == ROUTE_RECORD) {
                                        openRecordScreen(WorkoutFormPrefill(type = WorkoutType.STRENGTH))
                                    } else {
                                        navigateToTab(tab.route)
                                    }
                                },
                                icon = {
                                    Icon(imageVector = tab.icon, contentDescription = tab.label)
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = InkObsidian,
                                    selectedTextColor = InkObsidian,
                                    indicatorColor = VoltLimeSoft,
                                    unselectedIconColor = InkMuted,
                                    unselectedTextColor = InkMuted
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!needsOnboarding && useRail) {
                    NavigationRail(
                        containerColor = SurfaceWhite,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (tab.route == ROUTE_RECORD) {
                                        openRecordScreen(WorkoutFormPrefill(type = WorkoutType.STRENGTH))
                                    } else {
                                        navigateToTab(tab.route)
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = InkObsidian,
                                    selectedTextColor = InkObsidian,
                                    indicatorColor = VoltLimeSoft,
                                    unselectedIconColor = InkMuted,
                                    unselectedTextColor = InkMuted
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (val state = dashboardState) {
                        is UiState.Loading -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = InkObsidian)
                                Text(
                                    text = "Завантаження тренувань…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                        }

                        is UiState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = state.message,
                                    color = ZoneHighCrimson,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Button(
                                    onClick = {
                                        signOut(credentialManager, scope)
                                    },
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text("Повторити вхід")
                                }
                            }
                        }

                        is UiState.Success -> {
                            val data = state.data
                            if (needsOnboarding) {
                                OnboardingScreen(
                                    initialProfile = data.profile,
                                    isSaving = isSaving,
                                    onComplete = { name, weight, age, height, exp, goal, eq, focus, weekly ->
                                        viewModel.saveAthleteProfile(
                                            displayName = name,
                                            bodyWeightKg = weight,
                                            ageYears = age,
                                            heightCm = height,
                                            experience = exp,
                                            goal = goal,
                                            equipment = eq,
                                            focusMuscles = focus,
                                            weeklyTargetSessions = weekly,
                                            onDone = { onboardingDismissedLocally = true }
                                        )
                                    },
                                    onSkip = {
                                        onboardingDismissedLocally = true
                                        viewModel.saveAthleteProfile(
                                            displayName = data.profile.displayName,
                                            bodyWeightKg = data.profile.bodyWeightKg,
                                            ageYears = data.profile.ageYears.toInt(),
                                            heightCm = data.profile.heightCm.toInt(),
                                            experience = data.profile.experienceEnum(),
                                            goal = data.profile.goalEnum(),
                                            equipment = data.profile.equipmentEnum(),
                                            focusMuscles = data.profile.focusMuscleEnums(),
                                            weeklyTargetSessions = data.profile.weeklyTargetSessions.toInt()
                                        )
                                    }
                                )
                            } else {
                                NavHost(
                                    navController = navController,
                                    startDestination = ROUTE_OVERVIEW
                                ) {
                                    composable(ROUTE_OVERVIEW) {
                                        OverviewScreen(
                                            data = data,
                                            liveSession = liveSession,
                                            onStartLiveSession = { viewModel.startLiveSession(it) },
                                            onToggleLivePause = { viewModel.toggleLivePause() },
                                            onLogLiveSetWithRest = { secs ->
                                                viewModel.logLiveSetAndStartRest(secs)
                                            },
                                            onSkipLiveRest = { viewModel.skipRestCountdown() },
                                            onCancelLiveSession = { viewModel.stopLiveSession() },
                                            onFinishLiveAndSave = {
                                                val elapsedMins = (liveSession.elapsedSeconds / 60).coerceAtLeast(1)
                                                val sets = liveSession.completedSets.coerceAtLeast(1)
                                                openRecordScreen(
                                                    WorkoutFormPrefill(
                                                        type = liveSession.modality,
                                                        title = "Живе тренування (${liveSession.modality.labelUk})",
                                                        durationMinutes = elapsedMins,
                                                        workingSets = sets,
                                                        rpe = 8,
                                                        clearLiveTimerOnSave = true
                                                    )
                                                )
                                            },
                                            onQuickAdd = { modality ->
                                                openRecordScreen(WorkoutFormPrefill(type = modality))
                                            },
                                            onEditWorkout = { w ->
                                                openRecordScreen(
                                                    prefill = WorkoutFormPrefill(type = w.workoutTypeEnum()),
                                                    workoutToEdit = w
                                                )
                                            },
                                            onDeleteWorkout = { id -> viewModel.deleteWorkout(id) },
                                            onRepeatProgressive = { w -> openProgressiveRepeat(w) },
                                            onStartDeload = {
                                                openRecordScreen(
                                                    WorkoutFormPrefill(
                                                        type = WorkoutType.STRENGTH,
                                                        title = "Deload-сесія (розвантаження ЦНС)",
                                                        durationMinutes = 35,
                                                        rpe = 6,
                                                        muscleGroup = MuscleGroup.FULL_BODY,
                                                        workingSets = 5,
                                                        notes = "Зниження обсягу на 40–50% із запасом 4 RIR для відновлення."
                                                    )
                                                )
                                            },
                                            onStartLaggingMuscle = { mg ->
                                                openRecordScreen(
                                                    WorkoutFormPrefill(
                                                        type = WorkoutType.STRENGTH,
                                                        title = "Фокус: ${mg.labelUk} (MAV)",
                                                        durationMinutes = 45,
                                                        rpe = 8,
                                                        muscleGroup = mg,
                                                        workingSets = 6,
                                                        notes = "Додатковий обсяг для виходу вище порогу MEV."
                                                    )
                                                )
                                            },
                                            onOpenProfile = { showProfileSheet = true },
                                            onOpenJournal = { navigateToTab(ROUTE_JOURNAL) }
                                        )
                                    }

                                    composable(ROUTE_RECORD) {
                                        RecordWorkoutScreen(
                                            sessionKey = formSessionKey,
                                            initialType = formPrefill.type,
                                            editingWorkout = editingWorkout,
                                            prefillTitle = formPrefill.title,
                                            prefillDurationMinutes = formPrefill.durationMinutes,
                                            prefillDistanceValue = formPrefill.distanceValue,
                                            prefillDistanceUnit = formPrefill.distanceUnit,
                                            prefillRpe = formPrefill.rpe,
                                            prefillMuscleGroup = formPrefill.muscleGroup,
                                            prefillWorkingSets = formPrefill.workingSets,
                                            prefillNotes = formPrefill.notes,
                                            isSaving = isSaving,
                                            onCancel = {
                                                editingWorkout = null
                                                formPrefill = WorkoutFormPrefill()
                                                navigateToTab(ROUTE_OVERVIEW)
                                            },
                                            onSave = { type, title, duration, dist, unit, rpe, mg, sets, notes, timestamp ->
                                                val clearTimer = formPrefill.clearLiveTimerOnSave
                                                viewModel.saveWorkout(
                                                    editingWorkoutId = editingWorkout?.id,
                                                    type = type,
                                                    title = title,
                                                    durationMinutes = duration,
                                                    distanceValue = dist,
                                                    distanceUnit = unit,
                                                    rpe = rpe,
                                                    muscleGroup = mg,
                                                    workingSets = sets,
                                                    notes = notes,
                                                    performedAtMillis = timestamp,
                                                    onDone = {
                                                        if (clearTimer) {
                                                            viewModel.stopLiveSession()
                                                        }
                                                        editingWorkout = null
                                                        formPrefill = WorkoutFormPrefill()
                                                        navigateToTab(ROUTE_JOURNAL)
                                                    }
                                                )
                                            }
                                        )
                                    }

                                    composable(ROUTE_JOURNAL) {
                                        JournalScreen(
                                            workouts = data.workouts,
                                            selectedFilter = selectedFilter,
                                            onSelectFilter = { viewModel.setFilter(it) },
                                            onAddWorkout = {
                                                openRecordScreen(
                                                    WorkoutFormPrefill(type = selectedFilter ?: WorkoutType.STRENGTH)
                                                )
                                            },
                                            onEditWorkout = { w ->
                                                openRecordScreen(
                                                    prefill = WorkoutFormPrefill(type = w.workoutTypeEnum()),
                                                    workoutToEdit = w
                                                )
                                            },
                                            onDeleteWorkout = { id -> viewModel.deleteWorkout(id) },
                                            onRepeatProgressive = { w -> openProgressiveRepeat(w) }
                                        )
                                    }

                                    composable(ROUTE_SCIENCE) {
                                        ScienceScreen(profile = data.profile)
                                    }

                                    composable(ROUTE_COACH) {
                                        AiCoachScreen(
                                            insights = data.insights,
                                            aiAnalysisState = aiAnalysisState,
                                            aiChatState = aiChatState,
                                            onRunAnalysis = { viewModel.runWeeklyAiAnalysis() },
                                            onAskCoach = { q -> viewModel.askAiCoach(q) }
                                        )
                                    }
                                }

                                if (showProfileSheet) {
                                    ProfileSettingsSheet(
                                        profile = data.profile,
                                        isSaving = isSaving,
                                        onDismiss = { showProfileSheet = false },
                                        onSave = { name, weight, age, height, exp, goal, eq, focus, weekly ->
                                            viewModel.saveAthleteProfile(
                                                displayName = name,
                                                bodyWeightKg = weight,
                                                ageYears = age,
                                                heightCm = height,
                                                experience = exp,
                                                goal = goal,
                                                equipment = eq,
                                                focusMuscles = focus,
                                                weeklyTargetSessions = weekly,
                                                onDone = { showProfileSheet = false }
                                            )
                                        },
                                        onSignOut = {
                                            showProfileSheet = false
                                            signOut(credentialManager, scope)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
