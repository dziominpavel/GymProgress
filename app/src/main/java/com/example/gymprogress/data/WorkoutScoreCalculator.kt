package com.example.gymprogress.data

import kotlin.math.abs

enum class ProgressStatus {
    BETTER, SAME, WORSE, FIRST
}

/** Метрика для прогресса по цели: объём, E1RM или total reps */
enum class ProgressMetricType(val displayName: String) {
    VOLUME("Объём"),
    E1RM("E1RM"),
    TOTAL_REPS("Повторы")
}

/** Упрощённые компоненты для отображения причины */
data class ScoreComponents(
    val metricValue: Double,
    val metricLabel: String,
    val repQuality: Double,
    val fatiguePenalty: Double,
    val totalScore: Int
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

object WorkoutScoreCalculator {

    private const val TREND_SIZE = 3
    private const val MAX_SCORE = 1000
    private const val RECORD_SCORE = 100
    private const val PROGRESS_THRESHOLD_PCT = 5.0

    fun parseReps(entry: WorkoutEntry): List<Int> =
        entry.reps.split(",").mapNotNull { it.trim().toIntOrNull() }

    /** Объём нагрузки (гипертрофия) */
    private fun calcVolume(entry: WorkoutEntry): Double {
        val reps = parseReps(entry)
        return if (reps.isEmpty()) 0.0 else entry.weight * reps.sum()
    }

    /** E1RM по лучшему подходу (формула Epley), для силы. reps > 12 дают нестабильный E1RM — ограничиваем. */
    private fun calcE1RM(entry: WorkoutEntry): Double {
        val reps = parseReps(entry)
        if (reps.isEmpty()) return 0.0
        val bestSet = reps.maxOrNull() ?: return 0.0
        val weight = entry.weight
        if (weight <= 0) return 0.0
        // Epley: E1RM = weight * (1 + reps/30). Для reps > 15 формула менее точна — кэпируем
        val cappedReps = minOf(bestSet, 15)
        return weight * (1.0 + cappedReps / 30.0)
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

    /**
     * Балл сессии: 100 = личный рекорд.
     * yourBest = max(metric в истории БЕЗ текущей) или metric при первой сессии.
     * score = (metric / yourBest) × 100 − fatiguePenalty×10, cap 0–1000.
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

        val (metric, metricType) = calcMetric(entry, goal)
        val repQuality = calcRepQuality(reps, goal)
        val fatiguePenalty = calcFatiguePenalty(reps)

        // Личный рекорд = max(метрика в истории БЕЗ текущей); при первой сессии = metric
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
     * Прогресс в % считается по сырой метрике: (current - avg_last_3) / avg_last_3 × 100.
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
        val (currentMetric, metricType) = calcMetric(current, goal)

        val goalName = goal.displayName
        val typeName = exerciseType.displayName
        val targetStr = "${goal.targetRange.first}–${goal.targetRange.last}"

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

        // Последние 3 сессии БЕЗ текущей (предыдущие по времени)
        val trendEntries = history.drop(1).take(TREND_SIZE)
        if (trendEntries.isEmpty()) {
            val detail = buildDetail(current, previous, currentScore, null, 0.0, currentScore.score.toDouble(), goal, typeName, targetStr)
            return ComparisonResult(ProgressStatus.FIRST, 0.0, "Мало данных для сравнения", detail)
        }

        val trendMetrics = trendEntries.map { calcMetric(it, goal).first }
        val baselineMetric = trendMetrics.average()
        val baselineScores = trendEntries.map { calcSessionScore(it, history, goal, exerciseType).score }
        val baselineScore = baselineScores.average()

        // Прогресс в % по сырой метрике
        val deltaPercent = if (baselineMetric > 0) {
            ((currentMetric - baselineMetric) / baselineMetric) * 100
        } else 0.0

        val status = when {
            deltaPercent >= PROGRESS_THRESHOLD_PCT -> ProgressStatus.BETTER
            deltaPercent <= -PROGRESS_THRESHOLD_PCT -> ProgressStatus.WORSE
            else -> ProgressStatus.SAME
        }

        val reasons = mutableListOf<String>()
        when (metricType) {
            ProgressMetricType.VOLUME -> reasons += "Объём ${if (deltaPercent > 0) "↑" else "↓"}"
            ProgressMetricType.E1RM -> reasons += "E1RM ${if (deltaPercent > 0) "↑" else "↓"}"
            ProgressMetricType.TOTAL_REPS -> reasons += "Повторы ${if (deltaPercent > 0) "↑" else "↓"}"
        }
        if (currentScore.repQuality < 0.5) reasons += "повторы вне $targetStr"
        if (currentScore.fatiguePenalty > 0.05) reasons += "Усталость ↑"

        val prevScore = calcSessionScore(previous, history, goal, exerciseType)
        val baselineComponents = ScoreComponents(
            metricValue = baselineMetric,
            metricLabel = metricType.displayName,
            repQuality = trendEntries.map { calcRepQuality(parseReps(it), goal) }.average(),
            fatiguePenalty = trendEntries.map { calcFatiguePenalty(parseReps(it)) }.average(),
            totalScore = baselineScore.toInt()
        )

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
