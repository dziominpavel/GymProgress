package com.example.gymprogress.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object FormatUtils {

    const val STORAGE_DATE_PATTERN = "yyyy-MM-dd"
    const val DISPLAY_DATE_PATTERN = "dd.MM.yyyy"

    private val storageDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(STORAGE_DATE_PATTERN)
    private val displayDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(DISPLAY_DATE_PATTERN)

    fun toStorageDate(localDate: LocalDate): String = localDate.format(storageDateFormatter)

    fun parseStorageDate(date: String): LocalDate? {
        return runCatching { LocalDate.parse(date, storageDateFormatter) }.getOrNull()
            ?: runCatching { LocalDate.parse(date, displayDateFormatter) }.getOrNull()
    }

    fun formatWeight(w: Double): String {
        return if (w == w.toLong().toDouble()) w.toLong().toString() else w.toString()
    }

    fun formatWeightPrecise(w: Double): String {
        return if (w == w.toLong().toDouble()) w.toLong().toString() else String.format(java.util.Locale.US, "%.1f", w)
    }

    fun formatVolume(v: Double): String {
        return if (v == v.toLong().toDouble()) v.toLong().toString() else String.format(java.util.Locale.US, "%.0f", v)
    }

    /** Округление для отображения на графиках и в диалогах (два знака после запятой). */
    fun formatTwoDecimals(value: Double): String =
        String.format(java.util.Locale.US, "%.2f", value)

    fun formatRest(seconds: Int): String {
        return if (seconds >= 60) {
            val min = seconds / 60
            val sec = seconds % 60
            if (sec > 0) "${min}м ${sec}с" else "${min} мин"
        } else {
            "${seconds}с"
        }
    }

    /** Форматирует дату из формата хранения (yyyy-MM-dd) в отображаемый (dd.MM.yyyy). */
    fun formatDate(storageDate: String): String {
        return try {
            val parts = storageDate.split("-")
            if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else storageDate
        } catch (_: Exception) {
            storageDate
        }
    }

    /**
     * Ключ для сопоставления имён упражнений в записях журнала и в справочнике:
     * пробелы, неразрывные пробелы, регистр не должны давать «разные» упражнения.
     */
    fun normalizeExerciseNameKey(name: String): String =
        name.trim()
            .replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)

    /** Находит упражнение в справочнике по строке из записи журнала. */
    fun findExerciseByStoredName(exercises: List<Exercise>, storedExerciseName: String): Exercise? {
        val t = storedExerciseName.trim()
        return exercises.firstOrNull { ex ->
            ex.name.trim() == t || ex.name.trim().equals(t, ignoreCase = true)
        }
    }

    /**
     * Запись журнала относится к выбранному упражнению (в т.ч. другое написание имени в БД).
     * [historyNameHint] — строка с быстрого «+»; учитывается только если указывает на то же упражнение (тот же id).
     */
    fun workoutEntryMatchesExercise(
        entry: WorkoutEntry,
        selected: Exercise,
        allExercises: List<Exercise>,
        historyNameHint: String?
    ): Boolean {
        val stored = entry.exerciseName.trim()
        val catalog = selected.name.trim()
        if (normalizeExerciseNameKey(stored) == normalizeExerciseNameKey(catalog)) return true
        if (stored.equals(catalog, ignoreCase = true)) return true
        val resolved = findExerciseByStoredName(allExercises, stored)
        if (resolved?.id == selected.id) return true
        val p = historyNameHint?.trim()?.takeIf { it.isNotEmpty() }
        if (p != null) {
            val preResolved = findExerciseByStoredName(allExercises, p)
            if (preResolved?.id == selected.id &&
                (stored == p || normalizeExerciseNameKey(stored) == normalizeExerciseNameKey(p))
            ) {
                return true
            }
        }
        return false
    }
}
