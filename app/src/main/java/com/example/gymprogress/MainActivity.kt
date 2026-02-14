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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymprogress.ui.screens.AboutScreen
import com.example.gymprogress.ui.screens.AddEntryDialog
import com.example.gymprogress.ui.screens.ExercisesScreen
import com.example.gymprogress.ui.screens.JournalScreen
import com.example.gymprogress.ui.screens.SettingsScreen
import com.example.gymprogress.ui.screens.StatsScreen
import com.example.gymprogress.ui.screens.ActiveWorkoutScreen
import com.example.gymprogress.ui.screens.CompletedSet
import com.example.gymprogress.ui.screens.TrainerScreen
import com.example.gymprogress.ui.screens.TrainerSettingsScreen
import com.example.gymprogress.data.SetType
import com.example.gymprogress.data.WorkoutRecommendation
import com.example.gymprogress.ui.theme.GymProgressTheme
import com.example.gymprogress.viewmodel.WorkoutViewModel

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
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.JOURNAL) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showTrainer by rememberSaveable { mutableStateOf(false) }
    var showTrainerSettings by rememberSaveable { mutableStateOf(false) }
    var showActiveWorkout by rememberSaveable { mutableStateOf(false) }
    var activeWorkoutRec by remember { mutableStateOf<WorkoutRecommendation?>(null) }

    val entries by viewModel.allEntries.collectAsState()
    val exerciseNames by viewModel.exerciseNames.collectAsState()
    val selectedExercise by viewModel.selectedExercise.collectAsState()
    val entriesForExercise by viewModel.entriesForSelectedExercise.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()
    val trainingGoal by viewModel.trainingGoal.collectAsState()
    val selectedExerciseType by viewModel.selectedExerciseType.collectAsState()
    val trainerSettings by viewModel.trainerSettings.collectAsState()
    val workoutRecommendation by viewModel.workoutRecommendation.collectAsState()

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            currentGoal = trainingGoal,
            onGoalChanged = { viewModel.setTrainingGoal(it) },
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
        }
        TrainerSettingsScreen(
            settings = trainerSettings,
            onSettingsChanged = { viewModel.updateTrainerSettings(it) },
            onBack = { showTrainerSettings = false },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (showActiveWorkout && activeWorkoutRec == null && workoutRecommendation != null) {
        activeWorkoutRec = workoutRecommendation
    }
    if (showActiveWorkout && activeWorkoutRec == null) {
        showActiveWorkout = false
    }

    if (showActiveWorkout && activeWorkoutRec != null) {
        BackHandler {
            showActiveWorkout = false
            activeWorkoutRec = null
        }
        ActiveWorkoutScreen(
            recommendation = activeWorkoutRec!!,
            onFinish = { completedSets ->
                saveCompletedSets(completedSets, viewModel)
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
                showTrainer = false
                showTrainerSettings = true
            },
            onStartWorkout = { rec ->
                activeWorkoutRec = rec
                showTrainer = false
                showActiveWorkout = true
            },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = {
                        Text(
                            it.label,
                            fontWeight = if (it == currentDestination) FontWeight.Bold
                            else FontWeight.Normal
                        )
                    },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
            item(
                icon = {
                    Box {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Ещё"
                        )
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Тренер") },
                                onClick = {
                                    showMoreMenu = false
                                    showTrainer = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Настройки") },
                                onClick = {
                                    showMoreMenu = false
                                    showSettings = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("О приложении") },
                                onClick = {
                                    showMoreMenu = false
                                    showAbout = true
                                }
                            )
                        }
                    }
                },
                label = {
                    Text(
                        "Ещё",
                        fontWeight = FontWeight.Normal
                    )
                },
                selected = false,
                onClick = { showMoreMenu = !showMoreMenu }
            )
        },
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surface,
            navigationBarContentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        when (currentDestination) {
            AppDestinations.JOURNAL -> {
                JournalScreen(
                    entries = entries,
                    onAddClick = { showAddDialog = true },
                    onDeleteEntry = { viewModel.deleteEntry(it) },
                    onUpdateEntry = { viewModel.updateEntry(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppDestinations.EXERCISES -> {
                ExercisesScreen(
                    exercises = allExercises,
                    onAddExercise = { name, group, type -> viewModel.addExercise(name, group, type) },
                    onDeleteExercise = { viewModel.deleteExercise(it) },
                    onUpdateExercise = { viewModel.updateExercise(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppDestinations.STATS -> {
                StatsScreen(
                    exerciseNames = exerciseNames,
                    selectedExercise = selectedExercise,
                    entriesForExercise = entriesForExercise,
                    onExerciseSelected = { viewModel.selectExercise(it) },
                    trainingGoal = trainingGoal,
                    exerciseType = selectedExerciseType,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showAddDialog) {
        AddEntryDialog(
            exercises = allExercises,
            onDismiss = { showAddDialog = false },
            onConfirm = { date, name, weight, reps ->
                viewModel.addEntry(date, name, weight, reps)
                showAddDialog = false
            }
        )
    }
}

private fun saveCompletedSets(
    completedSets: List<CompletedSet>,
    viewModel: WorkoutViewModel
) {
    val today = java.time.LocalDate.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    val workingSets = completedSets.filter { it.setType == SetType.WORKING }
    val grouped = workingSets.groupBy { it.exerciseName }

    grouped.forEach { (name, sets) ->
        val distinctWeights = sets.map { it.weight }.distinct()
        if (distinctWeights.size == 1) {
            val reps = sets.joinToString(",") { it.reps.toString() }
            viewModel.addEntry(today, name, distinctWeights.first(), reps)
        } else {
            val mainWeight = sets.groupBy { it.weight }
                .maxByOrNull { it.value.size }?.key ?: sets.first().weight
            val mainSets = sets.filter { it.weight == mainWeight }
            val reps = mainSets.joinToString(",") { it.reps.toString() }
            viewModel.addEntry(today, name, mainWeight, reps)

            val otherGroups = sets.filter { it.weight != mainWeight }.groupBy { it.weight }
            otherGroups.forEach { (w, wSets) ->
                val otherReps = wSets.joinToString(",") { it.reps.toString() }
                viewModel.addEntry(today, name, w, otherReps)
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    JOURNAL("Журнал", Icons.Default.DateRange),
    @Suppress("DEPRECATION")
    EXERCISES("Упражнения", Icons.Default.List),
    STATS("Прогресс", Icons.Default.Star),
}
