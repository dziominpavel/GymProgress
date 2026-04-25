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

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    @Query("SELECT * FROM exercises ORDER BY muscleGroup ASC, name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE muscleGroup = :group ORDER BY name ASC")
    fun getExercisesByGroup(group: String): Flow<List<Exercise>>

    @Query("SELECT DISTINCT muscleGroup FROM exercises ORDER BY muscleGroup ASC")
    fun getUsedMuscleGroups(): Flow<List<String>>

    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    fun getExerciseByName(name: String): Flow<Exercise?>

    @Query("SELECT COUNT(*) FROM exercises WHERE name = :name")
    suspend fun countByName(name: String): Int

    /**
     * Поиск дубликатов с case-insensitive нормализацией пробелов и регистра
     * (соответствует [com.example.gymprogress.data.FormatUtils.normalizeExerciseNameKey]).
     * При обновлении упражнения исключающий [excludeId] позволяет не считать само себя.
     */
    @Query(
        "SELECT COUNT(*) FROM exercises " +
            "WHERE LOWER(TRIM(REPLACE(name, char(160), ' '))) = :normalizedName " +
            "AND id != :excludeId"
    )
    suspend fun countByNormalizedName(normalizedName: String, excludeId: Long = 0): Int
}
