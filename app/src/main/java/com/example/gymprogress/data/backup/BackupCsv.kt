package com.example.gymprogress.data.backup

import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.MuscleGroup
import com.example.gymprogress.data.WorkoutEntry

object BackupCsv {

    const val BOM = "\uFEFF"
    private const val HEADER = "Дата;Упражнение;Мышечная группа;Вес;Повторы;Объём"

    fun buildCsv(exercises: List<Exercise>, entries: List<WorkoutEntry>): String {
        val exerciseById = exercises.associateBy { it.id }
        val rows = entries
            .sortedWith(compareBy({ it.date }, { it.id }))
            .map { entry ->
                val exercise = entry.exerciseId?.let { exerciseById[it] }
                    ?: FormatUtils.findExerciseForEntry(entry, exercises)
                val muscleGroup = exercise?.muscleGroup
                    ?.let { raw -> MuscleGroup.entries.find { it.name == raw }?.displayName ?: raw }
                    .orEmpty()
                val totalReps = entry.reps.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .sum()
                val volume = entry.weight * totalReps
                listOf(
                    entry.date,
                    entry.exerciseName,
                    muscleGroup,
                    FormatUtils.formatWeight(entry.weight),
                    entry.reps,
                    FormatUtils.formatWeight(volume)
                ).joinToString(";") { escape(it) }
            }

        return buildString {
            append(BOM)
            appendLine(HEADER)
            rows.forEach { appendLine(it) }
        }
    }

    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ';' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
