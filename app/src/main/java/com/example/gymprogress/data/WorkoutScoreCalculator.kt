package com.example.gymprogress.data

import kotlin.math.abs
import kotlin.math.sqrt

enum class ProgressStatus {
    BETTER, SAME, WORSE, FIRST
}

// Покомпонентные баллы для отображения в UI
data class ScoreComponents(
    val intensityPoints: Double,   // вклад интенсивности (вес штанги)
    val effVolumePoints: Double,   // вклад эффективного объёма
    val repQualityPoints: Double,  // вклад качества повторений
    val prBonus: Double,           // бонус за новый максимальный вес
    val setsAdjust: Double,
    val fatiguePenalty: Double,
    val repTrendPenalty: Double,
    val totalScore: Double         // итоговый балл
)

data class SessionScore(
    val score: Double,
    val rawScore: Double,
    val intensityScore: Double,
    val effVolumeScore: Double,
    val repQuality: Double,
    val prBonus: Double,
    val setsAdjust: Double,
    val fatiguePenalty: Double,
    val repTrendPenalty: Double,
    val components: ScoreComponents
)

data class ScoreDetail(
    val currentWeight: Double,
    val previousWeight: Double,
    val currentVolume: Double,
    val previousVolume: Double,
    val currentEffVolume: Double,
    val previousEffVolume: Double,
    val currentTotalReps: Int,
    val previousTotalReps: Int,
    val currentSets: Int,
    val previousSets: Int,
    val currentRepQuality: Double,
    val previousRepQuality: Double,
    val currentSetsBonus: Double,
    val previousSetsBonus: Double,
    val currentFatiguePenalty: Double,
    val previousFatiguePenalty: Double,
    val currentScore: Double,
    val previousScore: Double,
    val currentReps: List<Int>,
    val previousReps: List<Int>,
    val goalName: String,
    val exerciseTypeName: String,
    val targetRange: String,
    val currentComponents: ScoreComponents,
    val previousComponents: ScoreComponents?,
    val trendScore: Double = 0.0,
    val trendComponents: ScoreComponents? = null,
    val currentRawScore: Double = 0.0
)

