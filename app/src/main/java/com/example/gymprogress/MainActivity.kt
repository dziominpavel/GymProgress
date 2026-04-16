package com.example.gymprogress

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.WorkoutRecommendation
import com.example.gymprogress.ui.navigation.AppNavigationScaffold
import com.example.gymprogress.ui.screens.AboutScreen
import com.example.gymprogress.ui.screens.ActiveWorkoutScreen
import com.example.gymprogress.ui.screens.AddEntryDialog
import com.example.gymprogress.ui.screens.ExerciseProgressChartScreen
import com.example.gymprogress.ui.screens.ExercisesScreen
import com.example.gymprogress.ui.screens.JournalScreen
import com.example.gymprogress.ui.screens.SettingsScreen
import com.example.gymprogress.ui.screens.StatsScreen
import com.example.gymprogress.ui.screens.TrainerScreen
import com.example.gymprogress.ui.screens.TrainerSettingsScreen
import com.example.gymprogress.ui.screens.WorkoutHistoryScreen
import com.example.gymprogress.ui.theme.GymProgressTheme
import com.example.gymprogress.viewmodel.WorkoutViewModel
import java.time.LocalDate

private data class AppOverlayState(
    val showProgressChart: Boolean = false,
    val showWorkoutHistory: Boolean = false,
    val showSettings: Boolean = false,
    val showAbout: Boolean = false,
    val showTrainerSettings: Boolean = false,
    val openedSettingsFromTrainer: Boolean = false,
    val showActiveWorkout: Boolean = false,
    val showTrainer: Boolean = false,
    val showAddDialog: Boolean = false,
)

private val AppOverlayStateSaver = Saver<MutableState<AppOverlayState>, List<Boolean>>(
    save = { state ->
        val s = state.value
        listOf(
            s.showProgressChart,
            s.showWorkoutHistory,
            s.showSettings,
            s.showAbout,
            s.showTrainerSettings,
            s.openedSettingsFromTrainer,
            s.showActiveWorkout,
            s.showTrainer,
            s.showAddDialog
        )
    },
    restore = {
        mutableStateOf(
            AppOverlayState(
                showProgressChart = it[0],
                showWorkoutHistory = it[1],
                showSettings = it[2],
                showAbout = it[3],
                showTrainerSettings = it[4],
                openedSettingsFromTrainer = it[5],
                showActiveWorkout = it[6],
                showTrainer = it[7],
                showAddDialog = it[8]
            )
        )
    }
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymProgressTheme {
                GymProgressApp()
            }
        }
    }
}

