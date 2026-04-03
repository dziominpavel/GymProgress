package com.example.gymprogress.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymprogress.data.ExerciseProgressChartPoint
import com.example.gymprogress.data.ExerciseType
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.ScoringEngine
import com.example.gymprogress.data.ScoringSystem
import com.example.gymprogress.data.TrainingGoal
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.data.buildExerciseProgressChartPoints
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.Volt
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max

private data class ChartLayout(
    val nodeOffsets: List<Pair<Offset, ExerciseProgressChartPoint>>,
    val yAxisMin: Double,
    val yAxisMax: Double,
)

private const val Y_AXIS_LABEL_STEP = 10.0

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
 * Границы оси для отрисовки и подписей: целые значения, шаг 10 (кг или баллы), все точки внутри,
 * размах не меньше одного шага.
 */
private fun chartDisplayYAxisRange(points: List<ExerciseProgressChartPoint>): Pair<Double, Double> {
    val dataMin = points.minOf { it.yValue }
    val dataMax = points.maxOf { it.yValue }
    val (rawMin, rawMax) = paddedRawYRange(dataMin, dataMax)
    var yLo = floor(rawMin / Y_AXIS_LABEL_STEP) * Y_AXIS_LABEL_STEP
    var yHi = ceil(rawMax / Y_AXIS_LABEL_STEP) * Y_AXIS_LABEL_STEP
    if (yHi <= yLo) yHi = yLo + Y_AXIS_LABEL_STEP
    while (yLo > dataMin) yLo -= Y_AXIS_LABEL_STEP
    while (yHi < dataMax) yHi += Y_AXIS_LABEL_STEP
    while (yHi - yLo < Y_AXIS_LABEL_STEP) yHi += Y_AXIS_LABEL_STEP
    return yLo to yHi
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
    ) {
        buildExerciseProgressChartPoints(
            entries = entries,
            scoringEngine = scoringEngine,
            scoringSystem = scoringSystem,
            goal = trainingGoal,
            exerciseType = exerciseType,
            bodyWeightKg = bodyWeightKg,
            isBodyweightExercise = isBodyweightExercise,
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
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
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
            Text(
                text = if (isSimplified) {
                    "Ось Y: оценочный 1RM (кг), масштаб по данным. Ось X: номер тренировки."
                } else {
                    "Ось Y: оценка, масштаб по данным. Ось X: номер тренировки."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    val (yAxisMin, yAxisMax) = remember(points) { chartDisplayYAxisRange(points) }
                    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    val dotInnerColor = MaterialTheme.colorScheme.surface
                    val labelColWidth = 56.dp
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
                                text = yAxisMax.toInt().toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = yAxisMin.toInt().toString(),
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
                                        color = Volt,
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                                    )
                                }

                                val dotR = 6.dp.toPx()
                                for ((center, _) in layout.nodeOffsets) {
                                    drawCircle(
                                        color = Volt,
                                        radius = dotR,
                                        center = center,
                                    )
                                    drawCircle(
                                        color = dotInnerColor,
                                        radius = dotR * 0.45f,
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
                    text = FormatUtils.formatDate(p.dateStorage),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = if (isSimplified) {
                        "Оценочный 1RM: ${FormatUtils.formatTwoDecimals(p.yValue)} кг"
                    } else {
                        "Оценка: ${FormatUtils.formatTwoDecimals(p.yValue)}"
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { tappedPoint = null }) {
                    Text("OK")
                }
            },
        )
    }
}
