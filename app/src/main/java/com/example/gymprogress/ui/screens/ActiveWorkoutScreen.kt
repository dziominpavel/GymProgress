package com.example.gymprogress.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymprogress.data.ExerciseRecommendation
import com.example.gymprogress.data.MuscleGroup
import com.example.gymprogress.data.SetType
import com.example.gymprogress.data.WorkoutRecommendation
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.ui.theme.CardShape
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.Volt
import kotlinx.coroutines.delay

data class CompletedSet(
    val exerciseName: String,
    val weight: Double,
    val reps: Int,
    val setType: SetType
)

@Composable
fun ActiveWorkoutScreen(
    recommendation: WorkoutRecommendation,
    onFinish: (List<CompletedSet>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var currentSetIndex by remember { mutableIntStateOf(0) }
    var isResting by remember { mutableStateOf(false) }
    var restTimeLeft by remember { mutableIntStateOf(0) }
    var workoutStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var elapsedMinutes by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            elapsedMinutes = (System.currentTimeMillis() - workoutStartTime) / 1000 / 60
            delay(30_000L)
        }
    }

    val completedSets = remember { mutableStateListOf<CompletedSet>() }

    val exercises = recommendation.exercises
    val isWorkoutFinished = currentExerciseIndex >= exercises.size

    val currentExercise = if (!isWorkoutFinished) exercises[currentExerciseIndex] else null
    val currentSet = if (currentExercise != null && currentSetIndex < currentExercise.sets.size)
        currentExercise.sets[currentSetIndex] else null

    var actualWeight by remember(currentExerciseIndex, currentSetIndex) {
        mutableStateOf(currentSet?.weight?.toString() ?: "")
    }
    var actualReps by remember(currentExerciseIndex, currentSetIndex) {
        mutableStateOf(currentSet?.targetReps?.first?.toString() ?: "")
    }

    LaunchedEffect(isResting, restTimeLeft) {
        if (isResting && restTimeLeft > 0) {
            delay(1000L)
            restTimeLeft -= 1
            if (restTimeLeft <= 0) {
                isResting = false
            }
        }
    }

    val totalSets = exercises.sumOf { it.sets.size }
    val doneSets = exercises.take(currentExerciseIndex).sumOf { it.sets.size } + currentSetIndex
    val progress = if (totalSets > 0) doneSets.toFloat() / totalSets else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

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

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Завершить",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.xs))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ТРЕНИРОВКА",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = recommendation.dayLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$elapsedMinutes мин",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xs))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Volt)
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Volt,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = "$doneSets / $totalSets подходов",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            if (isWorkoutFinished) {
                // Workout complete summary
                WorkoutSummary(
                    completedSets = completedSets,
                    exercises = exercises,
                    startTime = workoutStartTime,
                    onFinish = { onFinish(completedSets.toList()) }
                )
            } else if (isResting) {
                // Rest timer
                RestTimer(
                    secondsLeft = restTimeLeft,
                    totalSeconds = currentSet?.restSeconds ?: 60,
                    nextExerciseName = currentExercise?.exercise?.name ?: "",
                    nextSetIndex = currentSetIndex + 1,
                    totalSetsForExercise = currentExercise?.sets?.size ?: 0,
                    onSkip = {
                        isResting = false
                        restTimeLeft = 0
                    }
                )
            } else if (currentExercise != null && currentSet != null) {
                // Current set input
                CurrentSetView(
                    exerciseRec = currentExercise,
                    setIndex = currentSetIndex,
                    suggestedWeight = currentSet.weight,
                    suggestedReps = currentSet.targetReps,
                    setType = currentSet.type,
                    actualWeight = actualWeight,
                    actualReps = actualReps,
                    onWeightChange = { actualWeight = it },
                    onRepsChange = { actualReps = it },
                    onComplete = {
                        val w = actualWeight.toDoubleOrNull() ?: 0.0
                        val r = actualReps.toIntOrNull() ?: 0

                        completedSets.add(
                            CompletedSet(
                                exerciseName = currentExercise.exercise.name,
                                weight = w,
                                reps = r,
                                setType = currentSet.type
                            )
                        )

                        val nextSetIdx = currentSetIndex + 1
                        if (nextSetIdx < currentExercise.sets.size) {
                            restTimeLeft = currentSet.restSeconds
                            isResting = true
                            currentSetIndex = nextSetIdx
                        } else {
                            val nextExIdx = currentExerciseIndex + 1
                            if (nextExIdx < exercises.size) {
                                restTimeLeft = currentSet.restSeconds
                                isResting = true
                                currentExerciseIndex = nextExIdx
                                currentSetIndex = 0
                            } else {
                                currentExerciseIndex = nextExIdx
                            }
                        }
                    },
                    remainingExercises = exercises.drop(currentExerciseIndex + 1)
                )
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Завершить тренировку?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (completedSets.isEmpty()) "Выполненные подходы не будут сохранены."
                    else "Сохранить ${completedSets.size} выполненных подходов?"
                )
            },
            confirmButton = {
                if (completedSets.isNotEmpty()) {
                    TextButton(onClick = {
                        showCancelDialog = false
                        onFinish(completedSets.toList())
                    }) {
                        Text("Сохранить и выйти", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    onCancel()
                }) {
                    Text(
                        if (completedSets.isEmpty()) "Выйти" else "Выйти без сохранения",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }
}

@Composable
private fun CurrentSetView(
    exerciseRec: ExerciseRecommendation,
    setIndex: Int,
    suggestedWeight: Double?,
    suggestedReps: IntRange,
    setType: SetType,
    actualWeight: String,
    actualReps: String,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onComplete: () -> Unit,
    remainingExercises: List<ExerciseRecommendation>
) {
    val groupName = MuscleGroup.entries
        .find { it.name == exerciseRec.exercise.muscleGroup }?.displayName
        ?: exerciseRec.exercise.muscleGroup

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Volt.copy(alpha = 0.1f)
                ),
                shape = CardShape
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Volt)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = exerciseRec.exercise.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = Spacing.xs, vertical = 2.dp)
                        ) {
                            Text(
                                text = groupName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (setType == SetType.WARMUP) MaterialTheme.colorScheme.surfaceVariant
                                    else Volt.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = Spacing.xs, vertical = 2.dp)
                        ) {
                            Text(
                                text = setType.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (setType == SetType.WARMUP) MaterialTheme.colorScheme.onSurfaceVariant
                                else Volt
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.md))

                    Text(
                        text = "Подход ${setIndex + 1} из ${exerciseRec.sets.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (suggestedWeight != null) {
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(
                            text = "Цель: ${FormatUtils.formatWeightPrecise(suggestedWeight)} кг × ${suggestedReps.first}–${suggestedReps.last} повт.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    exerciseRec.note?.let { note ->
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Volt
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = actualWeight,
                            onValueChange = onWeightChange,
                            label = { Text("Вес (кг)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = actualReps,
                            onValueChange = onRepsChange,
                            label = { Text("Повторы") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.md))

                    Button(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Volt,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            "Подход выполнен",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (remainingExercises.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "Далее",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            itemsIndexed(remainingExercises) { _, exRec ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = CardShape
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = exRec.exercise.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${exRec.sets.size} подх.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(Spacing.xxl)) }
        }
    }
}

@Composable
private fun RestTimer(
    secondsLeft: Int,
    totalSeconds: Int,
    nextExerciseName: String,
    nextSetIndex: Int,
    totalSetsForExercise: Int,
    onSkip: () -> Unit
) {
    val progress = if (totalSeconds > 0) 1f - (secondsLeft.toFloat() / totalSeconds) else 1f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "rest")

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = Spacing.xxl)
        ) {
            Text(
                text = "ОТДЫХ",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.lg))

            val minutes = secondsLeft / 60
            val seconds = secondsLeft % 60
            Text(
                text = String.format(java.util.Locale.US, "%d:%02d", minutes, seconds),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = Volt
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Volt,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            Text(
                text = "Следующий: $nextExerciseName",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Подход $nextSetIndex из $totalSetsForExercise",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            TextButton(onClick = onSkip) {
                Text(
                    "Пропустить отдых",
                    fontWeight = FontWeight.Bold,
                    color = Volt
                )
            }
        }
    }
}

@Composable
private fun WorkoutSummary(
    completedSets: List<CompletedSet>,
    @Suppress("unused") exercises: List<ExerciseRecommendation>,
    startTime: Long,
    onFinish: () -> Unit
) {
    val durationMin = ((System.currentTimeMillis() - startTime) / 1000 / 60).toInt()
    val workingSets = completedSets.filter { it.setType == SetType.WORKING }
    val totalTonnage = workingSets.sumOf { it.weight * it.reps }
    val exerciseCount = completedSets.map { it.exerciseName }.distinct().size

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = Spacing.lg)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Volt.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "💪",
                    style = MaterialTheme.typography.displaySmall
                )
            }
            Spacer(modifier = Modifier.height(Spacing.lg))
            Text(
                "Тренировка завершена!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = CardShape
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    SummaryRow("Длительность", "$durationMin мин")
                    SummaryRow("Упражнений", "$exerciseCount")
                    SummaryRow("Рабочих подходов", "${workingSets.size}")
                    SummaryRow("Общий тоннаж", "${FormatUtils.formatWeightPrecise(totalTonnage)} кг")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Volt,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Сохранить результаты", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

