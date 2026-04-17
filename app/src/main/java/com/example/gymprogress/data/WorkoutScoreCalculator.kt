package com.example.gymprogress.data

import kotlin.math.abs
import kotlin.math.sqrt

enum class ProgressStatus {
    BETTER, SAME, WORSE, FIRST
}

/** Метрика для прогресса по цели: объём, E1RM или total reps */
enum class ProgressMetricType(val displayName: String) {
    VOLUME("Объём"),
    E1RM("E1RM"),
    TOTAL_REPS("Повторы"),
    STIMULUS("Стимул")
}

/** Упрощённые компоненты для отображения причины */
data class ScoreComponents(
    val metricValue: Double,
    val metricLabel: String,
    val repQuality: Double,
    val fatiguePenalty: Double,
    val totalScore: Int,
    val tensionScore: Double? = null,
    val productiveScore: Double? = null
)

data class SessionScore(
    val score: Int,              // 0–1000
    val rawMetric: Double,       // объём, E1RM или totalReps
    val metricType: ProgressMetricType,
    val repQuality: Double,
    val fatiguePenalty: Double,
    val components: ScoreComponents
)

data class ScoreDetail(
    val currentWeight: Double,
    val previousWeight: Double,
    val currentVolume: Double,
    val previousVolume: Double,
    val currentMetric: Double,
    val baselineMetric: Double,
    val currentTotalReps: Int,
    val previousTotalReps: Int,
    val currentSets: Int,
    val previousSets: Int,
    val currentRepQuality: Double,
    val previousRepQuality: Double,
    val currentScore: Int,
    val baselineScore: Double,
    val currentReps: List<Int>,
    val previousReps: List<Int>,
    val goalName: String,
    val exerciseTypeName: String,
    val targetRange: String,
    val metricType: ProgressMetricType,
    val currentComponents: ScoreComponents,
    val baselineComponents: ScoreComponents?
)

data class ExerciseDayScore(
    val exerciseName: String,
    val currentScore: Int?,
    val baselineScore: Double?,
    val deltaPercent: Double,
    val currentEntry: WorkoutEntry?,
    val previousEntry: WorkoutEntry?,
    val status: ProgressStatus,
    val comparisonResult: ComparisonResult?
)

data class WorkoutDayReport(
    val muscleGroupName: String,
    val currentDate: String,
    val previousDate: String?,
    val exercises: List<ExerciseDayScore>,
    val overallScore: Double,
    val previousOverallScore: Double?,
    val overallStatus: ProgressStatus,
    val overallDeltaPercent: Double
)

data class ComparisonResult(
    val status: ProgressStatus,
    val deltaPercent: Double,
    val reason: String,
    val details: ScoreDetail? = null
)

object WorkoutScoreCalculator : ScoringEngine {

    private const val TREND_SIZE = 3
    private const val MAX_SCORE = 1000
    private const val RECORD_SCORE = 100
    private const val PROGRESS_THRESHOLD_PCT = 5.0

    fun parseReps(entry: WorkoutEntry): List<Int> =
        entry.reps.split(",").mapNotNull { it.trim().toIntOrNull() }

    // ── ScoringEngine interface delegates (advanced ignores bodyWeight/isBodyweight) ──

    override fun calcSessionScore(
        entry: WorkoutEntry,
        history: List<WorkoutEntry>,
        goal: TrainingGoal,
        exerciseType: ExerciseType,
        bodyWeightKg: Double?,
        isBodyweightExercise: Boolean
    ): SessionScore = calcSessionScore(entry, history, goal, exerciseType)

    override fun compare(
        current: WorkoutEntry,
        previous: WorkoutEntry?,
        history: List<WorkoutEntry>,
        goal: TrainingGoal,
        exerciseType: ExerciseType,
        bodyWeightKg: Double?,
        isBodyweightExercise: Boolean
    ): ComparisonResult = compare(current, previous, history, goal, exerciseType)

    override fun compareDays(
        muscleGroupName: String,
        allExercises: List<Exercise>,
        allEntries: List<WorkoutEntry>,
        goal: TrainingGoal,
        bodyWeightKg: Double?
    ): WorkoutDayReport? = compareDays(muscleGroupName, allExercises, allEntries, goal)

