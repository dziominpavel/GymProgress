package com.example.gymprogress.data.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val BACKUP_SCHEMA_VERSION = 1

val BackupJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}

@Serializable
data class BackupFileV1(
    val schemaVersion: Int,
    val appVersion: String = "",
    val exportedAt: String = "",
    val settings: BackupSettingsV1? = null,
    val trainerSettings: BackupTrainerSettingsV1? = null,
    val exercises: List<BackupExerciseV1>,
    val entries: List<BackupEntryV1>
)

@Serializable
data class BackupSettingsV1(
    val trainingGoal: String? = null,
    val scoringSystem: String? = null,
    val bodyWeightKg: Double? = null,
    val heightCm: Int? = null,
    val gender: String? = null,
    val chartRange: String? = null,
    val chartMetric: String? = null,
    val timerSoundEnabled: Boolean? = null,
    val timerVibrationEnabled: Boolean? = null,
    val membershipExpiryDate: String? = null
)

@Serializable
data class BackupTrainerSettingsV1(
    val splitType: String? = null,
    val daysPerWeek: Int? = null,
    val priorityGroups: List<String>? = null,
    val customSplitDays: List<BackupCustomSplitDayV1>? = null,
    val includeWarmup: Boolean? = null,
    val autoDeload: Boolean? = null,
    val deloadIntervalWeeks: Int? = null,
    val progressionType: String? = null
)

@Serializable
data class BackupCustomSplitDayV1(
    val dayIndex: Int,
    val groups: List<String> = emptyList()
)

@Serializable
data class BackupExerciseV1(
    val clientId: String,
    val name: String,
    val muscleGroup: String,
    val exerciseType: String = "COMPOUND",
    val isBodyweight: Boolean = false,
    val isArchived: Boolean = false
)

@Serializable
data class BackupEntryV1(
    val clientId: String,
    val exerciseClientId: String? = null,
    val exerciseName: String,
    val date: String,
    val weight: Double,
    val reps: String
)
