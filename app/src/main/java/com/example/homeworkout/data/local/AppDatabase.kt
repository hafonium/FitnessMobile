package com.example.homeworkout.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.homeworkout.data.local.dao.ExerciseDao
import com.example.homeworkout.data.local.dao.BadgeDao
import com.example.homeworkout.data.local.dao.UserDao
import com.example.homeworkout.data.local.dao.WeightLogDao
import com.example.homeworkout.data.local.dao.WorkoutPlanDao
import com.example.homeworkout.data.local.dao.WorkoutSessionDao
import com.example.homeworkout.data.local.entities.EquipmentTypeEntity
import com.example.homeworkout.data.local.entities.ExerciseEntity
import com.example.homeworkout.data.local.entities.ExerciseImageEntity
import com.example.homeworkout.data.local.entities.ExerciseInstructionStepEntity
import com.example.homeworkout.data.local.entities.ExerciseMuscleEntity
import com.example.homeworkout.data.local.entities.MuscleEntity
import com.example.homeworkout.data.local.entities.UserEntity
import com.example.homeworkout.data.local.entities.UserBadgeEntity
import com.example.homeworkout.data.local.entities.UserFitnessProfileEntity
import com.example.homeworkout.data.local.entities.UserSettingsEntity
import com.example.homeworkout.data.local.entities.UserWeightLogEntity
import com.example.homeworkout.data.local.entities.WorkoutPlanDayEntity
import com.example.homeworkout.data.local.entities.WorkoutPlanEntity
import com.example.homeworkout.data.local.entities.WorkoutPlanExerciseEntity
import com.example.homeworkout.data.local.entities.WorkoutSessionEntity
import com.example.homeworkout.data.local.entities.WorkoutSessionExerciseEntity
import android.util.Log
import com.example.homeworkout.data.local.seed.AppDatabaseSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The single Room database for the app, covering every table in docs/db_diagram.dbml.
 * Registering a new entity/DAO always happens here — see docs/architecture.md.
 */
@Database(
    entities = [
        UserEntity::class,
        UserSettingsEntity::class,
        EquipmentTypeEntity::class,
        MuscleEntity::class,
        ExerciseEntity::class,
        ExerciseMuscleEntity::class,
        ExerciseInstructionStepEntity::class,
        ExerciseImageEntity::class,
        WorkoutPlanEntity::class,
        WorkoutPlanDayEntity::class,
        WorkoutPlanExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSessionExerciseEntity::class,
        UserWeightLogEntity::class,
        UserFitnessProfileEntity::class,
        UserBadgeEntity::class
    ],
    // Version 6 snapshots plan/day metadata so completed history survives later plan edits.
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun badgeDao(): BadgeDao

    companion object {
        private const val DATABASE_NAME = "home_workout.db"

        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_badges` (
                        `userId` INTEGER NOT NULL,
                        `badgeId` TEXT NOT NULL,
                        `unlockedAt` INTEGER NOT NULL,
                        `triggerSessionId` INTEGER,
                        `isSeen` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`, `badgeId`),
                        FOREIGN KEY(`userId`) REFERENCES `users`(`userId`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `planTitleSnapshot` TEXT")
                db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `planCoverImageSnapshot` TEXT")
                db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `planDayNumberSnapshot` INTEGER")
                db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `planDayTitleSnapshot` TEXT")
                db.execSQL(
                    """
                    UPDATE `workout_sessions`
                    SET
                        `planTitleSnapshot` = (
                            SELECT p.`title`
                            FROM `workout_plans` p
                            WHERE p.`planId` = `workout_sessions`.`planId`
                        ),
                        `planCoverImageSnapshot` = (
                            SELECT p.`coverImageUrl`
                            FROM `workout_plans` p
                            WHERE p.`planId` = `workout_sessions`.`planId`
                        ),
                        `planDayNumberSnapshot` = (
                            SELECT d.`dayNumber`
                            FROM `workout_plan_days` d
                            WHERE d.`planDayId` = `workout_sessions`.`planDayId`
                        ),
                        `planDayTitleSnapshot` = (
                            SELECT d.`title`
                            FROM `workout_plan_days` d
                            WHERE d.`planDayId` = `workout_sessions`.`planDayId`
                        )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * [applicationScope] is used once, from the [RoomDatabase.Callback.onCreate] seed
         * hook below, to populate the exercise library and starter plans on first launch.
         */
        fun getInstance(context: Context, applicationScope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context.applicationContext, applicationScope).also { INSTANCE = it }
            }
        }

        private fun build(appContext: Context, applicationScope: CoroutineScope): AppDatabase {
            return Room.databaseBuilder(appContext, AppDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                // Pre-release app: on any schema change, wipe and re-seed rather than ship migrations.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seed(appContext, applicationScope)
                    }

                    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                        super.onDestructiveMigration(db)
                        // A destructive upgrade recreates the tables but does not fire onCreate.
                        seed(appContext, applicationScope)
                    }
                })
                .build()
        }

        private fun seed(appContext: Context, applicationScope: CoroutineScope) {
            applicationScope.launch {
                try {
                    val database = getInstance(appContext, applicationScope)
                    AppDatabaseSeeder.seedIfNeeded(
                        context = appContext,
                        userDao = database.userDao(),
                        exerciseDao = database.exerciseDao(),
                        workoutPlanDao = database.workoutPlanDao()
                    )
                } catch (t: Throwable) {
                    // Never crash the app over a seed failure — the app still runs empty.
                    Log.e("AppDatabase", "Database seeding failed", t)
                }
            }
        }
    }
}
