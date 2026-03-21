package com.example.gymprogress.data

import kotlin.math.abs
import kotlin.math.max

/**
 * Упрощённая методика оценки прогресса.
 *
 * Основная метрика: оценочный одноразовый максимум (Estimated 1RM) в кг.
 * Формула Epley: E1RM = weight × (1 + reps / 30)
 *
 * ## Лестница усилия (Effort Ladder)
 * RIR не вводится пользователем. Допущение: последний рабочий подход ближе к отказу,
 * предыдущие — с большим запасом. Коэффициент отражает «уверенность» в оценке:
 * ранние подходы (далеко от отказа) получают пониженную оценку,
 * последний подход (близко к отказу) — полную.
 *
 * - Последний подход: коэфф. 1.00 (полная уверенность)
 * - Предпоследний:   коэфф. 0.97
 * - Третий с конца:  коэфф. 0.94
 * - Четвёртый+:      коэфф. 0.91
 *
 * adjusted_E1RM = raw_E1RM × коэфф.
 * Итоговый E1RM сессии = максимум среди всех подходов.
 *
 * ## Расширяемость
 * Коэффициенты лестницы вынесены в EffortProfile. В будущем пользователь сможет
 * выбрать стиль сессии (обратная пирамида, первый тяжёлый сет и т.д.),
 * и каждый стиль задаст свой EffortProfile.
 *
 * ## BW-упражнения
 * В БД для BW-упражнений entry.weight уже содержит полный вес (bodyWeight + addedWeight),
 * поэтому effectiveWeight = entry.weight (без повторного сложения).
 * bodyWeightKg нужен только для проверки что вес тела указан; без него → E1RM = 0.
 *
 * ## Краевые случаи
 * - 1 подход → E1RM по единственному подходу, RIR = 1
 * - Очень много повторений (>30) → формула Epley нестабильна, капаем на 30
 * - Вес = 0, не BW → E1RM = 0
 * - Несколько записей одного упражнения на одну дату → каждая оценивается отдельно,
 *   для прогресса берётся лучшая за дату
 */
object SimplifiedScoreCalculator : ScoringEngine {

    private const val TREND_SIZE = 3
    private const val PROGRESS_THRESHOLD_PCT = 5.0
    private const val MAX_REPS_FOR_E1RM = 30

    /**
     * Профиль усилия: задаёт коэффициент корректировки E1RM для каждого подхода
     * в зависимости от его позиции (с конца).
     * positionFromEnd: 0 = последний, 1 = предпоследний, ...
     */
    data class EffortProfile(
        val name: String,
        val description: String,
        private val coefficients: List<Double>
    ) {
        fun coefficientForPosition(positionFromEnd: Int): Double {
            return if (positionFromEnd < coefficients.size) coefficients[positionFromEnd]
            else coefficients.last()
        }
    }

    /** Стандартный профиль: классическая прямая пирамида / ровные подходы */
    val STANDARD_EFFORT = EffortProfile(
        name = "Стандартный",
        description = "Последний подход ≈ 1 RIR, предыдущие с большим запасом",
        coefficients = listOf(1.00, 0.97, 0.94, 0.91)
    )

    // ── E1RM расчёт ──────────────────────────────────────────────────────

    /** Epley E1RM для одного подхода */
    private fun epleyE1RM(weight: Double, reps: Int): Double {
        if (weight <= 0 || reps <= 0) return 0.0
        val cappedReps = reps.coerceAtMost(MAX_REPS_FOR_E1RM)
        return if (cappedReps == 1) weight
        else weight * (1.0 + cappedReps / 30.0)
    }

    /**
     * Скорректированный E1RM по подходу с учётом лестницы усилия.
     * positionFromEnd: 0 = последний подход в сессии.
     * Коэффициент умножается на raw E1RM: ранние подходы получают
     * пониженную оценку (меньше уверенности в близости к отказу).
     */
    private fun adjustedE1RM(
        weight: Double,
        reps: Int,
        positionFromEnd: Int,
        profile: EffortProfile = STANDARD_EFFORT
    ): Double {
        val raw = epleyE1RM(weight, reps)
        val coeff = profile.coefficientForPosition(positionFromEnd)
        return raw * coeff
    }

