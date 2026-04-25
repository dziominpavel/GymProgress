package com.example.gymprogress.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.ExerciseRecommendation
import com.example.gymprogress.data.ExerciseType
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.MuscleGroup
import com.example.gymprogress.data.ScoringEngine
import com.example.gymprogress.data.ScoringSystem
import com.example.gymprogress.data.SimplifiedScoreCalculator
import com.example.gymprogress.data.TrainingGoal
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.data.selectBestSessionEntry
import com.example.gymprogress.ui.components.rememberHaptics
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.Volt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddEntryDialog(
    exercises: List<Exercise>,
    history: List<WorkoutEntry>,
    trainingGoal: TrainingGoal,
    bodyWeightKg: Double?,
    scoringEngine: ScoringEngine,
    scoringSystem: ScoringSystem,
    onDismiss: () -> Unit,
    onConfirm: (date: String, exerciseName: String, weight: Double, reps: String) -> Unit,
    preselectedExercise: String? = null,
    exerciseRecommendation: ExerciseRecommendation? = null,
    onExerciseSelected: (catalogExerciseName: String?, historyNameHint: String?) -> Unit = { _, _ -> }
) {
    val today = LocalDate.now()
    var displayDate by remember { mutableStateOf(FormatUtils.formatDate(FormatUtils.toStorageDate(today))) }
    var storageDate by remember { mutableStateOf(FormatUtils.toStorageDate(today)) }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var exerciseDropdownExpanded by remember { mutableStateOf(false) }
    var expandedGroup by remember { mutableStateOf<String?>(null) }

    var weightText by remember { mutableStateOf("") }
    var addAdditionalWeight by remember { mutableStateOf(false) }
    val setReps = remember { mutableStateListOf("") }

    var exerciseError by remember { mutableStateOf(false) }
    var weightError by remember { mutableStateOf(false) }
    var bodyWeightError by remember { mutableStateOf(false) }
    var repsError by remember { mutableStateOf(false) }

    // Справочник может прийти после первого кадра; строка «+» из журнала = точный exerciseName в БД.
    LaunchedEffect(preselectedExercise, exercises) {
        if (preselectedExercise != null) {
            val sought = preselectedExercise.trim()
            val match = exercises.find { it.name.trim() == sought }
                ?: exercises.find { it.name.trim().equals(sought, ignoreCase = true) }
            if (match != null) selectedExercise = match
        }
    }

    LaunchedEffect(selectedExercise?.name, preselectedExercise) {
        onExerciseSelected(selectedExercise?.name, preselectedExercise)
    }
    LaunchedEffect(selectedExercise?.isBodyweight) {
        if (selectedExercise?.isBodyweight == true) {
            addAdditionalWeight = false
            weightText = ""
        }
    }

    val selectedExerciseType = remember(selectedExercise?.exerciseType) {
        selectedExercise?.exerciseType?.let { typeName ->
            ExerciseType.entries.find { it.name == typeName }
        } ?: ExerciseType.COMPOUND
    }
    val showBestWorkoutSection = selectedExercise != null || !preselectedExercise.isNullOrBlank()
    // Тот же набор записей, что на «Прогресс» → getEntriesByExercise(имя из справочника)
    val exerciseHistory = remember(selectedExercise, preselectedExercise, history) {
        when {
            selectedExercise != null -> {
                val exercise = selectedExercise
                FormatUtils.workoutEntriesMatchingCatalogName(
                    history,
                    checkNotNull(exercise).name
                ).sortedWith(compareBy({ it.date }, { it.id }))
            }
            else -> {
                val p = preselectedExercise?.trim()?.takeIf { it.isNotEmpty() }
                    ?: return@remember emptyList()
                FormatUtils.workoutEntriesMatchingCatalogName(history, p)
                    .sortedWith(compareBy({ it.date }, { it.id }))
            }
        }
    }
    val isBodyweightSelected = selectedExercise?.isBodyweight == true
    // Упрощённая система: лучшая сессия по оценочному 1RM — [SimplifiedScoreCalculator.bestEntryByEstimatedE1RM]
    val bestEntry = remember(
        exerciseHistory,
        trainingGoal,
        selectedExerciseType,
        scoringEngine,
        scoringSystem,
        bodyWeightKg,
        isBodyweightSelected
    ) {
        if (exerciseHistory.isEmpty()) null
        else when (scoringSystem) {
            ScoringSystem.SIMPLIFIED -> SimplifiedScoreCalculator.bestEntryByEstimatedE1RM(
                exerciseHistory,
                bodyWeightKg,
                isBodyweightSelected
            )
            ScoringSystem.ADVANCED -> selectBestSessionEntry(
                exerciseHistory,
                scoringEngine,
                scoringSystem,
                trainingGoal,
                selectedExerciseType,
                bodyWeightKg,
                isBodyweightSelected
            )
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
                            value = selectedExercise?.name ?: preselectedExercise.orEmpty(),
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

                    if (showBestWorkoutSection) {
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
                                val metricSession = scoringEngine.calcSessionScore(
                                    bestEntry,
                                    exerciseHistory,
                                    trainingGoal,
                                    selectedExerciseType,
                                    bodyWeightKg,
                                    isBodyweightSelected
                                )
                                when (scoringSystem) {
                                    ScoringSystem.SIMPLIFIED -> {
                                        if (metricSession.rawMetric > 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Оценочный 1RM: ${
                                                    String.format(
                                                        Locale.US,
                                                        "%.1f",
                                                        metricSession.rawMetric
                                                    )
                                                } кг",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = Volt
                                            )
                                        }
                                    }
                                    ScoringSystem.ADVANCED -> {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Балл тренировки: ${metricSession.score}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (exerciseRecommendation != null && (exerciseRecommendation.note != null || exerciseRecommendation.advice != null)) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(Spacing.sm)
                        ) {
                            Column {
                                Text(
                                    text = "💡 Совет тренера",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Volt
                                )
                                Spacer(modifier = Modifier.height(Spacing.xxs))
                                exerciseRecommendation.note?.let { note ->
                                    Text(
                                        text = note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (exerciseRecommendation.note != null && exerciseRecommendation.advice != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                exerciseRecommendation.advice?.let { advice ->
                                    Text(
                                        text = advice,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    val isBodyweightExercise = selectedExercise?.isBodyweight == true
                    val bodyWeightLabel = if (isBodyweightExercise) "Доп. вес (кг)" else "Вес (кг)"
                    val bodyWeightHint = if (isBodyweightExercise && bodyWeightKg != null) {
                        "Вес тела: ${FormatUtils.formatWeight(bodyWeightKg)} кг"
                    } else null
                    val weightFieldEnabled = !isBodyweightExercise || addAdditionalWeight

                    if (isBodyweightExercise) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = addAdditionalWeight,
                                onCheckedChange = {
                                    addAdditionalWeight = it
                                    if (!it) weightText = ""
                                }
                            )
                            Text(
                                text = "Ввод доп. веса",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

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
                        enabled = weightFieldEnabled,
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
                    if (bodyWeightError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Укажите вес тела в настройках",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Степпер веса: -2.5 / -0.5 / +0.5 / +2.5
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    StepperRow(
                        deltas = listOf(-2.5, -0.5, 0.5, 2.5),
                        enabled = weightFieldEnabled,
                        onDelta = { delta ->
                            weightText = stepWeight(weightText, delta)
                            weightError = false
                            bodyWeightError = false
                        },
                        formatLabel = { d ->
                            val sign = if (d > 0) "+" else "−"
                            val absStr = if (kotlin.math.abs(d) == kotlin.math.abs(d).toLong().toDouble()) {
                                kotlin.math.abs(d).toLong().toString()
                            } else {
                                String.format(Locale.US, "%.1f", kotlin.math.abs(d))
                            }
                            "$sign$absStr"
                        }
                    )

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

                            val repsHaptics = rememberHaptics()
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
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    IconButton(
                                        onClick = {
                                            repsHaptics.tap()
                                            setReps[index] = stepReps(setReps[index], -1)
                                            repsError = false
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Минус 1 повтор"
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            repsHaptics.tap()
                                            setReps[index] = stepReps(setReps[index], 1)
                                            repsError = false
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Плюс 1 повтор"
                                        )
                                    }
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

                    // Live-превью оценочного 1RM для введённых веса и повторов.
                    // Считаем через SimplifiedScoreCalculator независимо от выбранной системы —
                    // это интуитивная единица, понятная пользователю.
                    val previewE1RM = remember(
                        weightText,
                        setReps.toList(),
                        selectedExercise,
                        bodyWeightKg,
                        isBodyweightExercise,
                        addAdditionalWeight,
                        storageDate
                    ) {
                        val noAdd = isBodyweightExercise && !addAdditionalWeight
                        val weightInput = if (noAdd) 0.0 else parseWeightInput(weightText, isBodyweightExercise)
                        if (!noAdd && (weightInput == null || !isWeightInputValid(weightInput, isBodyweightExercise))) {
                            return@remember null
                        }
                        val finalWeight = if (noAdd) (bodyWeightKg ?: 0.0)
                            else calcFinalWeight(weightInput, isBodyweightExercise, bodyWeightKg)
                        if (finalWeight <= 0.0) return@remember null
                        val validReps = setReps.mapNotNull { it.toIntOrNull() }.filter { it > 0 }
                        if (validReps.isEmpty()) return@remember null
                        val tempEntry = WorkoutEntry(
                            id = 0L,
                            date = storageDate,
                            exerciseName = selectedExercise?.name ?: "",
                            weight = finalWeight,
                            reps = validReps.joinToString(",")
                        )
                        SimplifiedScoreCalculator.calcE1RMForEntry(
                            tempEntry,
                            bodyWeightKg,
                            isBodyweightExercise
                        ).takeIf { it > 0 }
                    }
                    val bestE1RM = remember(bestEntry, bodyWeightKg, isBodyweightExercise) {
                        bestEntry?.let {
                            SimplifiedScoreCalculator.calcE1RMForEntry(it, bodyWeightKg, isBodyweightExercise)
                        }?.takeIf { it > 0 }
                    }
                    if (previewE1RM != null) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        val deltaSuffix = bestE1RM?.let { best ->
                            val delta = previewE1RM - best
                            when {
                                delta > 0.05 -> " · +${FormatUtils.formatTwoDecimals(delta)} к рекорду"
                                delta < -0.05 -> " · ${FormatUtils.formatTwoDecimals(delta)} от рекорда"
                                else -> " · ≈ рекорд"
                            }
                        } ?: ""
                        Text(
                            text = "1RM ≈ ${FormatUtils.formatTwoDecimals(previewE1RM)} кг$deltaSuffix",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (bestE1RM != null && previewE1RM > bestE1RM + 0.05) Volt
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                        val noAdditionalWeight = isBodyweightExercise && !addAdditionalWeight
                        val weightInput = if (noAdditionalWeight) null else parseWeightInput(weightText, isBodyweightExercise)
                        val isBodyWeightReady = !isBodyweightExercise || bodyWeightKg != null
                        val isWeightValid = noAdditionalWeight || isWeightInputValid(weightInput, isBodyweightExercise)
                        val allRepsValid = setReps.all { isRepsValid(it) }

                        exerciseError = !isExerciseValid
                        weightError = !isWeightValid
                        bodyWeightError = isBodyweightExercise && !isBodyWeightReady
                        repsError = !allRepsValid

                        if (isExerciseValid && isWeightValid && allRepsValid && isBodyWeightReady) {
                            val finalWeight = if (noAdditionalWeight) (bodyWeightKg ?: 0.0) else calcFinalWeight(weightInput, isBodyweightExercise, bodyWeightKg)
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

/** Прибавляет/убавляет [delta] к численному значению веса. Пустое поле трактуется как 0.0. */
private fun stepWeight(current: String, delta: Double): String {
    val parsed = current.replace(',', '.').toDoubleOrNull() ?: 0.0
    val updated = (parsed + delta).coerceAtLeast(0.0)
    return if (updated == updated.toLong().toDouble()) {
        updated.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", updated)
    }
}

/** Прибавляет/убавляет [delta] к численному значению повторов. Пустое поле трактуется как 0. */
private fun stepReps(current: String, delta: Int): String {
    val parsed = current.toIntOrNull() ?: 0
    val updated = (parsed + delta).coerceAtLeast(0)
    return updated.toString()
}

/** Ряд чипов для пошагового изменения значения. Каждое нажатие — лёгкий haptic. */
@Composable
private fun StepperRow(
    deltas: List<Double>,
    enabled: Boolean,
    onDelta: (Double) -> Unit,
    formatLabel: (Double) -> String
) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        deltas.forEach { delta ->
            AssistChip(
                onClick = {
                    haptics.tap()
                    onDelta(delta)
                },
                enabled = enabled,
                label = {
                    Text(
                        text = formatLabel(delta),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
