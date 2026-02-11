package com.example.gymprogress.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insert(entry: WorkoutEntry)

    @Delete
    suspend fun delete(entry: WorkoutEntry)

    @Query("SELECT * FROM workout_entries ORDER BY date DESC, id DESC")
    fun getAllEntries(): Flow<List<WorkoutEntry>>

    @Query("SELECT DISTINCT exerciseName FROM workout_entries ORDER BY exerciseName ASC")
    fun getAllExerciseNames(): Flow<List<String>>

    @Query("SELECT * FROM workout_entries WHERE exerciseName = :name ORDER BY date ASC")
    fun getEntriesByExercise(name: String): Flow<List<WorkoutEntry>>
}
