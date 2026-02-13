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
import androidx.compose.foundation.shape.CircleShape
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
import com.example.gymprogress.data.ProgressStatus
import com.example.gymprogress.data.ScoreDetail
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
    exerciseNames: List<String>,
    selectedExercise: String?,
    entriesForExercise: List<WorkoutEntry>,
    onExerciseSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
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
                        Text("📊", style = MaterialTheme.typography.displaySmall)
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

                // Вычисляем результаты сравнения для каждой записи
                val comparisons = entriesForExercise.mapIndexed { index, entry ->
                    val previous = if (index < entriesForExercise.size - 1)
                        entriesForExercise[index + 1] else null
                    entry to WorkoutScoreCalculator.compare(entry, previous, entriesForExercise)
                }

                StatCard(
                    emoji = "🏆",
                    title = "Макс. вес",
                    value = "$maxWeight кг",
                    isHighlight = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
        ProgressStatus.BETTER -> "▲"
        ProgressStatus.WORSE -> "▼"
        ProgressStatus.SAME -> "→"
        ProgressStatus.FIRST -> "★"
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
                    text = entry.date,
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
                    text = repsList.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                // Бейдж прогресса (кликабельный)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Spacing.xxs))
                        .background(statusColor.copy(alpha = 0.12f))
                        .clickable { showDetailDialog = true }
                        .padding(horizontal = Spacing.xs, vertical = 2.dp)
                ) {
                    Text(
                        text = if (comparison.status == ProgressStatus.FIRST) statusIcon
                        else "$statusIcon ${String.format("%+.1f%%", comparison.deltaPercent)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            // Причина изменения
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
        ProgressStatus.BETTER -> "▲ Лучше"
        ProgressStatus.WORSE -> "▼ Хуже"
        ProgressStatus.SAME -> "→ Без изменений"
        ProgressStatus.FIRST -> "★ Первая тренировка"
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
                    "Анализ оценки",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Статус
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
                                text = String.format("%+.1f%%", comparison.deltaPercent),
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

                // Разбивка по компонентам
                if (isFirst) {
                    DetailRow("🏋️ Вес", "${formatWeight(d.currentWeight)} кг", null)
                    DetailRow("📊 Объём", "${formatVolume(d.currentVolume)} кг", null)
                    DetailRow("🔁 Подходы × Повторы",
                        "${d.currentSets} × [${d.currentReps.joinToString(", ")}]", null)
                    DetailRow("⭐ Качество повторов",
                        "${(d.currentRepQuality * 100).toInt()}%", null)
                    if (d.currentFatiguePenalty > 0) {
                        DetailRow("⚠️ Штраф усталости",
                            "-${(d.currentFatiguePenalty * 100).toInt()}%", null)
                    }
                    DetailRow("🎯 Итоговый балл",
                        String.format("%.3f", d.currentScore), null)
                } else {
                    Text(
                        "Параметр",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val weightDelta = d.currentWeight - d.previousWeight
                    DetailRow("🏋️ Вес",
                        "${formatWeight(d.previousWeight)} → ${formatWeight(d.currentWeight)} кг",
                        weightDelta)

                    val volumeDelta = d.currentVolume - d.previousVolume
                    DetailRow("📊 Объём",
                        "${formatVolume(d.previousVolume)} → ${formatVolume(d.currentVolume)} кг",
                        volumeDelta)

                    DetailRow("🔁 Повторы",
                        "[${d.previousReps.joinToString(",")}] → [${d.currentReps.joinToString(",")}]",
                        (d.currentTotalReps - d.previousTotalReps).toDouble())

                    val qualityDelta = d.currentRepQuality - d.previousRepQuality
                    DetailRow("⭐ Качество (8–12)",
                        "${(d.previousRepQuality * 100).toInt()}% → ${(d.currentRepQuality * 100).toInt()}%",
                        qualityDelta)

                    if (d.currentFatiguePenalty > 0 || d.previousFatiguePenalty > 0) {
                        val fatDelta = d.previousFatiguePenalty - d.currentFatiguePenalty
                        DetailRow("⚠️ Штраф усталости",
                            "${(d.previousFatiguePenalty * 100).toInt()}% → ${(d.currentFatiguePenalty * 100).toInt()}%",
                            fatDelta)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    DetailRow("🎯 Итоговый балл",
                        String.format("%.3f → %.3f", d.previousScore, d.currentScore),
                        d.currentScore - d.previousScore)
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                // Причина
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
                text = if (delta > 0) "▲" else "▼",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

private fun formatWeight(w: Double): String =
    if (w == w.toLong().toDouble()) w.toLong().toString() else String.format("%.1f", w)

private fun formatVolume(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.0f", v)
