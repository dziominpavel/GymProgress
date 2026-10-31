package com.example.gymprogress.data.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializationTest {

    private val sample = BackupFileV1(
        schemaVersion = BACKUP_SCHEMA_VERSION,
        appVersion = "1.0.634",
        exportedAt = "2026-10-31T12:00:00Z",
        settings = BackupSettingsV1(
            trainingGoal = "HYPERTROPHY",
            bodyWeightKg = 80.0,
            membershipExpiryDate = "2026-12-31"
        ),
        trainerSettings = BackupTrainerSettingsV1(
            splitType = "FULL_BODY",
            daysPerWeek = 3,
            priorityGroups = listOf("CHEST"),
            customSplitDays = listOf(BackupCustomSplitDayV1(0, listOf("CHEST", "BACK")))
        ),
        exercises = listOf(
            BackupExerciseV1(
                clientId = "exercise-1",
                name = "Жим лёжа",
                muscleGroup = "CHEST",
                isArchived = true
            )
        ),
        entries = listOf(
            BackupEntryV1(
                clientId = "entry-1",
                exerciseClientId = "exercise-1",
                exerciseName = "Жим лёжа",
                date = "2026-10-31",
                weight = 80.0,
                reps = "10,8"
            )
        )
    )

    @Test
    fun roundTrip_preservesData() {
        val encoded = BackupJson.encodeToString(sample)
        val decoded = BackupJson.decodeFromString<BackupFileV1>(encoded)

        assertEquals(sample, decoded)
    }

    @Test
    fun unknownFields_areIgnored() {
        val json = """
            {
              "schemaVersion": 1,
              "exercises": [],
              "entries": [],
              "futureField": {"nested": true}
            }
        """.trimIndent()

        val decoded = BackupJson.decodeFromString<BackupFileV1>(json)

        assertEquals(1, decoded.schemaVersion)
        assertTrue(decoded.exercises.isEmpty())
    }

    @Test
    fun missingRequiredLists_failToDecode() {
        val json = """{"schemaVersion":1,"exercises":[]}"""

        val error = runCatching {
            BackupJson.decodeFromString<BackupFileV1>(json)
        }.exceptionOrNull()

        assertTrue(error is SerializationException)
    }
}
