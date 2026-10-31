package com.example.gymprogress.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.gymprogress.AppVersion
import com.example.gymprogress.data.AppDatabase
import com.example.gymprogress.data.ChartMetric
import com.example.gymprogress.data.ChartRange
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.Gender
import com.example.gymprogress.data.MuscleGroup
import com.example.gymprogress.data.ProgressionType
import com.example.gymprogress.data.ScoringSystem
import com.example.gymprogress.data.SettingsRepository
import com.example.gymprogress.data.SettingsSnapshot
import com.example.gymprogress.data.SplitType
import com.example.gymprogress.data.TrainerSettings
import com.example.gymprogress.data.TrainingGoal
import com.example.gymprogress.data.WorkoutEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class BackupCounts(
    val exercises: Int,
    val entries: Int
)

data class ImportCounts(
    val exercises: Int,
    val entries: Int
)

data class MergePreview(
    val addedExercises: Int,
    val updatedExercises: Int,
    val addedEntries: Int,
    val matchedExercises: Int,
    val matchedEntries: Int
)

data class ImportPreview(
    val file: ImportCounts,
    val current: ImportCounts,
    val merge: MergePreview,
    val appVersion: String,
    val exportedAt: String,
    val hasSettings: Boolean
)

enum class ImportStrategy { MERGE, REPLACE }

data class ImportResult(
    val strategy: ImportStrategy,
    val addedExercises: Int,
    val updatedExercises: Int,
    val addedEntries: Int,
    val matchedExercises: Int,
    val matchedEntries: Int,
    val deletedExercises: Int = 0,
    val deletedEntries: Int = 0,
    val safetyBackupUri: Uri? = null,
    val settingsError: String? = null
)

class BackupRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val exerciseDao = db.exerciseDao()
    private val workoutDao = db.workoutDao()
    private val resolver = context.contentResolver

    suspend fun exportJson(uri: Uri): BackupCounts {
        val backup = buildBackupFile()
        val bytes = BackupJson.encodeToString(backup).toByteArray(Charsets.UTF_8)
        withContext(Dispatchers.IO) { writeBytes(uri, bytes) }
        return BackupCounts(backup.exercises.size, backup.entries.size)
    }

    suspend fun exportCsv(uri: Uri): Int {
        val exercises = exerciseDao.getAllOnce()
        val entries = workoutDao.getAllOnce()
        val csv = BackupCsv.buildCsv(exercises, entries)
        withContext(Dispatchers.IO) { writeBytes(uri, csv.toByteArray(Charsets.UTF_8)) }
        return entries.size
    }

    suspend fun createSafetyBackup(): Uri {
        val backup = buildBackupFile()
        val dir = File(context.cacheDir, "backups").apply { mkdirs() }
        cleanupOldBackups(dir)
        val name = "backup_before_import_${
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now())
        }.json"
        return withContext(Dispatchers.IO) {
            val file = File(dir, name)
            writeJsonToFile(file, backup)
            runCatching {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }.getOrElse { error ->
                throw BackupException("Не удалось подготовить файл страховочного бэкапа", error)
            }
        }
    }

    suspend fun readPreview(uri: Uri): ImportPreview {
        val backup = readBackup(uri)
        val currentExercises = exerciseDao.getAllOnce()
        val currentEntries = workoutDao.getAllOnce()
        val plan = ImportMerger.planMerge(currentExercises, currentEntries, backup)
        return ImportPreview(
            file = ImportCounts(backup.exercises.size, backup.entries.size),
            current = ImportCounts(currentExercises.size, currentEntries.size),
            merge = MergePreview(
                addedExercises = plan.addedExercises,
                updatedExercises = plan.updatedExercises.size,
                addedEntries = plan.addedEntries,
                matchedExercises = plan.matchedExercises,
                matchedEntries = plan.matchedEntries
            ),
            appVersion = backup.appVersion,
            exportedAt = backup.exportedAt,
            hasSettings = backup.settings != null || backup.trainerSettings != null
        )
    }

    suspend fun importMerge(uri: Uri, applySettings: Boolean): ImportResult {
        val backup = readBackup(uri)
        val currentExercises = exerciseDao.getAllOnce()
        val currentEntries = workoutDao.getAllOnce()
        val plan = ImportMerger.planMerge(currentExercises, currentEntries, backup)
        val settingsSnapshot = if (applySettings) {
            mergeSettings(settingsRepository.readSnapshot(), backup.settings, backup.trainerSettings)
        } else {
            null
        }

        val result = db.withTransaction {
            plan.updatedExercises.forEach { exercise ->
                exerciseDao.update(exercise)
                workoutDao.updateExerciseNameForEntries(exercise.id, exercise.name)
            }

            val newIds = if (plan.newExercises.isEmpty()) {
                emptyList()
            } else {
                exerciseDao.insertAll(plan.newExercises)
            }
            val newIdsByClientId = plan.newExercises.mapIndexed { index, exercise ->
                exercise.clientId to newIds[index]
            }.toMap()

            val entries = plan.newEntries.map { planned ->
                planned.entry.copy(
                    exerciseId = planned.entry.exerciseId
                        ?: planned.exerciseClientId?.let { newIdsByClientId[it] }
                )
            }
            if (entries.isNotEmpty()) {
                workoutDao.insertEntries(entries)
            }

            ImportResult(
                strategy = ImportStrategy.MERGE,
                addedExercises = plan.newExercises.size,
                updatedExercises = plan.updatedExercises.size,
                addedEntries = entries.size,
                matchedExercises = plan.matchedExercises,
                matchedEntries = plan.matchedEntries
            )
        }

        return result.withSettings(settingsSnapshot)
    }

    suspend fun importReplace(uri: Uri, applySettings: Boolean): ImportResult {
        val backup = readBackup(uri)
        val currentExercises = exerciseDao.getAllOnce()
        val currentEntries = workoutDao.getAllOnce()
        val settingsSnapshot = if (applySettings) {
            mergeSettings(settingsRepository.readSnapshot(), backup.settings, backup.trainerSettings)
        } else {
            null
        }

        // Страховочный бэкап обязателен и создаётся до изменения данных.
        val safetyUri = createSafetyBackup()

        val result = db.withTransaction {
            workoutDao.deleteAll()
            exerciseDao.deleteAll()

            val newIds = if (backup.exercises.isEmpty()) {
                emptyList()
            } else {
                exerciseDao.insertAll(backup.exercises.map { it.toExercise() })
            }
            val idsByClientId = backup.exercises.mapIndexed { index, exercise ->
                exercise.clientId to newIds[index]
            }.toMap()

            val entries = backup.entries.map { fileEntry ->
                val exercise = fileEntry.exerciseClientId?.let { clientId ->
                    backup.exercises.firstOrNull { it.clientId == clientId }
                }
                WorkoutEntry(
                    date = fileEntry.date,
                    exerciseName = exercise?.name ?: fileEntry.exerciseName,
                    weight = fileEntry.weight,
                    reps = fileEntry.reps,
                    exerciseId = fileEntry.exerciseClientId?.let { idsByClientId[it] },
                    clientId = fileEntry.clientId
                )
            }
            if (entries.isNotEmpty()) {
                workoutDao.insertEntries(entries)
            }

            ImportResult(
                strategy = ImportStrategy.REPLACE,
                addedExercises = backup.exercises.size,
                updatedExercises = 0,
                addedEntries = entries.size,
                matchedExercises = 0,
                matchedEntries = 0,
                deletedExercises = currentExercises.size,
                deletedEntries = currentEntries.size,
                safetyBackupUri = safetyUri
            )
        }

        return result.withSettings(settingsSnapshot)
    }

    private suspend fun ImportResult.withSettings(snapshot: SettingsSnapshot?): ImportResult {
        if (snapshot == null) return this
        return runCatching {
            settingsRepository.applySnapshot(snapshot)
            this
        }.getOrElse { error ->
            copy(settingsError = "Данные импортированы, но настройки не применены: ${error.message ?: "ошибка"}")
        }
    }

    private suspend fun buildBackupFile(): BackupFileV1 {
        val exercises = exerciseDao.getAllOnce()
        val entries = workoutDao.getAllOnce()
        val settings = settingsRepository.readSnapshot()
        val exercisesById = exercises.associateBy { it.id }

        return BackupFileV1(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            appVersion = AppVersion.NAME,
            exportedAt = Instant.now().toString(),
            settings = settings.toBackupSettings(),
            trainerSettings = settings.trainerSettings.toBackupTrainerSettings(),
            exercises = exercises.map { exercise ->
                BackupExerciseV1(
                    clientId = exercise.clientId,
                    name = exercise.name,
                    muscleGroup = exercise.muscleGroup,
                    exerciseType = exercise.exerciseType,
                    isBodyweight = exercise.isBodyweight,
                    isArchived = exercise.isArchived
                )
            },
            entries = entries.map { entry ->
                BackupEntryV1(
                    clientId = entry.clientId,
                    exerciseClientId = entry.exerciseId
                        ?.let { id -> exercisesById[id]?.clientId },
                    exerciseName = entry.exerciseName,
                    date = entry.date,
                    weight = entry.weight,
                    reps = entry.reps
                )
            }
        )
    }

    private suspend fun readBackup(uri: Uri): BackupFileV1 {
        val bytes = withContext(Dispatchers.IO) {
            try {
                resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw BackupException("Не удалось открыть файл")
            } catch (error: BackupException) {
                throw error
            } catch (error: Exception) {
                throw BackupException("Не удалось прочитать файл", error)
            }
        }
        if (bytes.size.toLong() > MAX_BACKUP_BYTES) {
            throw BackupException("Файл слишком большой")
        }
        val text = try {
            bytes.toString(Charsets.UTF_8)
        } catch (error: Exception) {
            throw BackupException("Файл повреждён или имеет неверную кодировку", error)
        }
        val backup = try {
            BackupJson.decodeFromString<BackupFileV1>(text)
        } catch (error: SerializationException) {
            throw BackupException("Файл повреждён или имеет неверный формат", error)
        } catch (error: IllegalArgumentException) {
            throw BackupException("Файл повреждён или имеет неверный формат", error)
        }
        validateBackup(backup)
        return backup
    }

    private fun validateBackup(backup: BackupFileV1) {
        if (backup.schemaVersion > BACKUP_SCHEMA_VERSION) {
            throw BackupException("Файл создан более новой версией приложения. Обновите приложение.")
        }
        if (backup.schemaVersion < 1) {
            throw BackupException("Неподдерживаемая версия формата файла")
        }

        val exerciseClientIds = mutableSetOf<String>()
        val exerciseNameKeys = mutableSetOf<String>()
        backup.exercises.forEach { exercise ->
            if (exercise.clientId.isBlank()) throw BackupException("В файле пустой clientId упражнения")
            if (exercise.name.isBlank()) throw BackupException("В файле пустое имя упражнения")
            if (!exerciseClientIds.add(exercise.clientId)) {
                throw BackupException("В файле повторяется упражнение: ${exercise.name}")
            }
            if (!exerciseNameKeys.add(FormatUtils.normalizeExerciseNameKey(exercise.name))) {
                throw BackupException("В файле два упражнения с одинаковым именем: ${exercise.name}")
            }
        }

        val entryClientIds = mutableSetOf<String>()
        backup.entries.forEach { entry ->
            if (entry.clientId.isBlank()) throw BackupException("В файле пустой clientId записи")
            if (entry.exerciseName.isBlank()) throw BackupException("В файле пустое имя упражнения у записи")
            if (!entryClientIds.add(entry.clientId)) {
                throw BackupException("В файле повторяется запись: ${entry.date}")
            }
            if (entry.weight.isFinite().not() || entry.weight < 0.0) {
                throw BackupException("В файле некорректный вес записи")
            }
            if (entry.reps.isBlank()) throw BackupException("В файле пустые повторы записи")
            val dateValid = runCatching { LocalDate.parse(entry.date) }.isSuccess
            if (!dateValid) throw BackupException("В файле некорректная дата записи")
            if (entry.exerciseClientId != null && entry.exerciseClientId.isBlank()) {
                throw BackupException("В файле пустая ссылка на упражнение")
            }
        }
    }

    private fun writeBytes(uri: Uri, bytes: ByteArray) {
        try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw BackupException("Не удалось открыть файл для записи")
        } catch (error: BackupException) {
            throw error
        } catch (error: Exception) {
            throw BackupException("Не удалось записать файл", error)
        }
    }

    private fun writeJsonToFile(file: File, backup: BackupFileV1) {
        try {
            file.writeText(BackupJson.encodeToString(backup), Charsets.UTF_8)
        } catch (error: IOException) {
            throw BackupException("Не удалось создать страховочный бэкап", error)
        }
    }

    private fun cleanupOldBackups(dir: File) {
        val cutoff = System.currentTimeMillis() - MAX_SAFETY_BACKUP_AGE_MILLIS
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                runCatching { file.delete() }
            }
        }
    }

    private fun BackupExerciseV1.toExercise(): Exercise = Exercise(
        name = name,
        muscleGroup = muscleGroup,
        exerciseType = exerciseType,
        isBodyweight = isBodyweight,
        isArchived = isArchived,
        clientId = clientId,
        nameKey = FormatUtils.normalizeExerciseNameKey(name)
    )

    private fun SettingsSnapshot.toBackupSettings(): BackupSettingsV1 = BackupSettingsV1(
        trainingGoal = trainingGoal.name,
        scoringSystem = scoringSystem.name,
        bodyWeightKg = bodyWeightKg,
        heightCm = heightCm,
        gender = gender?.name,
        chartRange = chartRange.name,
        chartMetric = chartMetric.name,
        timerSoundEnabled = timerSoundEnabled,
        timerVibrationEnabled = timerVibrationEnabled,
        membershipExpiryDate = membershipExpiryDate?.toString()
    )

    private fun TrainerSettings.toBackupTrainerSettings(): BackupTrainerSettingsV1 =
        BackupTrainerSettingsV1(
            splitType = splitType.name,
            daysPerWeek = daysPerWeek,
            priorityGroups = priorityGroups.map { it.name },
            customSplitDays = customSplitDays.map { (day, groups) ->
                BackupCustomSplitDayV1(day, groups.map { it.name })
            },
            includeWarmup = includeWarmup,
            autoDeload = autoDeload,
            deloadIntervalWeeks = deloadIntervalWeeks,
            progressionType = progressionType.name
        )

    private fun mergeSettings(
        current: SettingsSnapshot,
        settings: BackupSettingsV1?,
        trainer: BackupTrainerSettingsV1?
    ): SettingsSnapshot {
        var result = current

        settings?.trainingGoal?.let { name ->
            TrainingGoal.entries.find { it.name == name }?.let { result = result.copy(trainingGoal = it) }
        }
        settings?.scoringSystem?.let { name ->
            ScoringSystem.entries.find { it.name == name }?.let { result = result.copy(scoringSystem = it) }
        }
        settings?.bodyWeightKg?.takeIf { it > 0.0 }?.let { result = result.copy(bodyWeightKg = it) }
        settings?.heightCm?.takeIf { it in 50..300 }?.let { result = result.copy(heightCm = it) }
        settings?.gender?.let { name ->
            Gender.entries.find { it.name == name }?.let { result = result.copy(gender = it) }
        }
        settings?.chartRange?.let { name ->
            ChartRange.entries.find { it.name == name }?.let { result = result.copy(chartRange = it) }
        }
        settings?.chartMetric?.let { name ->
            ChartMetric.entries.find { it.name == name }?.let { result = result.copy(chartMetric = it) }
        }
        settings?.timerSoundEnabled?.let { result = result.copy(timerSoundEnabled = it) }
        settings?.timerVibrationEnabled?.let { result = result.copy(timerVibrationEnabled = it) }
        settings?.membershipExpiryDate?.let { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()?.let {
                result = result.copy(membershipExpiryDate = it)
            }
        }

        var trainerSettings = result.trainerSettings
        trainer?.splitType?.let { name ->
            SplitType.entries.find { it.name == name }?.let {
                trainerSettings = trainerSettings.copy(splitType = it)
            }
        }
        trainer?.daysPerWeek?.takeIf { it in 1..7 }?.let {
            trainerSettings = trainerSettings.copy(daysPerWeek = it)
        }
        trainer?.priorityGroups?.let { names ->
            trainerSettings = trainerSettings.copy(
                priorityGroups = names.mapNotNull { name -> MuscleGroup.entries.find { it.name == name } }
            )
        }
        trainer?.customSplitDays?.let { days ->
            trainerSettings = trainerSettings.copy(
                customSplitDays = days
                    .filter { it.dayIndex >= 0 }
                    .associate { day ->
                        day.dayIndex to day.groups.mapNotNull { name ->
                            MuscleGroup.entries.find { it.name == name }
                        }
                    }
            )
        }
        trainer?.includeWarmup?.let { trainerSettings = trainerSettings.copy(includeWarmup = it) }
        trainer?.autoDeload?.let { trainerSettings = trainerSettings.copy(autoDeload = it) }
        trainer?.deloadIntervalWeeks?.takeIf { it in 1..12 }?.let {
            trainerSettings = trainerSettings.copy(deloadIntervalWeeks = it)
        }
        trainer?.progressionType?.let { name ->
            ProgressionType.entries.find { it.name == name }?.let {
                trainerSettings = trainerSettings.copy(progressionType = it)
            }
        }
        return result.copy(trainerSettings = trainerSettings)
    }

    private companion object {
        const val MAX_BACKUP_BYTES = 50L * 1024 * 1024
        const val MAX_SAFETY_BACKUP_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}
