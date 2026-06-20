package com.example.gymprogress.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

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
    private val bodyWeightKey = stringPreferencesKey("body_weight_kg")
    private val scoringSystemKey = stringPreferencesKey("scoring_system")
    private val heightCmKey = stringPreferencesKey("height_cm")
    private val genderKey = stringPreferencesKey("user_gender")
    private val chartRangeKey = stringPreferencesKey("chart_range")
    private val chartMetricKey = stringPreferencesKey("chart_metric")
    private val timerSoundKey = booleanPreferencesKey("timer_sound_enabled")
    private val timerVibrationKey = booleanPreferencesKey("timer_vibration_enabled")
    private val membershipExpiryKey = stringPreferencesKey("membership_expiry")

    val trainingGoal: Flow<TrainingGoal> = context.dataStore.data.map { prefs ->
        val name = prefs[goalKey] ?: TrainingGoal.HYPERTROPHY.name
        TrainingGoal.entries.find { it.name == name } ?: TrainingGoal.HYPERTROPHY
    }

    suspend fun setTrainingGoal(goal: TrainingGoal) {
        context.dataStore.edit { prefs ->
            prefs[goalKey] = goal.name
        }
    }

    val scoringSystem: Flow<ScoringSystem> = context.dataStore.data.map { prefs ->
        val name = prefs[scoringSystemKey] ?: ScoringSystem.SIMPLIFIED.name
        ScoringSystem.entries.find { it.name == name } ?: ScoringSystem.SIMPLIFIED
    }

    suspend fun setScoringSystem(system: ScoringSystem) {
        context.dataStore.edit { prefs ->
            prefs[scoringSystemKey] = system.name
        }
    }

    val bodyWeightKg: Flow<Double?> = context.dataStore.data.map { prefs ->
        prefs[bodyWeightKey]?.toDoubleOrNull()?.takeIf { it > 0.0 }
    }

    suspend fun setBodyWeightKg(value: Double?) {
        context.dataStore.edit { prefs ->
            if (value == null || value <= 0.0) {
                prefs.remove(bodyWeightKey)
            } else {
                prefs[bodyWeightKey] = value.toString()
            }
        }
    }

    val heightCm: Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[heightCmKey]?.toIntOrNull()?.takeIf { it in 50..300 }
    }

    suspend fun setHeightCm(value: Int?) {
        context.dataStore.edit { prefs ->
            if (value == null || value !in 50..300) {
                prefs.remove(heightCmKey)
            } else {
                prefs[heightCmKey] = value.toString()
            }
        }
    }

    val gender: Flow<Gender?> = context.dataStore.data.map { prefs ->
        prefs[genderKey]?.let { name -> Gender.entries.find { it.name == name } }
    }

    suspend fun setGender(value: Gender?) {
        context.dataStore.edit { prefs ->
            if (value == null) prefs.remove(genderKey)
            else prefs[genderKey] = value.name
        }
    }

    val chartRange: Flow<ChartRange> = context.dataStore.data.map { prefs ->
        val name = prefs[chartRangeKey] ?: ChartRange.THREE_MONTHS.name
        ChartRange.entries.find { it.name == name } ?: ChartRange.THREE_MONTHS
    }

    suspend fun setChartRange(range: ChartRange) {
        context.dataStore.edit { prefs ->
            prefs[chartRangeKey] = range.name
        }
    }

    val chartMetric: Flow<ChartMetric> = context.dataStore.data.map { prefs ->
        val name = prefs[chartMetricKey] ?: ChartMetric.E1RM.name
        ChartMetric.entries.find { it.name == name } ?: ChartMetric.E1RM
    }

    suspend fun setChartMetric(metric: ChartMetric) {
        context.dataStore.edit { prefs ->
            prefs[chartMetricKey] = metric.name
        }
    }

    /** Звуковой сигнал на финале таймера отдыха (3 коротких бипа + длинный по нулю). */
    val timerSoundEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[timerSoundKey] ?: true
    }

    suspend fun setTimerSoundEnabled(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[timerSoundKey] = value
        }
    }

    /** Вибрация на финале таймера отдыха. */
    val timerVibrationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[timerVibrationKey] ?: true
    }

    suspend fun setTimerVibrationEnabled(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[timerVibrationKey] = value
        }
    }

    /** Дата окончания абонемента (ISO `LocalDate.toString()`). `null` — не указана. */
    val membershipExpiryDate: Flow<LocalDate?> = context.dataStore.data.map { prefs ->
        prefs[membershipExpiryKey]?.let { raw ->
            try {
                LocalDate.parse(raw)
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun setMembershipExpiryDate(value: LocalDate?) {
        context.dataStore.edit { prefs ->
            if (value == null) {
                prefs.remove(membershipExpiryKey)
            } else {
                prefs[membershipExpiryKey] = value.toString()
            }
        }
    }

    /** Проверка: все обязательные поля антропометрии заполнены для упрощённой системы */
    val isAnthropometryComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        val bw = prefs[bodyWeightKey]?.toDoubleOrNull()?.takeIf { it > 0.0 }
        bw != null
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
