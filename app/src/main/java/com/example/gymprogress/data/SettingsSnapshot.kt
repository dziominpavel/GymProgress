package com.example.gymprogress.data

import java.time.LocalDate

/** Полный снимок пользовательских настроек для экспорта/импорта. */
data class SettingsSnapshot(
    val trainingGoal: TrainingGoal,
    val scoringSystem: ScoringSystem,
    val bodyWeightKg: Double?,
    val heightCm: Int?,
    val gender: Gender?,
    val chartRange: ChartRange,
    val chartMetric: ChartMetric,
    val timerSoundEnabled: Boolean,
    val timerVibrationEnabled: Boolean,
    val membershipExpiryDate: LocalDate?,
    val trainerSettings: TrainerSettings
)
