package com.example.gymprogress.data

object FormatUtils {

    fun formatWeight(w: Double): String {
        return if (w == w.toLong().toDouble()) w.toLong().toString() else w.toString()
    }

    fun formatWeightPrecise(w: Double): String {
        return if (w == w.toLong().toDouble()) w.toLong().toString() else String.format(java.util.Locale.US, "%.1f", w)
    }

    fun formatVolume(v: Double): String {
        return if (v == v.toLong().toDouble()) v.toLong().toString() else String.format(java.util.Locale.US, "%.0f", v)
    }

    fun formatRest(seconds: Int): String {
        return if (seconds >= 60) {
            val min = seconds / 60
            val sec = seconds % 60
            if (sec > 0) "$minм ${sec}с" else "$min мин"
        } else {
            "${seconds}с"
        }
    }

    fun formatDate(storageDate: String): String {
        return try {
            val parts = storageDate.split("-")
            if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else storageDate
        } catch (_: Exception) {
            storageDate
        }
    }
}
