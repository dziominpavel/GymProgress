package com.example.gymprogress

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
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
import com.example.gymprogress.ui.screens.AddEntryDialog
import com.example.gymprogress.ui.screens.ExercisesScreen
import com.example.gymprogress.ui.screens.JournalScreen
import com.example.gymprogress.ui.screens.StatsScreen
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
    var showAddDialog by remember { mutableStateOf(false) }

    val entries by viewModel.allEntries.collectAsState()
    val exerciseNames by viewModel.exerciseNames.collectAsState()
    val selectedExercise by viewModel.selectedExercise.collectAsState()
    val entriesForExercise by viewModel.entriesForSelectedExercise.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()

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
                    onAddExercise = { name, group -> viewModel.addExercise(name, group) },
                    onDeleteExercise = { viewModel.deleteExercise(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppDestinations.STATS -> {
                StatsScreen(
                    exerciseNames = exerciseNames,
                    selectedExercise = selectedExercise,
                    entriesForExercise = entriesForExercise,
                    onExerciseSelected = { viewModel.selectExercise(it) },
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

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    JOURNAL("Журнал", Icons.Default.DateRange),
    EXERCISES("Упражнения", Icons.Default.List),
    STATS("Прогресс", Icons.Default.Star),
}