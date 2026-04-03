package com.example.gymprogress.data

/**
 * Общий интерфейс движка скоринга.
 * Расширяемость: в будущем можно добавить настройки стиля сессии
 * (обратная пирамида, первый тяжёлый сет и т.д.) через параметр SessionStyle.
 */
interface ScoringEngine {

    /** Балл сессии для одной записи */
    fun calcSessionScore(
        entry: WorkoutEntry,
        history: List<WorkoutEntry>,
        goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
        exerciseType: ExerciseType = ExerciseType.COMPOUND,
        bodyWeightKg: Double? = null,
        isBodyweightExercise: Boolean = false
    ): SessionScore

    /** Сравнение текущей записи с предыдущей + историей */
    fun compare(
        current: WorkoutEntry,
        previous: WorkoutEntry?,
        history: List<WorkoutEntry>,
        goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
        exerciseType: ExerciseType = ExerciseType.COMPOUND,
        bodyWeightKg: Double? = null,
        isBodyweightExercise: Boolean = false
    ): ComparisonResult

    /** Отчёт по группе мышц: сравнение двух последних тренировочных дней */
    fun compareDays(
        muscleGroupName: String,
        allExercises: List<Exercise>,
        allEntries: List<WorkoutEntry>,
        goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
        bodyWeightKg: Double? = null
    ): WorkoutDayReport?

    /** Отчёт по дате */
    fun compareSessionByDate(
        selectedDateStorage: String,
        allExercises: List<Exercise>,
        allEntries: List<WorkoutEntry>,
        goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
        bodyWeightKg: Double? = null
    ): WorkoutDayReport?
}

/**
 * Лучшая сессия по выбранной в настройках системе: упрощённая — максимальный оценочный 1RM (rawMetric),
 * продвинутая — максимальный балл сессии (score). Совпадает с логикой вкладки «Прогресс».
 */
fun selectBestSessionEntry(
    entries: List<WorkoutEntry>,
    scoringEngine: ScoringEngine,
    scoringSystem: ScoringSystem,
    goal: TrainingGoal,
    exerciseType: ExerciseType,
    bodyWeightKg: Double?,
    isBodyweightExercise: Boolean
): WorkoutEntry? {
    if (entries.isEmpty()) return null
    return when (scoringSystem) {
        ScoringSystem.SIMPLIFIED -> entries.maxWithOrNull(
            compareByDescending<WorkoutEntry> { e ->
                scoringEngine.calcSessionScore(
                    e, entries, goal, exerciseType, bodyWeightKg, isBodyweightExercise
                ).rawMetric
            }.thenByDescending { it.date }
                .thenByDescending { it.id }
        )
        ScoringSystem.ADVANCED -> entries.maxWithOrNull(
            compareByDescending<WorkoutEntry> { e ->
                scoringEngine.calcSessionScore(
                    e, entries, goal, exerciseType, bodyWeightKg, isBodyweightExercise
                ).score
            }.thenByDescending { it.date }
                .thenByDescending { it.id }
        )
    }
}
