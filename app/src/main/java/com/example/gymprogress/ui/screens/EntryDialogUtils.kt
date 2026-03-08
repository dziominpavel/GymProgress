package com.example.gymprogress.ui.screens

internal fun parseWeightInput(text: String, isBodyweight: Boolean): Double? {
    val raw = text.replace(",", ".").trim()
    return when {
        isBodyweight && raw.isEmpty() -> 0.0
        raw.isEmpty() -> null
        else -> raw.toDoubleOrNull()
    }
}

internal fun isWeightInputValid(weightInput: Double?, isBodyweight: Boolean): Boolean =
    if (isBodyweight) {
        weightInput != null && weightInput >= 0
    } else {
        weightInput != null && weightInput > 0
    }

internal fun calcFinalWeight(weightInput: Double?, isBodyweight: Boolean, bodyWeightKg: Double?): Double =
    if (isBodyweight) {
        (bodyWeightKg ?: 0.0) + (weightInput ?: 0.0)
    } else {
        weightInput ?: 0.0
    }

internal fun isRepsValid(value: String): Boolean =
    (value.toIntOrNull() ?: 0) > 0
