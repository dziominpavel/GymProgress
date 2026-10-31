package com.example.gymprogress.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseMergeTest {

    private fun exercise(id: Long, name: String) = Exercise(
        id = id,
        name = name,
        muscleGroup = MuscleGroup.CHEST.name
    )

    @Test
    fun canonicalExercise_prefersMoreEntries() {
        val first = exercise(1, "Жим лёжа")
        val second = exercise(2, "жим лёжа")

        val canonical = canonicalExercisesByKey(
            exercises = listOf(first, second),
            entryCountByExerciseId = mapOf(first.id to 2, second.id to 7)
        )

        assertEquals(second.id, canonical[first.nameKey]?.id)
    }

    @Test
    fun canonicalExercise_breaksTieBySmallerId() {
        val first = exercise(1, "Жим лёжа")
        val second = exercise(2, "жим лёжа")

        val canonical = canonicalExercisesByKey(
            exercises = listOf(second, first),
            entryCountByExerciseId = mapOf(first.id to 3, second.id to 3)
        )

        assertEquals(first.id, canonical[first.nameKey]?.id)
    }

    @Test
    fun canonicalExercise_keepsDistinctNamesSeparate() {
        val bench = exercise(1, "Жим лёжа")
        val squat = exercise(2, "Присед")

        val canonical = canonicalExercisesByKey(
            exercises = listOf(bench, squat),
            entryCountByExerciseId = emptyMap()
        )

        assertEquals(2, canonical.size)
        assertEquals(bench.id, canonical[bench.nameKey]?.id)
        assertEquals(squat.id, canonical[squat.nameKey]?.id)
    }
}
