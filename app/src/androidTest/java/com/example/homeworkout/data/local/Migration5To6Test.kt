package com.example.homeworkout.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Migration5To6Test {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "migration-5-6-test.db"
    private var openHelper: SupportSQLiteOpenHelper? = null

    private fun createDatabase(version: Int = 5): SupportSQLiteDatabase {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        openHelper = helper
        return helper.writableDatabase
    }

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        openHelper?.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationPreservesSessionAndBackfillsPlanMetadata() {
        val database = createDatabase(5)
        database.execSQL(
            "CREATE TABLE workout_plans (planId INTEGER PRIMARY KEY, title TEXT NOT NULL, coverImageUrl TEXT)"
        )
        database.execSQL(
            "CREATE TABLE workout_plan_days (planDayId INTEGER PRIMARY KEY, dayNumber INTEGER NOT NULL, title TEXT)"
        )
        database.execSQL(
            "CREATE TABLE workout_sessions (sessionId INTEGER PRIMARY KEY, planId INTEGER NOT NULL, planDayId INTEGER NOT NULL)"
        )
        database.execSQL("INSERT INTO workout_plans VALUES (10, 'Core Plan', 'cover.jpg')")
        database.execSQL("INSERT INTO workout_plan_days VALUES (20, 2, 'Lower Body')")
        database.execSQL("INSERT INTO workout_sessions VALUES (30, 10, 20)")

        AppDatabase.MIGRATION_5_6.migrate(database)

        database.query(
            "SELECT planTitleSnapshot, planCoverImageSnapshot, planDayNumberSnapshot, planDayTitleSnapshot " +
                "FROM workout_sessions WHERE sessionId = 30"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Core Plan", cursor.getString(0))
            assertEquals("cover.jpg", cursor.getString(1))
            assertEquals(2, cursor.getInt(2))
            assertEquals("Lower Body", cursor.getString(3))
        }
    }

    @Test
    fun migrationHandlesNullSourceFields() {
        val database = createDatabase(5)
        database.execSQL(
            "CREATE TABLE workout_plans (planId INTEGER PRIMARY KEY, title TEXT NOT NULL, coverImageUrl TEXT)"
        )
        database.execSQL(
            "CREATE TABLE workout_plan_days (planDayId INTEGER PRIMARY KEY, dayNumber INTEGER NOT NULL, title TEXT)"
        )
        database.execSQL(
            "CREATE TABLE workout_sessions (sessionId INTEGER PRIMARY KEY, planId INTEGER NOT NULL, planDayId INTEGER NOT NULL)"
        )
        database.execSQL("INSERT INTO workout_plans VALUES (100, 'Full Body Plan', NULL)")
        database.execSQL("INSERT INTO workout_plan_days VALUES (200, 1, NULL)")
        database.execSQL("INSERT INTO workout_sessions VALUES (300, 100, 200)")

        AppDatabase.MIGRATION_5_6.migrate(database)

        database.query(
            "SELECT planTitleSnapshot, planCoverImageSnapshot, planDayNumberSnapshot, planDayTitleSnapshot " +
                "FROM workout_sessions WHERE sessionId = 300"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Full Body Plan", cursor.getString(0))
            assertNull(cursor.getString(1))
            assertEquals(1, cursor.getInt(2))
            assertNull(cursor.getString(3))
        }
    }

    @Test
    fun migrationPreservesMultipleSessionsWithoutDataLoss() {
        val database = createDatabase(5)
        database.execSQL(
            "CREATE TABLE workout_plans (planId INTEGER PRIMARY KEY, title TEXT NOT NULL, coverImageUrl TEXT)"
        )
        database.execSQL(
            "CREATE TABLE workout_plan_days (planDayId INTEGER PRIMARY KEY, dayNumber INTEGER NOT NULL, title TEXT)"
        )
        database.execSQL(
            "CREATE TABLE workout_sessions (sessionId INTEGER PRIMARY KEY, planId INTEGER NOT NULL, planDayId INTEGER NOT NULL, status TEXT NOT NULL)"
        )
        database.execSQL("INSERT INTO workout_plans VALUES (1, 'Plan A', 'coverA.png')")
        database.execSQL("INSERT INTO workout_plans VALUES (2, 'Plan B', 'coverB.png')")
        database.execSQL("INSERT INTO workout_plan_days VALUES (10, 1, 'Day 1')")
        database.execSQL("INSERT INTO workout_plan_days VALUES (20, 2, 'Day 2')")
        database.execSQL("INSERT INTO workout_sessions VALUES (101, 1, 10, 'COMPLETED')")
        database.execSQL("INSERT INTO workout_sessions VALUES (102, 1, 10, 'IN_PROGRESS')")
        database.execSQL("INSERT INTO workout_sessions VALUES (103, 2, 20, 'COMPLETED')")

        AppDatabase.MIGRATION_5_6.migrate(database)

        database.query(
            "SELECT sessionId, planTitleSnapshot, planCoverImageSnapshot, planDayNumberSnapshot, planDayTitleSnapshot, status " +
                "FROM workout_sessions ORDER BY sessionId ASC"
        ).use { cursor ->
            assertEquals(3, cursor.count)

            assertTrue(cursor.moveToNext())
            assertEquals(101L, cursor.getLong(0))
            assertEquals("Plan A", cursor.getString(1))
            assertEquals("coverA.png", cursor.getString(2))
            assertEquals(1, cursor.getInt(3))
            assertEquals("Day 1", cursor.getString(4))
            assertEquals("COMPLETED", cursor.getString(5))

            assertTrue(cursor.moveToNext())
            assertEquals(102L, cursor.getLong(0))
            assertEquals("Plan A", cursor.getString(1))
            assertEquals("coverA.png", cursor.getString(2))
            assertEquals(1, cursor.getInt(3))
            assertEquals("Day 1", cursor.getString(4))
            assertEquals("IN_PROGRESS", cursor.getString(5))

            assertTrue(cursor.moveToNext())
            assertEquals(103L, cursor.getLong(0))
            assertEquals("Plan B", cursor.getString(1))
            assertEquals("coverB.png", cursor.getString(2))
            assertEquals(2, cursor.getInt(3))
            assertEquals("Day 2", cursor.getString(4))
            assertEquals("COMPLETED", cursor.getString(5))
        }
    }

    @Test
    fun migrationChain4To5To6Succeeds() {
        val database = createDatabase(4)
        database.execSQL("CREATE TABLE users (userId INTEGER PRIMARY KEY, email TEXT NOT NULL, password_hash TEXT NOT NULL)")
        database.execSQL("INSERT INTO users VALUES (1, 'user@test.com', 'hash')")
        database.execSQL(
            "CREATE TABLE workout_plans (planId INTEGER PRIMARY KEY, title TEXT NOT NULL, coverImageUrl TEXT)"
        )
        database.execSQL(
            "CREATE TABLE workout_plan_days (planDayId INTEGER PRIMARY KEY, dayNumber INTEGER NOT NULL, title TEXT)"
        )
        database.execSQL(
            "CREATE TABLE workout_sessions (sessionId INTEGER PRIMARY KEY, planId INTEGER NOT NULL, planDayId INTEGER NOT NULL)"
        )
        database.execSQL("INSERT INTO workout_plans VALUES (50, 'Chain Plan', 'chain_cover.jpg')")
        database.execSQL("INSERT INTO workout_plan_days VALUES (60, 3, 'Day Three')")
        database.execSQL("INSERT INTO workout_sessions VALUES (70, 50, 60)")

        // Apply 4 -> 5
        AppDatabase.MIGRATION_4_5.migrate(database)

        // Verify user_badges table was created in 4 -> 5
        database.query("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='user_badges'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }

        // Apply 5 -> 6
        AppDatabase.MIGRATION_5_6.migrate(database)

        database.query(
            "SELECT planTitleSnapshot, planCoverImageSnapshot, planDayNumberSnapshot, planDayTitleSnapshot " +
                "FROM workout_sessions WHERE sessionId = 70"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Chain Plan", cursor.getString(0))
            assertEquals("chain_cover.jpg", cursor.getString(1))
            assertEquals(3, cursor.getInt(2))
            assertEquals("Day Three", cursor.getString(3))
        }
    }
}
