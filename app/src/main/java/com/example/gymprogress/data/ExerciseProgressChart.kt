package com.example.gymprogress.data

/**
 * Одна точка графика прогресса по упражнению: одна тренировка (день) в хронологическом порядке.
 */
data class ExerciseProgressChartPoint(
    val sessionNumber: Int,
    val dateStorage: String,
    /** Упрощённый режим: оценочный 1RM (кг). Продвинутый: балл сессии. */
    val yValue: Double,
    val representativeEntry: WorkoutEntry,
)

/**
 * Строит точки графика: группировка по дате, одна точка на день.
 * Представитель дня — запись с максимальной метрикой (как [selectBestSessionEntry]) при той же
 * истории, что и на вкладке «Прогресс»: для каждой кандидатной записи history = все записи
 * упражнения с начала по эту запись, порядок от новой к старой (как take(idx+1).reversed()).
 *
 * Дни с нулевой/отрицательной метрикой пропускаются (нет осмысленного значения для графика).
 */
fun buildExerciseProgressChartPoints(
    entries: List<WorkoutEntry>,
    scoringEngine: ScoringEngine,
    scoringSystem: ScoringSystem,
    goal: TrainingGoal,
    exerciseType: ExerciseType,
    bodyWeightKg: Double?,
    isBodyweightExercise: Boolean,
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

    val result = mutableListOf<ExerciseProgressChartPoint>()
    var sessionNumber = 0

    for (dateKey in distinctDates) {
        val dayEntries = listOldestFirst.filter { normalizeStorageDate(it) == dateKey }

        var bestEntry: WorkoutEntry? = null
        var bestMetric = Double.NEGATIVE_INFINITY

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
            val metric = when (scoringSystem) {
                ScoringSystem.SIMPLIFIED -> sessionScore.rawMetric
                ScoringSystem.ADVANCED -> sessionScore.score.toDouble()
            }
            val better = when {
                metric > bestMetric -> true
                metric < bestMetric -> false
                else -> bestEntry == null || e.id > bestEntry.id
            }
            if (better) {
                bestMetric = metric
                bestEntry = e
            }
        }

        val chosen = bestEntry ?: continue
        if (bestMetric <= 0.0) continue

        sessionNumber++
        result.add(
            ExerciseProgressChartPoint(
                sessionNumber = sessionNumber,
                dateStorage = dateKey,
                yValue = bestMetric,
                representativeEntry = chosen,
            )
        )
    }

    return result
}
