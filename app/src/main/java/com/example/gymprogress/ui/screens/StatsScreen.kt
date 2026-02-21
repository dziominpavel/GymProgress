package com.example.gymprogress.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.gymprogress.data.ComparisonResult
import com.example.gymprogress.data.ExerciseDayScore
import com.example.gymprogress.data.MuscleGroup
import com.example.gymprogress.data.WorkoutDayReport
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.ExerciseType
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.ProgressStatus
import com.example.gymprogress.data.TrainingGoal
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.data.WorkoutScoreCalculator
import com.example.gymprogress.ui.theme.CardShape
import com.example.gymprogress.ui.theme.CardShapeSmall
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

@Composable
private fun StatCard(
    emoji: String,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false
) {
    Card(
        modifier = modifier
            .then(
                if (isHighlight) Modifier.border(
                    width = 1.5.dp,
                    color = Volt.copy(alpha = 0.4f),
                    shape = CardShape
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight)
                Volt.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = CardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (isHighlight) Volt
                else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryRow(
    entry: WorkoutEntry,
    maxWeight: Double,
    comparison: ComparisonResult
) {
    var showDetailDialog by remember { mutableStateOf(false) }

    val isMax = entry.weight == maxWeight
    val statusColor = when (comparison.status) {
        ProgressStatus.BETTER -> Color(0xFF4CAF50)
        ProgressStatus.WORSE -> MaterialTheme.colorScheme.error
        ProgressStatus.SAME -> MaterialTheme.colorScheme.onSurfaceVariant
        ProgressStatus.FIRST -> Volt
    }
    val statusIcon = when (comparison.status) {
        ProgressStatus.BETTER -> "\u25B2"
        ProgressStatus.WORSE -> "\u25BC"
        ProgressStatus.SAME -> "\u2192"
        ProgressStatus.FIRST -> "\u2605"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = CardShapeSmall
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = FormatUtils.formatDate(entry.date),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(86.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Spacing.xs))
                        .background(
                            if (isMax) Volt.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .then(
                            if (isMax) Modifier.border(
                                1.dp,
                                Volt.copy(alpha = 0.3f),
                                RoundedCornerShape(Spacing.xs)
                            ) else Modifier
                        )
                        .padding(horizontal = Spacing.xs, vertical = Spacing.xxs)
                ) {
                    Text(
                        text = "${entry.weight} кг",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isMax) Volt
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.sm))

                val repsList = entry.reps.split(",").map { it.trim() }
                Text(
                    text = repsList.joinToString(" \u00B7 "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Spacing.xxs))
                        .background(statusColor.copy(alpha = 0.12f))
                        .clickable { showDetailDialog = true }
                        .padding(horizontal = Spacing.xs, vertical = 2.dp)
                ) {
                    Text(
                        text = if (comparison.status == ProgressStatus.FIRST) statusIcon
                        else "$statusIcon ${String.format(java.util.Locale.US, "%+.1f%%", comparison.deltaPercent)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            if (comparison.reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = comparison.reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 86.dp)
                )
            }
        }
    }

    if (showDetailDialog && comparison.details != null) {
        ScoreDetailDialog(
            comparison = comparison,
            exerciseName = entry.exerciseName,
            onDismiss = { showDetailDialog = false }
        )
    }
}

