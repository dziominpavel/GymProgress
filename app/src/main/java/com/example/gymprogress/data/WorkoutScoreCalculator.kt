package com.example.gymprogress.data

enum class ProgressStatus {
    BETTER, SAME, WORSE, FIRST
}

data class SessionScore(
    val score: Double,
    val volumeScore: Double,
    val intensityScore: Double,
    val repQuality: Double,
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
    val currentFatiguePenalty: Double,
    val previousFatiguePenalty: Double,
    val currentScore: Double,
    val previousScore: Double,
    val currentReps: List<Int>,
    val previousReps: List<Int>
)

data class ComparisonResult(
    val status: ProgressStatus,
    val deltaPercent: Double,
    val reason: String,
    val details: ScoreDetail? = null
)

object WorkoutScoreCalculator {

    private const val WV = 0.45
    private const val WI = 0.25
    private const val WR = 0.30

    private val TARGET_RANGE = 8..12

    fun calcSessionScore(
        entry: WorkoutEntry,
        history: List<WorkoutEntry>
    ): SessionScore {
        val reps = entry.reps.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (reps.isEmpty()) return SessionScore(0.0, 0.0, 0.0, 0.0, 0.0)

        val totalReps = reps.sum()
        val volume = entry.weight * totalReps

        val allVolumes = history.map { e ->
            val r = e.reps.split(",").mapNotNull { it.trim().toIntOrNull() }
            e.weight * r.sum()
        } + volume
        val maxVolume = allVolumes.maxOrNull() ?: 1.0

        val allWeights = history.map { it.weight } + entry.weight
        val maxWeight = allWeights.maxOrNull() ?: 1.0

        val volumeScore = if (maxVolume > 0) volume / maxVolume else 0.0
        val intensityScore = if (maxWeight > 0) entry.weight / maxWeight else 0.0

        val repQuality = reps.map { r ->
            when (r) {
                in TARGET_RANGE -> 1.0
                in (TARGET_RANGE.first - 2) until TARGET_RANGE.first,
                in (TARGET_RANGE.last + 1)..(TARGET_RANGE.last + 3) -> 0.6
                else -> 0.3
            }
        }.average()

        val fatiguePenalty = if (reps.size >= 2 && reps.first() > 0) {
            val dropRate = 1.0 - reps.last().toDouble() / reps.first()
            when {
                dropRate <= 0.20 -> 0.00
                dropRate <= 0.35 -> 0.05
                dropRate <= 0.50 -> 0.10
                else -> 0.15
            }
        } else 0.0

        val raw = WV * volumeScore + WI * intensityScore + WR * repQuality - fatiguePenalty
        val score = raw.coerceIn(0.0, 1.0)

        return SessionScore(score, volumeScore, intensityScore, repQuality, fatiguePenalty)
    }

    fun compare(
        current: WorkoutEntry,
        previous: WorkoutEntry?,
        history: List<WorkoutEntry>
    ): ComparisonResult {
        val currentScore = calcSessionScore(current, history)
        val curReps = current.reps.split(",").mapNotNull { it.trim().toIntOrNull() }
        val curVolume = current.weight * curReps.sum()

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
                currentFatiguePenalty = currentScore.fatiguePenalty,
                previousFatiguePenalty = 0.0,
                currentScore = currentScore.score,
                previousScore = 0.0,
                currentReps = curReps,
                previousReps = emptyList()
            )
            return ComparisonResult(ProgressStatus.FIRST, 0.0, "Первая тренировка", detail)
        }

        val prevScore = calcSessionScore(previous, history)
        val prevReps = previous.reps.split(",").mapNotNull { it.trim().toIntOrNull() }
        val prevVolume = previous.weight * prevReps.sum()

        val delta = currentScore.score - prevScore.score
        val pct = if (prevScore.score > 0) (delta / prevScore.score) * 100 else 0.0

        val status = when {
            delta >= 0.03 -> ProgressStatus.BETTER
            delta <= -0.03 -> ProgressStatus.WORSE
            else -> ProgressStatus.SAME
        }

        val reasons = mutableListOf<String>()
        val volDelta = currentScore.volumeScore - prevScore.volumeScore
        val intDelta = currentScore.intensityScore - prevScore.intensityScore
        val repDelta = currentScore.repQuality - prevScore.repQuality

        val absVol = kotlin.math.abs(volDelta)
        val absInt = kotlin.math.abs(intDelta)
        val absRep = kotlin.math.abs(repDelta)
        val maxComponent = maxOf(absVol, absInt, absRep)

        when {
            maxComponent == absVol && absVol > 0.01 ->
                reasons += "Объём ${if (volDelta > 0) "↑" else "↓"}"
            maxComponent == absInt && absInt > 0.01 ->
                reasons += "Вес ${if (intDelta > 0) "↑" else "↓"}"
            maxComponent == absRep && absRep > 0.01 ->
                reasons += "Качество повторов ${if (repDelta > 0) "↑" else "↓"}"
        }
        if (currentScore.repQuality < 0.6) {
            reasons += "повторы вне 8–12"
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
            currentFatiguePenalty = currentScore.fatiguePenalty,
            previousFatiguePenalty = prevScore.fatiguePenalty,
            currentScore = currentScore.score,
            previousScore = prevScore.score,
            currentReps = curReps,
            previousReps = prevReps
        )

        val reasonText = if (reasons.isEmpty()) "Без значимых изменений" else reasons.joinToString(", ")
        return ComparisonResult(status, pct, reasonText, detail)
    }
}
