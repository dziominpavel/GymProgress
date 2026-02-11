package com.example.gymprogress.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_entries")
data class WorkoutEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val exerciseName: String,
    val weight: Double,
    val reps: String // comma-separated reps per set, e.g. "10,8,6"
)