@Composable
private fun ScoreDetailDialog(
    comparison: ComparisonResult,
    exerciseName: String,
    onDismiss: () -> Unit
) {
    val d = comparison.details ?: return
    val isFirst = comparison.status == ProgressStatus.FIRST

    val statusLabel = when (comparison.status) {
        ProgressStatus.BETTER -> "\u25B2 Лучше"
        ProgressStatus.WORSE -> "\u25BC Хуже"
        ProgressStatus.SAME -> "\u2192 Без изменений"
        ProgressStatus.FIRST -> "\u2605 Первая тренировка"
    }
    val statusColor = when (comparison.status) {
        ProgressStatus.BETTER -> Color(0xFF4CAF50)
        ProgressStatus.WORSE -> Color(0xFFE53935)
        ProgressStatus.SAME -> Color(0xFF9E9E9E)
        ProgressStatus.FIRST -> Volt
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    exerciseName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${d.goalName} \u00B7 ${d.exerciseTypeName} \u00B7 ${d.targetRange} повт.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                        if (!isFirst) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = String.format(java.util.Locale.US, "%+.1f%%", comparison.deltaPercent),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = statusColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                if (isFirst) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Параметр", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.8f))
                        Text("Значение", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.6f))
                        Text("Балл", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 4.dp))
                    val c = d.currentComponents
                    ScoreRow("🏋️ Интенсивность (вес)", "${FormatUtils.formatWeight(d.currentWeight)} кг", c.intensityPoints, null)
                    ScoreRow("📊 Эфф. объём", "${FormatUtils.formatVolume(d.currentEffVolume)} кг", c.effVolumePoints, null)
                    ScoreRow("🔁 Подходы × Повторы", "${d.currentSets} × [${d.currentReps.joinToString(", ")}]", null, null)
                    ScoreRow("⭐ Качество (${d.targetRange})", "${(d.currentRepQuality * 100).toInt()}%", c.repQualityPoints, null)
                    if (c.prBonus > 0) ScoreRow("🏆 Рекорд веса", "Новый максимум!", c.prBonus, null)
                    if (c.setsAdjust != 0.0) ScoreRow("⚡ Подходы (${d.currentSets} шт.)", if (c.setsAdjust > 0) "Оптимально" else "Неоптимально", c.setsAdjust, null)
                    if (c.fatiguePenalty > 0) ScoreRow("⚠️ Усталость", "Неравномерность", -c.fatiguePenalty, null)
                    if (c.repTrendPenalty > 0) ScoreRow("📈 Сэндбэгинг", "Рост повт. по подходам", -c.repTrendPenalty, null)
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    ScoreRow("🎯 Итоговый балл", String.format(java.util.Locale.US, "%.3f", d.currentScore), null, null)
                } else {
                    val c = d.currentComponents
                    val p = d.previousComponents
                    val pt = d.previousScore.coerceAtLeast(0.001)
                    val contribI  = (c.intensityPoints  - (p?.intensityPoints  ?: 0.0)) / pt * 100
                    val contribV  = (c.effVolumePoints  - (p?.effVolumePoints  ?: 0.0)) / pt * 100
                    val contribR  = (c.repQualityPoints - (p?.repQualityPoints ?: 0.0)) / pt * 100
                    val contribPR = (c.prBonus          - (p?.prBonus          ?: 0.0)) / pt * 100
                    val contribS  = (c.setsAdjust       - (p?.setsAdjust       ?: 0.0)) / pt * 100
                    val contribRT = -((c.repTrendPenalty) - (p?.repTrendPenalty ?: 0.0)) / pt * 100
                    val contribF  = -((c.fatiguePenalty)  - (p?.fatiguePenalty  ?: 0.0)) / pt * 100
                    Text("Вклад в итог (сумма = общий %)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Параметр", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.8f))
                        Text("Значение", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.6f))
                        Text("вклад", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 4.dp))
                    ScoreRowPct("🏋️ Интенсивность",
                        "${FormatUtils.formatWeight(d.previousWeight)} → ${FormatUtils.formatWeight(d.currentWeight)} кг",
                        null, 0.0, overridePct = contribI)
                    ScoreRowPct("📊 Эфф. объём",
                        "${FormatUtils.formatVolume(d.previousEffVolume)} → ${FormatUtils.formatVolume(d.currentEffVolume)} кг",
                        null, 0.0, overridePct = contribV)
                    ScoreRowPct("🔁 Качество",
                        "[${d.previousReps.joinToString(",")}]→[${d.currentReps.joinToString(",")}]",
                        null, 0.0, overridePct = contribR)
                    if (c.prBonus > 0 || (p?.prBonus ?: 0.0) > 0)
                        ScoreRowPct("🏆 Рекорд",
                            if (c.prBonus > 0) "Новый максимум!" else "—",
                            null, 0.0, overridePct = contribPR)
                    if (c.setsAdjust != 0.0 || (p?.setsAdjust ?: 0.0) != 0.0)
                        ScoreRowPct("⚡ Подходы", "${d.previousSets} → ${d.currentSets} шт.",
                            null, 0.0, overridePct = contribS)
                    if (c.repTrendPenalty > 0 || (p?.repTrendPenalty ?: 0.0) > 0)
                        ScoreRowPct("📈 Сэндбэгинг",
                            if (c.repTrendPenalty > 0) "Рост ↑" else "Нет",
                            null, 0.0, overridePct = contribRT)
                    if (c.fatiguePenalty > 0 || (p?.fatiguePenalty ?: 0.0) > 0)
                        ScoreRowPct("⚠️ Усталость",
                            "${String.format(java.util.Locale.US,"%.2f",d.previousFatiguePenalty)} → ${String.format(java.util.Locale.US,"%.2f",d.currentFatiguePenalty)}",
                            null, 0.0, overridePct = contribF)
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    ScoreRowPct("🎯 Итого", "сумма вкладов",
                        null, 0.0, highlight = true, overridePct = comparison.deltaPercent)
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = comparison.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Понятно", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// score: абсолютный балл этого параметра (для отображения), delta: изменение балла
@Composable
private fun ScoreRow(label: String, value: String, score: Double?, delta: Double?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.6f)
        )
        if (score != null) {
            Text(
                text = String.format(java.util.Locale.US, "%.3f", score),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        } else {
            Spacer(modifier = Modifier.width(40.dp))
        }
        if (delta != null && kotlin.math.abs(delta) > 0.0005) {
            val color = if (delta > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
            Text(
                text = String.format(java.util.Locale.US, "%+.3f", delta),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.width(48.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun ScoreRowPct(
    label: String,
    value: String,
    prevScore: Double?,
    curScore: Double,
    highlight: Boolean = false,
    badgeText: String? = null,
    overridePct: Double? = null
) {
    val hasPrev = prevScore != null && kotlin.math.abs(prevScore) > 0.001
    val computedPct = if (hasPrev) ((curScore - prevScore!!) / kotlin.math.abs(prevScore)) * 100.0 else null
    val effectivePct = overridePct ?: computedPct
    val pctColor = when {
        effectivePct == null -> MaterialTheme.colorScheme.onSurfaceVariant
        effectivePct > 0.5 -> Color(0xFF4CAF50)
        effectivePct < -0.5 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label,
            style = MaterialTheme.typography.bodySmall,
            color = if (highlight) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1.8f))
        Text(value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.6f))
        if (badgeText != null) {
            Text(badgeText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (badgeText == "—") MaterialTheme.colorScheme.onSurfaceVariant else Volt,
                modifier = Modifier.width(52.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End)
        } else if (effectivePct != null) {
            val sign = if (effectivePct >= 0) "+" else ""
            Text("$sign${String.format(java.util.Locale.US, "%.1f", effectivePct)}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (highlight) FontWeight.Black else FontWeight.Bold,
                color = pctColor,
                modifier = Modifier.width(52.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End)
        } else {
            Spacer(modifier = Modifier.width(52.dp))
        }
    }
}

@Composable
private fun WorkoutDaySection(
    exercises: List<Exercise>,
    allEntries: List<WorkoutEntry>,
    selectedMuscleGroup: MuscleGroup?,
    onMuscleGroupSelected: (MuscleGroup) -> Unit,
    trainingGoal: TrainingGoal
) {
    Text("Группа мышц", style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MuscleGroup.entries.forEach { mg ->
            val sel = selectedMuscleGroup == mg
            Box(modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (sel) Volt else MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onMuscleGroupSelected(mg) }
                .padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(mg.displayName, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (sel) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    if (selectedMuscleGroup != null) {
        val report = remember(selectedMuscleGroup, allEntries, trainingGoal) {
            WorkoutScoreCalculator.compareDays(
                muscleGroupName = selectedMuscleGroup.name,
                allExercises = exercises,
                allEntries = allEntries,
                goal = trainingGoal
            )
        }
        if (report != null) {
            WorkoutDayReportView(report)
        } else {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                contentAlignment = Alignment.Center) {
                Text("Нет данных для ${selectedMuscleGroup.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WorkoutDayReportView(report: WorkoutDayReport) {
    val displayName = MuscleGroup.entries.find { it.name == report.muscleGroupName }?.displayName
        ?: report.muscleGroupName
    val hasPrev = report.previousOverallScore != null
    val pct = report.overallDeltaPercent
    val statusColor = when (report.overallStatus) {
        ProgressStatus.BETTER -> Color(0xFF4CAF50)
        ProgressStatus.WORSE -> MaterialTheme.colorScheme.error
        ProgressStatus.SAME -> MaterialTheme.colorScheme.onSurfaceVariant
        ProgressStatus.FIRST -> Volt
    }
    val statusIcon = when (report.overallStatus) {
        ProgressStatus.BETTER -> "▲"; ProgressStatus.WORSE -> "▼"
        ProgressStatus.SAME -> "→"; ProgressStatus.FIRST -> "★"
    }
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = CardShape) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(displayName, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Итого", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black)
                    Text(report.currentDate, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (report.previousDate != null)
                        Text("vs ${report.previousDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (hasPrev) {
                        val sign = if (pct >= 0) "+" else ""
                        Text("$statusIcon $sign${String.format(java.util.Locale.US, "%.1f", pct)}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black, color = statusColor)
                        Text("от предыдущей",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("★ Первый раз", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = Volt)
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Column(modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        report.exercises.forEach { ex -> ExerciseDayRow(ex) }
        Spacer(modifier = Modifier.height(Spacing.md))
    }
}

@Composable
private fun ExerciseDayRow(ex: ExerciseDayScore) {
    var expanded by remember { mutableStateOf(false) }
    val hasPrev = ex.previousScore != null && ex.currentEntry != null
    val statusColor = when (ex.status) {
        ProgressStatus.BETTER -> Color(0xFF4CAF50)
        ProgressStatus.WORSE -> MaterialTheme.colorScheme.error
        ProgressStatus.SAME -> MaterialTheme.colorScheme.onSurfaceVariant
        ProgressStatus.FIRST -> Volt
    }
    val statusIcon = when (ex.status) {
        ProgressStatus.BETTER -> "▲"; ProgressStatus.WORSE -> "▼"
        ProgressStatus.SAME -> "→"; ProgressStatus.FIRST -> "★"
    }
    val hasDetail = ex.comparisonResult?.details != null
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = CardShapeSmall) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .then(if (hasDetail) Modifier.clickable { expanded = !expanded } else Modifier)
            .padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(ex.exerciseName, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold)
                    val entryLine = if (ex.currentEntry != null)
                        "${ex.currentEntry.weight} кг · ${ex.currentEntry.reps}"
                    else "Пропущено"
                    Text(entryLine, style = MaterialTheme.typography.bodySmall,
                        color = if (ex.currentEntry != null)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.error)
                    if (ex.previousEntry != null && ex.currentEntry != null)
                        Text("было: ${ex.previousEntry.weight} кг · ${ex.previousEntry.reps}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        if (hasPrev) {
                            val sign = if (ex.deltaPercent >= 0) "+" else ""
                            Text("$statusIcon $sign${String.format(java.util.Locale.US, "%.1f", ex.deltaPercent)}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Black, color = statusColor)
                        } else if (ex.currentEntry != null) {
                            Text("★ Первый", style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold, color = Volt)
                        } else {
                            Text("Пропущено", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (hasDetail) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (expanded && hasDetail) {
                val d = ex.comparisonResult!!.details!!
                val pc = d.currentComponents
                val pp = d.previousComponents
                val pt = d.previousScore.coerceAtLeast(0.001)
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Вклад в итог (сумма = общий %)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                DayComponentRow("🏋️ Интенсивность",
                    pp?.intensityPoints, pc.intensityPoints, prevTotal = pt)
                DayComponentRow("📊 Эфф. объём",
                    pp?.effVolumePoints, pc.effVolumePoints, prevTotal = pt)
                DayComponentRow("⭐ Качество",
                    pp?.repQualityPoints, pc.repQualityPoints, prevTotal = pt)
                if (pc.setsAdjust != 0.0 || (pp?.setsAdjust ?: 0.0) != 0.0)
                    DayComponentRow("⚡ Подходы", pp?.setsAdjust, pc.setsAdjust, prevTotal = pt)
                if (pc.fatiguePenalty > 0 || (pp?.fatiguePenalty ?: 0.0) > 0)
                    DayComponentRow("⚠️ Усталость", pp?.fatiguePenalty?.let { -it }, -pc.fatiguePenalty, prevTotal = pt)
                if (pc.repTrendPenalty > 0 || (pp?.repTrendPenalty ?: 0.0) > 0)
                    DayComponentRow("📈 Сэндбэгинг", pp?.repTrendPenalty?.let { -it }, -pc.repTrendPenalty, prevTotal = pt)
                if (pc.prBonus > 0 || (pp?.prBonus ?: 0.0) > 0)
                    DayComponentRow("🏆 Рекорд", pp?.prBonus, pc.prBonus, prevTotal = pt)
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(6.dp))
                DayComponentRow("🎯 Итого",
                    d.previousScore.takeIf { it > 0 }, d.currentScore,
                    highlight = true)
            }
        }
    }
}

@Composable
private fun DayComponentRow(
    label: String,
    prev: Double?,
    cur: Double,
    highlight: Boolean = false,
    prevTotal: Double? = null
) {
    val hasPrev = prev != null
    val pct: Double? = when {
        prevTotal != null && prevTotal > 0.001 ->
            ((cur - (prev ?: 0.0)) / prevTotal) * 100.0
        hasPrev && kotlin.math.abs(prev!!) > 0.001 ->
            ((cur - prev) / kotlin.math.abs(prev)) * 100.0
        else -> null
    }
    val pctColor = when {
        pct == null -> MaterialTheme.colorScheme.onSurfaceVariant
        pct > 0.5 -> Color(0xFF4CAF50)
        pct < -0.5 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            color = if (highlight) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal)
        if (pct != null) {
            val sign = if (pct >= 0) "+" else ""
            Text("$sign${String.format(java.util.Locale.US, "%.1f", pct)}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (highlight) FontWeight.Black else FontWeight.Bold,
                color = pctColor)
        } else {
            Text(String.format(java.util.Locale.US, "%.3f", cur),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ScoreFormulaHelpDialog(onDismiss: () -> Unit) {
    val scroll = rememberScrollState()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxHeight(0.92f)
        ) {
            Column {
                Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Volt,
                            modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Как считается оценка",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Разбор формулы прогресса по деталям",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scroll)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // --- Итоговый балл ---
                    HelpHeader("📊 Итоговый балл")
                    Text("Число от 0.0 до 1.0. Сравнивает сегодняшнюю тренировку с вашими же лучшими показателями за последние 20 сессий. 1.0 = абсолютный максимум с учётом всей истории.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    HelpFormula("Балл = Интенсивность + Эфф.Объём + Качество\n       + Бонусы − Штрафы   (обрезается до 1.0)")
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Компонент 1 ---
                    HelpHeader("🏋️ Компонент 1: Интенсивность (вес штанги)")
                    HelpChip("Гипертрофия / Базовые: 45%")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Насколько ваш рабочий вес близок к личному рекорду за последние 20 тренировок.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpFormula("Score  = вес_сегодня / лучший_вес_за_20_сессий\nВклад  = Score × 0.45")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpExample("60 кг / 60 кг = 1.000 → 1.000 × 0.45 = 0.450\n55 кг / 60 кг = 0.917 → 0.917 × 0.45 = 0.412")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpNote("«Балл» в строке Интенсивность (0.450) — это и есть количество очков, которые дал этот компонент сегодня. 0.450 = поднял максимальный вес.")
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Компонент 2 ---
                    HelpHeader("📊 Компонент 2: Эффективный объём")
                    HelpChip("Гипертрофия / Базовые: 35%")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Тоннаж, где повторения весят по-разному в зависимости от попадания в целевой диапазон:",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpRow("В целевом диапазоне (8–12)", "× 1.0  ✅")
                    HelpRow("Близко к диапазону (6–7 / 13–15)", "× 0.7  ⚠️")
                    HelpRow("Далеко от диапазона (≤5 / ≥16)", "× 0.3  ❌")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpFormula("Эфф.объём = вес × Σ(повторы × коэфф.)\nВклад = (Эфф.объём / лучший_за_20) × 0.35")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpExample("60 кг × 3×10 (всё в диапазоне):\n= 60 × 30 = 1800 кг\nЛучшее в истории: 55 кг × 4×10 = 2200 кг\n→ 1800/2200 × 0.35 = 0.286 балла (вместо 0.350)")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpNote("Именно здесь возникает «парадокс»: 3×10 при 60 кг даёт меньше объёмных баллов, чем 4×10 при 55 кг. Это нормально — вес компенсируется через Интенсивность и PR-бонус.")
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Компонент 3 ---
                    HelpHeader("⭐ Компонент 3: Качество повторений")
                    HelpChip("Гипертрофия / Базовые: 20%")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpRow("В целевом диапазоне", "1.0")
                    HelpRow("Близко к диапазону", "0.6")
                    HelpRow("За пределами диапазона", "0.2")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpFormula("Вклад = среднее_качество × 0.20")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpExample("3 × 10 при диапазоне 8–12:\n(1.0+1.0+1.0)/3 × 0.20 = 0.200 балла")
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Бонусы ---
                    HelpHeader("🎁 Бонусы")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpRow("🏆 Новый рекорд веса (PR)", "+0.060")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Итог выше 1.0 автоматически обрезается до 1.0.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Оптимальность подходов ---
                    HelpHeader("⚡ Оптимальность подходов")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Количество подходов влияет на оценку двусторонне: слишком мало = недостаточный стимул, слишком много = мусорный объём.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpRow("1 подход", "−0.04")
                    HelpRow("2 подхода", "−0.01")
                    HelpRow("3–5 подходов (оптимум)", "+0.02")
                    HelpRow("6 подходов", "0.00")
                    HelpRow("7+ подходов (мусорный объём)", "−0.03")
                    Spacer(modifier = Modifier.height(4.dp))
                    HelpNote("Наука: гипертрофия — 3-5 рабочих подходов оптимально. Больше 6 = жунк объём, растёт усталость, снижается результат. Для силы оптимум 3-5, для выносливости 2-4.")
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Сэндбэгинг ---
                    HelpHeader("📈 Сэндбэгинг (восходящие повторения)")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Если повторения растут от подхода к подходу (7,8,9) — это сэндбэгинг: первые подходы были слишком лёгкими (высокий RIR). В отличие от 9,8,7 — нормальный спад из-за усталости.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpFormula("Наклон = линейная регрессия повторений по номерам подходов")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpRow("Наклон > 2.0 (сильный рост)", "−0.08")
                    HelpRow("Наклон 1.0–2.0 (умеренный)", "−0.05")
                    HelpRow("Наклон 0.3–1.0 (лёгкий)", "−0.02")
                    HelpRow("Нисходящий или ровный (9,8,7)", "0.00")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpExample("7,8,9 → наклон = +1.0 → штраф −0.05\n9,8,7 → наклон = −1.0 → штраф 0.00")
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Штраф за усталость ---
                    HelpHeader("⚠️ Штраф за усталость (резкий спад)")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Анализирует разброс повторений и падение в последнем подходе. Резкое падение (10,6,4) = перегрузка.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpRow("Разброс ≤ 10%", "0.00")
                    HelpRow("Разброс 10–20%", "−0.02")
                    HelpRow("Разброс 20–30%", "−0.05")
                    HelpRow("Разброс 30–40%", "−0.09")
                    HelpRow("Разброс > 40%", "−0.12")
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Почему 0.992 ---
                    HelpHeader("❓ Почему балл 0.992, а не 1.0?")
                    Text("Теоретический потолок с PR и 5 подходами:",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    HelpFormula("0.450 + 0.350 + 0.200 + 0.060 + 0.050 = 1.110 → 1.000")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Балл 0.992 означает: один из компонентов не на максимуме. Чаще всего — эффективный объём (была сессия с большим числом подходов). Это не плохой результат — это честная оценка.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Таблица весов ---
                    HelpHeader("⚙️ Веса по цели тренировки")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Цель / Тип", style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.2f))
                        Text("💪", style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                        Text("📊", style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                        Text("⭐", style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant)
                    HelpGoalRow("Гипертрофия / Базовые", "45%", "35%", "20%", highlight = true)
                    HelpGoalRow("Гипертрофия / Изоляция", "30%", "35%", "35%")
                    HelpGoalRow("Сила / Базовые", "60%", "20%", "20%")
                    HelpGoalRow("Сила / Изоляция", "50%", "25%", "25%")
                    HelpGoalRow("Выносливость", "15%", "55%", "30%")
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Расшифровка чисел ---
                    HelpHeader("🔢 Что означают два числа в таблице результата")
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text("🏋️ Интенсивность", style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1.8f))
                        Text("55→60 кг", style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1.6f))
                        Text("0.450", style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                        Text("+0.038", style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50),
                            modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HelpRow("Балл (0.450)", "Очки этого компонента сегодня")
                    HelpRow("Δ (+0.038)", "Изменение очков vs прошлая тренировка")
                    Spacer(modifier = Modifier.height(8.dp))
                    HelpNote("0.450 = подняли лучший вес (1.0 × 0.45). Если бы вес был 55 из 60 кг лучшего → 0.917 × 0.45 = 0.412. Разница 0.038 — это и есть прогресс в баллах.")
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text("Понятно", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable private fun HelpHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Black, color = Volt)
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable private fun HelpFormula(text: String) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .background(Volt.copy(alpha = 0.10f)).padding(10.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium)
    }
}

@Composable private fun HelpExample(text: String) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp)) {
        Text("Пример:\n$text", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun HelpNote(text: String) {
    Text("💡 $text", style = MaterialTheme.typography.bodySmall,
        color = Volt.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
}

@Composable private fun HelpChip(text: String) {
    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
        .background(Volt.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 3.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold, color = Volt)
    }
}

@Composable private fun HelpRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun HelpGoalRow(
    goal: String, wI: String, wEV: String, wR: String, highlight: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(goal, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(2.2f))
        Text(wI, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (highlight) Volt else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
        Text(wEV, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
        Text(wR, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
    }
}

