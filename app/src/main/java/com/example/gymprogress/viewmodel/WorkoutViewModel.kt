package com.example.gymprogress.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymprogress.data.AppDatabase
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.ExerciseType
import com.example.gymprogress.data.GeminiService
import com.example.gymprogress.data.SettingsRepository
import com.example.gymprogress.data.TrainerRecommendationEngine
import com.example.gymprogress.data.TrainerSettings
import com.example.gymprogress.data.TrainingGoal
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.data.WorkoutRecommendation
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

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val workoutDao = db.workoutDao()
    private val exerciseDao = db.exerciseDao()
    private val settingsRepository = SettingsRepository(application)
    private val trainerEngine = TrainerRecommendationEngine()
    private val geminiService = GeminiService()

    val isAiAvailable: Boolean get() = geminiService.isAvailable()

    private val _aiAdvice = MutableStateFlow<String?>(null)
    val aiAdvice: StateFlow<String?> = _aiAdvice.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    val allEntries: StateFlow<List<WorkoutEntry>> = workoutDao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exerciseNames: StateFlow<List<String>> = workoutDao.getAllExerciseNames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExercises: StateFlow<List<Exercise>> = exerciseDao.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trainingGoal: StateFlow<TrainingGoal> = settingsRepository.trainingGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrainingGoal.HYPERTROPHY)

    val trainerSettings: StateFlow<TrainerSettings> = settingsRepository.trainerSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrainerSettings())

    val workoutRecommendation: StateFlow<WorkoutRecommendation?> = combine(
        settingsRepository.trainerSettings,
        settingsRepository.trainingGoal,
        exerciseDao.getAllExercises(),
        workoutDao.getAllEntries()
    ) { settings, goal, exercises, history ->
        if (exercises.isEmpty()) null
        else trainerEngine.generateRecommendation(settings, goal, exercises, history)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
                Log.e(TAG, "Failed to add entry", e)
            }
        }
    }

    fun updateEntry(entry: WorkoutEntry) {
        viewModelScope.launch {
            try {
                workoutDao.update(entry)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update entry", e)
            }
        }
    }

    fun deleteEntry(entry: WorkoutEntry) {
        viewModelScope.launch {
            try {
                workoutDao.delete(entry)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete entry", e)
            }
        }
    }

    fun selectExercise(name: String?) {
        _selectedExercise.value = name
    }

    fun addExercise(name: String, muscleGroup: String, exerciseType: String = ExerciseType.COMPOUND.name) {
        viewModelScope.launch {
            try {
                exerciseDao.insert(
                    Exercise(name = name.trim(), muscleGroup = muscleGroup, exerciseType = exerciseType)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add exercise", e)
            }
        }
    }

    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            try {
                exerciseDao.update(exercise)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update exercise", e)
            }
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            try {
                exerciseDao.delete(exercise)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete exercise", e)
            }
        }
    }

    fun setTrainingGoal(goal: TrainingGoal) {
        viewModelScope.launch {
            try {
                settingsRepository.setTrainingGoal(goal)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set training goal", e)
            }
        }
    }

    fun updateTrainerSettings(settings: TrainerSettings) {
        viewModelScope.launch {
            try {
                settingsRepository.updateTrainerSettings(settings)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update trainer settings", e)
            }
        }
    }

    fun getAlternatives(exercise: Exercise): List<Exercise> {
        return trainerEngine.getAlternatives(exercise, allExercises.value)
    }

    fun askAi() {
        val rec = workoutRecommendation.value ?: return
        if (_aiLoading.value) return
        _aiLoading.value = true
        _aiAdvice.value = null
        viewModelScope.launch {
            try {
                val advice = geminiService.getAdvice(
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

    companion object {
        private const val TAG = "WorkoutViewModel"
    }
}
