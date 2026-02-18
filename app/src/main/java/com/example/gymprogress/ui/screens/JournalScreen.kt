package com.example.gymprogress.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.ui.theme.CardShape
import com.example.gymprogress.ui.theme.FabShape
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.Volt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun JournalScreen(
    entries: List<WorkoutEntry>,
    exercises: List<Exercise>,
    bodyWeightKg: Double?,
    onAddClick: () -> Unit,
    onDeleteEntry: (WorkoutEntry) -> Unit,
    onUpdateEntry: (WorkoutEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedEntry by remember { mutableStateOf<WorkoutEntry?>(null) }
    var entryToEdit by remember { mutableStateOf<WorkoutEntry?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = Volt,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = FabShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить запись")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.md)
        ) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "ЖУРНАЛ",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = MaterialTheme.typography.headlineMedium.letterSpacing
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Volt)
            )
            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "${entries.size} записей",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(Spacing.md))

            if (entries.isEmpty()) {
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "📝",
                                style = MaterialTheme.typography.displaySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        Text(
                            "Записей пока нет",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(
                            "Нажмите + чтобы добавить\nпервую тренировку",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val sortedEntries = remember(entries) {
                    entries.sortedWith(
                        compareByDescending<WorkoutEntry> { parseEntryDate(it.date) }
                            .thenBy { it.id }
                    )
                }

                val grouped = sortedEntries.groupBy { it.date }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    grouped.forEach { (date, dateEntries) ->
                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(vertical = Spacing.xs)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp, 24.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Volt)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.sm))
                                    Text(
                                        text = FormatUtils.formatDate(date),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = Volt
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(Spacing.xxs))
                                            .background(Volt.copy(alpha = 0.15f))
                                            .padding(horizontal = Spacing.xs, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${dateEntries.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Volt
                                        )
                                    }
                                }
                            }
                        }

                        items(dateEntries, key = { it.id }) { entry ->
                            WorkoutEntryCard(
                                entry = entry,
                                onLongClick = { selectedEntry = entry }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        Dialog(onDismissRequest = { selectedEntry = null }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        entry.exerciseName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${FormatUtils.formatWeight(entry.weight)} кг — ${entry.reps.split(",").size} подходов (${FormatUtils.formatDate(entry.date)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = {
                                selectedEntry = null
                                entryToEdit = entry
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Редактировать")
                        }
                        TextButton(
                            onClick = {
                                onDeleteEntry(entry)
                                selectedEntry = null
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Удалить")
                        }
                    }
                }
            }
        }
    }

    entryToEdit?.let { entry ->
        EditEntryDialog(
            entry = entry,
            exercises = exercises,
            bodyWeightKg = bodyWeightKg,
            onDismiss = { entryToEdit = null },
            onConfirm = { updated ->
                onUpdateEntry(updated)
                entryToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun WorkoutEntryCard(
    entry: WorkoutEntry,
    onLongClick: () -> Unit
) {
    val accentColor = Volt

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(CardShape)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = CardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                verticalAlignment = Alignment.Top
            ) {
                // Weight badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(
                            width = 1.5.dp,
                            color = accentColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = FormatUtils.formatWeight(entry.weight),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                        Text(
                            text = "кг",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Spacing.sm))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.exerciseName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    val reps = remember(entry.reps) {
                        entry.reps.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                    }
                    FlowRow(
                        maxItemsInEachRow = 3,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        reps.forEachIndexed { index, rep ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${index + 1}: ${rep} повт.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

private val storageDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val displayDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

private fun parseEntryDate(date: String): LocalDate {
    return parseEntryDateOrNull(date) ?: LocalDate.MIN
}

private fun parseEntryDateOrNull(date: String): LocalDate? {
    return runCatching { LocalDate.parse(date, storageDateFormatter) }
        .getOrElse {
            runCatching { LocalDate.parse(date, displayDateFormatter) }.getOrNull()
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEntryDialog(
    entry: WorkoutEntry,
    exercises: List<Exercise>,
    bodyWeightKg: Double?,
    onDismiss: () -> Unit,
    onConfirm: (WorkoutEntry) -> Unit
) {
    val storageFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val displayFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val initialDate = remember(entry.id) {
        parseEntryDateOrNull(entry.date) ?: LocalDate.now()
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var storageDate by remember(entry.id) { mutableStateOf(initialDate.format(storageFormatter)) }
    var displayDate by remember(entry.id) { mutableStateOf(initialDate.format(displayFormatter)) }
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
                        modifier = Modifier.fillMaxWidth()
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
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    TextButton(onClick = {
                        val weightInput = weightText.replace(",", ".").toDoubleOrNull()
                        val isBodyWeightReady = !isBodyweightExercise || bodyWeightKg != null
                        val isWeightValid = if (isBodyweightExercise) {
                            weightInput != null && weightInput >= 0
                        } else {
                            weightInput != null && weightInput > 0
                        }
                        val allRepsValid = setReps.all {
                            it.isNotBlank() && it.toIntOrNull() != null && it.toInt() > 0
                        }
                        weightError = !isWeightValid
                        bodyWeightError = isBodyweightExercise && !isBodyWeightReady
                        repsError = !allRepsValid

                        if (isWeightValid && allRepsValid && isBodyWeightReady) {
                            val finalWeight = if (isBodyweightExercise) {
                                (bodyWeightKg ?: 0.0) + (weightInput ?: 0.0)
                            } else {
                                weightInput ?: 0.0
                            }
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
        val currentMillis = runCatching {
            LocalDate.parse(storageDate, storageFormatter)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrElse { System.currentTimeMillis() }

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = currentMillis)

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
