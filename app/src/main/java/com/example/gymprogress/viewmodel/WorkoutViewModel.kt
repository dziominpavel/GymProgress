package com.example.gymprogress.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.gymprogress.data.AiService
import com.example.gymprogress.data.AppDatabase
import com.example.gymprogress.data.ChartMetric
import com.example.gymprogress.data.ChartRange
import com.example.gymprogress.data.CompletedSet
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.ExerciseRecommendation
import com.example.gymprogress.data.ExerciseType
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.Gender
import com.example.gymprogress.data.ScoringEngine
import com.example.gymprogress.data.ScoringSystem
import com.example.gymprogress.data.SetType
import com.example.gymprogress.data.SettingsRepository
import com.example.gymprogress.data.SimplifiedScoreCalculator
import com.example.gymprogress.data.TrainerRecommendationEngine
import com.example.gymprogress.data.TrainerSettings
import com.example.gymprogress.data.TrainingGoal
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.data.WorkoutRecommendation
import com.example.gymprogress.data.WorkoutScoreCalculator
import com.example.gymprogress.data.backup.BackupException
import com.example.gymprogress.data.backup.BackupRepository
import com.example.gymprogress.data.backup.ImportPreview
import com.example.gymprogress.data.backup.ImportStrategy
import com.example.gymprogress.service.MembershipReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class TrainerRecommendationCore(
    val settings: TrainerSettings,
    val goal: TrainingGoal,
    val exercises: List<Exercise>,
    val history: List<WorkoutEntry>
)

private data class JournalSplitUiState(
    val sessionEntries: List<WorkoutEntry>,
    val sessionDate: String?,
    val titleOverride: String?,
    val dayMuscleGroups: List<String>
)

