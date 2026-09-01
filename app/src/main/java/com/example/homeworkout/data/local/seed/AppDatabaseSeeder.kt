package com.example.homeworkout.data.local.seed

import android.content.Context
import com.example.homeworkout.data.local.dao.ExerciseDao
import com.example.homeworkout.data.local.dao.UserDao
import com.example.homeworkout.data.local.dao.WorkoutPlanDao
import com.example.homeworkout.data.local.entities.EquipmentTypeEntity
import com.example.homeworkout.data.local.entities.ExerciseEntity
import com.example.homeworkout.data.local.entities.ExerciseImageEntity
import com.example.homeworkout.data.local.entities.ExerciseInstructionStepEntity
import com.example.homeworkout.data.local.entities.ExerciseMuscleEntity
import com.example.homeworkout.data.local.entities.MuscleEntity
import com.example.homeworkout.data.local.entities.UserEntity
import com.example.homeworkout.data.local.entities.UserSettingsEntity
import com.example.homeworkout.data.local.entities.WorkoutPlanDayEntity
import com.example.homeworkout.data.local.entities.WorkoutPlanEntity
import com.example.homeworkout.data.local.entities.WorkoutPlanExerciseEntity
import com.example.homeworkout.domain.models.enums.ExerciseCategory
import com.example.homeworkout.domain.models.enums.ExerciseForce
import com.example.homeworkout.domain.models.enums.ExerciseLevel
import com.example.homeworkout.domain.models.enums.MuscleRole
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.models.enums.WorkoutLevel
import com.example.homeworkout.domain.models.enums.WorkoutPlanSource
import org.json.JSONArray

/**
 * Populates a freshly-created database from the bundled `assets/exercises.json` (a copy of
 * data/data.json) plus a handful of hand-picked system [WorkoutPlanEntity] rows so the Training
 * screen has real content on first launch. Runs once, from [com.example.homeworkout.data.local.AppDatabase]'s
 * `onCreate` callback — see [countExercises] guard in [seedIfNeeded].
 */
object AppDatabaseSeeder {

    const val DEFAULT_USER_EMAIL = "me@homeworkout.local"

    /** Seeds the database if empty and returns the id of the single local user this app runs as. */
    suspend fun seedIfNeeded(
        context: Context,
        userDao: UserDao,
        exerciseDao: ExerciseDao,
        workoutPlanDao: WorkoutPlanDao
    ): Long {
        val userId = seedDefaultUser(userDao)
        if (exerciseDao.countExercises() == 0) {
            seedExercisesFromAssets(context, exerciseDao)
        }
        if (workoutPlanDao.countPlans() == 0) {
            // Every seeded plan below is a system plan (ownerUserId = null); a real custom plan
            // would be created for `userId` from the "Create your own" flow instead.
            seedSystemPlans(workoutPlanDao, exerciseDao)
        }
        return userId
    }

    private suspend fun seedDefaultUser(userDao: UserDao): Long {
        userDao.getUserByEmail(DEFAULT_USER_EMAIL)?.let { return it.userId }
        val userId = userDao.insertUser(UserEntity(email = DEFAULT_USER_EMAIL, passwordHash = "local-only"))
        userDao.upsertUserSettings(UserSettingsEntity(userId = userId))
        return userId
    }

    private suspend fun seedExercisesFromAssets(context: Context, dao: ExerciseDao) {
        val json = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)

