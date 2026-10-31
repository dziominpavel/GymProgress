package com.example.gymprogress.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "exercises",
    indices = [
        Index(value = ["clientId"], unique = true),
        Index(value = ["nameKey"], unique = true)
    ]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    val exerciseType: String = ExerciseType.COMPOUND.name,
    val isBodyweight: Boolean = false,
    val isArchived: Boolean = false,
    val clientId: String = UUID.randomUUID().toString(),
    val nameKey: String = FormatUtils.normalizeExerciseNameKey(name)
)
