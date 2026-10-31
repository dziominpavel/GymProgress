package com.example.gymprogress.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

/**
 * Миграция 6 → 7.
 *
 * Нормализация имён выполняется в Kotlin: SQLite `LOWER` не умеет Unicode
 * case folding для кириллицы. После заполнения колонок таблицы пересобираются,
 * чтобы получить NOT NULL, уникальные индексы и внешний ключ.
 */
internal val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN clientId TEXT")
        db.execSQL("ALTER TABLE exercises ADD COLUMN nameKey TEXT")
        db.execSQL("ALTER TABLE exercises ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE workout_entries ADD COLUMN clientId TEXT")
        db.execSQL("ALTER TABLE workout_entries ADD COLUMN exerciseId INTEGER")

        val legacyExercises = readLegacyExercises(db)
        val legacyEntries = readLegacyEntries(db)

        val preparedExercises = legacyExercises.map { exercise ->
            Exercise(
                id = exercise.id,
                name = exercise.name,
                muscleGroup = exercise.muscleGroup,
                exerciseType = exercise.exerciseType,
                isBodyweight = exercise.isBodyweight,
                clientId = UUID.randomUUID().toString(),
                nameKey = FormatUtils.normalizeExerciseNameKey(exercise.name)
            )
        }
        val entryCounts = preparedExercises.associate { exercise ->
            exercise.id to legacyEntries.count { it.exerciseName == exercise.name }
        }
        val canonicalByKey = canonicalExercisesByKey(preparedExercises, entryCounts)

        val preparedEntries = legacyEntries.map { entry ->
            val canonical = canonicalByKey[FormatUtils.normalizeExerciseNameKey(entry.exerciseName)]
            entry.copy(
                exerciseName = canonical?.name ?: entry.exerciseName,
                exerciseId = canonical?.id,
                clientId = UUID.randomUUID().toString()
            )
        }

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exercises_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `muscleGroup` TEXT NOT NULL,
                `exerciseType` TEXT NOT NULL,
                `isBodyweight` INTEGER NOT NULL,
                `isArchived` INTEGER NOT NULL,
                `clientId` TEXT NOT NULL,
                `nameKey` TEXT NOT NULL
            )
            """.trimIndent()
        )
        canonicalByKey.values.forEach { exercise ->
            db.execSQL(
                "INSERT INTO exercises_new " +
                    "(id, name, muscleGroup, exerciseType, isBodyweight, isArchived, clientId, nameKey) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(
                    exercise.id,
                    exercise.name,
                    exercise.muscleGroup,
                    exercise.exerciseType,
                    if (exercise.isBodyweight) 1 else 0,
                    0,
                    exercise.clientId,
                    exercise.nameKey
                )
            )
        }

        db.execSQL("DROP TABLE workout_entries")
        db.execSQL("DROP TABLE exercises")
        db.execSQL("ALTER TABLE exercises_new RENAME TO exercises")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `workout_entries_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `date` TEXT NOT NULL,
                `exerciseName` TEXT NOT NULL,
                `weight` REAL NOT NULL,
                `reps` TEXT NOT NULL,
                `exerciseId` INTEGER,
                `clientId` TEXT NOT NULL,
                FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        preparedEntries.forEach { entry ->
            db.execSQL(
                "INSERT INTO workout_entries_new " +
                    "(id, date, exerciseName, weight, reps, exerciseId, clientId) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(
                    entry.id,
                    entry.date,
                    entry.exerciseName,
                    entry.weight,
                    entry.reps,
                    entry.exerciseId,
                    entry.clientId
                )
            )
        }

        db.execSQL("ALTER TABLE workout_entries_new RENAME TO workout_entries")

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_exercises_clientId` ON `exercises` (`clientId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_exercises_nameKey` ON `exercises` (`nameKey`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_workout_entries_clientId` ON `workout_entries` (`clientId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_entries_exerciseId` ON `workout_entries` (`exerciseId`)")
    }
}

private data class LegacyExercise(
    val id: Long,
    val name: String,
    val muscleGroup: String,
    val exerciseType: String,
    val isBodyweight: Boolean
)

private data class LegacyEntry(
    val id: Long,
    val date: String,
    val exerciseName: String,
    val weight: Double,
    val reps: String,
    val exerciseId: Long?,
    val clientId: String
)

private fun readLegacyExercises(db: SupportSQLiteDatabase): List<LegacyExercise> {
    val result = mutableListOf<LegacyExercise>()
    db.query("SELECT id, name, muscleGroup, exerciseType, isBodyweight FROM exercises").use { cursor ->
        while (cursor.moveToNext()) {
            result += LegacyExercise(
                id = cursor.getLong(0),
                name = cursor.getString(1),
                muscleGroup = cursor.getString(2),
                exerciseType = cursor.getString(3),
                isBodyweight = cursor.getInt(4) != 0
            )
        }
    }
    return result
}

private fun readLegacyEntries(db: SupportSQLiteDatabase): List<LegacyEntry> {
    val result = mutableListOf<LegacyEntry>()
    db.query("SELECT id, date, exerciseName, weight, reps FROM workout_entries").use { cursor ->
        while (cursor.moveToNext()) {
            result += LegacyEntry(
                id = cursor.getLong(0),
                date = cursor.getString(1),
                exerciseName = cursor.getString(2),
                weight = cursor.getDouble(3),
                reps = cursor.getString(4),
                exerciseId = null,
                clientId = ""
            )
        }
    }
    return result
}
