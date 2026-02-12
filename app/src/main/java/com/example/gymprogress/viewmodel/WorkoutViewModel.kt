package com.example.gymprogress.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymprogress.data.AppDatabase
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.WorkoutEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val workoutDao = db.workoutDao()
    private val exerciseDao = db.exerciseDao()

    val allEntries: StateFlow<List<WorkoutEntry>> = workoutDao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exerciseNames: StateFlow<List<String>> = workoutDao.getAllExerciseNames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExercises: StateFlow<List<Exercise>> = exerciseDao.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedExercise = MutableStateFlow<String?>(null)
    val selectedExercise: StateFlow<String?> = _selectedExercise.asStateFlow()

    val entriesForSelectedExercise: StateFlow<List<WorkoutEntry>> = _selectedExercise
        .flatMapLatest { name ->
            if (name != null) workoutDao.getEntriesByExercise(name) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEntry(date: String, exerciseName: String, weight: Double, reps: String) {
        viewModelScope.launch {
            workoutDao.insert(
                WorkoutEntry(
                    date = date,
                    exerciseName = exerciseName.trim(),
                    weight = weight,
                    reps = reps.trim()
                )
            )
        }
    }

    fun updateEntry(entry: WorkoutEntry) {
        viewModelScope.launch {
            workoutDao.update(entry)
        }
    }

    fun deleteEntry(entry: WorkoutEntry) {
        viewModelScope.launch {
            workoutDao.delete(entry)
        }
    }

    fun selectExercise(name: String?) {
        _selectedExercise.value = name
    }

    fun addExercise(name: String, muscleGroup: String) {
        viewModelScope.launch {
            exerciseDao.insert(Exercise(name = name.trim(), muscleGroup = muscleGroup))
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            exerciseDao.delete(exercise)
        }
    }
}
