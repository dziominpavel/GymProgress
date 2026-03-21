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
