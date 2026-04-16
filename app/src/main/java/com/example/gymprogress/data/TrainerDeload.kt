package com.example.gymprogress.data

import java.time.temporal.ChronoUnit

/**
 * Логика авто-делоада для [TrainerRecommendationEngine].
 */
internal fun TrainerRecommendationEngine.shouldDeload(
    settings: TrainerSettings,
    history: List<WorkoutEntry>
): Boolean {
    if (!settings.autoDeload) return false
    if (history.isEmpty()) return false

    val dates = history.map { parseTrainerDate(it.date) }.distinct().sorted()
    if (dates.size < 2) return false

    val firstDate = dates.first()
    val lastDate = dates.last()
    val weeksSinceStart = ChronoUnit.WEEKS.between(firstDate, lastDate)

    val interval = settings.deloadIntervalWeeks
    if (interval <= 0) return false

    return weeksSinceStart > 0 && weeksSinceStart % interval == 0L
}
