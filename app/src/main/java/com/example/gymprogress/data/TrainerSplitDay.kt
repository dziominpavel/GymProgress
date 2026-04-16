package com.example.gymprogress.data

import java.time.LocalDate

/**
 * Логика дней сплита и ротации сессий для [TrainerRecommendationEngine].
 * Вынесено из основного файла без изменения поведения.
 */
internal fun TrainerRecommendationEngine.getTotalDays(settings: TrainerSettings): Int {
    return when (settings.splitType) {
        SplitType.FULL_BODY -> 1
        SplitType.UPPER_LOWER -> 2
        SplitType.PUSH_PULL_LEGS -> 3
        SplitType.CUSTOM -> settings.customSplitDays.size.coerceAtLeast(1)
    }
}

internal fun TrainerRecommendationEngine.getMuscleGroupsForDay(
    settings: TrainerSettings,
    dayIndex: Int
): List<MuscleGroup> {
    return when (settings.splitType) {
        SplitType.FULL_BODY -> MuscleGroup.entries.toList()
        SplitType.UPPER_LOWER -> {
            if (dayIndex % 2 == 0) {
                listOf(
                    MuscleGroup.CHEST, MuscleGroup.BACK,
                    MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS
                )
            } else {
                listOf(MuscleGroup.LEGS, MuscleGroup.ABS)
            }
        }
        SplitType.PUSH_PULL_LEGS -> {
            when (dayIndex % 3) {
                0 -> listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
                1 -> listOf(MuscleGroup.BACK, MuscleGroup.BICEPS)
                else -> listOf(MuscleGroup.LEGS, MuscleGroup.ABS)
            }
        }
        SplitType.CUSTOM -> {
            settings.customSplitDays[dayIndex]
                ?: MuscleGroup.entries.toList()
        }
    }
}

internal fun TrainerRecommendationEngine.getDayLabel(settings: TrainerSettings, dayIndex: Int): String {
    return when (settings.splitType) {
        SplitType.FULL_BODY -> "Full Body"
        SplitType.UPPER_LOWER -> if (dayIndex % 2 == 0) "Upper (Верх)" else "Lower (Низ)"
        SplitType.PUSH_PULL_LEGS -> when (dayIndex % 3) {
            0 -> "Push (Жим)"
            1 -> "Pull (Тяга)"
            else -> "Legs (Ноги)"
        }
        SplitType.CUSTOM -> "День ${dayIndex + 1}"
    }
}

internal fun TrainerRecommendationEngine.guessDayIndexFromExercises(
    settings: TrainerSettings,
    exerciseNames: Set<String>,
    exercises: List<Exercise> = emptyList()
): Int {
    if (exerciseNames.isEmpty()) return 0

    val usedGroups = exercises
        .filter { it.name in exerciseNames }
        .map { it.muscleGroup }
        .toSet()

    if (usedGroups.isEmpty()) return 0

    val totalDays = getTotalDays(settings)

    var bestDay = 0
    var bestOverlap = 0

    for (dayIndex in 0 until totalDays) {
        val dayGroups = getMuscleGroupsForDay(settings, dayIndex).map { it.name }.toSet()
        val overlap = usedGroups.intersect(dayGroups).size
        if (overlap > bestOverlap) {
            bestOverlap = overlap
            bestDay = dayIndex
        }
    }

    return bestDay
}

internal fun TrainerRecommendationEngine.determineNextDayIndex(
    settings: TrainerSettings,
    history: List<WorkoutEntry>,
    exercises: List<Exercise> = emptyList()
): Int {
    if (history.isEmpty()) return 0

    val totalDays = getTotalDays(settings)
    if (totalDays == 1) return 0

    val rotationHistory = historyForRotation(history)
    if (rotationHistory.isEmpty()) return 0

    val lastDate = rotationHistory.maxOfOrNull { parseTrainerDate(it.date) } ?: return 0
    val lastExerciseNames = rotationHistory
        .filter { it.date == FormatUtils.toStorageDate(lastDate) }
        .map { it.exerciseName }
        .toSet()

    val lastDayIndex = guessDayIndexFromExercises(settings, lastExerciseNames, exercises)
    return (lastDayIndex + 1) % totalDays
}

/**
 * История, по которой считаем «последний завершённый день» для сплита.
 */
internal fun TrainerRecommendationEngine.historyForRotation(history: List<WorkoutEntry>): List<WorkoutEntry> {
    if (history.isEmpty()) return emptyList()
    val today = LocalDate.now()
    val past = history.filter { parseTrainerDate(it.date).isBefore(today) }
    return if (past.isNotEmpty()) past else history
}

internal fun TrainerRecommendationEngine.parseTrainerDate(dateString: String): LocalDate {
    return FormatUtils.parseStorageDate(dateString) ?: LocalDate.now()
}
