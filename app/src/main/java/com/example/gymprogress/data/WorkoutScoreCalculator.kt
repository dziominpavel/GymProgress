package com.example.gymprogress.data

import kotlin.math.abs
import kotlin.math.sqrt

enum class ProgressStatus {
    BETTER, SAME, WORSE, FIRST
}

data class SessionScore(
    val score: Double,
    val volumeScore: Double,
    val intensityScore: Double,
    val repQuality: Double,
    val setsBonus: Double,
    val fatiguePenalty: Double
)

data class ScoreDetail(
    val currentWeight: Double,
    val previousWeight: Double,
    val currentVolume: Double,
    val previousVolume: Double,
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
    val targetRange: String
)

data class ComparisonResult(
    val status: ProgressStatus,
    val deltaPercent: Double,
    val reason: String,
    val details: ScoreDetail? = null
)

object WorkoutScoreCalculator {

    private const val WINDOW_SIZE = 20
    private const val TREND_SIZE = 3

    private data class Weights(val wV: Double, val wI: Double, val wR: Double)

    private fun getWeights(goal: TrainingGoal, type: ExerciseType): Weights = when (goal) {
        TrainingGoal.HYPERTROPHY -> when (type) {
            ExerciseType.COMPOUND -> Weights(0.45, 0.25, 0.30)
            ExerciseType.ISOLATION -> Weights(0.35, 0.15, 0.50)
        }
        TrainingGoal.STRENGTH -> when (type) {
            ExerciseType.COMPOUND -> Weights(0.25, 0.45, 0.30)
            ExerciseType.ISOLATION -> Weights(0.20, 0.40, 0.40)
        }
        TrainingGoal.ENDURANCE -> Weights(0.50, 0.10, 0.40)
    }

    private fun parseReps(entry: WorkoutEntry): List<Int> =
        entry.reps.split(",").mapNotNull { it.trim().toIntOrNull() }

    private fun calcVolume(entry: WorkoutEntry): Double {
        val reps = parseReps(entry)
        return entry.weight * reps.sum()
    }

