package com.example.gymprogress.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveWorkoutStateTest {

    private val exercise = Exercise(
        id = 7,
        clientId = "exercise-7",
        name = "Жим лёжа",
        muscleGroup = MuscleGroup.CHEST.name,
        exerciseType = ExerciseType.COMPOUND.name,
        isBodyweight = false,
        isArchived = false
    )

    private val recommendation = WorkoutRecommendation(
        dayLabel = "Push (Жим)",
        dayIndex = 2,
        muscleGroups = listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
        exercises = listOf(
            ExerciseRecommendation(
                exercise = exercise,
                sets = listOf(
                    SetRecommendation(SetType.WARMUP, 40.0, 10..12, 60),
                    SetRecommendation(SetType.WORKING, 80.0, 8..12, 90)
                ),
                note = "Первый раз — определите рабочий вес",
                advice = "Начните в диапазоне 8–12 повторов"
            )
        ),
        isDeloadWeek = false,
        missingGroups = listOf(MuscleGroup.SHOULDERS)
    )

    private val state = ActiveWorkoutUiState(
        recommendation = recommendation,
        workoutStartTime = 1_700_000_000_000L,
        currentExerciseIndex = 0,
        currentSetIndex = 1,
        isResting = true,
        restTimeLeft = 42,
        completedSets = listOf(
            CompletedSet("Жим лёжа", 80.0, 10, SetType.WORKING)
        )
    )

    @Test
    fun roundTrip_preservesActiveWorkout() {
        val encoded = ActiveWorkoutJson.encodeToString(state.toDto())
        val restored = ActiveWorkoutJson
            .decodeFromString<ActiveWorkoutStateDto>(encoded)
            .toUiState()

        assertEquals(state, restored)
    }

    @Test
    fun completedSetAndTimerSurviveRestore() {
        val restored = ActiveWorkoutJson
            .decodeFromString<ActiveWorkoutStateDto>(
                ActiveWorkoutJson.encodeToString(state.toDto())
            )
            .toUiState()

        assertEquals(42, restored.restTimeLeft)
        assertEquals(true, restored.isResting)
        assertEquals(1, restored.currentSetIndex)
        assertEquals("Жим лёжа", restored.completedSets.single().exerciseName)
        assertEquals(SetType.WORKING, restored.completedSets.single().setType)
    }
}
