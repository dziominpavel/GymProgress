package com.example.gymprogress

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.SimplifiedScoreCalculator
import com.example.gymprogress.data.WorkoutRecommendation
import com.example.gymprogress.ui.navigation.AppNavigationScaffold
import com.example.gymprogress.ui.navigation.AppOverlay
import com.example.gymprogress.ui.navigation.deserializeOverlay
import com.example.gymprogress.ui.navigation.serializeOverlay
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

/**
 * Saver для [SnapshotStateList] из [AppOverlay]. Сохраняет стек как список строк.
 */
private val OverlayStackSaver = listSaver<SnapshotStateList<AppOverlay>, String>(
    save = { stack -> stack.map { serializeOverlay(it) } },
    restore = { saved -> saved.mapNotNull { deserializeOverlay(it) }.toMutableStateList() }
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val workoutViewModel: WorkoutViewModel = viewModel()
            GymProgressTheme {
                GymProgressApp(workoutViewModel)
            }
        }
    }
}

@Composable
fun GymProgressApp(viewModel: WorkoutViewModel = viewModel()) {
    // Корневое состояние навигации
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.JOURNAL) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val overlayStack: SnapshotStateList<AppOverlay> = rememberSaveable(saver = OverlayStackSaver) {
        mutableStateListOf()
    }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var activeWorkoutRec by remember { mutableStateOf<WorkoutRecommendation?>(null) }
    var preselectedExerciseForAdd by rememberSaveable { mutableStateOf<String?>(null) }

    val topOverlay = overlayStack.lastOrNull()

    fun pushOverlay(overlay: AppOverlay) {
        if (overlayStack.lastOrNull() != overlay) overlayStack.add(overlay)
    }

    fun popOverlay() {
        if (overlayStack.isNotEmpty()) overlayStack.removeAt(overlayStack.lastIndex)
    }

    fun closeAllOverlays() {
        overlayStack.clear()
    }

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
    val chartRange by viewModel.chartRange.collectAsState()
    val chartMetric by viewModel.chartMetric.collectAsState()
    val isAnthropometryComplete by viewModel.isAnthropometryComplete.collectAsState()
    val scoringEngine by viewModel.scoringEngine.collectAsState()
    val selectedExerciseType by viewModel.selectedExerciseType.collectAsState()
    val trainerSettings by viewModel.trainerSettings.collectAsState()
    val workoutRecommendation by viewModel.workoutRecommendation.collectAsState()
    val aiAdvice by viewModel.aiAdvice.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // PR-карта: id записи с максимальным оценочным 1RM в рамках упражнения.
    // Используется в Журнале и Истории для отрисовки бейджа.
    val personalRecordEntryIds = remember(entries, allExercises, bodyWeightKg) {
        val byExerciseKey = entries.groupBy { FormatUtils.normalizeExerciseNameKey(it.exerciseName) }
        byExerciseKey.mapNotNull { (_, exerciseEntries) ->
            val first = exerciseEntries.firstOrNull() ?: return@mapNotNull null
            val ex = allExercises.find { it.name == first.exerciseName }
            val isBw = ex?.isBodyweight == true
            val best = exerciseEntries.maxByOrNull {
                SimplifiedScoreCalculator.calcE1RMForEntry(it, bodyWeightKg, isBw)
            } ?: return@mapNotNull null
            val bestE1RM = SimplifiedScoreCalculator.calcE1RMForEntry(best, bodyWeightKg, isBw)
            if (bestE1RM > 0) best.id else null
        }.toSet()
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    // Если выбранное упражнение пропало, закрываем график автоматически.
    LaunchedEffect(selectedExercise, topOverlay) {
        if (topOverlay == AppOverlay.ProgressChart && selectedExercise == null) {
            popOverlay()
        }
    }

    // Подхватываем рекомендацию для активной тренировки. Если рекомендации нет — закрываем экран.
    LaunchedEffect(topOverlay, workoutRecommendation) {
        if (topOverlay == AppOverlay.ActiveWorkout) {
            if (activeWorkoutRec == null && workoutRecommendation != null) {
                activeWorkoutRec = workoutRecommendation
            }
            if (activeWorkoutRec == null) popOverlay()
        }
    }

    // Единый системный back: пока есть оверлеи — снимаем их со стека.
    BackHandler(enabled = overlayStack.isNotEmpty()) {
        when (topOverlay) {
            AppOverlay.TrainerSettings -> {
                viewModel.updateTrainerSettings(trainerSettings)
                popOverlay()
            }
            AppOverlay.ActiveWorkout -> {
                activeWorkoutRec = null
                popOverlay()
            }
            AppOverlay.Trainer -> {
                viewModel.clearAiAdvice()
                popOverlay()
            }
            else -> popOverlay()
        }
    }

    val exerciseRecommendationForJournal by viewModel.exerciseRecommendationForJournal.collectAsState()

    // Основной экран навигации + snackbar
    Box(modifier = Modifier.fillMaxSize()) {
        AppNavigationScaffold(
            currentDestination = currentDestination,
            onDestinationChange = { currentDestination = it },
            moreMenuExpanded = showMoreMenu,
            onMoreMenuDismiss = { showMoreMenu = false },
            onMoreMenuToggle = { showMoreMenu = !showMoreMenu },
            onOpenTrainerSettings = { pushOverlay(AppOverlay.TrainerSettings) },
            onOpenHistory = { pushOverlay(AppOverlay.WorkoutHistory) },
            onOpenSettings = { pushOverlay(AppOverlay.Settings) },
            onOpenAbout = { pushOverlay(AppOverlay.About) },
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
                    personalRecordEntryIds = personalRecordEntryIds,
                    onAddClick = { showAddDialog = true },
                    onQuickAdd = { exerciseName ->
                        preselectedExerciseForAdd = exerciseName
                        showAddDialog = true
                    },
                    onOpenTrainer = { pushOverlay(AppOverlay.Trainer) },
                    onDeleteEntry = { viewModel.deleteEntry(it) },
                    onUpdateEntry = { viewModel.updateEntry(it) },
                    onRepeatEntry = { entry ->
                        viewModel.addEntry(
                            FormatUtils.toStorageDate(LocalDate.now()),
                            entry.exerciseName,
                            entry.weight,
                            entry.reps
                        )
                    },
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
                    onOpenProgressChart = { pushOverlay(AppOverlay.ProgressChart) },
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

        // Определяем направление перехода: push (стек растёт) → slide справа,
        // pop (стек уменьшается) → slide влево/вправо в обратную сторону.
        val stackSize = overlayStack.size
        var prevStackSize by remember { mutableIntStateOf(stackSize) }
        val isPushing = stackSize >= prevStackSize
        SideEffect { prevStackSize = stackSize }

        // Полноэкранные оверлеи рисуются поверх корня c анимацией перехода.
        AnimatedContent(
            targetState = topOverlay,
            label = "overlay",
            transitionSpec = {
                if (initialState == null || targetState == null) {
                    // Появление поверх корня / исчезновение в корень
                    val slideIn = slideInHorizontally(tween(220)) { full -> full } + fadeIn(tween(220))
                    val slideOut = slideOutHorizontally(tween(220)) { full -> full } + fadeOut(tween(220))
                    slideIn togetherWith slideOut
                } else if (isPushing) {
                    // Push: новый приходит справа, старый уезжает влево
                    (slideInHorizontally(tween(220)) { full -> full } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(220)) { full -> -full / 4 } + fadeOut(tween(220)))
                } else {
                    // Pop: новый приходит слева (или появляется уже на месте), старый уезжает вправо
                    (slideInHorizontally(tween(220)) { full -> -full / 4 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(220)) { full -> full } + fadeOut(tween(220)))
                }
            }
        ) { state ->
            when (state) {
            AppOverlay.ProgressChart -> {
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
                        chartRange = chartRange,
                        chartMetric = chartMetric,
                        onChartRangeChange = { viewModel.setChartRange(it) },
                        onChartMetricChange = { viewModel.setChartMetric(it) },
                        onBack = { popOverlay() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            AppOverlay.WorkoutHistory -> WorkoutHistoryScreen(
                entries = entries,
                exercises = allExercises,
                bodyWeightKg = bodyWeightKg,
                personalRecordEntryIds = personalRecordEntryIds,
                onDeleteEntry = { viewModel.deleteEntry(it) },
                onUpdateEntry = { viewModel.updateEntry(it) },
                onBack = { popOverlay() },
                modifier = Modifier.fillMaxSize()
            )
            AppOverlay.Settings -> SettingsScreen(
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
                onBack = { popOverlay() },
                modifier = Modifier.fillMaxSize()
            )
            AppOverlay.About -> AboutScreen(
                onBack = { popOverlay() },
                modifier = Modifier.fillMaxSize()
            )
            AppOverlay.TrainerSettings -> TrainerSettingsScreen(
                settings = trainerSettings,
                onSettingsChanged = { viewModel.updateTrainerSettings(it) },
                onBack = { popOverlay() },
                modifier = Modifier.fillMaxSize()
            )
            AppOverlay.ActiveWorkout -> {
                val rec = activeWorkoutRec
                if (rec != null) {
                    ActiveWorkoutScreen(
                        recommendation = rec,
                        onFinish = { completedSets ->
                            viewModel.saveCompletedWorkout(completedSets)
                            activeWorkoutRec = null
                            // Завершение тренировки целиком закрывает её и подложку (Trainer).
                            closeAllOverlays()
                        },
                        onCancel = {
                            activeWorkoutRec = null
                            popOverlay()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            AppOverlay.Trainer -> TrainerScreen(
                recommendation = workoutRecommendation,
                onBack = {
                    viewModel.clearAiAdvice()
                    popOverlay()
                },
                onOpenSettings = { pushOverlay(AppOverlay.TrainerSettings) },
                onStartWorkout = { rec ->
                    activeWorkoutRec = rec
                    pushOverlay(AppOverlay.ActiveWorkout)
                },
                isAiAvailable = viewModel.isAiAvailable,
                aiAdvice = aiAdvice,
                aiLoading = aiLoading,
                onAskAi = { viewModel.askAi() },
                modifier = Modifier.fillMaxSize()
            )
            null -> Unit
            }
        }
    }

    if (showAddDialog) {
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
                showAddDialog = false
                preselectedExerciseForAdd = null
                viewModel.clearExerciseRecommendationForJournal()
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
    STATS("Прогресс", Icons.AutoMirrored.Filled.TrendingUp),
    EXERCISES("Упражнения", Icons.Default.FitnessCenter),
}
