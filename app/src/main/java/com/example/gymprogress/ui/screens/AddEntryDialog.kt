package com.example.gymprogress.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.ExerciseType
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.MuscleGroup
import com.example.gymprogress.data.TrainingGoal
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.data.WorkoutScoreCalculator
import com.example.gymprogress.ui.theme.Volt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddEntryDialog(
    exercises: List<Exercise>,
    history: List<WorkoutEntry>,
    trainingGoal: TrainingGoal,
    bodyWeightKg: Double?,
    onDismiss: () -> Unit,
    onConfirm: (date: String, exerciseName: String, weight: Double, reps: String) -> Unit,
    preselectedExercise: String? = null
) {
    val today = LocalDate.now()
    var displayDate by remember { mutableStateOf(FormatUtils.formatDate(FormatUtils.toStorageDate(today))) }
    var storageDate by remember { mutableStateOf(FormatUtils.toStorageDate(today)) }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedExercise by remember {
        mutableStateOf(
            preselectedExercise?.let { name -> exercises.find { it.name == name } }
        )
    }
    var exerciseDropdownExpanded by remember { mutableStateOf(false) }
    var expandedGroup by remember { mutableStateOf<String?>(null) }

    var weightText by remember { mutableStateOf("") }
    val setReps = remember { mutableStateListOf("") }

    var exerciseError by remember { mutableStateOf(false) }
    var weightError by remember { mutableStateOf(false) }
    var bodyWeightError by remember { mutableStateOf(false) }
    var repsError by remember { mutableStateOf(false) }

    val selectedExerciseType = remember(selectedExercise?.exerciseType) {
        selectedExercise?.exerciseType?.let { typeName ->
            ExerciseType.entries.find { it.name == typeName }
        } ?: ExerciseType.COMPOUND
    }
    val exerciseHistory = remember(selectedExercise?.name, history) {
        val name = selectedExercise?.name ?: return@remember emptyList()
        history.filter { it.exerciseName == name }
            .sortedByDescending { it.date }
    }
    val bestEntry = remember(exerciseHistory, trainingGoal, selectedExerciseType) {
        exerciseHistory.maxByOrNull {
            WorkoutScoreCalculator
                .calcSessionScore(it, exerciseHistory, trainingGoal, selectedExerciseType)
                .score
        }
    }
    val bestReps = remember(bestEntry?.reps) {
        bestEntry?.reps?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }

    val grouped = remember(exercises) { exercises.groupBy { it.muscleGroup } }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val weightBringIntoViewRequester = remember { BringIntoViewRequester() }
    val setsBringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(setReps.size) {
        launch { scrollState.animateScrollTo(scrollState.maxValue) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            decorFitsSystemWindows = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    "Новая запись",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date field with calendar picker
                    OutlinedTextField(
                        value = displayDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Дата") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "Выбрать дату",
                                modifier = Modifier.clickable { showDatePicker = true }
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    )

                    // Exercise dropdown grouped by muscle group
                    ExposedDropdownMenuBox(
                        expanded = exerciseDropdownExpanded,
                        onExpandedChange = { exerciseDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedExercise?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Упражнение") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(exerciseDropdownExpanded)
                            },
                            isError = exerciseError,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )

                        ExposedDropdownMenu(
                            expanded = exerciseDropdownExpanded,
                            onDismissRequest = { exerciseDropdownExpanded = false }
                        ) {
                            if (exercises.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Сначала создайте упражнения\nна вкладке «Упражнения»",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = { exerciseDropdownExpanded = false }
                                )
                            } else {
                                grouped.forEach { (group, groupExercises) ->
                                    val displayName = MuscleGroup.entries
                                        .find { it.name == group }?.displayName ?: group

                                    val isExpanded = expandedGroup == group

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
                                            expandedGroup = if (isExpanded) null else group
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
                                                        "•  ${exercise.name}",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                onClick = {
                                                    selectedExercise = exercise
                                                    exerciseError = false
                                                    exerciseDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedExercise != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Лучшая тренировка",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (bestEntry == null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Пока нет данных по этому упражнению",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${FormatUtils.formatWeight(bestEntry.weight)} кг",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (bestReps.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Подходы: ${bestReps.joinToString(" · ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Дата: ${FormatUtils.formatDate(bestEntry.date)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    val isBodyweightExercise = selectedExercise?.isBodyweight == true
                    val bodyWeightLabel = if (isBodyweightExercise) "Доп. вес (кг)" else "Вес (кг)"
                    val bodyWeightHint = if (isBodyweightExercise && bodyWeightKg != null) {
                        "Вес тела: ${FormatUtils.formatWeight(bodyWeightKg)} кг"
                    } else null
                    val additionalWeightHint = if (isBodyweightExercise) {
                        "Это ДОП. вес — итоговый вес = вес тела + доп. вес"
                    } else null

                    // Weight
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = {
                            weightText = it
                            weightError = false
                            bodyWeightError = false
                        },
                        label = { Text(bodyWeightLabel) },
                        singleLine = true,
                        isError = weightError || bodyWeightError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(weightBringIntoViewRequester)
                            .onFocusEvent {
                                if (it.isFocused) {
                                    coroutineScope.launch { delay(300); weightBringIntoViewRequester.bringIntoView() }
                                }
                            }
                    )
                    bodyWeightHint?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    additionalWeightHint?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Volt
                        )
                    }
                    if (bodyWeightError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Укажите вес тела в настройках",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Sets
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.bringIntoViewRequester(setsBringIntoViewRequester)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Подходы",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            setReps.forEachIndexed { index, repsValue ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = repsValue,
                                        onValueChange = {
                                            setReps[index] = it
                                            repsError = false
                                        },
                                        label = { Text("Подход ${index + 1}") },
                                        singleLine = true,
                                        isError = repsError && repsValue.isBlank(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .onFocusEvent {
                                                if (it.isFocused) {
                                                    coroutineScope.launch { delay(300); setsBringIntoViewRequester.bringIntoView() }
                                                }
                                            }
                                    )
                                    if (setReps.size > 1) {
                                        IconButton(onClick = { setReps.removeAt(index) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Удалить подход",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(48.dp))
                                    }
                                }
                            }

                            TextButton(
                                onClick = { setReps.add("") },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Добавить подход")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    TextButton(onClick = {
                        val isExerciseValid = selectedExercise != null
                        val isBodyweightExercise = selectedExercise?.isBodyweight == true
                        val weightInput = parseWeightInput(weightText, isBodyweightExercise)
                        val isBodyWeightReady = !isBodyweightExercise || bodyWeightKg != null
                        val isWeightValid = isWeightInputValid(weightInput, isBodyweightExercise)
                        val allRepsValid = setReps.all { isRepsValid(it) }

                        exerciseError = !isExerciseValid
                        weightError = !isWeightValid
                        bodyWeightError = isBodyweightExercise && !isBodyWeightReady
                        repsError = !allRepsValid

                        if (isExerciseValid && isWeightValid && allRepsValid && isBodyWeightReady) {
                            val finalWeight = calcFinalWeight(weightInput, isBodyweightExercise, bodyWeightKg)
                            onConfirm(
                                storageDate,
                                selectedExercise!!.name,
                                finalWeight,
                                setReps.joinToString(",")
                            )
                        }
                    }) {
                        Text("Сохранить", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = today.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        storageDate = FormatUtils.toStorageDate(selected)
                        displayDate = FormatUtils.formatDate(storageDate)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