    override fun compareSessionByDate(
        selectedDateStorage: String,
        allExercises: List<Exercise>,
        allEntries: List<WorkoutEntry>,
        goal: TrainingGoal,
        bodyWeightKg: Double?
    ): WorkoutDayReport? = compareSessionByDate(selectedDateStorage, allExercises, allEntries, goal)

    /** Объём нагрузки (гипертрофия) */
    private fun calcVolume(entry: WorkoutEntry): Double {
        val reps = parseReps(entry)
        return if (reps.isEmpty()) 0.0 else entry.weight * reps.sum()
    }

    /** E1RM по лучшему подходу (гибрид Epley/Brzycki), для силы. */
    private fun calcE1RM(entry: WorkoutEntry): Double {
        val reps = parseReps(entry)
        if (reps.isEmpty()) return 0.0
        val bestSet = reps.maxOrNull() ?: return 0.0
        val weight = entry.weight
        if (weight <= 0) return 0.0
        val cappedReps = minOf(bestSet, 30)
        return when {
            cappedReps == 1 -> weight
            cappedReps <= 10 -> weight * (1.0 + cappedReps / 30.0)
            else -> {
                val brzyckiReps = minOf(cappedReps, 15)
                weight * 36.0 / (37.0 - brzyckiReps)
            }
        }
    }

    /** Total reps (выносливость) */
    private fun calcTotalReps(entry: WorkoutEntry): Double {
        return parseReps(entry).sum().toDouble()
    }

    /** Метрика по цели */
    private fun calcMetric(entry: WorkoutEntry, goal: TrainingGoal): Pair<Double, ProgressMetricType> {
        return when (goal) {
            TrainingGoal.HYPERTROPHY -> calcVolume(entry) to ProgressMetricType.VOLUME
            TrainingGoal.STRENGTH -> calcE1RM(entry) to ProgressMetricType.E1RM
            TrainingGoal.ENDURANCE -> calcVolume(entry) to ProgressMetricType.VOLUME
        }
    }

    /** Качество повторений (0.3..1.0) */
    private fun calcRepQuality(reps: List<Int>, goal: TrainingGoal): Double {
        if (reps.isEmpty()) return 1.0
        return reps.map { r ->
            when (r) {
                in goal.targetRange -> 1.0
                in goal.nearRange -> 0.6
                else -> 0.3
            }
        }.average()
    }

    /** Штраф за просадку повторений по подходам */
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

    // ── Hypertrophy v2.5 helpers ────────────────────────────────────────

    private fun getHypertrophyZoneCoefficient(reps: Int, exerciseType: ExerciseType): Double {
        return when (exerciseType) {
            ExerciseType.COMPOUND -> when (reps) {
                in 5..10 -> 1.00
                in 11..15 -> 0.95
                in 3..4 -> 0.75
                in 16..20 -> 0.80
                else -> 0.50
            }
            ExerciseType.ISOLATION -> when (reps) {
                in 8..15 -> 1.00
                in 6..7 -> 0.90
                in 16..20 -> 0.95
                in 21..30 -> 0.75
                else -> 0.50
            }
        }
    }

    private fun calcHypertrophyStimulusUnits(repsList: List<Int>, exerciseType: ExerciseType): Double {
        return repsList.sumOf { reps -> reps * getHypertrophyZoneCoefficient(reps, exerciseType) }
    }

    private fun calcHypertrophyRepQuality(repsList: List<Int>, exerciseType: ExerciseType): Double {
        if (repsList.isEmpty()) return 1.0
        return repsList.map { getHypertrophyZoneCoefficient(it, exerciseType) }.average()
    }

