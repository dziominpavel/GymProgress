package com.example.gymprogress.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SimplifiedScoreCalculatorBestEntryTest {

    @Test
    fun bestEntryByEstimatedE1RM_picks_session_with_higher_e1rm() {
        val weak = WorkoutEntry(
            id = 1L,
            date = "2026-01-01",
            exerciseName = "Жим",
            weight = 20.0,
            reps = "10,10,10"
        )
        val strong = WorkoutEntry(
            id = 2L,
            date = "2026-01-02",
            exerciseName = "Жим",
            weight = 36.0,
            reps = "5"
        )
        val list = listOf(weak, strong)
        val best = SimplifiedScoreCalculator.bestEntryByEstimatedE1RM(
            list,
            bodyWeightKg = null,
            isBodyweightExercise = false
        )
        assertEquals(strong.id, best?.id)
    }

    @Test
    fun bestEntryByEstimatedE1RM_tie_prefers_newer_date_then_higher_id() {
        val older = WorkoutEntry(
            id = 1L,
            date = "2026-01-01",
            exerciseName = "Жим",
            weight = 20.0,
            reps = "5"
        )
        val newer = WorkoutEntry(
            id = 2L,
            date = "2026-01-10",
            exerciseName = "Жим",
            weight = 20.0,
            reps = "5"
        )
        val sameDayLaterId = WorkoutEntry(
            id = 3L,
            date = "2026-01-10",
            exerciseName = "Жим",
            weight = 20.0,
            reps = "5"
        )
        val bestDate = SimplifiedScoreCalculator.bestEntryByEstimatedE1RM(
            listOf(older, newer),
            bodyWeightKg = null,
            isBodyweightExercise = false
        )
        assertEquals(newer.id, bestDate?.id)

        val bestId = SimplifiedScoreCalculator.bestEntryByEstimatedE1RM(
            listOf(newer, sameDayLaterId),
            bodyWeightKg = null,
            isBodyweightExercise = false
        )
        assertEquals(sameDayLaterId.id, bestId?.id)
    }
}
