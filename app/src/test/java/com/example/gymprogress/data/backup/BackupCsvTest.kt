package com.example.gymprogress.data.backup

import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.MuscleGroup
import com.example.gymprogress.data.WorkoutEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCsvTest {

    private val chest = Exercise(
        id = 1,
        name = "Жим лёжа",
        muscleGroup = MuscleGroup.CHEST.name
    )

    @Test
    fun csvStartsWithBomAndHeader() {
        val csv = BackupCsv.buildCsv(emptyList(), emptyList())

        assertTrue(csv.startsWith(BackupCsv.BOM + "Дата;Упражнение;Мышечная группа;Вес;Повторы;Объём"))
    }

    @Test
    fun csvUsesDateAndIdOrderAndCalculatesVolume() {
        val first = WorkoutEntry(
            id = 2,
            date = "2026-01-01",
            exerciseName = chest.name,
            weight = 80.0,
            reps = "8,8",
            exerciseId = chest.id
        )
        val later = WorkoutEntry(
            id = 1,
            date = "2026-01-02",
            exerciseName = chest.name,
            weight = 85.0,
            reps = "5",
            exerciseId = chest.id
        )

        val csv = BackupCsv.buildCsv(listOf(chest), listOf(later, first))
        val lines = csv.removePrefix(BackupCsv.BOM).trim().lines()

        assertTrue(lines[1].startsWith("2026-01-01;Жим лёжа;"))
        assertTrue(lines[1].endsWith(";80;8,8;1280"))
        assertTrue(lines[2].startsWith("2026-01-02;"))
        assertTrue(lines[2].endsWith(";85;5;425"))
    }

    @Test
    fun csvEscapesSemicolonsQuotesAndNewlines() {
        val exercise = chest.copy(name = "Жим; \"лёжа\"")
        val entry = WorkoutEntry(
            id = 1,
            date = "2026-01-01",
            exerciseName = exercise.name,
            weight = 80.0,
            reps = "8",
            exerciseId = exercise.id
        )

        val csv = BackupCsv.buildCsv(listOf(exercise), listOf(entry))

        assertTrue(csv.contains("\"Жим; \"\"лёжа\"\"\""))
    }

    @Test
    fun csvKeepsOrphanEntryNameAndDoesNotExposeClientId() {
        val entry = WorkoutEntry(
            id = 1,
            date = "2026-01-01",
            exerciseName = "Удалённое упражнение",
            weight = 50.0,
            reps = "10",
            clientId = "secret-client-id"
        )

        val csv = BackupCsv.buildCsv(emptyList(), listOf(entry))

        assertTrue(csv.contains("Удалённое упражнение"))
        assertFalse(csv.contains("secret-client-id"))
    }
}