    private fun calcHypertrophySessionScore(
        entry: WorkoutEntry,
        reps: List<Int>,
        history: List<WorkoutEntry>,
        exerciseType: ExerciseType
    ): SessionScore {
        val currentUnits = calcHypertrophyStimulusUnits(reps, exerciseType)
        val repQuality = calcHypertrophyRepQuality(reps, exerciseType)
        val fatiguePenalty = calcFatiguePenalty(reps)

        val historyExcludingCurrent = history.filter { it.id != entry.id }

        val bestWeight = historyExcludingCurrent.maxOfOrNull { it.weight } ?: entry.weight
        val tensionScore = if (bestWeight > 0) entry.weight / bestWeight else 1.0

        val bestUnits = historyExcludingCurrent.maxOfOrNull {
            calcHypertrophyStimulusUnits(parseReps(it), exerciseType)
        } ?: currentUnits
        val productiveScore = if (bestUnits > 0) sqrt(currentUnits / bestUnits) else 1.0

        val (wT, wP, wR) = when (exerciseType) {
            ExerciseType.COMPOUND -> Triple(0.55, 0.25, 0.20)
            ExerciseType.ISOLATION -> Triple(0.30, 0.45, 0.25)
        }

        val compositeRaw = wT * tensionScore + wP * productiveScore + wR * repQuality
        val rawMetric = compositeRaw * RECORD_SCORE
        val score = (rawMetric - fatiguePenalty * 10).toInt().coerceIn(0, MAX_SCORE)

        val components = ScoreComponents(
            metricValue = rawMetric,
            metricLabel = ProgressMetricType.STIMULUS.displayName,
            repQuality = repQuality,
            fatiguePenalty = fatiguePenalty,
            totalScore = score,
            tensionScore = tensionScore,
            productiveScore = productiveScore
        )

        return SessionScore(score, rawMetric, ProgressMetricType.STIMULUS, repQuality, fatiguePenalty, components)
    }

    /**
     * Guardrail: тяжёлая работа в продуктивной зоне не может быть WORSE.
     * Возвращает (новый статус, причина) или null если guardrail не применяется.
     */
    private fun applyHypertrophyGuardrail(
        current: WorkoutEntry,
        curReps: List<Int>,
        exerciseType: ExerciseType,
        trendEntries: List<WorkoutEntry>
    ): Pair<ProgressStatus, String>? {
        if (curReps.isEmpty() || trendEntries.isEmpty()) return null

        // Rule C: сверхтяжёлые синглы/дабллы (1–3) не гипертрофийный прогресс
        if (curReps.all { it <= 3 }) return null

        val avgPrevWeight = trendEntries.map { it.weight }.average()
        val weightIncrease = if (avgPrevWeight > 0) current.weight / avgPrevWeight else 1.0

        val allInProductiveZone = curReps.all {
            getHypertrophyZoneCoefficient(it, exerciseType) >= 0.75
        }

        val prevTotalReps = trendEntries.map { parseReps(it).sum().toDouble() }.average()
        val curTotalReps = curReps.sum().toDouble()
        val repsDropPercent = if (prevTotalReps > 0) (1.0 - curTotalReps / prevTotalReps) else 0.0

        val avgPrevPenalty = trendEntries.map { calcFatiguePenalty(parseReps(it)) }.average()
        val curPenalty = calcFatiguePenalty(curReps)
        val penaltyIncrease = curPenalty - avgPrevPenalty

        // Rule A: вес вырос ≥5%, повторы в зоне, объём не обвалился, усталость не выросла
        if (weightIncrease >= 1.05 && allInProductiveZone && repsDropPercent <= 0.30 && penaltyIncrease <= 0.03) {
            return ProgressStatus.SAME to "Тяжёлая работа в продуктивной зоне"
        }

        return null
    }

    // ── Session score (public API) ──────────────────────────────────────

