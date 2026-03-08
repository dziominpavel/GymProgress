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
import com.example.gymprogress.data.ComparisonResult
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.ExerciseType
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.MuscleGroup
import com.example.gymprogress.data.TrainingGoal
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.data.WorkoutScoreCalculator
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.TextFieldShape
import com.example.gymprogress.ui.theme.Volt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    exercises: List<Exercise>,
    selectedExercise: String?,
    entriesForExercise: List<WorkoutEntry>,
    allEntries: List<WorkoutEntry> = emptyList(),
    onExerciseSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    trainingGoal: TrainingGoal = TrainingGoal.HYPERTROPHY,
    exerciseType: ExerciseType = ExerciseType.COMPOUND
) {
    var showHelp by remember { mutableStateOf(false) }
    var dayViewMode by remember { mutableStateOf(false) }
    var selectedMuscleGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    if (showHelp) ScoreFormulaHelpDialog(onDismiss = { showHelp = false })

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
            listOf("Упражнение" to false, "Тренировка" to true).forEach { (label, mode) ->
                val selected = dayViewMode == mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) Volt else Color.Transparent)
                        .clickable { dayViewMode = mode }
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
        } else if (!dayViewMode) {
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
                val maxWeight = entriesForExercise.maxOf { it.weight }

                val comparisons = entriesForExercise.mapIndexed { index, entry ->
                    val previous = if (index < entriesForExercise.size - 1)
                        entriesForExercise[index + 1] else null
                    val historyFromHere = entriesForExercise.drop(index)
                    entry to WorkoutScoreCalculator.compare(
                        entry, previous, historyFromHere, trainingGoal, exerciseType
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    StatCard(
                        emoji = "\uD83C\uDFC6",
                        title = "Макс. вес",
                        value = "${FormatUtils.formatWeight(maxWeight)} кг",
                        isHighlight = true,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        emoji = "\uD83C\uDFAF",
                        title = "Цель",
                        value = trainingGoal.displayName,
                        modifier = Modifier.weight(1f)
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
                            text = "${exerciseType.displayName} · ${trainingGoal.targetRange.first}–${trainingGoal.targetRange.last} повт.",
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
                            comparison = comparison
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
        } else {
            WorkoutDaySection(
                exercises = exercises,
                allEntries = allEntries,
                selectedMuscleGroup = selectedMuscleGroup,
                onMuscleGroupSelected = { selectedMuscleGroup = it },
                trainingGoal = trainingGoal
            )
        }
    }
    }
}
