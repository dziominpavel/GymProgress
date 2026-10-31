package com.example.gymprogress.data.backup

import android.net.Uri
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gymprogress.data.AppDatabase
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.MuscleGroup
import com.example.gymprogress.data.SettingsRepository
import com.example.gymprogress.data.SettingsSnapshot
import com.example.gymprogress.data.WorkoutEntry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var repository: BackupRepository
    private lateinit var originalSettings: SettingsSnapshot
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settingsRepository = SettingsRepository(context)
        originalSettings = settingsRepository.readSnapshot()
        repository = BackupRepository(context, db, settingsRepository)
        db.exerciseDao().insertAll(
            listOf(
                Exercise(
                    clientId = "ex-local",
                    name = "Жим лёжа",
                    muscleGroup = MuscleGroup.CHEST.name
                )
            )
        )
        db.workoutDao().insert(
            WorkoutEntry(
                date = "2026-01-01",
                exerciseName = "Жим лёжа",
                weight = 80.0,
                reps = "8",
                exerciseId = db.exerciseDao().getAllOnce().single().id,
                clientId = "entry-local"
            )
        )
    }

    @After
    fun tearDown() = runBlocking {
        settingsRepository.applySnapshot(originalSettings)
        db.close()
    }

    @Test
    fun exportJson_writesAllData() = runBlocking {
        val file = File(context.cacheDir, "export-test.json")
        val counts = repository.exportJson(Uri.fromFile(file))

        assertEquals(1, counts.exercises)
        assertEquals(1, counts.entries)
        val decoded = BackupJson.decodeFromString<BackupFileV1>(file.readText())
        assertEquals(BACKUP_SCHEMA_VERSION, decoded.schemaVersion)
        assertEquals("ex-local", decoded.exercises.single().clientId)
        assertEquals("ex-local", decoded.entries.single().exerciseClientId)
        assertTrue(decoded.settings != null)
    }

    @Test
    fun previewAndMerge_doNotLoseExistingData() = runBlocking {
        val file = writeBackup(
            exercises = listOf(
                backupExercise("ex-file", "Присед", MuscleGroup.LEGS.name)
            ),
            entries = listOf(
                backupEntry("entry-file", "ex-file", "Присед", "2026-01-02", 100.0, "5")
            )
        )

        val preview = repository.readPreview(Uri.fromFile(file))
        assertEquals(1, preview.file.exercises)
        assertEquals(1, preview.merge.addedExercises)
        assertEquals(1, preview.merge.addedEntries)

        val result = repository.importMerge(Uri.fromFile(file), applySettings = false)

        assertEquals(1, result.addedExercises)
        assertEquals(1, result.addedEntries)
        assertEquals(2, db.exerciseDao().getAllOnce().size)
        assertEquals(2, db.workoutDao().getAllOnce().size)
        assertEquals(null, result.settingsError)
    }

    @Test
    fun importReplace_createsSafetyBackupAndReplacesData() = runBlocking {
        val file = writeBackup(
            exercises = listOf(backupExercise("ex-new", "Тяга", MuscleGroup.BACK.name)),
            entries = listOf(backupEntry("entry-new", "ex-new", "Тяга", "2026-02-01", 90.0, "6"))
        )

        val result = repository.importReplace(Uri.fromFile(file), applySettings = false)

        assertEquals(1, result.addedExercises)
        assertEquals(1, result.addedEntries)
        assertEquals(1, result.deletedExercises)
        assertEquals(1, result.deletedEntries)
        assertNotNull(result.safetyBackupUri)
        assertEquals("ex-new", db.exerciseDao().getAllOnce().single().clientId)
        assertEquals("ex-new", db.workoutDao().getAllOnce().single().let { entry ->
            db.exerciseDao().getById(entry.exerciseId!!)?.clientId
        })
    }

    @Test
    fun corruptedJson_isRejectedAndDatabaseIsUnchanged() = runBlocking {
        val file = File(context.cacheDir, "broken-backup.json")
        file.writeText("{not-json")

        val error = runCatching {
            repository.readPreview(Uri.fromFile(file))
        }.exceptionOrNull()

        assertTrue(error is BackupException)
        assertEquals(1, db.exerciseDao().getAllOnce().size)
        assertEquals(1, db.workoutDao().getAllOnce().size)
    }

    @Test
    fun newerSchema_isRejected() = runBlocking {
        val file = File(context.cacheDir, "newer-backup.json")
        file.writeText(
            """{"schemaVersion":2,"exercises":[],"entries":[]}"""
        )

        val error = runCatching {
            repository.readPreview(Uri.fromFile(file))
        }.exceptionOrNull()

        assertTrue(error is BackupException)
        assertTrue(error?.message?.contains("более новой") == true)
    }

    @Test
    fun invalidSettingsDoNotOverwriteCurrentValues() = runBlocking {
        val current = settingsRepository.readSnapshot()
        val file = writeBackup(
            exercises = emptyList(),
            entries = emptyList(),
            settings = BackupSettingsV1(
                trainingGoal = "UNKNOWN",
                bodyWeightKg = null,
                timerSoundEnabled = null
            )
        )

        repository.importMerge(Uri.fromFile(file), applySettings = true)

        val after = settingsRepository.readSnapshot()
        assertEquals(current.trainingGoal, after.trainingGoal)
        assertEquals(current.bodyWeightKg, after.bodyWeightKg)
        assertEquals(current.timerSoundEnabled, after.timerSoundEnabled)
    }

    @Test
    fun replaceWith10kEntries_keepsAllData() = runBlocking {
        val exercise = backupExercise("bulk-ex", "Жим лёжа", MuscleGroup.CHEST.name)
        val entries = (1..10_000).map { index ->
            backupEntry(
                clientId = "bulk-entry-$index",
                exerciseClientId = exercise.clientId,
                name = exercise.name,
                date = "2026-01-${(index % 28 + 1).toString().padStart(2, '0')}",
                weight = 80.0,
                reps = "5"
            )
        }
        val file = writeBackup(listOf(exercise), entries)
        val started = System.currentTimeMillis()

        val result = repository.importReplace(Uri.fromFile(file), applySettings = false)
        val elapsed = System.currentTimeMillis() - started

        assertEquals(10_000, result.addedEntries)
        assertEquals(10_000, db.workoutDao().getAllOnce().size)
        println("10k import took ${elapsed}ms")
    }

    @Test
    fun roomTransaction_rollsBackOnException() = runBlocking {
        val before = db.workoutDao().getAllOnce().size
        runCatching {
            db.withTransaction {
                db.workoutDao().deleteAll()
                throw IllegalStateException("test rollback")
            }
        }
        assertEquals(before, db.workoutDao().getAllOnce().size)
    }

    private fun writeBackup(
        exercises: List<BackupExerciseV1>,
        entries: List<BackupEntryV1>,
        settings: BackupSettingsV1? = null
    ): File {
        val file = File(context.cacheDir, "backup-${System.nanoTime()}.json")
        file.writeText(
            BackupJson.encodeToString(
                BackupFileV1(
                    schemaVersion = BACKUP_SCHEMA_VERSION,
                    appVersion = "test",
                    exportedAt = "2026-01-01T00:00:00Z",
                    settings = settings,
                    exercises = exercises,
                    entries = entries
                )
            )
        )
        return file
    }

    private fun backupExercise(clientId: String, name: String, group: String) =
        BackupExerciseV1(
            clientId = clientId,
            name = name,
            muscleGroup = group
        )

    private fun backupEntry(
        clientId: String,
        exerciseClientId: String,
        name: String,
        date: String,
        weight: Double,
        reps: String
    ) = BackupEntryV1(
        clientId = clientId,
        exerciseClientId = exerciseClientId,
        exerciseName = name,
        date = date,
        weight = weight,
        reps = reps
    )
}