    /**
     * Балл сессии.
     * Hypertrophy v2.5: composite = tension × wT + productive × wP + repQuality × wR.
     * Strength / Endurance: legacy metric / yourBest × 100.
     * 100 ≈ личный рекорд по всем компонентам; cap 0–1000.
     */
    fun calcSessionScore(
        entry: WorkoutEntry,
        history: List<WorkoutEntry>,
        goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
        exerciseType: ExerciseType = ExerciseType.COMPOUND
    ): SessionScore {
        val reps = parseReps(entry)
        if (reps.isEmpty()) {
            val empty = ScoreComponents(0.0, "", 0.0, 0.0, 0)
            return SessionScore(0, 0.0, ProgressMetricType.VOLUME, 0.0, 0.0, empty)
        }

        if (goal == TrainingGoal.HYPERTROPHY) {
            return calcHypertrophySessionScore(entry, reps, history, exerciseType)
        }

        val (metric, metricType) = calcMetric(entry, goal)
        val repQuality = calcRepQuality(reps, goal)
        val fatiguePenalty = calcFatiguePenalty(reps)

        val historyExcludingCurrent = history.filter { it.id != entry.id }
        val metricsHistory = historyExcludingCurrent.map { calcMetric(it, goal).first }
        val yourBest = metricsHistory.maxOrNull() ?: metric

        if (yourBest <= 0) {
            val empty = ScoreComponents(metric, metricType.displayName, repQuality, fatiguePenalty, 0)
            return SessionScore(0, metric, metricType, repQuality, fatiguePenalty, empty)
        }

        val rawScore = (metric / yourBest) * RECORD_SCORE
        val score = (rawScore - fatiguePenalty * 10).toInt().coerceIn(0, MAX_SCORE)

        val components = ScoreComponents(
            metricValue = metric,
            metricLabel = metricType.displayName,
            repQuality = repQuality,
            fatiguePenalty = fatiguePenalty,
            totalScore = score
        )

        return SessionScore(score, metric, metricType, repQuality, fatiguePenalty, components)
    }

