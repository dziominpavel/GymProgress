package com.example.gymprogress.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.gymprogress.data.ScoringEngine
import com.example.gymprogress.data.SimplifiedScoreCalculator
import com.example.gymprogress.data.TrainingGoal
import com.example.gymprogress.data.WorkoutDayReport
import com.example.gymprogress.data.WorkoutEntry
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
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false,
    @Suppress("UNUSED_PARAMETER") emoji: String? = null
) {
    Card(
        modifier = modifier
            .then(
                if (isHighlight) Modifier.border(
                    width = 1.dp,
                    color = Volt.copy(alpha = 0.35f),
                    shape = CardShape
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight)
                Volt.copy(alpha = 0.06f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = CardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            Text(
                text = title.uppercase(Locale.forLanguageTag("ru")),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = if (isHighlight) Volt
                else MaterialTheme.colorScheme.onSurface
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
    val statusColor = progressColor(comparison.status, comparison.deltaPercent)

    Card(
        modifier = Modifier.clickable(enabled = comparison.details != null) { showDetailDialog = true },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = CardShapeSmall
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = FormatUtils.formatDate(entry.date),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isMax) {
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Volt.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "макс",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Volt
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                val repsList = entry.reps.split(",").map { it.trim() }
                Text(
                    text = "${FormatUtils.formatTwoDecimals(entry.weight)} кг × ${repsList.joinToString(",")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (comparison.status == ProgressStatus.FIRST) {
                    Text(
                        text = "★",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Volt
                    )
                    Text(
                        text = "первая",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val sign = if (comparison.deltaPercent >= 0) "+" else ""
                    Text(
                        text = "$sign${String.format(java.util.Locale.US, "%.2f", comparison.deltaPercent)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                }
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
                    if (isSimplified) "Оценочный 1RM"
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
                                text = String.format(java.util.Locale.US, "%+.2f%%", comparison.deltaPercent),
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
                    ScoreRow("💪 Оценочный 1RM", "${FormatUtils.formatTwoDecimals(c.metricValue)} кг", null, null)
                    if (!isFirst) {
                        ScoreRow("📊 Базовый 1RM", "${FormatUtils.formatTwoDecimals(d.baselineMetric)} кг", null, null)
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
                        "Объём" -> "${FormatUtils.formatTwoDecimals(c.metricValue)} кг"
                        "E1RM" -> "${FormatUtils.formatTwoDecimals(c.metricValue)} кг"
                        "Стимул" -> "${FormatUtils.formatTwoDecimals(c.metricValue)} / 100"
                        else -> "${c.metricValue.toInt()} повт."
                    }
                    ScoreRow("📊 ${c.metricLabel}", metricStr, null, null)
                    if (c.tensionScore != null) {
                        ScoreRow(
                            "💪 Напряжение",
                            "${FormatUtils.formatTwoDecimals(c.tensionScore * 100.0)}%",
                            null,
                            null,
                        )
                    }
                    if (c.productiveScore != null) {
                        ScoreRow(
                            "🔄 Продуктивность",
                            "${FormatUtils.formatTwoDecimals(c.productiveScore * 100.0)}%",
                            null,
                            null,
                        )
                    }
                    ScoreRow("🔁 Подходы × Повторы", "${d.currentSets} × [${d.currentReps.joinToString(", ")}]", null, null)
                    ScoreRow(
                        "⭐ Качество (${d.targetRange})",
                        "${FormatUtils.formatTwoDecimals(d.currentRepQuality * 100.0)}%",
                        null,
                        null,
                    )
                    if (c.fatiguePenalty > 0) ScoreRow("⚠️ Усталость", "Неравномерность", null, -c.fatiguePenalty)
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isFirst) {
                        ScoreRow(
                            "🎯 Балл",
                            "${FormatUtils.formatTwoDecimals(d.currentScore.toDouble())} / 1000",
                            null,
                            null,
                        )
                    } else {
                        val baselineMetricStr = when (d.metricType.displayName) {
                            "Объём" -> FormatUtils.formatTwoDecimals(d.baselineMetric)
                            "E1RM" -> FormatUtils.formatTwoDecimals(d.baselineMetric)
                            "Стимул" -> FormatUtils.formatTwoDecimals(d.baselineMetric)
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
                text = FormatUtils.formatTwoDecimals(score),
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
                text = String.format(java.util.Locale.US, "%+.2f", delta),
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
            Text("$sign${String.format(java.util.Locale.US, "%.2f", effectivePct)}%",
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
    scoringEngine: ScoringEngine = SimplifiedScoreCalculator,
    bodyWeightKg: Double? = null,
    isSimplified: Boolean = false,
    sparklineFor: (String) -> List<Double> = { emptyList() }
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
            WorkoutDayReportView(report, isSimplified = isSimplified, sparklineFor = sparklineFor)
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
    isSimplified: Boolean = false,
    sparklineFor: (String) -> List<Double> = { emptyList() }
) {
    if (showOverallCard) {
        val displayName = MuscleGroup.entries.find { it.name == report.muscleGroupName }?.displayName
            ?: report.muscleGroupName
        val hasPrev = report.previousOverallScore != null
        val pct = report.overallDeltaPercent
        val statusColor = progressColor(report.overallStatus, pct)
        Card(modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = CardShape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        displayName.uppercase(Locale.forLanguageTag("ru")),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(FormatUtils.formatDate(report.currentDate),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface)
                }
                if (hasPrev) {
                    val sign = if (pct >= 0) "+" else ""
                    Text("$sign${String.format(java.util.Locale.US, "%.2f", pct)}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black, color = statusColor)
                } else {
                    Text("★ первый раз", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, color = Volt)
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
    }
    // Без verticalScroll: WorkoutDayReportView используется как item() внутри LazyColumn;
    // вложенная прокрутка даёт бесконечную высоту и IllegalStateException.
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        report.exercises.forEach { ex ->
            ExerciseDayRow(
                ex,
                isSimplified = isSimplified,
                sparklinePoints = sparklineFor(ex.exerciseName)
            )
        }
        Spacer(modifier = Modifier.height(Spacing.md))
    }
}

@Composable
internal fun ExerciseDayRow(
    ex: ExerciseDayScore,
    isSimplified: Boolean = false,
    sparklinePoints: List<Double> = emptyList()
) {
    var expanded by remember { mutableStateOf(false) }
    val hasPrev = ex.previousEntry != null && ex.currentEntry != null
    val statusColor = progressColor(ex.status, ex.deltaPercent)
    val hasDetail = ex.comparisonResult?.details != null
    val metricValue = ex.comparisonResult?.details?.currentComponents?.metricValue
    val baselineMetric = ex.comparisonResult?.details?.baselineComponents?.metricValue

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = CardShapeSmall
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (hasDetail) Modifier.clickable { expanded = !expanded } else Modifier)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        ex.exerciseName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val entryLine = if (ex.currentEntry != null)
                        "${FormatUtils.formatTwoDecimals(ex.currentEntry.weight)} кг × ${ex.currentEntry.reps}"
                    else "пропущено"
                    Text(
                        entryLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ex.currentEntry != null)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.error
                    )
                    if (isSimplified && metricValue != null && metricValue > 0) {
                        Text(
                            text = "1RM ${FormatUtils.formatTwoDecimals(metricValue)} кг",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Volt
                        )
                    }
                }

                if (sparklinePoints.size >= 2) {
                    Sparkline(
                        points = sparklinePoints,
                        color = statusColor.copy(alpha = 0.9f),
                        modifier = Modifier
                            .padding(horizontal = Spacing.sm)
                            .width(52.dp)
                            .height(24.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    when {
                        hasPrev -> {
                            val sign = if (ex.deltaPercent >= 0) "+" else ""
                            Text(
                                "$sign${String.format(java.util.Locale.US, "%.2f", ex.deltaPercent)}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = statusColor
                            )
                        }
                        ex.currentEntry != null -> {
                            Text("★", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black, color = Volt)
                            Text("первая", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        else -> {
                            Text("—", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (hasDetail) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = Spacing.xxs)
                            .size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expanded && hasDetail) {
                val d = ex.comparisonResult!!.details!!
                val pc = d.currentComponents
                val ptr = d.baselineComponents
                Spacer(modifier = Modifier.height(Spacing.sm))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(Spacing.xs))

                if (isSimplified) {
                    if (ex.previousEntry != null) {
                        DetailLine(
                            "Предыдущая",
                            "${FormatUtils.formatDate(ex.previousEntry.date)} · ${FormatUtils.formatTwoDecimals(ex.previousEntry.weight)} кг × ${ex.previousEntry.reps}"
                        )
                    }
                    if (baselineMetric != null && hasPrev) {
                        DetailLine(
                            "Базовый 1RM",
                            "${FormatUtils.formatTwoDecimals(baselineMetric)} кг",
                            caption = "среднее за 3 сессии"
                        )
                    }
                } else {
                    DayComponentRow(pc.metricLabel, ptr?.metricValue, pc.metricValue, highlight = false)
                    DayComponentRow("Качество", ptr?.repQuality, pc.repQuality, highlight = false)
                    if (pc.fatiguePenalty > 0 || (ptr?.fatiguePenalty ?: 0.0) > 0)
                        DayComponentRow("Усталость", ptr?.fatiguePenalty?.let { -it }, -pc.fatiguePenalty, highlight = false)
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    DayComponentRow("Балл", ptr?.totalScore?.toDouble(), pc.totalScore.toDouble(), highlight = true)
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, caption: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (caption != null) {
                Text(
                    caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
internal fun Sparkline(
    points: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = Volt
) {
    if (points.size < 2) return
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val min = points.min()
        val max = points.max()
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        val path = Path()
        points.forEachIndexed { i, v ->
            val x = w * i.toFloat() / (points.size - 1)
            val y = h - ((v - min) / range * h).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )
        // маркер последней точки
        val lastX = w
        val lastY = h - ((points.last() - min) / range * h).toFloat()
        drawCircle(color = color, radius = 2.5f, center = Offset(lastX, lastY))
    }
}

@Composable
private fun progressColor(status: ProgressStatus, deltaPercent: Double): Color {
    return when (status) {
        ProgressStatus.FIRST -> Volt
        else -> when {
            deltaPercent >= 1.0 -> Color(0xFF4CAF50)
            deltaPercent <= -1.0 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
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
            Text("$sign${String.format(java.util.Locale.US, "%.2f", pct)}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (highlight) FontWeight.Black else FontWeight.Bold,
                color = pctColor)
        } else {
            Text(FormatUtils.formatTwoDecimals(cur),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
