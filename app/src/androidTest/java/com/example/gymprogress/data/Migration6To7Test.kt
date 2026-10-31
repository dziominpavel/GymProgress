package com.example.gymprogress.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration6To7Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate6To7_mergesDuplicatesAndPreservesHistory() {
        helper.createDatabase(TEST_DB, 6).apply {
            insertLegacyFixture()
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)
        try {
            assertEquals(3, db.count("SELECT COUNT(*) FROM exercises"))
            assertEquals(3, db.count("SELECT COUNT(DISTINCT nameKey) FROM exercises"))
            assertEquals(0, db.count("SELECT COUNT(*) FROM exercises WHERE clientId IS NULL OR clientId = ''"))
            assertEquals(6, db.count("SELECT COUNT(*) FROM workout_entries"))
            assertEquals(6, db.count("SELECT COUNT(DISTINCT clientId) FROM workout_entries"))
            assertEquals(0, db.count("SELECT COUNT(*) FROM workout_entries WHERE clientId IS NULL OR clientId = ''"))

            assertEquals(1L, db.longValue("SELECT exerciseId FROM workout_entries WHERE id = 1"))
            assertEquals(1L, db.longValue("SELECT exerciseId FROM workout_entries WHERE id = 2"))
            assertEquals(1L, db.longValue("SELECT exerciseId FROM workout_entries WHERE id = 3"))
            assertNull(db.longValueOrNull("SELECT exerciseId FROM workout_entries WHERE id = 4"))
            assertEquals(3L, db.longValue("SELECT exerciseId FROM workout_entries WHERE id = 5"))
            assertEquals(4L, db.longValue("SELECT exerciseId FROM workout_entries WHERE id = 6"))

            assertEquals(
                "Жим лёжа",
                db.stringValue("SELECT exerciseName FROM workout_entries WHERE id = 3")
            )
            assertEquals(
                FormatUtils.normalizeExerciseNameKey("Присед\u00A0со штангой"),
                db.stringValue("SELECT nameKey FROM exercises WHERE id = 4")
            )

            db.execSQL("PRAGMA foreign_keys = ON")
            db.execSQL("DELETE FROM exercises WHERE id = 3")
            assertNull(db.longValueOrNull("SELECT exerciseId FROM workout_entries WHERE id = 5"))
            assertEquals(
                "Подтягивания",
                db.stringValue("SELECT exerciseName FROM workout_entries WHERE id = 5")
            )
        } finally {
            db.close()
        }
    }

    private fun SupportSQLiteDatabase.insertLegacyFixture() {
        execSQL(
            "INSERT INTO exercises (id, name, muscleGroup, exerciseType, isBodyweight) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(1L, "Жим лёжа", "CHEST", "COMPOUND", 0)
        )
        execSQL(
            "INSERT INTO exercises (id, name, muscleGroup, exerciseType, isBodyweight) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(2L, "жим лёжа", "CHEST", "COMPOUND", 0)
        )
        execSQL(
            "INSERT INTO exercises (id, name, muscleGroup, exerciseType, isBodyweight) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(3L, "Подтягивания", "BACK", "COMPOUND", 1)
        )
        execSQL(
            "INSERT INTO exercises (id, name, muscleGroup, exerciseType, isBodyweight) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(4L, "Присед\u00A0со штангой", "LEGS", "COMPOUND", 0)
        )

        execSQL(
            "INSERT INTO workout_entries (id, date, exerciseName, weight, reps) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(1L, "2026-01-01", "Жим лёжа", 80.0, "8,8")
        )
        execSQL(
            "INSERT INTO workout_entries (id, date, exerciseName, weight, reps) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(2L, "2026-01-02", "Жим лёжа", 82.5, "6")
        )
        execSQL(
            "INSERT INTO workout_entries (id, date, exerciseName, weight, reps) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(3L, "2026-01-03", "жим лёжа", 85.0, "5")
        )
        execSQL(
            "INSERT INTO workout_entries (id, date, exerciseName, weight, reps) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(4L, "2026-01-04", "Тяга", 100.0, "5")
        )
        execSQL(
            "INSERT INTO workout_entries (id, date, exerciseName, weight, reps) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(5L, "2026-01-05", "Подтягивания", 0.0, "10")
        )
        execSQL(
            "INSERT INTO workout_entries (id, date, exerciseName, weight, reps) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(6L, "2026-01-06", "Присед со штангой", 100.0, "5")
        )
    }

    private fun SupportSQLiteDatabase.count(sql: String): Int =
        query(sql).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private fun SupportSQLiteDatabase.longValue(sql: String): Long =
        query(sql).use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }

    private fun SupportSQLiteDatabase.longValueOrNull(sql: String): Long? =
        query(sql).use { cursor ->
            if (!cursor.moveToFirst() || cursor.isNull(0)) null else cursor.getLong(0)
        }

    private fun SupportSQLiteDatabase.stringValue(sql: String): String =
        query(sql).use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }

    private companion object {
        const val TEST_DB = "migration-6-7-test.db"
    }
}