    /**
     * Сравнение текущей сессии с последними 3 (или 1–2 при малой истории).
     * Hypertrophy v2.5: дельта считается по composite score, применяется guardrail.
     * Strength / Endurance: дельта по сырой метрике (legacy).
     */
    fun compare(
        current: WorkoutEntry,
        previous: WorkoutEntry?,
        history: List<WorkoutEntry>,
        goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
        exerciseType: ExerciseType = ExerciseType.COMPOUND
    ): ComparisonResult {
        val currentScore = calcSessionScore(current, history, goal, exerciseType)
        val curReps = parseReps(current)
        val curVolume = calcVolume(current)
        val isHypertrophy = goal == TrainingGoal.HYPERTROPHY

        val currentMetric = if (isHypertrophy) currentScore.rawMetric else calcMetric(current, goal).first
        val metricType = currentScore.metricType

        val goalName = goal.displayName
        val typeName = exerciseType.displayName
        val targetStr = if (isHypertrophy) {
            when (exerciseType) {
                ExerciseType.COMPOUND -> "5–15"
                ExerciseType.ISOLATION -> "8–20"
            }
        } else {
            "${goal.targetRange.first}–${goal.targetRange.last}"
        }

        if (previous == null) {
            val detail = ScoreDetail(
                currentWeight = current.weight,
                previousWeight = 0.0,
                currentVolume = curVolume,
                previousVolume = 0.0,
                currentMetric = currentMetric,
                baselineMetric = currentMetric,
                currentTotalReps = curReps.sum(),
                previousTotalReps = 0,
                currentSets = curReps.size,
                previousSets = 0,
                currentRepQuality = currentScore.repQuality,
                previousRepQuality = 0.0,
                currentScore = currentScore.score,
                baselineScore = currentScore.score.toDouble(),
                currentReps = curReps,
                previousReps = emptyList(),
                goalName = goalName,
                exerciseTypeName = typeName,
                targetRange = targetStr,
                metricType = metricType,
                currentComponents = currentScore.components,
                baselineComponents = null
            )
            return ComparisonResult(ProgressStatus.FIRST, 0.0, "Первая тренировка", detail)
        }

        val trendEntries = history.drop(1).take(TREND_SIZE)
        if (trendEntries.isEmpty()) {
            val detail = buildDetail(current, previous, currentScore, null, 0.0, currentScore.score.toDouble(), goal, typeName, targetStr)
            return ComparisonResult(ProgressStatus.FIRST, 0.0, "Мало данных для сравнения", detail)
        }

        val trendScores = trendEntries.map { calcSessionScore(it, history, goal, exerciseType) }
        val trendMetrics = if (isHypertrophy) {
            trendScores.map { it.rawMetric }
        } else {
            trendEntries.map { calcMetric(it, goal).first }
        }
        val baselineMetric = trendMetrics.average()
        val baselineScore = trendScores.map { it.score }.average()

        val deltaPercent = if (baselineMetric > 0) {
            ((currentMetric - baselineMetric) / baselineMetric) * 100
        } else 0.0

        var status = when {
            deltaPercent >= PROGRESS_THRESHOLD_PCT -> ProgressStatus.BETTER
            deltaPercent <= -PROGRESS_THRESHOLD_PCT -> ProgressStatus.WORSE
            else -> ProgressStatus.SAME
        }

        val reasons = mutableListOf<String>()

        if (isHypertrophy) {
            val avgTension = trendScores.mapNotNull { it.components.tensionScore }
                .let { if (it.isNotEmpty()) it.average() else null }
            val avgProductive = trendScores.mapNotNull { it.components.productiveScore }
                .let { if (it.isNotEmpty()) it.average() else null }
            val avgQuality = trendScores.map { it.repQuality }.average()

            val curTension = currentScore.components.tensionScore ?: 0.0
            val curProductive = currentScore.components.productiveScore ?: 0.0

            val tensionDelta = if (avgTension != null) curTension - avgTension else 0.0
            val productiveDelta = if (avgProductive != null) curProductive - avgProductive else 0.0
            val qualityDelta = currentScore.repQuality - avgQuality

            val absTension = abs(tensionDelta)
            val absProductive = abs(productiveDelta)
            val absQuality = abs(qualityDelta)
            val maxAbs = maxOf(absTension, absProductive, absQuality)

            when {
                maxAbs < 0.01 -> reasons += "Стимул ${if (deltaPercent >= 0) "↑" else "↓"}"
                maxAbs == absTension -> reasons += "Напряжение ${if (tensionDelta > 0) "↑" else "↓"}"
                maxAbs == absProductive -> reasons += "Продуктивные повторы ${if (productiveDelta > 0) "↑" else "↓"}"
                else -> reasons += "Качество диапазона ${if (qualityDelta > 0) "↑" else "↓"}"
            }

            if (status == ProgressStatus.WORSE) {
                val guardrailResult = applyHypertrophyGuardrail(current, curReps, exerciseType, trendEntries)
                if (guardrailResult != null) {
                    status = guardrailResult.first
                    reasons += guardrailResult.second
                }
            }
        } else {
            when (metricType) {
                ProgressMetricType.VOLUME -> reasons += "Объём ${if (deltaPercent > 0) "↑" else "↓"}"
                ProgressMetricType.E1RM -> reasons += "E1RM ${if (deltaPercent > 0) "↑" else "↓"}"
                ProgressMetricType.TOTAL_REPS -> reasons += "Повторы ${if (deltaPercent > 0) "↑" else "↓"}"
                else -> {}
            }
        }

        if (currentScore.repQuality < 0.5) reasons += "повторы вне $targetStr"
        if (currentScore.fatiguePenalty > 0.05) reasons += "Усталость ↑"

        val prevScore = calcSessionScore(previous, history, goal, exerciseType)
        val baselineComponents = if (isHypertrophy) {
            ScoreComponents(
                metricValue = baselineMetric,
                metricLabel = metricType.displayName,
                repQuality = trendScores.map { it.repQuality }.average(),
                fatiguePenalty = trendScores.map { it.fatiguePenalty }.average(),
                totalScore = baselineScore.toInt(),
                tensionScore = trendScores.mapNotNull { it.components.tensionScore }
                    .let { if (it.isNotEmpty()) it.average() else null },
                productiveScore = trendScores.mapNotNull { it.components.productiveScore }
                    .let { if (it.isNotEmpty()) it.average() else null }
            )
        } else {
            ScoreComponents(
                metricValue = baselineMetric,
                metricLabel = metricType.displayName,
                repQuality = trendEntries.map { calcRepQuality(parseReps(it), goal) }.average(),
                fatiguePenalty = trendEntries.map { calcFatiguePenalty(parseReps(it)) }.average(),
                totalScore = baselineScore.toInt()
            )
        }

        val detail = buildDetail(current, previous, currentScore, prevScore, baselineMetric, baselineScore, goal, typeName, targetStr)
            .copy(baselineComponents = baselineComponents)

        val reasonText = if (reasons.isEmpty()) "Без значимых изменений" else reasons.joinToString(", ")
        return ComparisonResult(status, deltaPercent, reasonText, detail)
    }

