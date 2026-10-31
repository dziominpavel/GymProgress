package com.example.gymprogress.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Insert
    suspend fun insert(exercise: Exercise)

    @Insert
    suspend fun insertAll(exercises: List<Exercise>): List<Long>

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    @Query("DELETE FROM exercises")
    suspend fun deleteAll()

    @Query("SELECT * FROM exercises ORDER BY muscleGroup ASC, name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises ORDER BY muscleGroup ASC, name ASC")
    suspend fun getAllOnce(): List<Exercise>

    @Query("SELECT * FROM exercises WHERE isArchived = 0 ORDER BY muscleGroup ASC, name ASC")
    fun getActiveExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE muscleGroup = :group ORDER BY name ASC")
    fun getExercisesByGroup(group: String): Flow<List<Exercise>>

    @Query("SELECT DISTINCT muscleGroup FROM exercises ORDER BY muscleGroup ASC")
    fun getUsedMuscleGroups(): Flow<List<String>>

    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    fun getExerciseByName(name: String): Flow<Exercise?>

    @Query("SELECT COUNT(*) FROM exercises WHERE name = :name")
    suspend fun countByName(name: String): Int

    @Query("SELECT * FROM exercises WHERE nameKey = :nameKey LIMIT 1")
    suspend fun getByNameKey(nameKey: String): Exercise?

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Exercise?

    /**
     * Поиск дубликатов по нормализованному имени, которое вычисляется в Kotlin
     * ([com.example.gymprogress.data.FormatUtils.normalizeExerciseNameKey]).
     * При обновлении упражнения исключающий [excludeId] позволяет не считать само себя.
     */
    @Query("SELECT COUNT(*) FROM exercises WHERE nameKey = :nameKey AND id != :excludeId")
    suspend fun countByNameKey(nameKey: String, excludeId: Long = 0): Int
}