@Composable
fun GymProgressApp(viewModel: WorkoutViewModel = viewModel()) {
    // Navigation state: tab, modals, overlays
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.JOURNAL) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var overlay by rememberSaveable(saver = AppOverlayStateSaver) { mutableStateOf(AppOverlayState()) }
    var activeWorkoutRec by remember { mutableStateOf<WorkoutRecommendation?>(null) }
    var preselectedExerciseForAdd by rememberSaveable { mutableStateOf<String?>(null) }

    val entries by viewModel.allEntries.collectAsState()
    val todayEntries = remember(entries) {
        val today = FormatUtils.toStorageDate(LocalDate.now())
        entries.filter { it.date == today }
    }
    val previousSessionForJournal by viewModel.previousSessionForJournal.collectAsState()
    val previousSessionDateForJournal by viewModel.previousSessionDateForJournal.collectAsState()
    val previousSessionTitleOverride by viewModel.previousSessionTitleOverride.collectAsState()
    val journalSplitDayOptions by viewModel.journalSplitDayOptions.collectAsState()
    val journalSelectedDayIndex by viewModel.journalSelectedDayIndex.collectAsState()
    val previousSessionDayMuscleGroups by viewModel.previousSessionDayMuscleGroups.collectAsState()
    val selectedExercise by viewModel.selectedExercise.collectAsState()
    val entriesForExercise by viewModel.entriesForSelectedExercise.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()
    val trainingGoal by viewModel.trainingGoal.collectAsState()
    val bodyWeightKg by viewModel.bodyWeightKg.collectAsState()
    val scoringSystem by viewModel.scoringSystem.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val heightCm by viewModel.heightCm.collectAsState()
    val isAnthropometryComplete by viewModel.isAnthropometryComplete.collectAsState()
    val scoringEngine by viewModel.scoringEngine.collectAsState()
    val selectedExerciseType by viewModel.selectedExerciseType.collectAsState()
    val trainerSettings by viewModel.trainerSettings.collectAsState()
    val workoutRecommendation by viewModel.workoutRecommendation.collectAsState()
    val aiAdvice by viewModel.aiAdvice.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    LaunchedEffect(selectedExercise, overlay.showProgressChart) {
        if (overlay.showProgressChart && selectedExercise == null) {
            overlay = overlay.copy(showProgressChart = false)
        }
    }

    LaunchedEffect(overlay.showActiveWorkout, workoutRecommendation) {
        if (overlay.showActiveWorkout) {
            if (activeWorkoutRec == null && workoutRecommendation != null) {
                activeWorkoutRec = workoutRecommendation
            }
            if (activeWorkoutRec == null) {
                overlay = overlay.copy(showActiveWorkout = false)
            }
        }
    }

    // Full-screen overlays: early returns are safe here (composable body, not a lambda)
    if (overlay.showProgressChart) {
        BackHandler { overlay = overlay.copy(showProgressChart = false) }
        val chartExercise = selectedExercise
        if (chartExercise != null) {
            val chartExerciseMeta = allExercises.find { it.name == chartExercise }
            ExerciseProgressChartScreen(
                exerciseName = chartExercise,
                entries = entriesForExercise,
                scoringEngine = scoringEngine,
                scoringSystem = scoringSystem,
                trainingGoal = trainingGoal,
                exerciseType = selectedExerciseType,
                bodyWeightKg = bodyWeightKg,
                isBodyweightExercise = chartExerciseMeta?.isBodyweight == true,
                isAnthropometryIncompleteForBw = !isAnthropometryComplete,
                onBack = { overlay = overlay.copy(showProgressChart = false) },
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    if (overlay.showWorkoutHistory) {
        BackHandler { overlay = overlay.copy(showWorkoutHistory = false) }
        WorkoutHistoryScreen(
            entries = entries,
            exercises = allExercises,
            bodyWeightKg = bodyWeightKg,
            onDeleteEntry = { viewModel.deleteEntry(it) },
            onUpdateEntry = { viewModel.updateEntry(it) },
            onBack = { overlay = overlay.copy(showWorkoutHistory = false) },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (overlay.showSettings) {
        BackHandler { overlay = overlay.copy(showSettings = false) }
        SettingsScreen(
            currentGoal = trainingGoal,
            bodyWeightKg = bodyWeightKg,
            currentScoringSystem = scoringSystem,
            currentGender = gender,
            currentHeightCm = heightCm,
            isAnthropometryComplete = isAnthropometryComplete,
            onGoalChanged = { viewModel.setTrainingGoal(it) },
            onBodyWeightChanged = { viewModel.setBodyWeightKg(it) },
            onScoringSystemChanged = { viewModel.setScoringSystem(it) },
            onGenderChanged = { viewModel.setGender(it) },
            onHeightCmChanged = { viewModel.setHeightCm(it) },
            onBack = { overlay = overlay.copy(showSettings = false) },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (overlay.showAbout) {
        BackHandler { overlay = overlay.copy(showAbout = false) }
        AboutScreen(
            onBack = { overlay = overlay.copy(showAbout = false) },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (overlay.showTrainerSettings) {
        BackHandler {
            viewModel.updateTrainerSettings(trainerSettings)
            overlay = overlay.copy(
                showTrainerSettings = false,
                showTrainer = overlay.openedSettingsFromTrainer
            )
        }
        TrainerSettingsScreen(
            settings = trainerSettings,
            onSettingsChanged = { viewModel.updateTrainerSettings(it) },
            onBack = {
                overlay = overlay.copy(
                    showTrainerSettings = false,
                    showTrainer = overlay.openedSettingsFromTrainer
                )
            },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (overlay.showActiveWorkout && activeWorkoutRec != null) {
        BackHandler {
            overlay = overlay.copy(showActiveWorkout = false)
            activeWorkoutRec = null
        }
        ActiveWorkoutScreen(
            recommendation = activeWorkoutRec!!,
            onFinish = { completedSets ->
                viewModel.saveCompletedWorkout(completedSets)
                overlay = overlay.copy(showActiveWorkout = false)
                activeWorkoutRec = null
            },
            onCancel = {
                overlay = overlay.copy(showActiveWorkout = false)
                activeWorkoutRec = null
            },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (overlay.showTrainer) {
        BackHandler {
            viewModel.clearAiAdvice()
            overlay = overlay.copy(showTrainer = false)
        }
        TrainerScreen(
            recommendation = workoutRecommendation,
            onBack = {
                viewModel.clearAiAdvice()
                overlay = overlay.copy(showTrainer = false)
            },
            onOpenSettings = {
                overlay = overlay.copy(
                    openedSettingsFromTrainer = true,
                    showTrainer = false,
                    showTrainerSettings = true
                )
            },
            onStartWorkout = { rec ->
                activeWorkoutRec = rec
                overlay = overlay.copy(showTrainer = false, showActiveWorkout = true)
            },
            isAiAvailable = viewModel.isAiAvailable,
            aiAdvice = aiAdvice,
            aiLoading = aiLoading,
            onAskAi = { viewModel.askAi() },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    // Main navigation + snackbar for DB errors
    Box(modifier = Modifier.fillMaxSize()) {
        AppNavigationScaffold(
            currentDestination = currentDestination,
            onDestinationChange = { currentDestination = it },
            moreMenuExpanded = showMoreMenu,
            onMoreMenuDismiss = { showMoreMenu = false },
            onMoreMenuToggle = { showMoreMenu = !showMoreMenu },
            onOpenTrainerSettings = {
                overlay = overlay.copy(openedSettingsFromTrainer = false, showTrainerSettings = true)
            },
            onOpenHistory = { overlay = overlay.copy(showWorkoutHistory = true) },
            onOpenSettings = { overlay = overlay.copy(showSettings = true) },
            onOpenAbout = { overlay = overlay.copy(showAbout = true) },
            modifier = Modifier.fillMaxSize()
        ) { destination ->
            when (destination) {
                AppDestinations.JOURNAL -> JournalScreen(
                    entries = todayEntries,
                    exercises = allExercises,
                    bodyWeightKg = bodyWeightKg,
                    previousSession = previousSessionForJournal,
                    previousSessionDate = previousSessionDateForJournal,
                    previousSessionTitleOverride = previousSessionTitleOverride,
                    previousSessionDayMuscleGroups = previousSessionDayMuscleGroups,
                    splitDayOptions = journalSplitDayOptions,
                    selectedDayIndex = journalSelectedDayIndex,
                    onSelectDay = { viewModel.setJournalPreviousDay(it) },
                    workoutRecommendation = workoutRecommendation,
                    onAddClick = { overlay = overlay.copy(showAddDialog = true) },
                    onQuickAdd = { exerciseName ->
                        preselectedExerciseForAdd = exerciseName
                        overlay = overlay.copy(showAddDialog = true)
                    },
                    onOpenTrainer = { overlay = overlay.copy(showTrainer = true) },
                    onDeleteEntry = { viewModel.deleteEntry(it) },
                    onUpdateEntry = { viewModel.updateEntry(it) },
                    modifier = Modifier.fillMaxSize()
                )
                AppDestinations.EXERCISES -> ExercisesScreen(
                    exercises = allExercises,
                    onAddExercise = { name, group, type, isBodyweight ->
                        viewModel.addExercise(name, group, type, isBodyweight)
                    },
                    onDeleteExercise = { viewModel.deleteExercise(it) },
                    onUpdateExercise = { exercise, oldName -> viewModel.updateExercise(exercise, oldName) },
                    modifier = Modifier.fillMaxSize()
                )
                AppDestinations.STATS -> StatsScreen(
                    exercises = allExercises,
                    selectedExercise = selectedExercise,
                    entriesForExercise = entriesForExercise,
                    allEntries = entries,
                    onExerciseSelected = { viewModel.selectExercise(it) },
                    onOpenProgressChart = { overlay = overlay.copy(showProgressChart = true) },
                    trainingGoal = trainingGoal,
                    exerciseType = selectedExerciseType,
                    scoringEngine = scoringEngine,
                    scoringSystem = scoringSystem,
                    bodyWeightKg = bodyWeightKg,
                    isAnthropometryComplete = isAnthropometryComplete,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    val exerciseRecommendationForJournal by viewModel.exerciseRecommendationForJournal.collectAsState()

    if (overlay.showAddDialog) {
        AddEntryDialog(
            exercises = allExercises,
            history = entries,
            trainingGoal = trainingGoal,
            bodyWeightKg = bodyWeightKg,
            scoringEngine = scoringEngine,
            scoringSystem = scoringSystem,
            preselectedExercise = preselectedExerciseForAdd,
            exerciseRecommendation = exerciseRecommendationForJournal,
            onExerciseSelected = { catalogName, historyHint ->
                viewModel.loadExerciseRecommendationForJournal(catalogName, historyHint)
            },
            onDismiss = {
                overlay = overlay.copy(showAddDialog = false)
                preselectedExerciseForAdd = null
                viewModel.clearExerciseRecommendationForJournal()
            },
            onConfirm = { date, name, weight, reps ->
                viewModel.addEntry(date, name, weight, reps)
                overlay = overlay.copy(showAddDialog = false)
                preselectedExerciseForAdd = null
            }
        )
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    JOURNAL("Журнал", Icons.Default.DateRange),
    STATS("Прогресс", Icons.Default.Star),
    @Suppress("DEPRECATION")
    EXERCISES("Упражнения", Icons.Default.List),
}
