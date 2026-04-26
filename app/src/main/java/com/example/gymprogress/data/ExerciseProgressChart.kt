package com.example.gymprogress.data

import java.time.LocalDate

/**
 * Одна точка графика прогресса по упражнению: одна тренировка (день) в хронологическом порядке.
 *
 * Все метрики рассчитаны для [representativeEntry] — это «лучший» подход дня по выбранной
 * на UI метрике [ChartMetric].
 */
data class ExerciseProgressChartPoint(
    val sessionNumber: Int,
    val dateStorage: String,
    /** Значение по выбранной метрике (см. [ChartMetric]). */
    val yValue: Double,
    val representativeEntry: WorkoutEntry,
    /** Точка-рекорд: значение [yValue] не меньше всех предыдущих в полной истории упражнения. */
    val isPersonalRecord: Boolean = false,
    /** Оценочный 1RM (кг) представителя дня. */
    val e1rm: Double = 0.0,
    /** Объём представителя (кг = вес × сумма повторов). */
    val volume: Double = 0.0,
    /** Рабочий вес записи (кг). */
    val workingWeight: Double = 0.0,
    /** Балл сессии 0..1000 по выбранной системе скоринга. */
    val score: Int = 0,
)

private fun computeYByMetric(
    metric: ChartMetric,
    e1rm: Double,
    volume: Double,
    workingWeight: Double,
    score: Int,
): Double = when (metric) {
    ChartMetric.E1RM -> e1rm
    ChartMetric.VOLUME -> volume
    ChartMetric.WORKING_WEIGHT -> workingWeight
    ChartMetric.SCORE -> score.toDouble()
}

/**
 * Строит точки графика: группировка по дате, одна точка на день.
 * Представитель дня — запись с максимальным значением выбранной [metric] при той же
 * истории, что и на вкладке «Прогресс»: для каждой кандидатной записи history = все записи
 * упражнения с начала по эту запись, порядок от новой к старой (как take(idx+1).reversed()).
 *
 * Флаг [ExerciseProgressChartPoint.isPersonalRecord] вычисляется по полной истории до фильтра
 * по диапазону: точка считается рекордом, если её значение метрики не меньше всех предыдущих.
 *
 * После расстановки PR применяется фильтр [range]: точки с датой раньше, чем `today − range.days`,
 * отбрасываются. При [ChartRange.ALL] фильтр отсутствует.
 *
 * Дни с нулевой/отрицательной метрикой пропускаются (нет осмысленного значения для графика).
 */
fun buildExerciseProgressChartPoints(
    entries: List<WorkoutEntry>,
    scoringEngine: ScoringEngine,
    goal: TrainingGoal,
    exerciseType: ExerciseType,
    bodyWeightKg: Double?,
    isBodyweightExercise: Boolean,
    metric: ChartMetric = ChartMetric.E1RM,
    range: ChartRange = ChartRange.ALL,
    today: LocalDate = LocalDate.now(),
): List<ExerciseProgressChartPoint> {
    if (entries.isEmpty()) return emptyList()

    fun normalizeStorageDate(e: WorkoutEntry): String {
        val parsed = FormatUtils.parseStorageDate(e.date)
        return if (parsed != null) FormatUtils.toStorageDate(parsed) else e.date
    }

    val listOldestFirst = entries.sortedWith(
        compareBy<WorkoutEntry>({ normalizeStorageDate(it) }, { it.id })
    )

    val indexById = listOldestFirst.withIndex().associate { it.value.id to it.index }
    val distinctDates = listOldestFirst.map { normalizeStorageDate(it) }.distinct()

    data class Candidate(
        val entry: WorkoutEntry,
        val e1rm: Double,
        val volume: Double,
        val workingWeight: Double,
        val score: Int,
    )

    val allPoints = mutableListOf<ExerciseProgressChartPoint>()
    var sessionNumber = 0

    for (dateKey in distinctDates) {
        val dayEntries = listOldestFirst.filter { normalizeStorageDate(it) == dateKey }

        var best: Candidate? = null
        var bestY = Double.NEGATIVE_INFINITY

        for (e in dayEntries) {
            val idx = indexById[e.id] ?: continue
            val history = listOldestFirst.take(idx + 1).asReversed()
            val sessionScore = scoringEngine.calcSessionScore(
                e,
                history,
                goal,
                exerciseType,
                bodyWeightKg,
                isBodyweightExercise,
            )
            val e1rm = SimplifiedScoreCalculator.calcE1RMForEntry(
                e, bodyWeightKg, isBodyweightExercise
            )
            val reps = SimplifiedScoreCalculator.parseReps(e)
            val volume = e.weight * reps.sum()
            val workingWeight = e.weight
            val score = sessionScore.score

            val y = computeYByMetric(metric, e1rm, volume, workingWeight, score)

            val better = when {
                y > bestY -> true
                y < bestY -> false
                else -> best == null || e.id > best.entry.id
            }
            if (better) {
                bestY = y
                best = Candidate(e, e1rm, volume, workingWeight, score)
            }
        }

        val chosen = best ?: continue
        if (bestY <= 0.0) continue

        sessionNumber++
        allPoints.add(
            ExerciseProgressChartPoint(
                sessionNumber = sessionNumber,
                dateStorage = dateKey,
                yValue = bestY,
                representativeEntry = chosen.entry,
                isPersonalRecord = false,
                e1rm = chosen.e1rm,
                volume = chosen.volume,
                workingWeight = chosen.workingWeight,
                score = chosen.score,
            )
        )
    }

    // Running max → отметки PR по полной истории.
    var runningMax = Double.NEGATIVE_INFINITY
    val withPr = allPoints.map { p ->
        val isPr = p.yValue >= runningMax - 1e-9
        if (p.yValue > runningMax) runningMax = p.yValue
        p.copy(isPersonalRecord = isPr)
    }

    val days = range.days ?: return withPr
    val cutoff = today.minusDays(days.toLong())
    return withPr.filter { p ->
        val parsed = FormatUtils.parseStorageDate(p.dateStorage) ?: return@filter true
        !parsed.isBefore(cutoff)
    }
}

