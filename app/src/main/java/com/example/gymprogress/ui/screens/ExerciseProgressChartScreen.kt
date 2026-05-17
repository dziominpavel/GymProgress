package com.example.gymprogress.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymprogress.data.ChartMetric
import com.example.gymprogress.data.ChartRange
import com.example.gymprogress.data.ExerciseProgressChartPoint
import com.example.gymprogress.data.ExerciseType
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.ScoringEngine
import com.example.gymprogress.data.ScoringSystem
import com.example.gymprogress.data.TrainingGoal
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.data.buildExerciseProgressChartPoints
import com.example.gymprogress.ui.theme.GymTheme
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.Volt
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

private data class ChartLayout(
    val nodeOffsets: List<Pair<Offset, ExerciseProgressChartPoint>>,
    val yAxisMin: Double,
    val yAxisMax: Double,
)

/**
 * «Сырые» границы: точки занимают ~80% высоты, по 10% воздуха сверху и снизу (отступ = ⅛ размаха).
 */
private fun paddedRawYRange(dataMin: Double, dataMax: Double): Pair<Double, Double> {
    val range = dataMax - dataMin
    if (range < 1e-9) {
        val mid = dataMin
        val pad = when {
            abs(mid) < 1e-9 -> 1.0
            else -> max(abs(mid) * 0.05, 1e-6)
        }.coerceAtLeast(0.5)
        return mid - pad to mid + pad
    }
    val pad = range / 8.0
    return dataMin - pad to dataMax + pad
}

/**
 * «Красивый» шаг для оси: округление в ряд 1, 2, 5, 10, 20, 50, 100, 200, 500…
 * Целевая длина деления равна примерно [rough]; алгоритм выбирает ближайший
 * псевдо-стандартный шаг той же магнитуды. Возвращает не меньше 1.0, чтобы
 * границы оси оставались целочисленными даже при малых значениях.
 */
private fun niceStep(rough: Double): Double {
    if (rough <= 0.0) return 1.0
    val mag = 10.0.pow(floor(log10(rough)))
    val norm = rough / mag
    val multiplier = when {
        norm < 1.5 -> 1.0
        norm < 3.0 -> 2.0
        norm < 7.0 -> 5.0
        else -> 10.0
    }
    return (multiplier * mag).coerceAtLeast(1.0)
}

/**
 * Границы оси для отрисовки и подписей: значения округлены до удобного шага
 * (см. [niceStep]), все точки гарантированно попадают в `[yLo; yHi]`.
 * Возвращается также шаг — для возможной отрисовки промежуточных подписей.
 */
private data class YAxisRange(val yLo: Double, val yHi: Double, val step: Double)

private fun chartDisplayYAxisRange(points: List<ExerciseProgressChartPoint>): YAxisRange {
    val dataMin = points.minOf { it.yValue }
    val dataMax = points.maxOf { it.yValue }
    val (rawMin, rawMax) = paddedRawYRange(dataMin, dataMax)
    val targetTicks = 5.0
    val step = niceStep((rawMax - rawMin) / targetTicks)
    var yLo = floor(rawMin / step) * step
    var yHi = ceil(rawMax / step) * step
    if (yHi <= yLo) yHi = yLo + step
    while (yLo > dataMin) yLo -= step
    while (yHi < dataMax) yHi += step
    while (yHi - yLo < step) yHi += step
    return YAxisRange(yLo, yHi, step)
}

