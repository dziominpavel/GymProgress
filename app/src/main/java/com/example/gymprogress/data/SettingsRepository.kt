package com.example.gymprogress.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val goalKey = stringPreferencesKey("training_goal")
    private val splitTypeKey = stringPreferencesKey("trainer_split_type")
    private val daysPerWeekKey = intPreferencesKey("trainer_days_per_week")
    private val priorityGroupsKey = stringPreferencesKey("trainer_priority_groups")
    private val customSplitDaysKey = stringPreferencesKey("trainer_custom_split_days")
    private val includeWarmupKey = booleanPreferencesKey("trainer_include_warmup")
    private val autoDeloadKey = booleanPreferencesKey("trainer_auto_deload")
    private val deloadIntervalKey = intPreferencesKey("trainer_deload_interval_weeks")
    private val progressionTypeKey = stringPreferencesKey("trainer_progression_type")

    val trainingGoal: Flow<TrainingGoal> = context.dataStore.data.map { prefs ->
        val name = prefs[goalKey] ?: TrainingGoal.HYPERTROPHY.name
        TrainingGoal.entries.find { it.name == name } ?: TrainingGoal.HYPERTROPHY
    }

    suspend fun setTrainingGoal(goal: TrainingGoal) {
        context.dataStore.edit { prefs ->
            prefs[goalKey] = goal.name
        }
    }

    val trainerSettings: Flow<TrainerSettings> = context.dataStore.data.map { prefs ->
        val splitName = prefs[splitTypeKey] ?: SplitType.FULL_BODY.name
        val split = SplitType.entries.find { it.name == splitName } ?: SplitType.FULL_BODY

        val days = prefs[daysPerWeekKey] ?: 3

        val priorityRaw = prefs[priorityGroupsKey] ?: ""
        val priority = if (priorityRaw.isBlank()) emptyList()
        else priorityRaw.split(",").mapNotNull { g ->
            MuscleGroup.entries.find { it.name == g }
        }

        val customRaw = prefs[customSplitDaysKey] ?: ""
        val customDays = parseCustomSplitDays(customRaw)

        val warmup = prefs[includeWarmupKey] ?: true
        val deload = prefs[autoDeloadKey] ?: true
        val deloadInterval = prefs[deloadIntervalKey] ?: 4

        val progName = prefs[progressionTypeKey] ?: ProgressionType.DOUBLE.name
        val progression = ProgressionType.entries.find { it.name == progName } ?: ProgressionType.DOUBLE

        TrainerSettings(
            splitType = split,
            daysPerWeek = days,
            priorityGroups = priority,
            customSplitDays = customDays,
            includeWarmup = warmup,
            autoDeload = deload,
            deloadIntervalWeeks = deloadInterval,
            progressionType = progression
        )
    }

    suspend fun updateTrainerSettings(settings: TrainerSettings) {
        context.dataStore.edit { prefs ->
            prefs[splitTypeKey] = settings.splitType.name
            prefs[daysPerWeekKey] = settings.daysPerWeek
            prefs[priorityGroupsKey] = settings.priorityGroups.joinToString(",") { it.name }
            prefs[customSplitDaysKey] = serializeCustomSplitDays(settings.customSplitDays)
            prefs[includeWarmupKey] = settings.includeWarmup
            prefs[autoDeloadKey] = settings.autoDeload
            prefs[deloadIntervalKey] = settings.deloadIntervalWeeks
            prefs[progressionTypeKey] = settings.progressionType.name
        }
    }

    private fun parseCustomSplitDays(raw: String): Map<Int, List<MuscleGroup>> {
        if (raw.isBlank()) return emptyMap()
        val result = mutableMapOf<Int, List<MuscleGroup>>()
        raw.split(";").forEach { dayEntry ->
            val parts = dayEntry.split(":")
            if (parts.size == 2) {
                val dayIndex = parts[0].toIntOrNull() ?: return@forEach
                val groups = parts[1].split(",").mapNotNull { g ->
                    MuscleGroup.entries.find { it.name == g }
                }
                result[dayIndex] = groups
            }
        }
        return result
    }

    private fun serializeCustomSplitDays(days: Map<Int, List<MuscleGroup>>): String {
        return days.entries.joinToString(";") { (index, groups) ->
            "$index:${groups.joinToString(",") { it.name }}"
        }
    }
}
