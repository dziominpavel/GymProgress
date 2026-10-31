package com.example.gymprogress.data.backup

import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.MuscleGroup
import com.example.gymprogress.data.WorkoutEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportMergerTest {

    private fun exercise(
        id: Long,
        clientId: String,
        name: String,
        archived: Boolean = false
    ) = Exercise(
        id = id,
        clientId = clientId,
        name = name,
        muscleGroup = MuscleGroup.CHEST.name,
        isArchived = archived
    )

    private fun backup(
        exercises: List<BackupExerciseV1> = emptyList(),
        entries: List<BackupEntryV1> = emptyList()
    ) = BackupFileV1(
        schemaVersion = 1,
        exercises = exercises,
        entries = entries
    )

    private fun fileExercise(clientId: String, name: String, archived: Boolean = false) =
        BackupExerciseV1(
            clientId = clientId,
            name = name,
            muscleGroup = MuscleGroup.CHEST.name,
            isArchived = archived
        )

    private fun fileEntry(
        clientId: String,
        exerciseClientId: String?,
        exerciseName: String,
        date: String = "2026-01-01",
        weight: Double = 80.0,
        reps: String = "8"
    ) = BackupEntryV1(
        clientId = clientId,
        exerciseClientId = exerciseClientId,
        exerciseName = exerciseName,
        date = date,
        weight = weight,
        reps = reps
    )

    @Test
    fun emptyDatabase_addsAllFileData() {
        val plan = ImportMerger.planMerge(
            currentExercises = emptyList(),
            currentEntries = emptyList(),
            backup = backup(
                exercises = listOf(fileExercise("ex-1", "Жим лёжа", archived = true)),
                entries = listOf(fileEntry("entry-1", "ex-1", "Жим лёжа"))
            )
        )

        assertEquals(1, plan.addedExercises)
        assertEquals(1, plan.addedEntries)
        assertEquals(0, plan.matchedExercises)
        assertEquals(0, plan.matchedEntries)
        assertTrue(plan.newExercises.single().isArchived)
        assertEquals("ex-1", plan.newEntries.single().exerciseClientId)
        assertNull(plan.newEntries.single().entry.exerciseId)
    }

    @Test
    fun matchingByClientId_updatesExerciseAndKeepsArchiveState() {
        val current = exercise(1, "ex-1", "Жим лёжа")
        val plan = ImportMerger.planMerge(
            currentExercises = listOf(current),
            currentEntries = emptyList(),
            backup = backup(
                exercises = listOf(
                    fileExercise("ex-1", "Жим лёжа", archived = true)
                        .copy(muscleGroup = MuscleGroup.BACK.name)
                )
            )
        )

        assertEquals(0, plan.addedExercises)
        assertEquals(1, plan.matchedExercises)
        assertEquals(1, plan.updatedExercises.single().id)
        assertEquals(MuscleGroup.BACK.name, plan.updatedExercises.single().muscleGroup)
        assertEquals(false, plan.updatedExercises.single().isArchived)
    }

    @Test
    fun matchingByName_keepsLocalClientIdAndLinksEntry() {
        val current = exercise(7, "local-id", "Жим лёжа")
        val plan = ImportMerger.planMerge(
            currentExercises = listOf(current),
            currentEntries = emptyList(),
            backup = backup(
                exercises = listOf(fileExercise("file-id", "жим лёжа")),
                entries = listOf(fileEntry("entry-1", "file-id", "жим лёжа"))
            )
        )

        assertEquals(0, plan.addedExercises)
        assertEquals(1, plan.matchedExercises)
        assertEquals("local-id", plan.updatedExercises.single().clientId)
        assertEquals(7L, plan.newEntries.single().entry.exerciseId)
    }

    @Test
    fun duplicateEntryByClientId_isSkipped() {
        val current = exercise(1, "ex-1", "Жим лёжа")
        val currentEntry = WorkoutEntry(
            id = 1,
            date = "2026-01-01",
            exerciseName = "Жим лёжа",
            weight = 80.0,
            reps = "8",
            exerciseId = 1,
            clientId = "entry-1"
        )
        val plan = ImportMerger.planMerge(
            currentExercises = listOf(current),
            currentEntries = listOf(currentEntry),
            backup = backup(
                exercises = listOf(fileExercise("ex-1", "Жим лёжа")),
                entries = listOf(fileEntry("entry-1", "ex-1", "Жим лёжа"))
            )
        )

        assertEquals(0, plan.addedEntries)
        assertEquals(1, plan.matchedEntries)
    }

    @Test
    fun duplicateNaturalKey_isSkipped() {
        val current = exercise(1, "ex-1", "Жим лёжа")
        val currentEntry = WorkoutEntry(
            id = 1,
            date = "2026-01-01",
            exerciseName = "Жим лёжа",
            weight = 80.0,
            reps = "8",
            exerciseId = 1,
            clientId = "local-entry"
        )
        val plan = ImportMerger.planMerge(
            currentExercises = listOf(current),
            currentEntries = listOf(currentEntry),
            backup = backup(
                exercises = listOf(fileExercise("ex-1", "Жим лёжа")),
                entries = listOf(fileEntry("entry-2", "ex-1", "жим лёжа"))
            )
        )

        assertEquals(0, plan.addedEntries)
        assertEquals(1, plan.matchedEntries)
    }

    @Test
    fun renameDoesNotDuplicateExistingEntry() {
        val current = exercise(1, "ex-1", "Старое имя")
        val currentEntry = WorkoutEntry(
            id = 1,
            date = "2026-01-01",
            exerciseName = "Старое имя",
            weight = 80.0,
            reps = "8",
            exerciseId = 1,
            clientId = "entry-local"
        )
        val plan = ImportMerger.planMerge(
            currentExercises = listOf(current),
            currentEntries = listOf(currentEntry),
            backup = backup(
                exercises = listOf(fileExercise("ex-1", "Новое имя")),
                entries = listOf(fileEntry("entry-file", "ex-1", "Новое имя"))
            )
        )

        assertEquals(0, plan.addedEntries)
        assertEquals(1, plan.matchedEntries)
        assertEquals("Новое имя", plan.updatedExercises.single().name)
    }

    @Test
    fun orphanEntry_staysOrphan() {
        val plan = ImportMerger.planMerge(
            currentExercises = emptyList(),
            currentEntries = emptyList(),
            backup = backup(
                entries = listOf(fileEntry("entry-1", null, "Удалённое упражнение"))
            )
        )

        assertNull(plan.newEntries.single().entry.exerciseId)
        assertEquals("Удалённое упражнение", plan.newEntries.single().entry.exerciseName)
    }

    @Test
    fun duplicateFileIdentities_areRejected() {
        val duplicateClientId = runCatching {
            ImportMerger.planMerge(
                emptyList(),
                emptyList(),
                backup(
                    exercises = listOf(
                        fileExercise("same", "Жим лёжа"),
                        fileExercise("same", "Присед")
                    )
                )
            )
        }.exceptionOrNull()

        val duplicateName = runCatching {
            ImportMerger.planMerge(
                emptyList(),
                emptyList(),
                backup(
                    exercises = listOf(
                        fileExercise("one", "Жим лёжа"),
                        fileExercise("two", "жим лёжа")
                    )
                )
            )
        }.exceptionOrNull()

        assertTrue(duplicateClientId is IllegalArgumentException)
        assertTrue(duplicateName is IllegalArgumentException)
    }
}
