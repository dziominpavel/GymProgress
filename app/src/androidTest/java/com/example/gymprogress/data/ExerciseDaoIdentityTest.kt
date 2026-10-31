package com.example.gymprogress.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseDaoIdentityTest {

    private lateinit var db: AppDatabase
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var workoutDao: WorkoutDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        exerciseDao = db.exerciseDao()
        workoutDao = db.workoutDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun archiveAndRestore_keepLinkingHistory() = runBlocking {
        val exercise = Exercise(name = "Жим лёжа", muscleGroup = MuscleGroup.CHEST.name)
        exerciseDao.insert(exercise)
        val saved = exerciseDao.getAllExercises().first().single()

        exerciseDao.update(saved.copy(isArchived = true))
        assertTrue(exerciseDao.getActiveExercises().first().isEmpty())
        assertEquals(1, exerciseDao.getAllExercises().first().size)

        exerciseDao.update(saved.copy(isArchived = false))
        assertEquals(1, exerciseDao.getActiveExercises().first().size)
    }

    @Test
    fun batchInsertAndDeleteAll_workForBackupReplace() = runBlocking {
        val ids = exerciseDao.insertAll(
            listOf(
                Exercise(clientId = "backup-1", name = "Жим", muscleGroup = MuscleGroup.CHEST.name),
                Exercise(clientId = "backup-2", name = "Присед", muscleGroup = MuscleGroup.LEGS.name)
            )
        )
        assertEquals(2, ids.size)
        assertEquals(2, exerciseDao.getAllOnce().size)

        workoutDao.insert(
            WorkoutEntry(
                date = "2026-01-01",
                exerciseName = "Жим",
                weight = 80.0,
                reps = "8",
                exerciseId = ids.first()
            )
        )
        workoutDao.deleteAll()
        exerciseDao.deleteAll()

        assertTrue(workoutDao.getAllOnce().isEmpty())
        assertTrue(exerciseDao.getAllOnce().isEmpty())
    }

    @Test
    fun deleteExercise_setsExerciseIdToNullAndKeepsEntry() = runBlocking {
        val exercise = Exercise(name = "Присед", muscleGroup = MuscleGroup.LEGS.name)
        exerciseDao.insert(exercise)
        val saved = exerciseDao.getAllExercises().first().single()

        workoutDao.insert(
            WorkoutEntry(
                date = "2026-01-01",
                exerciseName = saved.name,
                weight = 100.0,
                reps = "5",
                exerciseId = saved.id
            )
        )
        assertEquals(1, workoutDao.getEntriesByExerciseId(saved.id).first().size)
        assertEquals(1, workoutDao.countEntriesByExerciseId(saved.id))

        exerciseDao.delete(saved)

        val entry = workoutDao.getAllEntries().first().single()
        assertNull(entry.exerciseId)
        assertEquals("Присед", entry.exerciseName)
        assertTrue(workoutDao.getEntriesByExerciseId(saved.id).first().isEmpty())
        assertEquals(0, workoutDao.countEntriesByExerciseId(saved.id))
    }
}
