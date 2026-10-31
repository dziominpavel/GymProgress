package com.example.gymprogress.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Состояние активной тренировки, которое переживает пересоздание Activity
 * и восстановление процесса. Хранится в `rememberSaveable` через JSON-схему ниже.
 */
data class ActiveWorkoutUiState(
    val recommendation: WorkoutRecommendation,
    val workoutStartTime: Long,
    val currentExerciseIndex: Int = 0,
    val currentSetIndex: Int = 0,
    val isResting: Boolean = false,
    val restTimeLeft: Int = 0,
    val completedSets: List<CompletedSet> = emptyList()
)

val ActiveWorkoutJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
data class ActiveWorkoutStateDto(
    val dayLabel: String,
    val dayIndex: Int,
    val muscleGroups: List<String>,
    val isDeloadWeek: Boolean,
    val missingGroups: List<String>,
    val exercises: List<ActiveWorkoutExerciseDto>,
    val workoutStartTime: Long,
    val currentExerciseIndex: Int,
    val currentSetIndex: Int,
    val isResting: Boolean,
    val restTimeLeft: Int,
    val completedSets: List<ActiveWorkoutCompletedSetDto>
)

@Serializable
data class ActiveWorkoutExerciseDto(
    val id: Long,
    val clientId: String,
    val name: String,
    val muscleGroup: String,
    val exerciseType: String,
    val isBodyweight: Boolean,
    val isArchived: Boolean,
    val note: String? = null,
    val advice: String? = null,
    val sets: List<ActiveWorkoutSetDto>
)

@Serializable
data class ActiveWorkoutSetDto(
    val type: String,
    val weight: Double? = null,
    val targetFirst: Int,
    val targetLast: Int,
    val restSeconds: Int
)

@Serializable
data class ActiveWorkoutCompletedSetDto(
    val exerciseName: String,
    val weight: Double,
    val reps: Int,
    val setType: String
)

fun ActiveWorkoutUiState.toDto(): ActiveWorkoutStateDto = ActiveWorkoutStateDto(
    dayLabel = recommendation.dayLabel,
    dayIndex = recommendation.dayIndex,
    muscleGroups = recommendation.muscleGroups.map { it.name },
    isDeloadWeek = recommendation.isDeloadWeek,
    missingGroups = recommendation.missingGroups.map { it.name },
    exercises = recommendation.exercises.map { rec ->
        ActiveWorkoutExerciseDto(
            id = rec.exercise.id,
            clientId = rec.exercise.clientId,
            name = rec.exercise.name,
            muscleGroup = rec.exercise.muscleGroup,
            exerciseType = rec.exercise.exerciseType,
            isBodyweight = rec.exercise.isBodyweight,
            isArchived = rec.exercise.isArchived,
            note = rec.note,
            advice = rec.advice,
            sets = rec.sets.map { set ->
                ActiveWorkoutSetDto(
                    type = set.type.name,
                    weight = set.weight,
                    targetFirst = set.targetReps.first,
                    targetLast = set.targetReps.last,
                    restSeconds = set.restSeconds
                )
            }
        )
    },
    workoutStartTime = workoutStartTime,
    currentExerciseIndex = currentExerciseIndex,
    currentSetIndex = currentSetIndex,
    isResting = isResting,
    restTimeLeft = restTimeLeft,
    completedSets = completedSets.map { completed ->
        ActiveWorkoutCompletedSetDto(
            exerciseName = completed.exerciseName,
            weight = completed.weight,
            reps = completed.reps,
            setType = completed.setType.name
        )
    }
)

fun ActiveWorkoutStateDto.toUiState(): ActiveWorkoutUiState {
    val restoredExercises = exercises.map { dto ->
        val exercise = Exercise(
            id = dto.id,
            name = dto.name,
            muscleGroup = dto.muscleGroup,
            exerciseType = dto.exerciseType,
            isBodyweight = dto.isBodyweight,
            isArchived = dto.isArchived,
            clientId = dto.clientId,
            nameKey = FormatUtils.normalizeExerciseNameKey(dto.name)
        )
        ExerciseRecommendation(
            exercise = exercise,
            sets = dto.sets.map { set ->
                SetRecommendation(
                    type = SetType.entries.find { it.name == set.type } ?: SetType.WORKING,
                    weight = set.weight,
                    targetReps = set.targetFirst..set.targetLast,
                    restSeconds = set.restSeconds
                )
            },
            note = dto.note,
            advice = dto.advice
        )
    }

    return ActiveWorkoutUiState(
        recommendation = WorkoutRecommendation(
            dayLabel = dayLabel,
            dayIndex = dayIndex,
            muscleGroups = muscleGroups.mapNotNull { name ->
                MuscleGroup.entries.find { it.name == name }
            },
            exercises = restoredExercises,
            isDeloadWeek = isDeloadWeek,
            missingGroups = missingGroups.mapNotNull { name ->
                MuscleGroup.entries.find { it.name == name }
            }
        ),
        workoutStartTime = workoutStartTime,
        currentExerciseIndex = currentExerciseIndex,
        currentSetIndex = currentSetIndex,
        isResting = isResting,
        restTimeLeft = restTimeLeft,
        completedSets = completedSets.map { dto ->
            CompletedSet(
                exerciseName = dto.exerciseName,
                weight = dto.weight,
                reps = dto.reps,
                setType = SetType.entries.find { it.name == dto.setType } ?: SetType.WORKING
            )
        }
    )
}
