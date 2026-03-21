package com.example.gymprogress.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.gymprogress.data.ComparisonResult
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.ExerciseDayScore
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.MuscleGroup
import com.example.gymprogress.data.ProgressStatus
import com.example.gymprogress.data.TrainingGoal
import com.example.gymprogress.data.WorkoutDayReport
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.data.ScoringEngine
import com.example.gymprogress.data.WorkoutScoreCalculator
import com.example.gymprogress.ui.theme.CardShape
import com.example.gymprogress.ui.theme.CardShapeSmall
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.Volt
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun StatCard(
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
internal fun HistoryRow(
    entry: WorkoutEntry,
    maxWeight: Double,
    comparison: ComparisonResult,
    isSimplified: Boolean = false
) {
    var showDetailDialog by remember { mutableStateOf(false) }

    val isMax = entry.weight == maxWeight
    val statusColor = when (comparison.status) {
        ProgressStatus.FIRST -> Volt
        else -> when {
            comparison.deltaPercent >= 1.0 -> Color(0xFF4CAF50)
            comparison.deltaPercent <= -1.0 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
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
            isSimplified = isSimplified,
            onDismiss = { showDetailDialog = false }
        )
    }
}

@Composable
internal fun ScoreDetailDialog(
    comparison: ComparisonResult,
    exerciseName: String,
    isSimplified: Boolean = false,
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
                    if (isSimplified) "Оценочный 1RM · Epley"
                    else "${d.goalName} \u00B7 ${d.exerciseTypeName} \u00B7 ${d.targetRange} повт.",
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

                val c = d.currentComponents

                if (isSimplified) {
                    ScoreRow("💪 Оценочный 1RM", "${String.format(java.util.Locale.US, "%.1f", c.metricValue)} кг", null, null)
                    if (!isFirst) {
                        ScoreRow("📊 Базовый 1RM", "${String.format(java.util.Locale.US, "%.1f", d.baselineMetric)} кг", null, null)
                    }
                    ScoreRow("🔁 Подходы", "${d.currentSets} × [${d.currentReps.joinToString(", ")}]", null, null)
                    if (!isFirst) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        ScoreRowPct("📈 Прогресс", "vs среднее за 3 сессии",
                            null, 0.0, highlight = true, overridePct = comparison.deltaPercent)
                    }
                } else {
                    val metricStr = when (d.metricType.displayName) {
                        "Объём" -> "${FormatUtils.formatVolume(c.metricValue)} кг"
                        "E1RM" -> "${FormatUtils.formatWeight(c.metricValue)} кг"
                        "Стимул" -> "${c.metricValue.toInt()} / 100"
                        else -> "${c.metricValue.toInt()} повт."
                    }
                    ScoreRow("📊 ${c.metricLabel}", metricStr, null, null)
                    if (c.tensionScore != null) {
                        ScoreRow("💪 Напряжение", "${(c.tensionScore * 100).toInt()}%", null, null)
                    }
                    if (c.productiveScore != null) {
                        ScoreRow("🔄 Продуктивность", "${(c.productiveScore * 100).toInt()}%", null, null)
                    }
                    ScoreRow("🔁 Подходы × Повторы", "${d.currentSets} × [${d.currentReps.joinToString(", ")}]", null, null)
                    ScoreRow("⭐ Качество (${d.targetRange})", "${(d.currentRepQuality * 100).toInt()}%", null, null)
                    if (c.fatiguePenalty > 0) ScoreRow("⚠️ Усталость", "Неравномерность", null, -c.fatiguePenalty)
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isFirst) {
                        ScoreRow("🎯 Балл", "${d.currentScore} / 1000", null, null)
                    } else {
                        val baselineMetricStr = when (d.metricType.displayName) {
                            "Объём" -> FormatUtils.formatVolume(d.baselineMetric)
                            "E1RM" -> FormatUtils.formatWeight(d.baselineMetric)
                            "Стимул" -> d.baselineMetric.toInt().toString()
                            else -> d.baselineMetric.toInt().toString()
                        }
                        ScoreRow("📊 ${c.metricLabel}", "$metricStr (база: $baselineMetricStr)", null, null)
                        ScoreRowPct("🎯 Прогресс", "vs среднее за 3 сессии",
                            null, 0.0, highlight = true, overridePct = comparison.deltaPercent)
                    }
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
internal fun ScoreRow(label: String, value: String, score: Double?, delta: Double?) {
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
                textAlign = TextAlign.End
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
                textAlign = TextAlign.End
            )
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
internal fun ScoreRowPct(
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
        effectivePct >= 1.0 -> Color(0xFF4CAF50)
        effectivePct <= -1.0 -> MaterialTheme.colorScheme.error
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
                textAlign = TextAlign.End)
        } else if (effectivePct != null) {
            val sign = if (effectivePct >= 0) "+" else ""
            Text("$sign${String.format(java.util.Locale.US, "%.1f", effectivePct)}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (highlight) FontWeight.Black else FontWeight.Bold,
                color = pctColor,
                modifier = Modifier.width(52.dp),
                textAlign = TextAlign.End)
        } else {
            Spacer(modifier = Modifier.width(52.dp))
        }
    }
}

