package com.example.gymprogress.data

import com.example.gymprogress.data.ActiveWorkoutSession.state
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Снимок состояния текущей активной тренировки, который читает foreground-сервис
 * и отображает в постоянном уведомлении.
 *
 * Хранит только то, что нужно для уведомления — без полей ввода (вес/повторы):
 * - [exerciseName] — название текущего упражнения,
 * - [setIndex] и [totalSets] — номер подхода (1-based) и всего по упражнению,
 * - [doneSets] и [overallTotalSets] — общий прогресс по тренировке,
 * - [restTimeLeft] — оставшийся отдых в секундах, либо `null` если идёт ввод подхода,
 * - [workoutTitle] — короткий заголовок (например, день сплита).
 */
data class ActiveWorkoutSnapshot(
    val workoutTitle: String,
    val exerciseName: String,
    val setIndex: Int,
    val totalSets: Int,
    val doneSets: Int,
    val overallTotalSets: Int,
    val restTimeLeft: Int?
)

/**
 * Singleton-источник снимков активной тренировки. Compose обновляет [state] при
 * каждом изменении ключевых полей, [com.example.gymprogress.service.ActiveWorkoutService]
 * подписывается и обновляет уведомление.
 *
 * Когда состояние становится `null` — сервис должен остановиться.
 */
object ActiveWorkoutSession {
    private val _state = MutableStateFlow<ActiveWorkoutSnapshot?>(null)
    val state: StateFlow<ActiveWorkoutSnapshot?> = _state.asStateFlow()

    fun update(snapshot: ActiveWorkoutSnapshot) {
        _state.value = snapshot
    }

    fun clear() {
        _state.value = null
    }
}
