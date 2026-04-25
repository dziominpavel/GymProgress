package com.example.gymprogress.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insert(entry: WorkoutEntry)

    /** Вставка списка в одной транзакции (см. Room: @Insert для коллекции). */
    @Insert
    suspend fun insertEntries(entries: List<WorkoutEntry>)

    @Update
    suspend fun update(entry: WorkoutEntry)

    @Delete
    suspend fun delete(entry: WorkoutEntry)

    /** Порядок: старые записи первыми (как в AGENTS.md: date ASC, id ASC). */
    @Query("SELECT * FROM workout_entries ORDER BY date ASC, id ASC")
    fun getAllEntries(): Flow<List<WorkoutEntry>>

    @Query("SELECT * FROM workout_entries WHERE exerciseName = :name ORDER BY date ASC, id ASC")
    fun getEntriesByExercise(name: String): Flow<List<WorkoutEntry>>

    @Query("UPDATE workout_entries SET exerciseName = :newName WHERE exerciseName = :oldName")
    suspend fun renameExercise(oldName: String, newName: String)
}