private fun computeChartLayout(
    data: List<ExerciseProgressChartPoint>,
    width: Float,
    height: Float,
    paddingStart: Float,
    paddingEnd: Float,
    paddingTop: Float,
    paddingBottom: Float,
    yAxisMin: Double,
    yAxisMax: Double,
): ChartLayout {
    if (data.isEmpty() || width <= 0f || height <= 0f) {
        return ChartLayout(emptyList(), yAxisMin, yAxisMax)
    }
    val span = (yAxisMax - yAxisMin).coerceAtLeast(1e-9)
    val innerW = (width - paddingStart - paddingEnd).coerceAtLeast(1f)
    val innerH = (height - paddingTop - paddingBottom).coerceAtLeast(1f)
    val n = data.size
    val nodeOffsets = data.mapIndexed { i, p ->
        val x = paddingStart + innerW * if (n == 1) 0.5f else i.toFloat() / (n - 1)
        val t = ((p.yValue - yAxisMin) / span).toFloat().coerceIn(0f, 1f)
        val y = paddingTop + innerH * (1f - t)
        Offset(x, y) to p
    }
    return ChartLayout(nodeOffsets, yAxisMin, yAxisMax)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressChartScreen(
    exerciseName: String,
    entries: List<WorkoutEntry>,
    scoringEngine: ScoringEngine,
    scoringSystem: ScoringSystem,
    trainingGoal: TrainingGoal,
    exerciseType: ExerciseType,
    bodyWeightKg: Double?,
    isBodyweightExercise: Boolean,
    isAnthropometryIncompleteForBw: Boolean,
    chartRange: ChartRange,
    chartMetric: ChartMetric,
    onChartRangeChange: (ChartRange) -> Unit,
    onChartMetricChange: (ChartMetric) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSimplified = scoringSystem == ScoringSystem.SIMPLIFIED
    val points = remember(
        entries,
        scoringEngine,
        scoringSystem,
        trainingGoal,
        exerciseType,
        bodyWeightKg,
        isBodyweightExercise,
        chartMetric,
        chartRange,
    ) {
        buildExerciseProgressChartPoints(
            entries = entries,
            scoringEngine = scoringEngine,
            goal = trainingGoal,
            exerciseType = exerciseType,
            bodyWeightKg = bodyWeightKg,
            isBodyweightExercise = isBodyweightExercise,
            metric = chartMetric,
            range = chartRange,
        )
    }

    var tappedPoint by remember { mutableStateOf<ExerciseProgressChartPoint?>(null) }
    val density = LocalDensity.current
    val hitRadiusPx = remember(density) { with(density) { 40.dp.toPx() } }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.md),
        ) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = "ГРАФИК",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Volt),
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val unitSuffix = if (chartMetric.unit.isNotBlank()) " (${chartMetric.unit})" else ""
            Text(
                text = "Ось Y: ${chartMetric.displayName.lowercase()}$unitSuffix, масштаб по данным. Ось X: номер тренировки.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            ChartRangeChips(
                selected = chartRange,
                onSelect = onChartRangeChange,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            ChartMetricChips(
                selected = chartMetric,
                onSelect = onChartMetricChange,
            )

            if (isSimplified && isAnthropometryIncompleteForBw && isBodyweightExercise) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Укажите вес тела в настройках для корректной оценки упражнений с собственным весом.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            when {
                entries.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Нет записей для этого упражнения",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                points.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Недостаточно данных для графика (проверьте подходы и вес)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                else -> {
                    val yAxisRange = remember(points) { chartDisplayYAxisRange(points) }
                    val yAxisMin = yAxisRange.yLo
                    val yAxisMax = yAxisRange.yHi
                    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    val dotInnerColor = MaterialTheme.colorScheme.surface
                    val lineColor = MaterialTheme.colorScheme.primary
                    val prColor = GymTheme.colors.success
                    val trendColor = MaterialTheme.colorScheme.onSurfaceVariant
                    val labelColWidth = 56.dp
                    val trend = remember(points) { computeTrend(points) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        Column(
                            modifier = Modifier
                                .width(labelColWidth)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = formatAxisLabel(yAxisMax),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = formatAxisLabel(yAxisMin),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                        ) {
                            val paddingStart = with(density) { 4.dp.toPx() }
                            val paddingEnd = with(density) { 8.dp.toPx() }
                            val paddingTop = with(density) { 12.dp.toPx() }
                            val paddingBottom = with(density) { 28.dp.toPx() }

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(points, yAxisMin, yAxisMax, hitRadiusPx) {
                                        detectTapGestures { offset ->
                                            val w = size.width.toFloat()
                                            val h = size.height.toFloat()
                                            val layout = computeChartLayout(
                                                points,
                                                w,
                                                h,
                                                paddingStart,
                                                paddingEnd,
                                                paddingTop,
                                                paddingBottom,
                                                yAxisMin,
                                                yAxisMax,
                                            )
                                            val nearest = layout.nodeOffsets.minByOrNull { (o, _) ->
                                                hypot(
                                                    offset.x - o.x,
                                                    offset.y - o.y,
                                                )
                                            }
                                            if (nearest != null) {
                                                val d = hypot(
                                                    offset.x - nearest.first.x,
                                                    offset.y - nearest.first.y,
                                                )
                                                if (d <= hitRadiusPx) {
                                                    tappedPoint = nearest.second
                                                }
                                            }
                                        }
                                    },
                            ) {
                                val layout = computeChartLayout(
                                    points,
                                    size.width,
                                    size.height,
                                    paddingStart,
                                    paddingEnd,
                                    paddingTop,
                                    paddingBottom,
                                    yAxisMin,
                                    yAxisMax,
                                )
                                drawLine(
                                    gridLineColor,
                                    Offset(paddingStart, size.height - paddingBottom),
                                    Offset(size.width - paddingEnd, size.height - paddingBottom),
                                    strokeWidth = 2.dp.toPx(),
                                )
                                drawLine(
                                    gridLineColor,
                                    Offset(paddingStart, paddingTop),
                                    Offset(paddingStart, size.height - paddingBottom),
                                    strokeWidth = 2.dp.toPx(),
                                )

                                if (layout.nodeOffsets.size >= 2) {
                                    val path = Path()
                                    path.moveTo(layout.nodeOffsets[0].first.x, layout.nodeOffsets[0].first.y)
                                    for (i in 1 until layout.nodeOffsets.size) {
                                        path.lineTo(
                                            layout.nodeOffsets[i].first.x,
                                            layout.nodeOffsets[i].first.y,
                                        )
                                    }
                                    drawPath(
                                        path = path,
                                        color = lineColor,
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                                    )
                                }

                                if (trend != null && layout.nodeOffsets.size >= 2) {
                                    val first = layout.nodeOffsets.first().first
                                    val last = layout.nodeOffsets.last().first
                                    val span = (yAxisMax - yAxisMin).coerceAtLeast(1e-9)
                                    val innerH = (size.height - paddingTop - paddingBottom).coerceAtLeast(1f)
                                    fun toScreenY(value: Double): Float {
                                        val t = ((value - yAxisMin) / span).toFloat().coerceIn(0f, 1f)
                                        return paddingTop + innerH * (1f - t)
                                    }
                                    val y0 = toScreenY(trend.intercept)
                                    val y1 = toScreenY(trend.intercept + trend.slope * (layout.nodeOffsets.size - 1))
                                    drawLine(
                                        color = trendColor.copy(alpha = 0.7f),
                                        start = Offset(first.x, y0),
                                        end = Offset(last.x, y1),
                                        strokeWidth = 2.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(8.dp.toPx(), 6.dp.toPx())
                                        ),
                                    )
                                }

                                val dotR = 6.dp.toPx()
                                val prDotR = 9.dp.toPx()
                                for ((center, p) in layout.nodeOffsets) {
                                    val isPr = p.isPersonalRecord
                                    val outer = if (isPr) prColor else lineColor
                                    val r = if (isPr) prDotR else dotR
                                    drawCircle(
                                        color = outer,
                                        radius = r,
                                        center = center,
                                    )
                                    drawCircle(
                                        color = dotInnerColor,
                                        radius = r * 0.45f,
                                        center = center,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "1",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (points.size > 2) {
                            Text(
                                text = "${points.size / 2}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${points.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (trend != null) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = formatTrendText(trend, chartMetric),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.md))
                }
            }
        }
    }

    tappedPoint?.let { p ->
        AlertDialog(
            onDismissRequest = { tappedPoint = null },
            title = {
                Text(
                    text = FormatUtils.formatDate(p.dateStorage) +
                        if (p.isPersonalRecord) "  ·  Рекорд" else "",
                    fontWeight = FontWeight.Bold,
                    color = if (p.isPersonalRecord) GymTheme.colors.success
                    else MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Column {
                    Text(
                        text = "Подход: ${FormatUtils.formatWeight(p.workingWeight)} кг × ${p.representativeEntry.reps}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    Text(
                        text = "Объём: ${FormatUtils.formatVolume(p.volume)} кг",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "1RM: ${FormatUtils.formatTwoDecimals(p.e1rm)} кг",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Score: ${p.score}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { tappedPoint = null }) {
                    Text("OK")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChartRangeChips(
    selected: ChartRange,
    onSelect: (ChartRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        ChartRange.entries.forEach { range ->
            FilterChip(
                selected = range == selected,
                onClick = { onSelect(range) },
                label = { Text(range.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChartMetricChips(
    selected: ChartMetric,
    onSelect: (ChartMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        ChartMetric.entries.forEach { metric ->
            FilterChip(
                selected = metric == selected,
                onClick = { onSelect(metric) },
                label = { Text(metric.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
        }
    }
}

/**
 * Подписи целочисленных меток для оси Y. Для метрик с большими значениями (например, объём)
 * округляем до целого; для дробных — два знака.
 */
private fun formatAxisLabel(value: Double): String {
    val absV = abs(value)
    return if (absV >= 100.0) value.toLong().toString()
    else FormatUtils.formatWeight(value)
}

/**
 * Линейная регрессия по yValue от индекса точки.
 * @return slope (изменение yValue на 1 индекс), intercept, perWeek (изменение метрики в неделю)
 *         и текстовый знак тренда. `null`, если точек < 2 или диапазон дат вырожден.
 */
private data class TrendLine(
    val slope: Double,
    val intercept: Double,
    val perWeek: Double,
)

private fun computeTrend(points: List<ExerciseProgressChartPoint>): TrendLine? {
    val n = points.size
    if (n < 2) return null
    val xs = (0 until n).map { it.toDouble() }
    val ys = points.map { it.yValue }
    val xMean = xs.average()
    val yMean = ys.average()
    var num = 0.0
    var den = 0.0
    for (i in 0 until n) {
        val dx = xs[i] - xMean
        num += dx * (ys[i] - yMean)
        den += dx * dx
    }
    if (den < 1e-9) return null
    val slope = num / den
    val intercept = yMean - slope * xMean

    val firstDate = FormatUtils.parseStorageDate(points.first().dateStorage)
    val lastDate = FormatUtils.parseStorageDate(points.last().dateStorage)
    val daysSpan = if (firstDate != null && lastDate != null)
        ChronoUnit.DAYS.between(firstDate, lastDate).toDouble()
    else 0.0
    if (daysSpan < 1.0) return null
    val totalChange = slope * (n - 1)
    val perWeek = totalChange / (daysSpan / 7.0)
    return TrendLine(slope, intercept, perWeek)
}

private fun formatTrendText(trend: TrendLine, metric: ChartMetric): String {
    val unit = metric.unit
    val absV = abs(trend.perWeek)
    val numberText = if (absV >= 10.0) trend.perWeek.toLong().toString()
    else String.format(java.util.Locale.US, "%.1f", trend.perWeek)
    val signed = if (trend.perWeek > 0) "+$numberText" else numberText
    val unitPart = if (unit.isNotBlank()) " $unit" else ""
    return "Тренд: $signed$unitPart/нед"
}
