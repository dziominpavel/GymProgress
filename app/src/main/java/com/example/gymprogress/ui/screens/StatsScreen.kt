package com.example.gymprogress.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.ExerciseType
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.MuscleGroup
import com.example.gymprogress.data.ScoringEngine
import com.example.gymprogress.data.ScoringSystem
import com.example.gymprogress.data.SimplifiedScoreCalculator
import com.example.gymprogress.data.TrainingGoal
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.TextFieldShape
import com.example.gymprogress.ui.theme.Volt
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    exercises: List<Exercise>,
    selectedExercise: String?,
    entriesForExercise: List<WorkoutEntry>,
    allEntries: List<WorkoutEntry> = emptyList(),
    onExerciseSelected: (String?) -> Unit,
    onOpenProgressChart: () -> Unit = {},
    modifier: Modifier = Modifier,
    trainingGoal: TrainingGoal = TrainingGoal.HYPERTROPHY,
    exerciseType: ExerciseType = ExerciseType.COMPOUND,
    scoringEngine: ScoringEngine = SimplifiedScoreCalculator,
    scoringSystem: ScoringSystem = ScoringSystem.SIMPLIFIED,
    bodyWeightKg: Double? = null,
    isAnthropometryComplete: Boolean = true
) {
    var showHelp by remember { mutableStateOf(false) }
    var statsGroupMode by remember { mutableStateOf(0) } // 0 = Упражнение, 1 = Тренировка, 2 = Дата
    var selectedMuscleGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    val isSimplified = scoringSystem == ScoringSystem.SIMPLIFIED
    val selectedEx = selectedExercise?.let { name -> exercises.find { it.name == name } }
    val isBodyweightExercise = selectedEx?.isBodyweight ?: false

    if (showHelp) {
        if (isSimplified) SimplifiedHelpDialog(onDismiss = { showHelp = false })
        else ScoreFormulaHelpDialog(onDismiss = { showHelp = false })
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = Spacing.md)
    ) {
        Spacer(modifier = Modifier.height(Spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ПРОГРЕСС",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showHelp = true }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Как считается оценка",
                    tint = Volt,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Volt)
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "Отслеживайте свои результаты",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(2.dp)
        ) {
            listOf("Упражнение" to 0, "Тренировка" to 1, "Дата" to 2).forEach { (label, mode) ->
                val selected = statsGroupMode == mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) Volt else Color.Transparent)
                        .clickable { statsGroupMode = mode }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        if (exercises.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = Spacing.xxl)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("\uD83D\uDCCA", style = MaterialTheme.typography.displaySmall)
                    }
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    Text(
                        "Добавьте записи в журнал,\nчтобы отслеживать прогресс",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (statsGroupMode == 0) {
            var expanded by remember { mutableStateOf(false) }
            var expandedGroup by remember { mutableStateOf<String?>(null) }
            val groupedExercises = remember(exercises) {
                exercises
                    .groupBy { it.muscleGroup }
                    .map { (groupKey, groupExercises) ->
                        val displayName = MuscleGroup.entries
                            .find { it.name == groupKey }?.displayName ?: groupKey
                        displayName to groupExercises.sortedBy { it.name }
                    }
                    .sortedBy { it.first }
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedExercise ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Выберите упражнение") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    shape = TextFieldShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    groupedExercises.forEach { (displayName, groupExercises) ->
                        val groupKey = groupExercises.firstOrNull()?.muscleGroup ?: displayName
                        val isExpanded = expandedGroup == groupKey

                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Volt
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Volt
                                    )
                                }
                            },
                            onClick = {
                                expandedGroup = if (isExpanded) null else groupKey
                            },
                            modifier = Modifier.background(
                                if (isExpanded) Volt.copy(alpha = 0.08f)
                                else Color.Transparent
                            )
                        )

                        if (isExpanded) {
                            groupExercises.forEach { exercise ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "•  ${exercise.name}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = {
                                        onExerciseSelected(exercise.name)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedExercise != null && entriesForExercise.isNotEmpty()) {
                // Список: сверху первые по дате (старые), ниже — следующие (консистентно по проекту)
                val listOldestFirst = remember(entriesForExercise) {
                    entriesForExercise.sortedWith(compareBy({ it.date }, { it.id }))
                }
                val maxWeight = listOldestFirst.maxOf { it.weight }

                val comparisons = listOldestFirst.mapIndexed { index, entry ->
                    val previous = listOldestFirst.getOrNull(index - 1)
                    val historyFromHere = listOldestFirst.take(index + 1).reversed()
                    entry to scoringEngine.compare(
                        entry, previous, historyFromHere, trainingGoal, exerciseType,
                        bodyWeightKg, isBodyweightExercise
                    )
                }

                val bestE1RM = if (isSimplified) {
                    listOldestFirst.maxOfOrNull {
                        SimplifiedScoreCalculator.calcE1RMForEntry(it, bodyWeightKg, isBodyweightExercise)
                    } ?: 0.0
                } else 0.0

                if (isSimplified && !isAnthropometryComplete && isBodyweightExercise) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(Spacing.sm)
                    ) {
                        Text(
                            "Укажите вес тела в настройках для корректной оценки упражнений с собственным весом",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    if (isSimplified && bestE1RM > 0) {
                        StatCard(
                            emoji = "\uD83D\uDCAA",
                            title = "Оценочный 1RM",
                            value = "${FormatUtils.formatTwoDecimals(bestE1RM)} кг",
                            isHighlight = true,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        StatCard(
                            emoji = "\uD83C\uDFC6",
                            title = "Макс. вес",
                            value = "${FormatUtils.formatTwoDecimals(maxWeight)} кг",
                            isHighlight = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    StatCard(
                        emoji = "\uD83C\uDFAF",
                        title = if (isSimplified) "Система" else "Цель",
                        value = if (isSimplified) "1RM" else trainingGoal.displayName,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))
                OutlinedButton(
                    onClick = onOpenProgressChart,
                    modifier = Modifier.fillMaxWidth(),
                    shape = TextFieldShape,
                ) {
                    Text(
                        text = "График",
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xl))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(4.dp, 24.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Volt)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "История",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Spacing.xxs))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = Spacing.xs, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isSimplified) "1RM · Epley"
                                   else "${exerciseType.displayName} · ${trainingGoal.targetRange.first}–${trainingGoal.targetRange.last} повт.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(comparisons, key = { it.first.id }) { (entry, comparison) ->
                        HistoryRow(
                            entry = entry,
                            maxWeight = maxWeight,
                            comparison = comparison,
                            isSimplified = isSimplified
                        )
                    }
                    item { Spacer(modifier = Modifier.height(Spacing.md)) }
                }
            } else if (selectedExercise != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Нет данных для этого упражнения",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (statsGroupMode == 1) {
            WorkoutDaySection(
                exercises = exercises,
                allEntries = allEntries,
                selectedMuscleGroup = selectedMuscleGroup,
                onMuscleGroupSelected = { selectedMuscleGroup = it },
                trainingGoal = trainingGoal,
                scoringEngine = scoringEngine,
                bodyWeightKg = bodyWeightKg,
                isSimplified = isSimplified
            )
        } else {
            // Режим «Дата»: один общий скролл — календарь, блок «Итого», затем список записей (сверху первые по дате/id)
            val workoutDates = remember(allEntries) {
                allEntries.mapNotNull { FormatUtils.parseStorageDate(it.date) }.toSet()
            }
            val storageDate = selectedDate?.let { FormatUtils.toStorageDate(it) }
            val entriesForSelectedDate = remember(allEntries, storageDate) {
                if (storageDate == null) emptyList()
                else allEntries.filter { it.date == storageDate }.sortedBy { it.id }
            }
            val dateReport = remember(storageDate, allEntries, exercises, trainingGoal, scoringEngine) {
                if (storageDate != null)
                    scoringEngine.compareSessionByDate(
                        selectedDateStorage = storageDate,
                        allExercises = exercises,
                        allEntries = allEntries,
                        goal = trainingGoal,
                        bodyWeightKg = bodyWeightKg
                    )
                else null
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                item {
                    WorkoutCalendar(
                        month = displayedMonth,
                        workoutDates = workoutDates,
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        onPreviousMonth = { displayedMonth = displayedMonth.minusMonths(1) },
                        onNextMonth = { displayedMonth = displayedMonth.plusMonths(1) }
                    )
                }
                item { Spacer(modifier = Modifier.height(Spacing.md)) }
                when {
                    selectedDate == null -> { }
                    entriesForSelectedDate.isEmpty() -> item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.xl),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Нет записей за эту дату",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        dateReport?.let { report -> item { WorkoutDayReportView(report, showOverallCard = false, isSimplified = isSimplified) } }
                    }
                }
                item { Spacer(modifier = Modifier.height(Spacing.md)) }
            }
        }
    }
    }
}
