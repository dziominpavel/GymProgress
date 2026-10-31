package com.example.gymprogress.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "workout_entries",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["exerciseId"]),
        Index(value = ["clientId"], unique = true)
    ]
)
data class WorkoutEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val exerciseName: String,
    val weight: Double,
    val reps: String, // comma-separated reps per set, e.g. "10,8,6"
    val exerciseId: Long? = null,
    val clientId: String = UUID.randomUUID().toString()
)