@Composable
internal fun WorkoutCalendar(
    month: YearMonth,
    workoutDates: Set<LocalDate>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = CardShape
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Предыдущий месяц")
                }
                val monthName = month.month
                    .getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru"))
                    .replaceFirstChar { it.uppercase() }
                Text(
                    text = "$monthName ${month.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Следующий месяц", modifier = Modifier.graphicsLayer { scaleX = -1f })
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val firstDay = month.atDay(1)
            val startOffset = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
            val daysInMonth = month.lengthOfMonth()
            val rows = (startOffset + daysInMonth + 6) / 7

            (0 until rows).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    (0 until 7).forEach { col ->
                        val dayNumber = row * 7 + col - startOffset + 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNumber in 1..daysInMonth) {
                                val date = month.atDay(dayNumber)
                                val hasWorkout = date in workoutDates
                                val isSelected = date == selectedDate
                                val isToday = date == LocalDate.now()

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> Volt
                                                hasWorkout -> Volt.copy(alpha = 0.2f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .then(
                                            if (isToday && !isSelected)
                                                Modifier.border(1.dp, Volt.copy(alpha = 0.6f), CircleShape)
                                            else Modifier
                                        )
                                        .clickable { onDateSelected(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNumber",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected || hasWorkout || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.background
                                            hasWorkout -> Volt
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WorkoutDaySection(
    exercises: List<Exercise>,
    allEntries: List<WorkoutEntry>,
    selectedMuscleGroup: MuscleGroup?,
    onMuscleGroupSelected: (MuscleGroup) -> Unit,
    trainingGoal: TrainingGoal,
    scoringEngine: ScoringEngine = WorkoutScoreCalculator,
    bodyWeightKg: Double? = null,
    isSimplified: Boolean = false
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
        val report = remember(selectedMuscleGroup, allEntries, trainingGoal, scoringEngine) {
            scoringEngine.compareDays(
                muscleGroupName = selectedMuscleGroup.name,
                allExercises = exercises,
                allEntries = allEntries,
                goal = trainingGoal,
                bodyWeightKg = bodyWeightKg
            )
        }
        if (report != null) {
            WorkoutDayReportView(report, isSimplified = isSimplified)
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
internal fun WorkoutDayReportView(
    report: WorkoutDayReport,
    showOverallCard: Boolean = true,
    isSimplified: Boolean = false
) {
    if (showOverallCard) {
        val displayName = MuscleGroup.entries.find { it.name == report.muscleGroupName }?.displayName
            ?: report.muscleGroupName
        val hasPrev = report.previousOverallScore != null
        val pct = report.overallDeltaPercent
        val statusColor = when (report.overallStatus) {
            ProgressStatus.FIRST -> Volt
            else -> when {
                pct >= 1.0 -> Color(0xFF4CAF50)
                pct <= -1.0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
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
                        Text(FormatUtils.formatDate(report.currentDate), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (report.previousDate != null)
                            Text("vs ${FormatUtils.formatDate(report.previousDate)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (hasPrev) {
                            val sign = if (pct >= 0) "+" else ""
                            Text("$statusIcon $sign${String.format(java.util.Locale.US, "%.1f", pct)}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black, color = statusColor)
                            Text("vs среднее за 3 сессии",
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
    }
    // Без verticalScroll: WorkoutDayReportView используется как item() внутри LazyColumn;
    // вложенная прокрутка даёт бесконечную высоту и IllegalStateException.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        report.exercises.forEach { ex -> ExerciseDayRow(ex, isSimplified = isSimplified) }
        Spacer(modifier = Modifier.height(Spacing.md))
    }
}

@Composable
internal fun ExerciseDayRow(ex: ExerciseDayScore, isSimplified: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    val hasPrev = ex.previousEntry != null && ex.currentEntry != null
    val statusColor = when (ex.status) {
        ProgressStatus.FIRST -> Volt
        else -> when {
            ex.deltaPercent >= 1.0 -> Color(0xFF4CAF50)
            ex.deltaPercent <= -1.0 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
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
                        Text("vs ${FormatUtils.formatDate(ex.previousEntry.date)}: ${ex.previousEntry.weight} кг · ${ex.previousEntry.reps}",
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
                val ptr = d.baselineComponents
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                if (isSimplified) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("💪 Оценочный 1RM", style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("${String.format(java.util.Locale.US, "%.1f", pc.metricValue)} кг",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Black, color = Volt)
                    }
                    if (ptr != null) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("📊 Базовый 1RM", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format(java.util.Locale.US, "%.1f", ptr.metricValue)} кг",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("📈 Прогресс", style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        val pctColor = when {
                            ex.deltaPercent >= 1.0 -> Color(0xFF4CAF50)
                            ex.deltaPercent <= -1.0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val sign = if (ex.deltaPercent >= 0) "+" else ""
                        Text("$sign${String.format(java.util.Locale.US, "%.1f", ex.deltaPercent)}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Black, color = pctColor)
                    }
                } else {
                    DayComponentRow("📊 ${pc.metricLabel}", ptr?.metricValue, pc.metricValue, highlight = false)
                    DayComponentRow("⭐ Качество", ptr?.repQuality, pc.repQuality, highlight = false)
                    if (pc.fatiguePenalty > 0 || (ptr?.fatiguePenalty ?: 0.0) > 0)
                        DayComponentRow("⚠️ Усталость", ptr?.fatiguePenalty?.let { -it }, -pc.fatiguePenalty, highlight = false)
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    DayComponentRow("🎯 Балл", ptr?.totalScore?.toDouble(), pc.totalScore.toDouble(), highlight = true)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📈 Прогресс", style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface)
                        val pctColor = when {
                            ex.deltaPercent >= 1.0 -> Color(0xFF4CAF50)
                            ex.deltaPercent <= -1.0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val sign = if (ex.deltaPercent >= 0) "+" else ""
                        Text("$sign${String.format(java.util.Locale.US, "%.1f", ex.deltaPercent)}%",
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = pctColor)
                    }
                }
            }
        }
    }
}

@Composable
internal fun DayComponentRow(
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
        pct >= 1.0 -> Color(0xFF4CAF50)
        pct <= -1.0 -> MaterialTheme.colorScheme.error
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