data class BackupUiState(
    val preview: ImportPreview? = null,
    val pendingUri: Uri? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val safetyBackupUri: Uri? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val workoutDao = db.workoutDao()
    private val exerciseDao = db.exerciseDao()
    private val settingsRepository = SettingsRepository(application)
    private val trainerEngine = TrainerRecommendationEngine()
    private val aiService = AiService()
    private val backupRepository = BackupRepository(application, db, settingsRepository)

    val isAiAvailable: Boolean get() = aiService.isAvailable()

    private val _aiAdvice = MutableStateFlow<String?>(null)
    val aiAdvice: StateFlow<String?> = _aiAdvice.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _backupState = MutableStateFlow(BackupUiState())
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    val allEntries: StateFlow<List<WorkoutEntry>> = workoutDao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExercises: StateFlow<List<Exercise>> = exerciseDao.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trainingGoal: StateFlow<TrainingGoal> = settingsRepository.trainingGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrainingGoal.HYPERTROPHY)

    val bodyWeightKg: StateFlow<Double?> = settingsRepository.bodyWeightKg
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val scoringSystem: StateFlow<ScoringSystem> = settingsRepository.scoringSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScoringSystem.SIMPLIFIED)

    val heightCm: StateFlow<Int?> = settingsRepository.heightCm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val gender: StateFlow<Gender?> = settingsRepository.gender
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val chartRange: StateFlow<ChartRange> = settingsRepository.chartRange
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChartRange.THREE_MONTHS)

    val chartMetric: StateFlow<ChartMetric> = settingsRepository.chartMetric
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChartMetric.E1RM)

    val timerSoundEnabled: StateFlow<Boolean> = settingsRepository.timerSoundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val timerVibrationEnabled: StateFlow<Boolean> = settingsRepository.timerVibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val membershipExpiryDate: StateFlow<LocalDate?> = settingsRepository.membershipExpiryDate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isAnthropometryComplete: StateFlow<Boolean> = settingsRepository.isAnthropometryComplete
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Текущий движок скоринга на основе выбранной системы */
    val scoringEngine: StateFlow<ScoringEngine> = scoringSystem
        .map { system ->
            when (system) {
                ScoringSystem.SIMPLIFIED -> SimplifiedScoreCalculator
                ScoringSystem.ADVANCED -> WorkoutScoreCalculator
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SimplifiedScoreCalculator)

    val trainerSettings: StateFlow<TrainerSettings> = settingsRepository.trainerSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrainerSettings())

    val workoutRecommendation: StateFlow<WorkoutRecommendation?> = combine(
        combine(
            settingsRepository.trainerSettings,
            settingsRepository.trainingGoal,
            exerciseDao.getAllExercises(),
            workoutDao.getAllEntries()
        ) { settings, goal, exercises, history ->
            TrainerRecommendationCore(settings, goal, exercises, history)
        },
        bodyWeightKg,
        scoringEngine,
        scoringSystem
    ) { core, bw, engine, system ->
        if (core.exercises.isEmpty()) null
        else trainerEngine.generateRecommendation(
            settings = core.settings,
            trainingGoal = core.goal,
            exercises = core.exercises,
            history = core.history,
            bodyWeightKg = bw,
            scoringEngine = engine,
            scoringSystem = system
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _journalSelectedDayIndex = MutableStateFlow<Int?>(null)
    val journalSelectedDayIndex: StateFlow<Int?> = _journalSelectedDayIndex.asStateFlow()

    private val _exerciseRecommendationForJournal = MutableStateFlow<ExerciseRecommendation?>(null)
    val exerciseRecommendationForJournal: StateFlow<ExerciseRecommendation?> = _exerciseRecommendationForJournal.asStateFlow()

    val journalSplitDayOptions: StateFlow<List<Pair<Int, String>>> = trainerSettings
        .map { trainerEngine.getSplitDayOptions(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val journalSplitUi: StateFlow<JournalSplitUiState> = combine(
        allEntries,
        trainerSettings,
        allExercises,
        _journalSelectedDayIndex
    ) { entries, settings, exercises, selectedDay ->
        val nextSession = trainerEngine.findNextDaySessionInSplit(settings, entries, exercises)
        val result = when {
            selectedDay != null -> trainerEngine.findSessionForDayIndex(settings, entries, exercises, selectedDay)
            else -> nextSession
        }
        val (sessionEntries, sessionDate, titleOverride) = when (val session = result) {
            null -> {
                if (selectedDay != null) {
                    val options = trainerEngine.getSplitDayOptions(settings)
                    val dayLabel = options.find { it.first == selectedDay }?.second ?: ""
                    Triple(emptyList(), null, "Прошлая тренировка в сплите: $dayLabel · нет записей")
                } else {
                    val fallback = findPreviousSession(entries, LocalDate.now())
                    Triple(
                        fallback?.first ?: emptyList(),
                        fallback?.second,
                        null
                    )
                }
            }
            else -> Triple(
                session.entries,
                session.date,
                "Прошлая тренировка в сплите: ${session.dayLabel} · ${FormatUtils.formatDate(session.date)}"
            )
        }
        val displayedDayIndex = when {
            selectedDay != null -> selectedDay
            else -> if (nextSession != null) {
                trainerEngine.getNextDayIndex(settings, entries, exercises)
            } else {
                null
            }
        }
        val dayMuscleGroups = displayedDayIndex?.let { dayIndex ->
            trainerEngine.getMuscleGroupsForDayPublic(settings, dayIndex).map { it.displayName }
        } ?: emptyList()
        JournalSplitUiState(sessionEntries, sessionDate, titleOverride, dayMuscleGroups)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        JournalSplitUiState(emptyList(), null, null, emptyList())
    )

    val previousSessionForJournal: StateFlow<List<WorkoutEntry>> = journalSplitUi
        .map { it.sessionEntries }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val previousSessionDateForJournal: StateFlow<String?> = journalSplitUi
        .map { it.sessionDate }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val previousSessionTitleOverride: StateFlow<String?> = journalSplitUi
        .map { it.titleOverride }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setJournalPreviousDay(dayIndex: Int?) {
        _journalSelectedDayIndex.value = dayIndex
    }

    val previousSessionDayMuscleGroups: StateFlow<List<String>> = journalSplitUi
        .map { it.dayMuscleGroups }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun findPreviousSession(
        entries: List<WorkoutEntry>,
        today: LocalDate
    ): Pair<List<WorkoutEntry>, String?>? {
        val todayStorage = FormatUtils.toStorageDate(today)
        val grouped = entries.filter { it.date != todayStorage }.groupBy { it.date }
        if (grouped.isEmpty()) return null

        val sameDay = grouped.entries
            .filter { (d, _) -> FormatUtils.parseStorageDate(d)?.dayOfWeek == today.dayOfWeek }
            .maxByOrNull { (d, _) -> FormatUtils.parseStorageDate(d) ?: LocalDate.MIN }

        if (sameDay != null) return Pair(sameDay.value, sameDay.key)

        val fallback = grouped.entries
            .maxByOrNull { (d, _) -> FormatUtils.parseStorageDate(d) ?: LocalDate.MIN }

        return fallback?.let { Pair(it.value, it.key) }
    }

    private val _selectedExercise = MutableStateFlow<String?>(null)
    val selectedExercise: StateFlow<String?> = _selectedExercise.asStateFlow()

    val entriesForSelectedExercise: StateFlow<List<WorkoutEntry>> =
        combine(_selectedExercise, allEntries, allExercises) { name, entries, exercises ->
            if (name == null) {
                emptyList()
            } else {
                val nameKey = FormatUtils.normalizeExerciseNameKey(name)
                val selected = exercises.firstOrNull { it.nameKey == nameKey }
                entries.filter { entry ->
                    when {
                        selected != null && entry.exerciseId != null ->
                            entry.exerciseId == selected.id
                        selected != null ->
                            entry.exerciseId == null &&
                                FormatUtils.normalizeExerciseNameKey(entry.exerciseName) == nameKey
                        else ->
                            FormatUtils.normalizeExerciseNameKey(entry.exerciseName) == nameKey
                    }
                }.sortedWith(compareBy({ it.date }, { it.id }))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedExerciseType: StateFlow<ExerciseType> = _selectedExercise
        .flatMapLatest { name ->
            if (name != null) {
                exerciseDao.getExerciseByName(name).map { exercise ->
                    val typeName = exercise?.exerciseType ?: ExerciseType.COMPOUND.name
                    ExerciseType.entries.find { it.name == typeName } ?: ExerciseType.COMPOUND
                }
            } else flowOf(ExerciseType.COMPOUND)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExerciseType.COMPOUND)

    private fun safeDb(errorFallback: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: errorFallback
                Log.e(TAG, errorFallback, e)
            }
        }
    }

    fun addEntry(date: String, exerciseName: String, weight: Double, reps: String) {
        safeDb("Не удалось добавить запись") {
            val cleanName = exerciseName.trim()
            val exercise = exerciseDao.getByNameKey(FormatUtils.normalizeExerciseNameKey(cleanName))
            if (exercise?.isArchived == true) {
                _errorMessage.value = "Упражнение в архиве. Восстановите его, чтобы добавить запись."
                return@safeDb
            }
            workoutDao.insert(
                WorkoutEntry(
                    date = date,
                    exerciseName = cleanName,
                    weight = weight,
                    reps = reps.trim(),
                    exerciseId = exercise?.id
                )
            )
        }
    }

    fun updateEntry(entry: WorkoutEntry) {
        safeDb("Не удалось обновить запись") {
            workoutDao.update(entry)
        }
    }

    fun deleteEntry(entry: WorkoutEntry) {
        safeDb("Не удалось удалить запись") {
            workoutDao.delete(entry)
        }
    }

    fun selectExercise(name: String?) {
        _selectedExercise.value = name
    }

    fun addExercise(
        name: String,
        muscleGroup: String,
        exerciseType: String = ExerciseType.COMPOUND.name,
        isBodyweight: Boolean = false
    ) {
        safeDb("Не удалось добавить упражнение") {
            val cleanName = name.trim()
            if (cleanName.isEmpty()) {
                _errorMessage.value = "Название упражнения не может быть пустым"
                return@safeDb
            }
            val nameKey = FormatUtils.normalizeExerciseNameKey(cleanName)
            if (exerciseDao.countByNameKey(nameKey) > 0) {
                val existing = exerciseDao.getByNameKey(nameKey)
                _errorMessage.value = if (existing?.isArchived == true) {
                    "Упражнение с таким именем уже есть в архиве. Восстановите его."
                } else {
                    "Упражнение с таким именем уже есть"
                }
                return@safeDb
            }
            exerciseDao.insert(
                Exercise(
                    name = cleanName,
                    muscleGroup = muscleGroup,
                    exerciseType = exerciseType,
                    isBodyweight = isBodyweight,
                    nameKey = nameKey
                )
            )
        }
    }

    fun updateExercise(exercise: Exercise) {
        safeDb("Не удалось обновить упражнение") {
            val cleanName = exercise.name.trim()
            if (cleanName.isEmpty()) {
                _errorMessage.value = "Название упражнения не может быть пустым"
                return@safeDb
            }
            val nameKey = FormatUtils.normalizeExerciseNameKey(cleanName)
            val duplicate = exerciseDao.getByNameKey(nameKey)
            if (duplicate != null && duplicate.id != exercise.id) {
                _errorMessage.value = if (duplicate.isArchived) {
                    "Упражнение с таким именем уже есть в архиве. Восстановите его."
                } else {
                    "Упражнение с таким именем уже есть"
                }
                return@safeDb
            }
            val current = exerciseDao.getById(exercise.id)
            val finalExercise = exercise.copy(name = cleanName, nameKey = nameKey)
            db.withTransaction {
                exerciseDao.update(finalExercise)
                if (current != null && current.name != finalExercise.name) {
                    workoutDao.updateExerciseNameForEntries(finalExercise.id, finalExercise.name)
                }
            }
        }
    }

    fun archiveExercise(exercise: Exercise) {
        safeDb("Не удалось архивировать упражнение") {
            exerciseDao.update(exercise.copy(isArchived = true))
        }
    }

    fun restoreExercise(exercise: Exercise) {
        safeDb("Не удалось восстановить упражнение") {
            exerciseDao.update(exercise.copy(isArchived = false))
        }
    }

    fun deleteExercisePermanently(exercise: Exercise) {
        safeDb("Не удалось удалить упражнение") {
            exerciseDao.delete(exercise)
        }
    }

    fun setTrainingGoal(goal: TrainingGoal) {
        safeDb("Не удалось сохранить цель") {
            settingsRepository.setTrainingGoal(goal)
        }
    }

    fun setBodyWeightKg(value: Double?) {
        safeDb("Не удалось сохранить вес") {
            settingsRepository.setBodyWeightKg(value)
        }
    }

    fun setScoringSystem(system: ScoringSystem) {
        safeDb("Не удалось сохранить систему оценки") {
            settingsRepository.setScoringSystem(system)
        }
    }

    fun setHeightCm(value: Int?) {
        safeDb("Не удалось сохранить рост") {
            settingsRepository.setHeightCm(value)
        }
    }

    fun setGender(value: Gender?) {
        safeDb("Не удалось сохранить пол") {
            settingsRepository.setGender(value)
        }
    }

    fun setChartRange(range: ChartRange) {
        safeDb("Не удалось сохранить диапазон графика") {
            settingsRepository.setChartRange(range)
        }
    }

    fun setChartMetric(metric: ChartMetric) {
        safeDb("Не удалось сохранить метрику графика") {
            settingsRepository.setChartMetric(metric)
        }
    }

    fun setTimerSoundEnabled(value: Boolean) {
        safeDb("Не удалось сохранить настройку звука таймера") {
            settingsRepository.setTimerSoundEnabled(value)
        }
    }

    fun setTimerVibrationEnabled(value: Boolean) {
        safeDb("Не удалось сохранить настройку вибрации таймера") {
            settingsRepository.setTimerVibrationEnabled(value)
        }
    }

    fun setMembershipExpiryDate(value: LocalDate?) {
        safeDb("Не удалось сохранить дату абонемента") {
            settingsRepository.setMembershipExpiryDate(value)
        }
    }

    fun updateTrainerSettings(settings: TrainerSettings) {
        safeDb("Не удалось сохранить настройки тренера") {
            settingsRepository.updateTrainerSettings(settings)
        }
    }

    fun clearBackupMessage() {
        _backupState.value = _backupState.value.copy(message = null, safetyBackupUri = null)
    }

    fun dismissBackupPreview() {
        _backupState.value = _backupState.value.copy(preview = null, pendingUri = null)
    }

    fun exportBackupJson(uri: Uri) = launchBackup("Не удалось экспортировать JSON") {
        val counts = backupRepository.exportJson(uri)
        _backupState.value = _backupState.value.copy(
            message = "Экспортировано: ${counts.exercises} упражнений, ${counts.entries} записей"
        )
    }

    fun exportBackupCsv(uri: Uri) = launchBackup("Не удалось экспортировать CSV") {
        val count = backupRepository.exportCsv(uri)
        _backupState.value = _backupState.value.copy(
            message = "Экспортировано записей: $count"
        )
    }

    fun previewBackup(uri: Uri) = launchBackup("Не удалось прочитать файл") {
        val preview = backupRepository.readPreview(uri)
        _backupState.value = _backupState.value.copy(
            preview = preview,
            pendingUri = uri,
            message = null
        )
    }

    fun confirmBackupImport(strategy: ImportStrategy, applySettings: Boolean) {
        val state = _backupState.value
        val uri = state.pendingUri
        if (uri == null || state.preview == null) {
            _errorMessage.value = "Сначала выберите файл для импорта"
            return
        }
        launchBackup("Не удалось импортировать данные") {
            val result = when (strategy) {
                ImportStrategy.MERGE -> backupRepository.importMerge(uri, applySettings)
                ImportStrategy.REPLACE -> backupRepository.importReplace(uri, applySettings)
            }
            val message = when (strategy) {
                ImportStrategy.MERGE ->
                    "Импорт завершён: добавлено ${result.addedEntries} записей и " +
                        "${result.addedExercises} упражнений, пропущено ${result.matchedEntries}"
                ImportStrategy.REPLACE ->
                    "Данные заменены: ${result.addedEntries} записей, " +
                        "${result.addedExercises} упражнений"
            }
            _backupState.value = _backupState.value.copy(
                preview = null,
                pendingUri = null,
                message = message,
                safetyBackupUri = result.safetyBackupUri
            )
            result.settingsError?.let { _errorMessage.value = it }
        }
    }

    private fun launchBackup(errorFallback: String, block: suspend () -> Unit) {
        if (_backupState.value.busy) return
        _backupState.value = _backupState.value.copy(busy = true, message = null)
        viewModelScope.launch {
            try {
                block()
            } catch (e: BackupException) {
                _errorMessage.value = e.message ?: errorFallback
                Log.e(TAG, errorFallback, e)
            } catch (e: Exception) {
                _errorMessage.value = errorFallback
                Log.e(TAG, errorFallback, e)
            } finally {
                _backupState.value = _backupState.value.copy(busy = false)
            }
        }
    }

    /**
     * Рекомендация для диалога «Новая запись».
     * @param exerciseName имя из справочника (после сопоставления)
     * @param historyNameHint точная строка exerciseName из записи журнала (при «+» у прошлой тренировки)
     */
    fun loadExerciseRecommendationForJournal(
        exerciseName: String?,
        historyNameHint: String? = null
    ) {
        viewModelScope.launch {
            if (exerciseName == null) {
                _exerciseRecommendationForJournal.value = null
                return@launch
            }
            val exercise = allExercises.value.find { it.name == exerciseName } ?: run {
                _exerciseRecommendationForJournal.value = null
                return@launch
            }
            if (exercise.isArchived) {
                _exerciseRecommendationForJournal.value = null
                return@launch
            }
            _exerciseRecommendationForJournal.value = trainerEngine.getRecommendationForExercise(
                exercise = exercise,
                history = allEntries.value,
                allExercises = allExercises.value,
                trainingGoal = trainingGoal.value,
                settings = trainerSettings.value,
                bodyWeightKg = bodyWeightKg.value,
                scoringEngine = scoringEngine.value,
                scoringSystem = scoringSystem.value,
                historyNameHint = historyNameHint
            )
        }
    }

    fun clearExerciseRecommendationForJournal() {
        _exerciseRecommendationForJournal.value = null
    }

    fun askAi() {
        val rec = workoutRecommendation.value ?: return
        if (_aiLoading.value) return
        _aiLoading.value = true
        _aiAdvice.value = null
        viewModelScope.launch {
            try {
                val advice = aiService.getAdvice(
                    recommendation = rec,
                    history = allEntries.value,
                    exercises = allExercises.value,
                    settings = trainerSettings.value,
                    trainingGoal = trainingGoal.value
                )
                _aiAdvice.value = advice
            } catch (e: Exception) {
                _aiAdvice.value = "Ошибка: ${e.message}"
                Log.e(TAG, "AI advice failed", e)
            } finally {
                _aiLoading.value = false
            }
        }
    }

    fun clearAiAdvice() {
        _aiAdvice.value = null
    }

    fun saveCompletedWorkout(completedSets: List<CompletedSet>) {
        val today = FormatUtils.toStorageDate(LocalDate.now())
        val workingSets = completedSets.filter { it.setType == SetType.WORKING }
        val grouped = workingSets.groupBy { it.exerciseName }
        if (grouped.isEmpty()) return

        safeDb("Не удалось сохранить тренировку") {
            val exerciseByKey = grouped.keys.associate { name ->
                val key = FormatUtils.normalizeExerciseNameKey(name)
                key to exerciseDao.getByNameKey(key)
            }
            val archived = exerciseByKey.values.filterNotNull().firstOrNull { it.isArchived }
            if (archived != null) {
                _errorMessage.value =
                    "Упражнение «${archived.name}» в архиве. Восстановите его, чтобы сохранить тренировку."
                return@safeDb
            }

            val toInsert = mutableListOf<WorkoutEntry>()
            grouped.forEach { (name, sets) ->
                val exerciseId = exerciseByKey[FormatUtils.normalizeExerciseNameKey(name)]?.id
                val distinctWeights = sets.map { it.weight }.distinct()
                if (distinctWeights.size == 1) {
                    val reps = sets.joinToString(",") { it.reps.toString() }
                    toInsert.add(
                        WorkoutEntry(
                            date = today,
                            exerciseName = name.trim(),
                            weight = distinctWeights.first(),
                            reps = reps.trim(),
                            exerciseId = exerciseId
                        )
                    )
                } else {
                    val mainWeight = sets.groupBy { it.weight }
                        .maxByOrNull { it.value.size }?.key ?: sets.first().weight
                    val mainSets = sets.filter { it.weight == mainWeight }
                    val reps = mainSets.joinToString(",") { it.reps.toString() }
                    toInsert.add(
                        WorkoutEntry(
                            date = today,
                            exerciseName = name.trim(),
                            weight = mainWeight,
                            reps = reps.trim(),
                            exerciseId = exerciseId
                        )
                    )
                    val otherGroups = sets.filter { it.weight != mainWeight }.groupBy { it.weight }
                    otherGroups.forEach { (w, wSets) ->
                        val otherReps = wSets.joinToString(",") { it.reps.toString() }
                        toInsert.add(
                            WorkoutEntry(
                                date = today,
                                exerciseName = name.trim(),
                                weight = w,
                                reps = otherReps.trim(),
                                exerciseId = exerciseId
                            )
                        )
                    }
                }
            }
            if (toInsert.isNotEmpty()) {
                workoutDao.insertEntries(toInsert)
            }
        }
    }

    init {
        // Перепланируем ежедневную проверку срока абонемента при изменении даты.
        // null → отменяем воркер; не-null → перепланируем с пересчётом initialDelay.
        viewModelScope.launch {
            membershipExpiryDate.collect { expiry ->
                val context = getApplication<Application>()
                if (expiry == null) {
                    MembershipReminderScheduler.cancel(context)
                } else {
                    MembershipReminderScheduler.schedule(context)
                }
            }
        }
    }

    companion object {
        private const val TAG = "WorkoutViewModel"
    }
}
