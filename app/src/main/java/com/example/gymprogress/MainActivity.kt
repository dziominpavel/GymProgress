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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymprogress.ui.navigation.AppNavigationScaffold
import com.example.gymprogress.ui.screens.AboutScreen
import com.example.gymprogress.ui.screens.AddEntryDialog
import com.example.gymprogress.ui.screens.ExercisesScreen
import com.example.gymprogress.ui.screens.JournalScreen
import com.example.gymprogress.ui.screens.SettingsScreen
import com.example.gymprogress.ui.screens.StatsScreen
import com.example.gymprogress.data.CompletedSet
import com.example.gymprogress.ui.screens.ActiveWorkoutScreen
import com.example.gymprogress.ui.screens.TrainerScreen
import com.example.gymprogress.ui.screens.TrainerSettingsScreen
import com.example.gymprogress.ui.screens.WorkoutHistoryScreen
import com.example.gymprogress.data.WorkoutRecommendation
import com.example.gymprogress.ui.theme.GymProgressTheme
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.viewmodel.WorkoutViewModel
import java.time.LocalDate

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
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showTrainer by rememberSaveable { mutableStateOf(false) }
    var showTrainerSettings by rememberSaveable { mutableStateOf(false) }
    var openedSettingsFromTrainer by rememberSaveable { mutableStateOf(false) }
    var showActiveWorkout by rememberSaveable { mutableStateOf(false) }
    var showWorkoutHistory by rememberSaveable { mutableStateOf(false) }
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

    // Resolve active workout state before branching
    if (showActiveWorkout && activeWorkoutRec == null && workoutRecommendation != null) {
        activeWorkoutRec = workoutRecommendation
    }
    if (showActiveWorkout && activeWorkoutRec == null) {
        showActiveWorkout = false
    }

    // Full-screen overlays: early returns are safe here (composable body, not a lambda)
    if (showWorkoutHistory) {
        BackHandler { showWorkoutHistory = false }
        WorkoutHistoryScreen(
            entries = entries,
            exercises = allExercises,
            bodyWeightKg = bodyWeightKg,
            onDeleteEntry = { viewModel.deleteEntry(it) },
            onUpdateEntry = { viewModel.updateEntry(it) },
            onBack = { showWorkoutHistory = false },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            currentGoal = trainingGoal,
            bodyWeightKg = bodyWeightKg,
            onGoalChanged = { viewModel.setTrainingGoal(it) },
            onBodyWeightChanged = { viewModel.setBodyWeightKg(it) },
            onBack = { showSettings = false },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (showAbout) {
        BackHandler { showAbout = false }
        AboutScreen(
            onBack = { showAbout = false },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (showTrainerSettings) {
        BackHandler {
            viewModel.updateTrainerSettings(trainerSettings)
            showTrainerSettings = false
            if (openedSettingsFromTrainer) showTrainer = true
        }
        TrainerSettingsScreen(
            settings = trainerSettings,
            onSettingsChanged = { viewModel.updateTrainerSettings(it) },
            onBack = {
                showTrainerSettings = false
                if (openedSettingsFromTrainer) showTrainer = true
            },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (showActiveWorkout && activeWorkoutRec != null) {
        BackHandler {
            showActiveWorkout = false
            activeWorkoutRec = null
        }
        ActiveWorkoutScreen(
            recommendation = activeWorkoutRec!!,
            onFinish = { completedSets ->
                viewModel.saveCompletedWorkout(completedSets)
                showActiveWorkout = false
                activeWorkoutRec = null
            },
            onCancel = {
                showActiveWorkout = false
                activeWorkoutRec = null
            },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (showTrainer) {
        BackHandler { showTrainer = false }
        TrainerScreen(
            recommendation = workoutRecommendation,
            onBack = { showTrainer = false },
            onOpenSettings = {
                openedSettingsFromTrainer = true
                showTrainer = false
                showTrainerSettings = true
            },
            onStartWorkout = { rec ->
                activeWorkoutRec = rec
                showTrainer = false
                showActiveWorkout = true
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
                openedSettingsFromTrainer = false
                showTrainerSettings = true
            },
            onOpenHistory = { showWorkoutHistory = true },
            onOpenSettings = { showSettings = true },
            onOpenAbout = { showAbout = true },
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
                    onAddClick = { showAddDialog = true },
                    onQuickAdd = { exerciseName ->
                        preselectedExerciseForAdd = exerciseName
                        showAddDialog = true
                    },
                    onOpenTrainer = { showTrainer = true },
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
                    onUpdateExercise = { viewModel.updateExercise(it) },
                    modifier = Modifier.fillMaxSize()
                )
                AppDestinations.STATS -> StatsScreen(
                    exercises = allExercises,
                    selectedExercise = selectedExercise,
                    entriesForExercise = entriesForExercise,
                    allEntries = entries,
                    onExerciseSelected = { viewModel.selectExercise(it) },
                    trainingGoal = trainingGoal,
                    exerciseType = selectedExerciseType,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showAddDialog) {
        AddEntryDialog(
            exercises = allExercises,
            history = entries,
            trainingGoal = trainingGoal,
            bodyWeightKg = bodyWeightKg,
            preselectedExercise = preselectedExerciseForAdd,
            onDismiss = {
                showAddDialog = false
                preselectedExerciseForAdd = null
            },
            onConfirm = { date, name, weight, reps ->
                viewModel.addEntry(date, name, weight, reps)
                showAddDialog = false
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