data class ExerciseDayScore(
    val exerciseName: String,
    val currentScore: Double?,
    val previousScore: Double?,
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

    private const val WINDOW_SIZE = 5
    private const val TREND_SIZE = 3

    // Веса компонентов в итоговом балле (сумма = 1.0, бонусы/штрафы сверху)
    data class Weights(val wI: Double, val wEV: Double, val wR: Double)

    fun getWeights(goal: TrainingGoal, type: ExerciseType): Weights = when (goal) {
        TrainingGoal.HYPERTROPHY -> when (type) {
            // Гипертрофия: интенсивность (вес) важнее всего, затем эффективный объём, затем качество
            ExerciseType.COMPOUND -> Weights(wI = 0.45, wEV = 0.35, wR = 0.20)
            ExerciseType.ISOLATION -> Weights(wI = 0.30, wEV = 0.35, wR = 0.35)
        }
        TrainingGoal.STRENGTH -> when (type) {
            // Сила: максимальный вес критичен
            ExerciseType.COMPOUND -> Weights(wI = 0.60, wEV = 0.20, wR = 0.20)
            ExerciseType.ISOLATION -> Weights(wI = 0.50, wEV = 0.25, wR = 0.25)
        }
        // Выносливость: объём и качество повторений важнее веса
        TrainingGoal.ENDURANCE -> Weights(wI = 0.15, wEV = 0.55, wR = 0.30)
    }

    fun parseReps(entry: WorkoutEntry): List<Int> =
        entry.reps.split(",").mapNotNull { it.trim().toIntOrNull() }

    // Эффективный объём = вес × сумма повторений, взвешенных по качеству
    private fun calcEffectiveVolume(entry: WorkoutEntry, goal: TrainingGoal): Double {
        val reps = parseReps(entry)
        val targetRange = goal.targetRange
        val qualityWeightedReps = reps.sumOf { r ->
            val q = when (r) {
                in targetRange -> 1.0
                in goal.nearRange -> 0.7
                else -> 0.3
            }
            r * q
        }
        return entry.weight * qualityWeightedReps
    }

    fun calcSessionScore(
        entry: WorkoutEntry,
        history: List<WorkoutEntry>,
        goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
        exerciseType: ExerciseType = ExerciseType.COMPOUND
    ): SessionScore {
        val w = getWeights(goal, exerciseType)
        val reps = parseReps(entry)
        val emptyComponents = ScoreComponents(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        if (reps.isEmpty()) return SessionScore(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, emptyComponents)

        val targetRange = goal.targetRange
        val window = history.take(WINDOW_SIZE)

        // --- Intensity Score: вес относительно среднего за последние WINDOW_SIZE тренировок ---
        val windowWeights = window.map { it.weight }
        val avgWeight = if (windowWeights.isNotEmpty()) windowWeights.average() else entry.weight
        val intensityScore = entry.weight / avgWeight.coerceAtLeast(0.001)

        // --- Effective Volume Score: эффективный объём относительно среднего ---
        val effVolume = calcEffectiveVolume(entry, goal)
        val windowEffVolumes = window.map { calcEffectiveVolume(it, goal) }
        val avgEffVolume = if (windowEffVolumes.isNotEmpty()) windowEffVolumes.average() else effVolume
        val effVolumeScore = effVolume / avgEffVolume.coerceAtLeast(0.001)

        // --- Rep Quality: среднее качество попадания в диапазон ---
        val repQuality = reps.map { r ->
            when (r) {
                in targetRange -> 1.0
                in goal.nearRange -> 0.6
                else -> 0.2
            }
        }.average()
        // Нормализация качества по среднему историческому
        val windowRepQualities = window.map { histEntry ->
            val hReps = parseReps(histEntry)
            if (hReps.isEmpty()) 1.0
            else hReps.map { r ->
                when (r) { in targetRange -> 1.0; in goal.nearRange -> 0.6; else -> 0.2 }
            }.average()
        }
        val avgRepQuality = if (windowRepQualities.isNotEmpty()) windowRepQualities.average() else repQuality
        val repQualityScore = repQuality / avgRepQuality.coerceAtLeast(0.001)

        // --- PR Bonus: бонус за строгий рекорд веса (только при реальном увеличении) ---
        val bestWeightHistory = if (windowWeights.isNotEmpty()) windowWeights.maxOrNull() ?: 0.0 else 0.0
        val isPR = entry.weight > bestWeightHistory
        val prBonus = if (isPR) 0.06 else 0.0

        // --- Sets Adjustment: 3-5 подходов = оптимум, >6 или <2 = штраф ---
        val setsAdjust = calcSetsAdjustment(reps.size, goal)

        // --- Fatigue Penalty: резкий спад (10,6,4) ---
        val fatiguePenalty = calcFatiguePenalty(reps)

        // --- Rep Trend Penalty: сэндбэгинг (7,8,9 хуже чем 9,8,7) ---
        val repTrendPenalty = calcRepTrendPenalty(reps)

        // --- Итоговый балл (нормализован по среднему окна, 1.0 = обычная тренировка) ---
        val intensityPoints = w.wI * intensityScore
        val effVolumePoints = w.wEV * effVolumeScore
        val repQualityPoints = w.wR * repQualityScore

        val raw = intensityPoints + effVolumePoints + repQualityPoints + prBonus + setsAdjust - fatiguePenalty - repTrendPenalty
        val score = raw.coerceAtLeast(0.0)

        val components = ScoreComponents(
            intensityPoints = intensityPoints,
            effVolumePoints = effVolumePoints,
            repQualityPoints = repQualityPoints,
            prBonus = prBonus,
            setsAdjust = setsAdjust,
            fatiguePenalty = fatiguePenalty,
            repTrendPenalty = repTrendPenalty,
            totalScore = score
        )

        return SessionScore(score, raw, intensityScore, effVolumeScore, repQuality, prBonus, setsAdjust, fatiguePenalty, repTrendPenalty, components)
    }

    // Цель-зависимая корректировка: 3-5 подходов оптимально, >6 = мусорный объём
    private fun calcSetsAdjustment(setsCount: Int, goal: TrainingGoal): Double = when (goal) {
        TrainingGoal.HYPERTROPHY -> when {
            setsCount == 1    -> -0.04
            setsCount == 2    -> -0.01
            setsCount in 3..5 ->  0.02
            setsCount == 6    ->  0.00
            else              -> -0.03
        }
        TrainingGoal.STRENGTH -> when {
            setsCount <= 1    -> -0.03
            setsCount == 2    ->  0.00
            setsCount in 3..5 ->  0.02
            else              -> -0.02
        }
        TrainingGoal.ENDURANCE -> when {
            setsCount == 1    -> -0.04
            setsCount in 2..4 ->  0.02
            setsCount in 5..6 ->  0.01
            else              -> -0.01
        }
    }

    // Сэндбэгинг: линейная регрессия по сетам, положительный наклон = восходящий (плохо)
    private fun calcRepTrendPenalty(reps: List<Int>): Double {
        if (reps.size < 3) return 0.0
        val n = reps.size
        val meanIdx = (n - 1) / 2.0
        val meanReps = reps.average()
        var cov = 0.0; var varIdx = 0.0
        for (i in reps.indices) {
            cov += (i - meanIdx) * (reps[i] - meanReps)
            varIdx += (i - meanIdx) * (i - meanIdx)
        }
        val slope = if (varIdx > 0) cov / varIdx else 0.0
        return when {
            slope > 2.0 -> 0.08
            slope > 1.0 -> 0.05
            slope > 0.3 -> 0.02
            else        -> 0.0
        }
    }

    private fun calcFatiguePenalty(reps: List<Int>): Double {
        if (reps.size < 2) return 0.0

        val mean = reps.average()
        if (mean == 0.0) return 0.0

        val variance = reps.map { (it - mean) * (it - mean) }.average()
        val cv = sqrt(variance) / mean

        val dropRate = if (reps.first() > 0)
            1.0 - reps.last().toDouble() / reps.first() else 0.0

        val combined = (cv + maxOf(dropRate, 0.0)) / 2.0

        return when {
            combined <= 0.10 -> 0.00
            combined <= 0.20 -> 0.02
            combined <= 0.30 -> 0.05
            combined <= 0.40 -> 0.09
            else -> 0.12
        }
    }

    fun compare(
        current: WorkoutEntry,
        previous: WorkoutEntry?,
        history: List<WorkoutEntry>,
        goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
        exerciseType: ExerciseType = ExerciseType.COMPOUND
    ): ComparisonResult {
        val currentScore = calcSessionScore(current, history, goal, exerciseType)
        val curReps = parseReps(current)
        val curVolume = current.weight * curReps.sum()
        val curEffVolume = calcEffectiveVolume(current, goal)

        val goalName = goal.displayName
        val typeName = exerciseType.displayName
        val targetStr = "${goal.targetRange.first}–${goal.targetRange.last}"

        if (previous == null) {
            val detail = ScoreDetail(
                currentWeight = current.weight,
                previousWeight = 0.0,
                currentVolume = curVolume,
                previousVolume = 0.0,
                currentEffVolume = curEffVolume,
                previousEffVolume = 0.0,
                currentTotalReps = curReps.sum(),
                previousTotalReps = 0,
                currentSets = curReps.size,
                previousSets = 0,
                currentRepQuality = currentScore.repQuality,
                previousRepQuality = 0.0,
                currentSetsBonus = currentScore.setsAdjust,
                previousSetsBonus = 0.0,
                currentFatiguePenalty = currentScore.fatiguePenalty,
                previousFatiguePenalty = 0.0,
                currentScore = currentScore.score,
                previousScore = 0.0,
                currentReps = curReps,
                previousReps = emptyList(),
                goalName = goalName,
                exerciseTypeName = typeName,
                targetRange = targetStr,
                currentComponents = currentScore.components,
                previousComponents = null
            )
            return ComparisonResult(ProgressStatus.FIRST, 0.0, "Первая тренировка", detail)
        }

        val trendEntries = history.drop(1).take(TREND_SIZE)
        val trendSessionScores = if (trendEntries.isNotEmpty()) {
            trendEntries.map { calcSessionScore(it, history, goal, exerciseType) }
        } else {
            listOf(calcSessionScore(previous, history, goal, exerciseType))
        }
        val trendScore = trendSessionScores.map { it.score }.average()
        val trendRawScore = trendSessionScores.map { it.rawScore }.average()
        val trendComponents = ScoreComponents(
            intensityPoints  = trendSessionScores.map { it.components.intensityPoints }.average(),
            effVolumePoints  = trendSessionScores.map { it.components.effVolumePoints }.average(),
            repQualityPoints = trendSessionScores.map { it.components.repQualityPoints }.average(),
            prBonus          = trendSessionScores.map { it.components.prBonus }.average(),
            setsAdjust       = trendSessionScores.map { it.components.setsAdjust }.average(),
            fatiguePenalty   = trendSessionScores.map { it.components.fatiguePenalty }.average(),
            repTrendPenalty  = trendSessionScores.map { it.components.repTrendPenalty }.average(),
            totalScore       = trendRawScore
        )

        val prevScore = calcSessionScore(previous, history, goal, exerciseType)
        val prevReps = parseReps(previous)
        val prevVolume = previous.weight * prevReps.sum()
        val prevEffVolume = calcEffectiveVolume(previous, goal)

        val delta = currentScore.rawScore - trendRawScore
        val pct = if (trendRawScore > 0) (delta / trendRawScore) * 100 else 0.0

        val status = when {
            delta >= 0.025 -> ProgressStatus.BETTER
            delta <= -0.025 -> ProgressStatus.WORSE
            else -> ProgressStatus.SAME
        }

        val reasons = mutableListOf<String>()
        val intDelta = currentScore.intensityScore - prevScore.intensityScore
        val evDelta = currentScore.effVolumeScore - prevScore.effVolumeScore
        val repDelta = currentScore.repQuality - prevScore.repQuality

        if (currentScore.prBonus > 0) reasons += "Новый рекорд веса 🏆"
        if (abs(intDelta) > 0.01) reasons += "Вес ${if (intDelta > 0) "↑" else "↓"}"
        if (abs(evDelta) > 0.01) reasons += "Эфф. объём ${if (evDelta > 0) "↑" else "↓"}"
        if (abs(repDelta) > 0.05) reasons += "Качество повторов ${if (repDelta > 0) "↑" else "↓"}"
        if (currentScore.repQuality < 0.5) reasons += "повторы вне ${targetStr}"
        if (currentScore.fatiguePenalty > prevScore.fatiguePenalty + 0.02) reasons += "Усталость ↑"
        if (currentScore.repTrendPenalty > 0.03) reasons += "Сэндбэгинг (повт. растут)"
        if (currentScore.setsAdjust < -0.01) reasons += "Неоптимальное кол-во подходов"

        val detail = ScoreDetail(
            currentWeight = current.weight,
            previousWeight = previous.weight,
            currentVolume = curVolume,
            previousVolume = prevVolume,
            currentEffVolume = curEffVolume,
            previousEffVolume = prevEffVolume,
            currentTotalReps = curReps.sum(),
            previousTotalReps = prevReps.sum(),
            currentSets = curReps.size,
            previousSets = prevReps.size,
            currentRepQuality = currentScore.repQuality,
            previousRepQuality = prevScore.repQuality,
            currentSetsBonus = currentScore.setsAdjust,
            previousSetsBonus = prevScore.setsAdjust,
            currentFatiguePenalty = currentScore.fatiguePenalty,
            previousFatiguePenalty = prevScore.fatiguePenalty,
            currentScore = currentScore.score,
            previousScore = prevScore.score,
            currentReps = curReps,
            previousReps = prevReps,
            goalName = goalName,
            exerciseTypeName = typeName,
            targetRange = targetStr,
            currentComponents = currentScore.components,
            previousComponents = prevScore.components,
            trendScore = trendRawScore,
            trendComponents = trendComponents,
            currentRawScore = currentScore.rawScore
        )

        val reasonText = if (reasons.isEmpty()) "Без значимых изменений" else reasons.joinToString(", ")
        return ComparisonResult(status, pct, reasonText, detail)
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
        val curDate = dates[0]; val prevDate = dates.getOrNull(1)
        val curDay = dayGroups[curDate] ?: emptyList()
        val prevDay = prevDate?.let { dayGroups[it] } ?: emptyList()

        val exerciseScores = allExercises.filter { it.muscleGroup == muscleGroupName }.mapNotNull { ex ->
            val exType = ExerciseType.entries.find { it.name == ex.exerciseType } ?: ExerciseType.COMPOUND
            val history = allEntries.filter { it.exerciseName == ex.name }
            val curEntry = curDay.filter { it.exerciseName == ex.name }.maxByOrNull { it.id }
            val prevEntry = prevDay.filter { it.exerciseName == ex.name }.maxByOrNull { it.id }
            if (curEntry == null && prevEntry == null) return@mapNotNull null
            val curScore = curEntry?.let { calcSessionScore(it, history, goal, exType).score }
            val prevScore = prevEntry?.let { calcSessionScore(it, history, goal, exType).score }
            val st = when {
                curScore == null -> ProgressStatus.WORSE
                prevScore == null -> ProgressStatus.FIRST
                curScore - prevScore >= 0.025 -> ProgressStatus.BETTER
                curScore - prevScore <= -0.025 -> ProgressStatus.WORSE
                else -> ProgressStatus.SAME
            }
            val deltaPercent = when {
                curScore != null && prevScore != null && prevScore > 0 ->
                    ((curScore - prevScore) / prevScore) * 100
                else -> 0.0
            }
            val comparison = curEntry?.let { compare(it, prevEntry, history, goal, exType) }
            ExerciseDayScore(ex.name, curScore, prevScore, deltaPercent, curEntry, prevEntry, st, comparison)
        }
        if (exerciseScores.isEmpty()) return null
        val curScores = exerciseScores.mapNotNull { it.currentScore }
        val prevScores = exerciseScores.mapNotNull { it.previousScore }
        val overall = if (curScores.isNotEmpty()) curScores.average() else 0.0
        val prevOverall = if (prevScores.isNotEmpty()) prevScores.average() else null
        val delta = prevOverall?.let { overall - it } ?: 0.0
        val pct = prevOverall?.let { if (it > 0) (delta / it) * 100 else 0.0 } ?: 0.0
        val st = when {
            prevOverall == null -> ProgressStatus.FIRST
            delta >= 0.025 -> ProgressStatus.BETTER
            delta <= -0.025 -> ProgressStatus.WORSE
            else -> ProgressStatus.SAME
        }
        return WorkoutDayReport(muscleGroupName, curDate, prevDate, exerciseScores, overall, prevOverall, st, pct)
    }
}