    private fun buildDetail(
        current: WorkoutEntry,
        previous: WorkoutEntry,
        currentScore: SessionScore,
        prevScore: SessionScore?,
        baselineMetric: Double,
        baselineScore: Double,
        goal: TrainingGoal,
        typeName: String,
        targetStr: String
    ): ScoreDetail {
        val curReps = parseReps(current)
        val prevReps = parseReps(previous)
        val curVolume = calcVolume(current)
        val prevVolume = calcVolume(previous)
        return ScoreDetail(
            currentWeight = current.weight,
            previousWeight = previous.weight,
            currentVolume = curVolume,
            previousVolume = prevVolume,
            currentMetric = currentScore.rawMetric,
            baselineMetric = baselineMetric,
            currentTotalReps = curReps.sum(),
            previousTotalReps = prevReps.sum(),
            currentSets = curReps.size,
            previousSets = prevReps.size,
            currentRepQuality = currentScore.repQuality,
            previousRepQuality = prevScore?.repQuality ?: 0.0,
            currentScore = currentScore.score,
            baselineScore = baselineScore,
            currentReps = curReps,
            previousReps = prevReps,
            goalName = goal.displayName,
            exerciseTypeName = typeName,
            targetRange = targetStr,
            metricType = currentScore.metricType,
            currentComponents = currentScore.components,
            baselineComponents = null
        )
    }

    fun compareDays(
        muscleGroupName: String,
        allExercises: List<Exercise>,
        allEntries: List<WorkoutEntry>,
        goal: TrainingGoal = TrainingGoal.HYPERTROPHY
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
            val history = allEntries.filter { it.exerciseName == ex.name }
                .sortedWith(compareByDescending<WorkoutEntry> { it.date }.thenByDescending { it.id })
            val curEntry = curDay.filter { it.exerciseName == ex.name }.maxByOrNull { it.id }
            val prevEntry = prevDay.filter { it.exerciseName == ex.name }.maxByOrNull { it.id }
            if (curEntry == null && prevEntry == null) return@mapNotNull null
            val curScore = curEntry?.let { calcSessionScore(it, history, goal, exType).score }
            val prevScore = prevEntry?.let { calcSessionScore(it, history, goal, exType).score }
            val comparison = curEntry?.let { compare(it, prevEntry, history, goal, exType) }
            val st = comparison?.status ?: when {
                curScore == null -> ProgressStatus.WORSE
                prevScore == null -> ProgressStatus.FIRST
                else -> ProgressStatus.SAME
            }
            val deltaPercent = comparison?.deltaPercent ?: 0.0
            val baselineScore = comparison?.details?.baselineScore
            ExerciseDayScore(
                ex.name,
                curScore,
                baselineScore,
                deltaPercent,
                curEntry,
                prevEntry,
                st,
                comparison
            )
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

    fun compareSessionByDate(
        selectedDateStorage: String,
        allExercises: List<Exercise>,
        allEntries: List<WorkoutEntry>,
        goal: TrainingGoal = TrainingGoal.HYPERTROPHY
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
            val history = allEntries.filter { it.exerciseName == exName && it.date <= selectedDateStorage }
                .sortedWith(compareByDescending<WorkoutEntry> { it.date }.thenByDescending { it.id })
            val curEntry = curDay.filter { it.exerciseName == exName }.maxByOrNull { it.id }
            val prevEntry = history
                .filter { it.date < selectedDateStorage }
                .maxWithOrNull(compareBy<WorkoutEntry> { it.date }.thenBy { it.id })
            if (curEntry == null && prevEntry == null) return@mapNotNull null
            val comparison = curEntry?.let { compare(it, prevEntry, history, goal, exType) }
            val curScore = curEntry?.let { calcSessionScore(it, history, goal, exType).score }
            val baselineScore = comparison?.details?.baselineScore
            val st = comparison?.status ?: when {
                curScore == null -> ProgressStatus.WORSE
                prevEntry == null -> ProgressStatus.FIRST
                else -> ProgressStatus.SAME
            }
            val deltaPercent = comparison?.deltaPercent ?: 0.0
            ExerciseDayScore(exName, curScore, baselineScore, deltaPercent, curEntry, prevEntry, st, comparison)
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
