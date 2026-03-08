package com.example.gymprogress.data

/**
 * Результат выполненного подхода в активной тренировке.
 * Используется при сохранении завершённой тренировки в журнал.
 */
data class CompletedSet(
    val exerciseName: String,
    val weight: Double,
    val reps: Int,
    val setType: SetType
)
