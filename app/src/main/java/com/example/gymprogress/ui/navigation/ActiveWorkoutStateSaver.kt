package com.example.gymprogress.ui.navigation

import androidx.compose.runtime.saveable.Saver
import com.example.gymprogress.data.ActiveWorkoutJson
import com.example.gymprogress.data.ActiveWorkoutStateDto
import com.example.gymprogress.data.ActiveWorkoutUiState
import com.example.gymprogress.data.toDto
import com.example.gymprogress.data.toUiState
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * Сохраняет активную тренировку в Bundle как JSON: переживает поворот экрана
 * и восстановление Activity после смерти процесса, пока задача остаётся в recents.
 */
val ActiveWorkoutStateSaver: Saver<ActiveWorkoutUiState?, String> = Saver(
    save = { state ->
        state?.let { ActiveWorkoutJson.encodeToString(it.toDto()) } ?: ""
    },
    restore = { raw ->
        if (raw.isBlank()) {
            null
        } else {
            runCatching {
                ActiveWorkoutJson.decodeFromString<ActiveWorkoutStateDto>(raw).toUiState()
            }.getOrNull()
        }
    }
)
