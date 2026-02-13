package com.example.gymprogress.data

data class TrainerSettings(
    val splitType: SplitType = SplitType.FULL_BODY,
    val daysPerWeek: Int = 3,
    val priorityGroups: List<MuscleGroup> = emptyList(),
    val customSplitDays: Map<Int, List<MuscleGroup>> = emptyMap(),
    val includeWarmup: Boolean = true,
    val autoDeload: Boolean = true,
    val deloadIntervalWeeks: Int = 4,
    val progressionType: ProgressionType = ProgressionType.DOUBLE
)

enum class SetType(val displayName: String) {
    WARMUP("Разминка"),
    WORKING("Рабочий")
}

data class SetRecommendation(
    val type: SetType,
    val weight: Double?,
    val targetReps: IntRange,
    val restSeconds: Int
)

data class ExerciseRecommendation(
    val exercise: Exercise,
    val sets: List<SetRecommendation>,
    val note: String?
)

data class WorkoutRecommendation(
    val dayLabel: String,
    val dayIndex: Int,
    val muscleGroups: List<MuscleGroup>,
    val exercises: List<ExerciseRecommendation>,
    val isDeloadWeek: Boolean = false
)