    fun calcSessionScore(
        entry: WorkoutEntry,
        history: List<WorkoutEntry>,
        goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
        exerciseType: ExerciseType = ExerciseType.COMPOUND
    ): SessionScore {
        val reps = parseReps(entry)
        if (reps.isEmpty()) return SessionScore(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

        val w = getWeights(goal, exerciseType)
        val targetRange = goal.targetRange

        val window = history.take(WINDOW_SIZE)

        val totalReps = reps.sum()
        val volume = entry.weight * totalReps

        val allVolumes = window.map { calcVolume(it) } + volume
        val maxVolume = allVolumes.maxOrNull() ?: 1.0

        val allWeights = window.map { it.weight } + entry.weight
        val maxWeight = allWeights.maxOrNull() ?: 1.0

        val volumeScore = if (maxVolume > 0) volume / maxVolume else 0.0
        val intensityScore = if (maxWeight > 0) entry.weight / maxWeight else 0.0

        val repQuality = reps.map { r ->
            when (r) {
                in targetRange -> 1.0
                in goal.nearRange -> 0.6
                else -> 0.3
            }
        }.average()

        val setsBonus = when {
            reps.size >= 4 -> 0.04
            reps.size >= 3 -> 0.02
            else -> 0.0
        }

        val fatiguePenalty = calcFatiguePenalty(reps)

        val raw = w.wV * volumeScore + w.wI * intensityScore + w.wR * repQuality +
                setsBonus - fatiguePenalty
        val score = raw.coerceIn(0.0, 1.0)

        return SessionScore(score, volumeScore, intensityScore, repQuality, setsBonus, fatiguePenalty)
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
            combined <= 0.20 -> 0.03
            combined <= 0.30 -> 0.07
            combined <= 0.40 -> 0.12
            else -> 0.15
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

        val goalName = goal.displayName
        val typeName = exerciseType.displayName
        val targetStr = "${goal.targetRange.first}–${goal.targetRange.last}"

        if (previous == null) {
            val detail = ScoreDetail(
                currentWeight = current.weight,
                previousWeight = 0.0,
                currentVolume = curVolume,
                previousVolume = 0.0,
                currentTotalReps = curReps.sum(),
                previousTotalReps = 0,
                currentSets = curReps.size,
                previousSets = 0,
                currentRepQuality = currentScore.repQuality,
                previousRepQuality = 0.0,
                currentSetsBonus = currentScore.setsBonus,
                previousSetsBonus = 0.0,
                currentFatiguePenalty = currentScore.fatiguePenalty,
                previousFatiguePenalty = 0.0,
                currentScore = currentScore.score,
                previousScore = 0.0,
                currentReps = curReps,
                previousReps = emptyList(),
                goalName = goalName,
                exerciseTypeName = typeName,
                targetRange = targetStr
            )
            return ComparisonResult(ProgressStatus.FIRST, 0.0, "Первая тренировка", detail)
        }

        val trendEntries = history.drop(1).take(TREND_SIZE)
        val trendScore = if (trendEntries.isNotEmpty()) {
            trendEntries.map { calcSessionScore(it, history, goal, exerciseType).score }.average()
        } else {
            calcSessionScore(previous, history, goal, exerciseType).score
        }

        val prevScore = calcSessionScore(previous, history, goal, exerciseType)
        val prevReps = parseReps(previous)
        val prevVolume = previous.weight * prevReps.sum()

        val delta = currentScore.score - trendScore
        val pct = if (trendScore > 0) (delta / trendScore) * 100 else 0.0

        val status = when {
            delta >= 0.03 -> ProgressStatus.BETTER
            delta <= -0.03 -> ProgressStatus.WORSE
            else -> ProgressStatus.SAME
        }

        val reasons = mutableListOf<String>()
        val volDelta = currentScore.volumeScore - prevScore.volumeScore
        val intDelta = currentScore.intensityScore - prevScore.intensityScore
        val repDelta = currentScore.repQuality - prevScore.repQuality

        val absVol = abs(volDelta)
        val absInt = abs(intDelta)
        val absRep = abs(repDelta)
        val maxComponent = maxOf(absVol, absInt, absRep)

        when (maxComponent) {
            absVol -> if (absVol > 0.01)
                reasons += "Объём ${if (volDelta > 0) "↑" else "↓"}"
            absInt -> if (absInt > 0.01)
                reasons += "Вес ${if (intDelta > 0) "↑" else "↓"}"
            absRep -> if (absRep > 0.01)
                reasons += "Качество повторов ${if (repDelta > 0) "↑" else "↓"}"
        }
        if (currentScore.repQuality < 0.6) {
            reasons += "повторы вне $targetStr"
        }
        if (currentScore.setsBonus > prevScore.setsBonus) {
            reasons += "Больше подходов ↑"
        }
        if (currentScore.fatiguePenalty > prevScore.fatiguePenalty + 0.02) {
            reasons += "Усталость ↑"
        }

        val detail = ScoreDetail(
            currentWeight = current.weight,
            previousWeight = previous.weight,
            currentVolume = curVolume,
            previousVolume = prevVolume,
            currentTotalReps = curReps.sum(),
            previousTotalReps = prevReps.sum(),
            currentSets = curReps.size,
            previousSets = prevReps.size,
            currentRepQuality = currentScore.repQuality,
            previousRepQuality = prevScore.repQuality,
            currentSetsBonus = currentScore.setsBonus,
            previousSetsBonus = prevScore.setsBonus,
            currentFatiguePenalty = currentScore.fatiguePenalty,
            previousFatiguePenalty = prevScore.fatiguePenalty,
            currentScore = currentScore.score,
            previousScore = prevScore.score,
            currentReps = curReps,
            previousReps = prevReps,
            goalName = goalName,
            exerciseTypeName = typeName,
            targetRange = targetStr
        )

        val reasonText = if (reasons.isEmpty()) "Без значимых изменений" else reasons.joinToString(", ")
        return ComparisonResult(status, pct, reasonText, detail)
    }
}
