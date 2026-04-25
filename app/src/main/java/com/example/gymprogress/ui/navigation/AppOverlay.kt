package com.example.gymprogress.ui.navigation

/**
 * Полноэкранные оверлеи поверх основного NavigationSuiteScaffold.
 *
 * Хранятся в [androidx.compose.runtime.snapshots.SnapshotStateList],
 * которая сохраняется через [overlayStackSaver] (см. ниже) — это позволяет
 * корректно работать с back-стеком и пережить configuration change.
 *
 * Диалог «Новая запись» (`AddDialog`) сюда не входит — он не fullscreen.
 */
sealed interface AppOverlay {
    data object ProgressChart : AppOverlay
    data object WorkoutHistory : AppOverlay
    data object Settings : AppOverlay
    data object About : AppOverlay
    data object Trainer : AppOverlay
    data object TrainerSettings : AppOverlay
    data object ActiveWorkout : AppOverlay
}

/** Сериализация для [androidx.compose.runtime.saveable.listSaver]. */
internal fun serializeOverlay(overlay: AppOverlay): String = when (overlay) {
    AppOverlay.ProgressChart -> "ProgressChart"
    AppOverlay.WorkoutHistory -> "WorkoutHistory"
    AppOverlay.Settings -> "Settings"
    AppOverlay.About -> "About"
    AppOverlay.Trainer -> "Trainer"
    AppOverlay.TrainerSettings -> "TrainerSettings"
    AppOverlay.ActiveWorkout -> "ActiveWorkout"
}

internal fun deserializeOverlay(raw: String): AppOverlay? = when (raw) {
    "ProgressChart" -> AppOverlay.ProgressChart
    "WorkoutHistory" -> AppOverlay.WorkoutHistory
    "Settings" -> AppOverlay.Settings
    "About" -> AppOverlay.About
    "Trainer" -> AppOverlay.Trainer
    "TrainerSettings" -> AppOverlay.TrainerSettings
    "ActiveWorkout" -> AppOverlay.ActiveWorkout
    else -> null
}
