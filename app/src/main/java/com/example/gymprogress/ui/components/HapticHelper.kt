package com.example.gymprogress.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Унифицированный набор тактильных откликов.
 *
 * - [tap] — лёгкий тактильный отклик для частых действий (кнопки, FAB, чипы).
 * - [confirm] — заметный отклик для значимых событий (подход зафиксирован, тренировка завершена).
 * - [warn] — отклик уровня long-press для деструктивных/предупреждающих действий.
 *
 * Реализация — поверх [LocalHapticFeedback]. При появлении новых типов
 * `HapticFeedbackType` в Compose их можно подмешать без правок вызовов.
 */
class HapticActions internal constructor(private val haptic: HapticFeedback) {
    fun tap() = haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    fun confirm() = haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    fun warn() = haptic.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberHaptics(): HapticActions {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { HapticActions(haptic) }
}
