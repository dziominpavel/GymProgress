package com.example.gymprogress.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymprogress.data.AppDatabase
import com.example.gymprogress.data.CompletedSet
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.ExerciseType
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.AiService
import com.example.gymprogress.data.SetType
import com.example.gymprogress.data.SettingsRepository
import com.example.gymprogress.data.TrainerRecommendationEngine
import com.example.gymprogress.data.TrainerSettings
import com.example.gymprogress.data.TrainingGoal
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.data.WorkoutRecommendation
import com.example.gymprogress.data.ExerciseRecommendation
import com.example.gymprogress.data.Gender
import com.example.gymprogress.data.PreviousSessionInSplit
import com.example.gymprogress.data.ScoringEngine
import com.example.gymprogress.data.ScoringSystem
import com.example.gymprogress.data.SimplifiedScoreCalculator
import com.example.gymprogress.data.WorkoutScoreCalculator
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

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val workoutDao = db.workoutDao()
    private val exerciseDao = db.exerciseDao()
    private val settingsRepository = SettingsRepository(application)
    private val trainerEngine = TrainerRecommendationEngine()
    private val aiService = AiService()

    val isAiAvailable: Boolean get() = aiService.isAvailable()

    private val _aiAdvice = MutableStateFlow<String?>(null)
    val aiAdvice: StateFlow<String?> = _aiAdvice.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    val allEntries: StateFlow<List<WorkoutEntry>> = workoutDao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exerciseNames: StateFlow<List<String>> = workoutDao.getAllExerciseNames()
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

    private val journalSessionState: StateFlow<Triple<List<WorkoutEntry>, String?, String?>> = combine(
        allEntries,
        trainerSettings,
        allExercises,
        _journalSelectedDayIndex
    ) { entries, settings, exercises, selectedDay ->
        val result = when {
            selectedDay != null -> trainerEngine.findSessionForDayIndex(settings, entries, exercises, selectedDay)
            else -> trainerEngine.findNextDaySessionInSplit(settings, entries, exercises)
        }
        when (val session = result) {
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(emptyList(), null, null))

    val previousSessionForJournal: StateFlow<List<WorkoutEntry>> = journalSessionState
        .map { it.first }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val previousSessionDateForJournal: StateFlow<String?> = journalSessionState
        .map { it.second }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val previousSessionTitleOverride: StateFlow<String?> = journalSessionState
        .map { it.third }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setJournalPreviousDay(dayIndex: Int?) {
        _journalSelectedDayIndex.value = dayIndex
    }

    val previousSessionDayMuscleGroups: StateFlow<List<String>> = combine(
        trainerSettings,
        allEntries,
        allExercises,
        _journalSelectedDayIndex
    ) { settings, entries, exercises, selectedDay ->
        val displayedDayIndex = when {
            selectedDay != null -> selectedDay
            else -> if (trainerEngine.findNextDaySessionInSplit(settings, entries, exercises) != null)
                trainerEngine.getNextDayIndex(settings, entries, exercises)
            else null
        }
        displayedDayIndex?.let { dayIndex ->
            trainerEngine.getMuscleGroupsForDayPublic(settings, dayIndex).map { it.displayName }
        } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val previousSameDaySession: StateFlow<List<WorkoutEntry>> = previousSessionForJournal
    val previousSameDaySessionDate: StateFlow<String?> = previousSessionDateForJournal

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

    val entriesForSelectedExercise: StateFlow<List<WorkoutEntry>> = _selectedExercise
        .flatMapLatest { name ->
            if (name != null) workoutDao.getEntriesByExercise(name) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun addEntry(date: String, exerciseName: String, weight: Double, reps: String) {
        viewModelScope.launch {
            try {
                workoutDao.insert(
                    WorkoutEntry(
                        date = date,
                        exerciseName = exerciseName.trim(),
                        weight = weight,
                        reps = reps.trim()
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось добавить запись"
                Log.e(TAG, "Failed to add entry", e)
            }
        }
    }

    fun updateEntry(entry: WorkoutEntry) {
        viewModelScope.launch {
            try {
                workoutDao.update(entry)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось обновить запись"
                Log.e(TAG, "Failed to update entry", e)
            }
        }
    }

    fun deleteEntry(entry: WorkoutEntry) {
        viewModelScope.launch {
            try {
                workoutDao.delete(entry)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось удалить запись"
                Log.e(TAG, "Failed to delete entry", e)
            }
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
        viewModelScope.launch {
            try {
                exerciseDao.insert(
                    Exercise(
                        name = name.trim(),
                        muscleGroup = muscleGroup,
                        exerciseType = exerciseType,
                        isBodyweight = isBodyweight
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось добавить упражнение"
                Log.e(TAG, "Failed to add exercise", e)
            }
        }
    }

    fun updateExercise(exercise: Exercise, oldName: String? = null) {
        viewModelScope.launch {
            try {
                exerciseDao.update(exercise)
                if (oldName != null && oldName != exercise.name) {
                    workoutDao.renameExercise(oldName, exercise.name)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось обновить упражнение"
                Log.e(TAG, "Failed to update exercise", e)
            }
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            try {
                exerciseDao.delete(exercise)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось удалить упражнение"
                Log.e(TAG, "Failed to delete exercise", e)
            }
        }
    }

    fun setTrainingGoal(goal: TrainingGoal) {
        viewModelScope.launch {
            try {
                settingsRepository.setTrainingGoal(goal)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось сохранить цель"
                Log.e(TAG, "Failed to set training goal", e)
            }
        }
    }

    fun setBodyWeightKg(value: Double?) {
        viewModelScope.launch {
            try {
                settingsRepository.setBodyWeightKg(value)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось сохранить вес"
                Log.e(TAG, "Failed to set body weight", e)
            }
        }
    }

    fun setScoringSystem(system: ScoringSystem) {
        viewModelScope.launch {
            try {
                settingsRepository.setScoringSystem(system)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось сохранить систему оценки"
                Log.e(TAG, "Failed to set scoring system", e)
            }
        }
    }

    fun setHeightCm(value: Int?) {
        viewModelScope.launch {
            try {
                settingsRepository.setHeightCm(value)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось сохранить рост"
                Log.e(TAG, "Failed to set height", e)
            }
        }
    }

    fun setGender(value: Gender?) {
        viewModelScope.launch {
            try {
                settingsRepository.setGender(value)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось сохранить пол"
                Log.e(TAG, "Failed to set gender", e)
            }
        }
    }

    fun updateTrainerSettings(settings: TrainerSettings) {
        viewModelScope.launch {
            try {
                settingsRepository.updateTrainerSettings(settings)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось сохранить настройки тренера"
                Log.e(TAG, "Failed to update trainer settings", e)
            }
        }
    }

    fun getAlternatives(exercise: Exercise): List<Exercise> {
        return trainerEngine.getAlternatives(exercise, allExercises.value)
    }

    /** Загружает рекомендацию по упражнению для отображения в диалоге добавления записи (совет тренера). */
    fun loadExerciseRecommendationForJournal(exerciseName: String?) {
        viewModelScope.launch {
            if (exerciseName == null) {
                _exerciseRecommendationForJournal.value = null
                return@launch
            }
            val exercise = allExercises.value.find { it.name == exerciseName } ?: run {
                _exerciseRecommendationForJournal.value = null
                return@launch
            }
            _exerciseRecommendationForJournal.value = trainerEngine.getRecommendationForExercise(
                exercise = exercise,
                history = allEntries.value,
                trainingGoal = trainingGoal.value,
                settings = trainerSettings.value,
                bodyWeightKg = bodyWeightKg.value,
                scoringEngine = scoringEngine.value,
                scoringSystem = scoringSystem.value
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

        grouped.forEach { (name, sets) ->
            val distinctWeights = sets.map { it.weight }.distinct()
            if (distinctWeights.size == 1) {
                val reps = sets.joinToString(",") { it.reps.toString() }
                addEntry(today, name, distinctWeights.first(), reps)
            } else {
                val mainWeight = sets.groupBy { it.weight }
                    .maxByOrNull { it.value.size }?.key ?: sets.first().weight
                val mainSets = sets.filter { it.weight == mainWeight }
                val reps = mainSets.joinToString(",") { it.reps.toString() }
                addEntry(today, name, mainWeight, reps)

                val otherGroups = sets.filter { it.weight != mainWeight }.groupBy { it.weight }
                otherGroups.forEach { (w, wSets) ->
                    val otherReps = wSets.joinToString(",") { it.reps.toString() }
                    addEntry(today, name, w, otherReps)
                }
            }
        }
    }

    companion object {
        private const val TAG = "WorkoutViewModel"
    }
}
