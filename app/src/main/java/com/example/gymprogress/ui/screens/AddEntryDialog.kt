package com.example.gymprogress.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.MuscleGroup
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onConfirm: (date: String, exerciseName: String, weight: Double, reps: String) -> Unit
) {
    val today = LocalDate.now()
    val displayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val storageFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var displayDate by remember { mutableStateOf(today.format(displayFormatter)) }
    var storageDate by remember { mutableStateOf(today.format(storageFormatter)) }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var exerciseDropdownExpanded by remember { mutableStateOf(false) }

    var weightText by remember { mutableStateOf("") }
    val setReps = remember { mutableStateListOf("") }

    var exerciseError by remember { mutableStateOf(false) }
    var weightError by remember { mutableStateOf(false) }
    var repsError by remember { mutableStateOf(false) }

    val grouped = exercises.groupBy { it.muscleGroup }

    val scrollState = rememberScrollState()

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

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                displayName,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        onClick = {},
                                        enabled = false
                                    )

                                    groupExercises.forEach { exercise ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "    ${exercise.name}",
                                                    style = MaterialTheme.typography.bodyMedium
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

                    // Weight
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = {
                            weightText = it
                            weightError = false
                        },
                        label = { Text("Вес (кг)") },
                        singleLine = true,
                        isError = weightError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Sets
                    Spacer(modifier = Modifier.height(4.dp))
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
                                modifier = Modifier.weight(1f)
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
                        val weight = weightText.replace(",", ".").toDoubleOrNull()
                        val isWeightValid = weight != null && weight > 0
                        val allRepsValid = setReps.all {
                            it.isNotBlank() && it.toIntOrNull() != null && it.toInt() > 0
                        }

                        exerciseError = !isExerciseValid
                        weightError = !isWeightValid
                        repsError = !allRepsValid

                        if (isExerciseValid && isWeightValid && allRepsValid) {
                            onConfirm(
                                storageDate,
                                selectedExercise!!.name,
                                weight!!,
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
                        displayDate = selected.format(displayFormatter)
                        storageDate = selected.format(storageFormatter)
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