        val equipmentNames = sortedSetOf<String>()
        val muscleNames = sortedSetOf<String>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            equipmentNames += obj.getString("equipment")
            obj.optJSONArray("primaryMuscles")?.let { arr -> for (j in 0 until arr.length()) muscleNames += arr.getString(j) }
            obj.optJSONArray("secondaryMuscles")?.let { arr -> for (j in 0 until arr.length()) muscleNames += arr.getString(j) }
        }

        val equipmentIds = dao.insertEquipmentTypes(equipmentNames.map { EquipmentTypeEntity(name = it) })
        val equipmentIdByName = equipmentNames.toList().zip(equipmentIds).toMap()

        val muscleIds = dao.insertMuscles(muscleNames.map { MuscleEntity(name = it) })
        val muscleIdByName = muscleNames.toList().zip(muscleIds).toMap()

        val externalIdOrder = ArrayList<String>(array.length())
        val exerciseEntities = ArrayList<ExerciseEntity>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val externalId = obj.getString("id")
            externalIdOrder += externalId
            exerciseEntities += ExerciseEntity(
                externalExerciseId = externalId,
                title = obj.getString("name"),
                originalName = obj.getString("originalName"),
                gifUrl = obj.optString("gifUrl").ifBlank { null },
                category = parseEnum(obj.optString("category"), ExerciseCategory.GENERAL_FITNESS),
                equipmentId = equipmentIdByName.getValue(obj.getString("equipment")),
                level = parseEnum(obj.optString("level"), ExerciseLevel.BEGINNER),
                // ~30 records in the dataset have a null `force`; treat those as a static hold.
                force = parseEnum(obj.optString("force"), ExerciseForce.STATIC)
            )
        }
        val exerciseIds = dao.insertExercises(exerciseEntities)
        val exerciseIdByExternalId = externalIdOrder.zip(exerciseIds).toMap()

        val muscleCrossRefs = ArrayList<ExerciseMuscleEntity>()
        val instructionSteps = ArrayList<ExerciseInstructionStepEntity>()
        val images = ArrayList<ExerciseImageEntity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val exerciseId = exerciseIdByExternalId.getValue(obj.getString("id"))

            obj.optJSONArray("primaryMuscles")?.let { arr ->
                for (j in 0 until arr.length()) {
                    muscleCrossRefs += ExerciseMuscleEntity(exerciseId, muscleIdByName.getValue(arr.getString(j)), MuscleRole.PRIMARY)
                }
            }
            obj.optJSONArray("secondaryMuscles")?.let { arr ->
                for (j in 0 until arr.length()) {
                    muscleCrossRefs += ExerciseMuscleEntity(exerciseId, muscleIdByName.getValue(arr.getString(j)), MuscleRole.SECONDARY)
                }
            }
            obj.optJSONArray("instructions")?.let { arr ->
                for (j in 0 until arr.length()) {
                    instructionSteps += ExerciseInstructionStepEntity(
                        exerciseId = exerciseId,
                        stepNumber = j + 1,
                        instructionText = arr.getString(j)
                    )
                }
            }
            obj.optJSONArray("imageUrls")?.let { arr ->
                for (j in 0 until arr.length()) {
                    images += ExerciseImageEntity(exerciseId = exerciseId, orderIndex = j, imageUrl = arr.getString(j))
                }
            }
        }
        dao.insertExerciseMuscles(muscleCrossRefs)
        dao.insertInstructionSteps(instructionSteps)
        dao.insertExerciseImages(images)
    }

    private inline fun <reified T : Enum<T>> parseEnum(raw: String?, fallback: T): T {
        val name = raw?.trim()?.uppercase().orEmpty()
        return enumValues<T>().firstOrNull { it.name == name } ?: fallback
    }

    private data class SeedExercise(val title: String, val reps: Int? = null, val durationSec: Int? = null)

    private suspend fun seedSystemPlans(planDao: WorkoutPlanDao, exerciseDao: ExerciseDao) {
        createSystemPlan(
            planDao, exerciseDao,
            title = "Abs Beginner",
            description = "A gentle bodyweight core circuit to build the habit.",
            category = WorkoutCategory.BUILD_MUSCLE,
            level = WorkoutLevel.BEGINNER,
            exercises = listOf(
                SeedExercise("3/4 Sit - Up", reps = 12),
                SeedExercise("Air Bike", reps = 20),
                SeedExercise("Alternate Heel Touchers", reps = 16),
                SeedExercise("Bent - Knee Hip Raise", reps = 12),
                SeedExercise("Butt - Ups", reps = 15)
            )
        )
        createSystemPlan(
            planDao, exerciseDao,
            title = "Full Body Shred",
            description = "No-equipment full body burner covering legs, chest and core.",
            category = WorkoutCategory.BUILD_MUSCLE,
            level = WorkoutLevel.ADVANCED,
            exercises = listOf(
                SeedExercise("Bodyweight Squat", reps = 20),
                SeedExercise("Incline Push - Up", reps = 12),
                SeedExercise("Walking Lunge", reps = 16),
                SeedExercise("Air Bike", reps = 20),
                SeedExercise("Bench Dips", reps = 12)
            )
        )
        createSystemPlan(
            planDao, exerciseDao,
            title = "7 Min Strong Arms",
            description = "Quick arms and shoulders finisher, no equipment needed.",
            category = WorkoutCategory.BUILD_MUSCLE,
            level = WorkoutLevel.BEGINNER,
            exercises = listOf(
                SeedExercise("Arm Circles", durationSec = 30),
                SeedExercise("Body Tricep Press", reps = 12),
                SeedExercise("Shoulder Circles", durationSec = 30),
                SeedExercise("Shoulder Raise", reps = 12),
                SeedExercise("Bench Dips", reps = 12)
            )
        )
        createSystemPlan(
            planDao, exerciseDao,
            title = "Quick Cardio Burn",
            description = "Short, sweaty cardio intervals to burn fat fast.",
            category = WorkoutCategory.BURN_FAT,
            level = WorkoutLevel.BEGINNER,
            exercises = listOf(
                SeedExercise("Crunch", reps = 20),
                SeedExercise("Cross - Body Crunch", reps = 16),
                SeedExercise("Carioca Quick Step", durationSec = 30),
                SeedExercise("Alternate Leg Diagonal Bound", durationSec = 30)
            )
        )
        createSystemPlan(
            planDao, exerciseDao,
            title = "Stretch & Warm Up",
            description = "Loosen up before or after training with these mobility moves.",
            category = WorkoutCategory.STRETCH_AND_WARM_UP,
            level = WorkoutLevel.BEGINNER,
            exercises = listOf(
                SeedExercise("Cat Stretch", durationSec = 30),
                SeedExercise("Child's Pose", durationSec = 30),
                SeedExercise("Chin To Chest Stretch", durationSec = 20),
                SeedExercise("Hug Knees To Chest", durationSec = 20),
                SeedExercise("Side Neck Stretch", durationSec = 20)
            )
        )
    }

    private suspend fun createSystemPlan(
        planDao: WorkoutPlanDao,
        exerciseDao: ExerciseDao,
        title: String,
        description: String,
        category: WorkoutCategory,
        level: WorkoutLevel,
        exercises: List<SeedExercise>
    ) {
        val planId = planDao.insertPlan(
            WorkoutPlanEntity(
                title = title,
                description = description,
                category = category,
                level = level,
                source = WorkoutPlanSource.SYSTEM
            )
        )
        val dayId = planDao.insertPlanDay(WorkoutPlanDayEntity(planId = planId, dayNumber = 1, title = "Day 1"))
        exercises.forEachIndexed { index, seed ->
            val exercise = exerciseDao.getExerciseByTitle(seed.title) ?: return@forEachIndexed
            planDao.insertPlanExercise(
                WorkoutPlanExerciseEntity(
                    planDayId = dayId,
                    exerciseId = exercise.exerciseId,
                    orderIndex = index,
                    targetReps = seed.reps,
                    targetDurationSec = seed.durationSec,
                    restAfterSec = 15
                )
            )
        }
    }
}
