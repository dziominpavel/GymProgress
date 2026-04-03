package com.example.gymprogress.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
}