    /**
     * E1RM сессии: максимум среди скорректированных E1RM всех подходов.
     * Для BW-упражнений weight уже должен быть effectiveWeight.
     */
    fun calcE1RMForEntry(
        entry: WorkoutEntry,
        bodyWeightKg: Double? = null,
        isBodyweightExercise: Boolean = false,
        profile: EffortProfile = STANDARD_EFFORT
    ): Double {
        val reps = parseReps(entry)
        if (reps.isEmpty()) return 0.0
        val effectiveWeight = effectiveWeight(entry, bodyWeightKg, isBodyweightExercise)
        if (effectiveWeight <= 0) return 0.0

        return reps.mapIndexed { index, rep ->
            val positionFromEnd = reps.size - 1 - index
            adjustedE1RM(effectiveWeight, rep, positionFromEnd, profile)
        }.maxOrNull() ?: 0.0
    }

    private fun effectiveWeight(
        entry: WorkoutEntry,
        bodyWeightKg: Double?,
        isBodyweightExercise: Boolean
    ): Double {
        return if (isBodyweightExercise) {
            if (bodyWeightKg == null) return 0.0
            entry.weight
        } else {
            entry.weight
        }
    }

    fun parseReps(entry: WorkoutEntry): List<Int> =
        entry.reps.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }

    private fun calcFatiguePenalty(reps: List<Int>): Double {
        if (reps.size < 2) return 0.0
        val first = reps.first().toDouble()
        if (first <= 0) return 0.0
        val dropRate = 1.0 - reps.last() / first
        return when {
            dropRate <= 0.20 -> 0.00
            dropRate <= 0.35 -> 0.03
            dropRate <= 0.50 -> 0.06
            else -> 0.10
        }
    }

    // ── ScoringEngine implementation ─────────────────────────────────────

    override fun calcSessionScore(
        entry: WorkoutEntry,
        history: List<WorkoutEntry>,
        goal: TrainingGoal,
        exerciseType: ExerciseType,
        bodyWeightKg: Double?,
        isBodyweightExercise: Boolean
    ): SessionScore {
        val reps = parseReps(entry)
        if (reps.isEmpty()) {
            val empty = ScoreComponents(0.0, "", 0.0, 0.0, 0)
            return SessionScore(0, 0.0, ProgressMetricType.E1RM, 0.0, 0.0, empty)
        }

        val e1rm = calcE1RMForEntry(entry, bodyWeightKg, isBodyweightExercise)
        val fatiguePenalty = calcFatiguePenalty(reps)

        val historyExcludingCurrent = history.filter { it.id != entry.id }
        val bestE1RM = historyExcludingCurrent.maxOfOrNull {
            calcE1RMForEntry(it, bodyWeightKg, isBodyweightExercise)
        } ?: e1rm
        val bestForScore = max(bestE1RM, e1rm)

        val rawScore = if (bestForScore > 0) (e1rm / bestForScore) * 100.0 else 0.0
        val score = (rawScore - fatiguePenalty * 10).toInt().coerceIn(0, 1000)

        val components = ScoreComponents(
            metricValue = e1rm,
            metricLabel = "Оценочный 1RM",
            repQuality = 1.0,
            fatiguePenalty = fatiguePenalty,
            totalScore = score
        )

        return SessionScore(score, e1rm, ProgressMetricType.E1RM, 1.0, fatiguePenalty, components)
    }

    override fun compare(
        current: WorkoutEntry,
        previous: WorkoutEntry?,
        history: List<WorkoutEntry>,
        goal: TrainingGoal,
        exerciseType: ExerciseType,
        bodyWeightKg: Double?,
        isBodyweightExercise: Boolean
    ): ComparisonResult {
        val currentScore = calcSessionScore(current, history, goal, exerciseType, bodyWeightKg, isBodyweightExercise)
        val curReps = parseReps(current)
        val curE1RM = currentScore.rawMetric
        val curVolume = effectiveWeight(current, bodyWeightKg, isBodyweightExercise) * curReps.sum()

        val goalName = goal.displayName
        val typeName = exerciseType.displayName
        val targetStr = "${goal.targetRange.first}–${goal.targetRange.last}"

        if (previous == null) {
            val detail = ScoreDetail(
                currentWeight = current.weight,
                previousWeight = 0.0,
                currentVolume = curVolume,
                previousVolume = 0.0,
                currentMetric = curE1RM,
                baselineMetric = curE1RM,
                currentTotalReps = curReps.sum(),
                previousTotalReps = 0,
                currentSets = curReps.size,
                previousSets = 0,
                currentRepQuality = 1.0,
                previousRepQuality = 0.0,
                currentScore = currentScore.score,
                baselineScore = currentScore.score.toDouble(),
                currentReps = curReps,
                previousReps = emptyList(),
                goalName = goalName,
                exerciseTypeName = typeName,
                targetRange = targetStr,
                metricType = ProgressMetricType.E1RM,
                currentComponents = currentScore.components,
                baselineComponents = null
            )
            return ComparisonResult(ProgressStatus.FIRST, 0.0, "Первая тренировка", detail)
        }

        val trendEntries = history.filter { it.id != current.id }
            .sortedWith(compareByDescending<WorkoutEntry> { it.date }.thenByDescending { it.id })
            .take(TREND_SIZE)

        if (trendEntries.isEmpty()) {
            val detail = buildSimplifiedDetail(current, previous, currentScore, null, curE1RM, currentScore.score.toDouble(), goal, typeName, targetStr, bodyWeightKg, isBodyweightExercise)
            return ComparisonResult(ProgressStatus.FIRST, 0.0, "Мало данных для сравнения", detail)
        }

        val trendE1RMs = trendEntries.map { calcE1RMForEntry(it, bodyWeightKg, isBodyweightExercise) }
        val baselineE1RM = trendE1RMs.average()
        val trendScores = trendEntries.map { calcSessionScore(it, history, goal, exerciseType, bodyWeightKg, isBodyweightExercise) }
        val baselineScore = trendScores.map { it.score }.average()

        val deltaPercent = if (baselineE1RM > 0) {
            ((curE1RM - baselineE1RM) / baselineE1RM) * 100
        } else 0.0

        val status = when {
            deltaPercent >= PROGRESS_THRESHOLD_PCT -> ProgressStatus.BETTER
            deltaPercent <= -PROGRESS_THRESHOLD_PCT -> ProgressStatus.WORSE
            else -> ProgressStatus.SAME
        }

        val reasons = mutableListOf<String>()
        val prevE1RM = calcE1RMForEntry(previous, bodyWeightKg, isBodyweightExercise)
        val weightDelta = current.weight - previous.weight

        when {
            abs(weightDelta) > 0.01 && abs(deltaPercent) >= 1.0 ->
                reasons += "1RM: ${FormatUtils.formatWeight(curE1RM)} кг ${if (deltaPercent > 0) "↑" else "↓"}"
            abs(deltaPercent) >= 1.0 ->
                reasons += "1RM ${if (deltaPercent > 0) "↑" else "↓"}"
            else ->
                reasons += "1RM стабилен"
        }

        if (weightDelta > 0.01) reasons += "+${FormatUtils.formatWeight(weightDelta)} кг"
        else if (weightDelta < -0.01) reasons += "${FormatUtils.formatWeight(weightDelta)} кг"

        val prevScore = calcSessionScore(previous, history, goal, exerciseType, bodyWeightKg, isBodyweightExercise)
        val baselineComponents = ScoreComponents(
            metricValue = baselineE1RM,
            metricLabel = "Оценочный 1RM",
            repQuality = 1.0,
            fatiguePenalty = trendScores.map { it.fatiguePenalty }.average(),
            totalScore = baselineScore.toInt()
        )

        val detail = buildSimplifiedDetail(current, previous, currentScore, prevScore, baselineE1RM, baselineScore, goal, typeName, targetStr, bodyWeightKg, isBodyweightExercise)
            .copy(baselineComponents = baselineComponents)

        val reasonText = if (reasons.isEmpty()) "Без значимых изменений" else reasons.joinToString(", ")
        return ComparisonResult(status, deltaPercent, reasonText, detail)
    }

    private fun buildSimplifiedDetail(
        current: WorkoutEntry,
        previous: WorkoutEntry,
        currentScore: SessionScore,
        prevScore: SessionScore?,
        baselineMetric: Double,
        baselineScore: Double,
        goal: TrainingGoal,
        typeName: String,
        targetStr: String,
        bodyWeightKg: Double?,
        isBodyweightExercise: Boolean
    ): ScoreDetail {
        val curReps = parseReps(current)
        val prevReps = parseReps(previous)
        val curWeight = effectiveWeight(current, bodyWeightKg, isBodyweightExercise)
        val prevWeight = effectiveWeight(previous, bodyWeightKg, isBodyweightExercise)
        return ScoreDetail(
            currentWeight = current.weight,
            previousWeight = previous.weight,
            currentVolume = curWeight * curReps.sum(),
            previousVolume = prevWeight * prevReps.sum(),
            currentMetric = currentScore.rawMetric,
            baselineMetric = baselineMetric,
            currentTotalReps = curReps.sum(),
            previousTotalReps = prevReps.sum(),
            currentSets = curReps.size,
            previousSets = prevReps.size,
            currentRepQuality = 1.0,
            previousRepQuality = prevScore?.repQuality ?: 0.0,
            currentScore = currentScore.score,
            baselineScore = baselineScore,
            currentReps = curReps,
            previousReps = prevReps,
            goalName = goalName(goal),
            exerciseTypeName = typeName,
            targetRange = targetStr,
            metricType = ProgressMetricType.E1RM,
            currentComponents = currentScore.components,
            baselineComponents = null
        )
    }

    private fun goalName(goal: TrainingGoal): String = "${goal.displayName} (1RM)"

    // ── Day-level reports ────────────────────────────────────────────────

    override fun compareDays(
        muscleGroupName: String,
        allExercises: List<Exercise>,
        allEntries: List<WorkoutEntry>,
        goal: TrainingGoal,
        bodyWeightKg: Double?
    ): WorkoutDayReport? {
        val mgExNames = allExercises.filter { it.muscleGroup == muscleGroupName }.map { it.name }.toSet()
        val dayGroups = allEntries.filter { it.exerciseName in mgExNames }
            .groupBy { it.date.take(10) }.toSortedMap(reverseOrder())
        val dates = dayGroups.keys.toList()
        if (dates.isEmpty()) return null
        val curDate = dates[0]
        val prevDate = dates.getOrNull(1)
        val curDay = dayGroups[curDate] ?: emptyList()
        val prevDay = prevDate?.let { dayGroups[it] } ?: emptyList()

        val exerciseScores = allExercises.filter { it.muscleGroup == muscleGroupName }.mapNotNull { ex ->
            val exType = ExerciseType.entries.find { it.name == ex.exerciseType } ?: ExerciseType.COMPOUND
            val isBw = ex.isBodyweight
            val history = allEntries.filter { it.exerciseName == ex.name }
                .sortedWith(compareByDescending<WorkoutEntry> { it.date }.thenByDescending { it.id })
            val curEntry = curDay.filter { it.exerciseName == ex.name }.maxByOrNull { it.id }
            val prevEntry = prevDay.filter { it.exerciseName == ex.name }.maxByOrNull { it.id }
            if (curEntry == null && prevEntry == null) return@mapNotNull null
            val curScore = curEntry?.let { calcSessionScore(it, history, goal, exType, bodyWeightKg, isBw).score }
            val prevScore = prevEntry?.let { calcSessionScore(it, history, goal, exType, bodyWeightKg, isBw).score }
            val comparison = curEntry?.let { compare(it, prevEntry, history, goal, exType, bodyWeightKg, isBw) }
            val st = comparison?.status ?: when {
                curScore == null -> ProgressStatus.WORSE
                prevScore == null -> ProgressStatus.FIRST
                else -> ProgressStatus.SAME
            }
            val deltaPercent = comparison?.deltaPercent ?: 0.0
            val baselineScoreVal = comparison?.details?.baselineScore
            ExerciseDayScore(ex.name, curScore, baselineScoreVal, deltaPercent, curEntry, prevEntry, st, comparison)
        }
        if (exerciseScores.isEmpty()) return null

        val curScores = exerciseScores.mapNotNull { it.currentScore?.toDouble() }
        val prevScores = exerciseScores.mapNotNull { it.baselineScore }
        val overall = if (curScores.isNotEmpty()) curScores.average() else 0.0
        val prevOverall = if (prevScores.isNotEmpty()) prevScores.average() else null
        val delta = prevOverall?.let { overall - it } ?: 0.0
        val pct = prevOverall?.let { if (it > 0) (delta / it) * 100 else 0.0 } ?: 0.0
        val st = when {
            prevOverall == null -> ProgressStatus.FIRST
            pct >= PROGRESS_THRESHOLD_PCT -> ProgressStatus.BETTER
            pct <= -PROGRESS_THRESHOLD_PCT -> ProgressStatus.WORSE
            else -> ProgressStatus.SAME
        }
        return WorkoutDayReport(muscleGroupName, curDate, prevDate, exerciseScores, overall, prevOverall, st, pct)
    }

    override fun compareSessionByDate(
        selectedDateStorage: String,
        allExercises: List<Exercise>,
        allEntries: List<WorkoutEntry>,
        goal: TrainingGoal,
        bodyWeightKg: Double?
    ): WorkoutDayReport? {
        val dayGroups = allEntries.groupBy { it.date }.toSortedMap(reverseOrder())
        val dates = dayGroups.keys.toList()
        if (selectedDateStorage !in dates) return null
        val idx = dates.indexOf(selectedDateStorage)
        val prevDate = dates.getOrNull(idx + 1)
        val curDay = dayGroups[selectedDateStorage] ?: emptyList()

        val exerciseNamesOnCurDay = curDay.sortedBy { it.id }.map { it.exerciseName }.distinct()
        val exerciseScores = exerciseNamesOnCurDay.mapNotNull { exName ->
            val ex = allExercises.find { it.name == exName }
            val exType = ex?.let { ExerciseType.entries.find { t -> t.name == it.exerciseType } ?: ExerciseType.COMPOUND }
                ?: ExerciseType.COMPOUND
            val isBw = ex?.isBodyweight ?: false
            val history = allEntries.filter { it.exerciseName == exName && it.date <= selectedDateStorage }
                .sortedWith(compareByDescending<WorkoutEntry> { it.date }.thenByDescending { it.id })
            val curEntry = curDay.filter { it.exerciseName == exName }.maxByOrNull { it.id }
            val prevEntry = history
                .filter { it.date < selectedDateStorage }
                .maxWithOrNull(compareBy<WorkoutEntry> { it.date }.thenBy { it.id })
            if (curEntry == null && prevEntry == null) return@mapNotNull null
            val comparison = curEntry?.let { compare(it, prevEntry, history, goal, exType, bodyWeightKg, isBw) }
            val curScore = curEntry?.let { calcSessionScore(it, history, goal, exType, bodyWeightKg, isBw).score }
            val baselineScoreVal = comparison?.details?.baselineScore
            val st = comparison?.status ?: when {
                curScore == null -> ProgressStatus.WORSE
                prevEntry == null -> ProgressStatus.FIRST
                else -> ProgressStatus.SAME
            }
            val deltaPercent = comparison?.deltaPercent ?: 0.0
            ExerciseDayScore(exName, curScore, baselineScoreVal, deltaPercent, curEntry, prevEntry, st, comparison)
        }
        if (exerciseScores.isEmpty()) return null

        val curScores = exerciseScores.mapNotNull { it.currentScore?.toDouble() }
        val prevScores = exerciseScores.mapNotNull { it.baselineScore }
        val overall = if (curScores.isNotEmpty()) curScores.average() else 0.0
        val prevOverall = if (prevScores.isNotEmpty()) prevScores.average() else null
        val delta = prevOverall?.let { overall - it } ?: 0.0
        val pct = prevOverall?.let { if (it > 0) (delta / it) * 100 else 0.0 } ?: 0.0
        val st = when {
            prevOverall == null -> ProgressStatus.FIRST
            pct >= PROGRESS_THRESHOLD_PCT -> ProgressStatus.BETTER
            pct <= -PROGRESS_THRESHOLD_PCT -> ProgressStatus.WORSE
            else -> ProgressStatus.SAME
        }
        return WorkoutDayReport("Тренировка", selectedDateStorage, prevDate, exerciseScores, overall, prevOverall, st, pct)
    }
}
