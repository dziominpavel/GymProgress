package com.example.gymprogress.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.ui.theme.Volt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditEntryDialog(
    entry: WorkoutEntry,
    exercises: List<Exercise>,
    bodyWeightKg: Double?,
    onDismiss: () -> Unit,
    onConfirm: (WorkoutEntry) -> Unit
) {
    val initialDate = remember(entry.id) {
        FormatUtils.parseStorageDate(entry.date) ?: LocalDate.now()
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var storageDate by remember(entry.id) { mutableStateOf(FormatUtils.toStorageDate(initialDate)) }
    var displayDate by remember(entry.id) { mutableStateOf(FormatUtils.formatDate(FormatUtils.toStorageDate(initialDate))) }
    val isBodyweightExercise = remember(entry.exerciseName, exercises) {
        exercises.firstOrNull { it.name == entry.exerciseName }?.isBodyweight == true
    }
    val initialWeightText = remember(entry.weight, bodyWeightKg, isBodyweightExercise) {
        if (isBodyweightExercise && bodyWeightKg != null) {
            FormatUtils.formatWeight(maxOf(0.0, entry.weight - bodyWeightKg))
        } else {
            FormatUtils.formatWeight(entry.weight)
        }
    }
    var weightText by remember { mutableStateOf(initialWeightText) }
    val setReps = remember {
        mutableStateListOf(*entry.reps.split(",").map { it.trim() }.toTypedArray())
    }
    var weightError by remember { mutableStateOf(false) }
    var repsError by remember { mutableStateOf(false) }
    var bodyWeightError by remember { mutableStateOf(false) }

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
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Редактировать", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    entry.exerciseName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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

                    val weightLabel = if (isBodyweightExercise) "Доп. вес (кг)" else "Вес (кг)"
                    val bodyWeightHint = if (isBodyweightExercise && bodyWeightKg != null) {
                        "Вес тела: ${FormatUtils.formatWeight(bodyWeightKg)} кг"
                    } else null
                    val additionalWeightHint = if (isBodyweightExercise) {
                        "Это ДОП. вес — итоговый вес = вес тела + доп. вес"
                    } else null

                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it; weightError = false; bodyWeightError = false },
                        label = { Text(weightLabel) },
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
                                        onValueChange = { setReps[index] = it; repsError = false },
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
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    TextButton(onClick = {
                        val weightInput = parseWeightInput(weightText, isBodyweightExercise)
                        val isBodyWeightReady = !isBodyweightExercise || bodyWeightKg != null
                        val isWeightValid = isWeightInputValid(weightInput, isBodyweightExercise)
                        val allRepsValid = setReps.all { isRepsValid(it) }
                        weightError = !isWeightValid
                        bodyWeightError = isBodyweightExercise && !isBodyWeightReady
                        repsError = !allRepsValid

                        if (isWeightValid && allRepsValid && isBodyWeightReady) {
                            val finalWeight = calcFinalWeight(weightInput, isBodyweightExercise, bodyWeightKg)
                            onConfirm(
                                entry.copy(
                                    date = storageDate,
                                    weight = finalWeight,
                                    reps = setReps.joinToString(",")
                                )
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
        val currentMillis = FormatUtils.parseStorageDate(storageDate)
            ?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli() ?: System.currentTimeMillis()

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = currentMillis)

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
