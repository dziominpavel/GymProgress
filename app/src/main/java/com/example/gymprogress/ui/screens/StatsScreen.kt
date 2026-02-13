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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.gymprogress.data.ComparisonResult
import com.example.gymprogress.data.ExerciseType
import com.example.gymprogress.data.ProgressStatus
import com.example.gymprogress.data.TrainingGoal
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.data.WorkoutScoreCalculator
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.ui.theme.CardShape
import com.example.gymprogress.ui.theme.CardShapeSmall
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.TextFieldShape
import com.example.gymprogress.ui.theme.Volt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    exerciseNames: List<String>,
    selectedExercise: String?,
    entriesForExercise: List<WorkoutEntry>,
    onExerciseSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    trainingGoal: TrainingGoal = TrainingGoal.HYPERTROPHY,
    exerciseType: ExerciseType = ExerciseType.COMPOUND
) {
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
        Text(
            text = "ПРОГРЕСС",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
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

        Spacer(modifier = Modifier.height(Spacing.lg))

        if (exerciseNames.isEmpty()) {
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
        } else {
            var expanded by remember { mutableStateOf(false) }

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
                    exerciseNames.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onExerciseSelected(name)
                                expanded = false
                            }
                        )
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
                    DetailRow("\uD83C\uDFCB\uFE0F Вес", "${FormatUtils.formatWeight(d.currentWeight)} кг", null)
                    DetailRow("\uD83D\uDCCA Объём", "${FormatUtils.formatVolume(d.currentVolume)} кг", null)
                    DetailRow("\uD83D\uDD01 Подходы \u00D7 Повторы",
                        "${d.currentSets} \u00D7 [${d.currentReps.joinToString(", ")}]", null)
                    DetailRow("\u2B50 Качество повторов",
                        "${(d.currentRepQuality * 100).toInt()}%", null)
                    if (d.currentSetsBonus > 0) {
                        DetailRow("\uD83D\uDCAA Бонус подходов",
                            "+${(d.currentSetsBonus * 100).toInt()}%", null)
                    }
                    if (d.currentFatiguePenalty > 0) {
                        DetailRow("\u26A0\uFE0F Штраф усталости",
                            "-${(d.currentFatiguePenalty * 100).toInt()}%", null)
                    }
                    DetailRow("\uD83C\uDFAF Итоговый балл",
                        String.format(java.util.Locale.US, "%.3f", d.currentScore), null)
                } else {
                    Text(
                        "Параметр",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val weightDelta = d.currentWeight - d.previousWeight
                    DetailRow("\uD83C\uDFCB\uFE0F Вес",
                        "${FormatUtils.formatWeight(d.previousWeight)} \u2192 ${FormatUtils.formatWeight(d.currentWeight)} кг",
                        weightDelta)

                    val volumeDelta = d.currentVolume - d.previousVolume
                    DetailRow("\uD83D\uDCCA Объём",
                        "${FormatUtils.formatVolume(d.previousVolume)} \u2192 ${FormatUtils.formatVolume(d.currentVolume)} кг",
                        volumeDelta)

                    DetailRow("\uD83D\uDD01 Повторы",
                        "[${d.previousReps.joinToString(",")}] \u2192 [${d.currentReps.joinToString(",")}]",
                        (d.currentTotalReps - d.previousTotalReps).toDouble())

                    val qualityDelta = d.currentRepQuality - d.previousRepQuality
                    DetailRow("\u2B50 Качество (${d.targetRange})",
                        "${(d.previousRepQuality * 100).toInt()}% \u2192 ${(d.currentRepQuality * 100).toInt()}%",
                        qualityDelta)

                    if (d.currentSetsBonus > 0 || d.previousSetsBonus > 0) {
                        val setsDelta = d.currentSetsBonus - d.previousSetsBonus
                        DetailRow("\uD83D\uDCAA Бонус подходов",
                            "${(d.previousSetsBonus * 100).toInt()}% \u2192 ${(d.currentSetsBonus * 100).toInt()}%",
                            setsDelta)
                    }

                    if (d.currentFatiguePenalty > 0 || d.previousFatiguePenalty > 0) {
                        val fatDelta = d.previousFatiguePenalty - d.currentFatiguePenalty
                        DetailRow("\u26A0\uFE0F Штраф усталости",
                            "${(d.previousFatiguePenalty * 100).toInt()}% \u2192 ${(d.currentFatiguePenalty * 100).toInt()}%",
                            fatDelta)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    DetailRow("\uD83C\uDFAF Итоговый балл",
                        String.format(java.util.Locale.US, "%.3f \u2192 %.3f", d.previousScore, d.currentScore),
                        d.currentScore - d.previousScore)
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

@Composable
private fun DetailRow(label: String, value: String, delta: Double?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (delta != null && delta != 0.0) {
            val color = if (delta > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
            Text(
                text = if (delta > 0) "\u25B2" else "\u25BC",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

