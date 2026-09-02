package com.example.homeworkout.data.local.seed

import android.content.Context
import com.example.homeworkout.data.catalog.WorkoutPlanCatalogParser
import com.example.homeworkout.data.catalog.WorkoutPlanCatalogSource
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
import com.example.homeworkout.domain.models.catalog.CatalogPlan
import com.example.homeworkout.domain.models.catalog.PatternSlot
import com.example.homeworkout.domain.models.catalog.Prescription
import com.example.homeworkout.domain.models.catalog.WorkoutPlanCatalog
import com.example.homeworkout.domain.models.enums.ExerciseCategory
import com.example.homeworkout.domain.models.enums.ExerciseForce
import com.example.homeworkout.domain.models.enums.ExerciseLevel
import com.example.homeworkout.domain.models.enums.MuscleRole
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.models.enums.WorkoutLevel
import com.example.homeworkout.domain.models.enums.WorkoutPlanSource
import org.json.JSONArray

/**
 * Populates a freshly-created database on first launch (or after a destructive upgrade):
 *  - the full exercise library from `assets/exercises.json` (a copy of data/data.json), and
 *  - the 16 system workout plans from `assets/workout_plan_catalog.json`, materialized by
 *    resolving every day-pattern slot to real exercises (docs/workout_plan_selection_guide.md §7).
 *
 * Every step is guarded by a row-count check so re-invocation is a no-op.
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

        val librarySeeded = exerciseDao.countExercises() > 0
        val exerciseRefs = when {
            !librarySeeded -> seedExercisesFromAssets(context, exerciseDao)
            workoutPlanDao.countPlans() == 0 -> loadExerciseRefs(exerciseDao)
            else -> emptyList()
        }

        if (workoutPlanDao.countPlans() == 0 && exerciseRefs.isNotEmpty()) {
            val catalog = WorkoutPlanCatalogParser.parse(
                context.assets.open(WorkoutPlanCatalogSource.ASSET_NAME).bufferedReader().use { it.readText() }
            )
            seedCatalogPlans(workoutPlanDao, catalog, exerciseRefs)
        }
        return userId
    }

    private suspend fun seedDefaultUser(userDao: UserDao): Long {
        userDao.getUserByEmail(DEFAULT_USER_EMAIL)?.let { return it.userId }
        val userId = userDao.insertUser(UserEntity(email = DEFAULT_USER_EMAIL, passwordHash = "local-only"))
        userDao.upsertUserSettings(UserSettingsEntity(userId = userId))
        return userId
    }

    // ---------------------------------------------------------------------------------------------
    // Exercise library
    // ---------------------------------------------------------------------------------------------

    /** Everything the plan seeder needs to know about one seeded exercise, kept in memory. */
    private data class ExerciseRef(
        val exerciseId: Long,
        val category: ExerciseCategory,
        val level: ExerciseLevel,
        val force: ExerciseForce,
        val equipment: String,
        val primaryMuscles: Set<String>
    )

    private suspend fun seedExercisesFromAssets(context: Context, dao: ExerciseDao): List<ExerciseRef> {
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
        val refs = ArrayList<ExerciseRef>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val exerciseId = exerciseIdByExternalId.getValue(obj.getString("id"))
            val entity = exerciseEntities[i]
            val primary = LinkedHashSet<String>()

            obj.optJSONArray("primaryMuscles")?.let { arr ->
                for (j in 0 until arr.length()) {
                    val name = arr.getString(j)
                    primary += name
                    muscleCrossRefs += ExerciseMuscleEntity(exerciseId, muscleIdByName.getValue(name), MuscleRole.PRIMARY)
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

            refs += ExerciseRef(
                exerciseId = exerciseId,
                category = entity.category,
                level = entity.level,
                force = entity.force,
                equipment = obj.getString("equipment"),
                primaryMuscles = primary
            )
        }
        dao.insertExerciseMuscles(muscleCrossRefs)
        dao.insertInstructionSteps(instructionSteps)
        dao.insertExerciseImages(images)
        return refs
    }

    /** Rebuilds [ExerciseRef]s from the DB when the library is already seeded but plans are not. */
    private suspend fun loadExerciseRefs(dao: ExerciseDao): List<ExerciseRef> {
        return dao.getAllExerciseRefs().map { row ->
            ExerciseRef(
                exerciseId = row.exerciseId,
                category = row.category,
                level = row.level,
                force = row.force,
                equipment = row.equipmentName,
                primaryMuscles = row.primaryMusclesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            )
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Catalog plans
    // ---------------------------------------------------------------------------------------------

    private val TIMED_CATEGORIES = setOf(ExerciseCategory.CARDIO_HIIT, ExerciseCategory.STRETCHING)
    private val TIMED_ROLES = setOf("warm_up", "cool_down", "mobility", "circuit")

    private suspend fun seedCatalogPlans(
        planDao: WorkoutPlanDao,
        catalog: WorkoutPlanCatalog,
        refs: List<ExerciseRef>
    ) {
        val byCategory = refs.groupBy { it.category }
        val allPlanExercises = ArrayList<WorkoutPlanExerciseEntity>()

        for (plan in catalog.plans) {
            val prescription = catalog.prescriptions[prescriptionKey(plan.level)]
                ?: catalog.prescriptions.getValue("intermediate")
            val allowedLevels = allowedLevels(plan.level)
            val equipment = (plan.allowedEquipment ?: plan.preferredEquipment).toMutableSet().apply { add("bodyweight") }

            val planId = planDao.insertPlan(
                WorkoutPlanEntity(
                    title = plan.title,
                    description = describe(plan),
                    category = goalToWorkoutCategory(plan.goal),
                    level = levelToWorkoutLevel(plan.level),
                    source = WorkoutPlanSource.SYSTEM
                )
            )

            // Non-focus plans avoid repeating an exercise anywhere in the week; focus/specialization
            // plans deliberately re-use the same pattern, so they only dedupe within a single day.
            val usedInPlan = HashSet<Long>()
            val isFocusPlan = plan.goal == "focus_area"

            plan.schedule.forEachIndexed { dayIndex, patternId ->
                val pattern = catalog.dayPatterns[patternId] ?: return@forEachIndexed
                val dayId = planDao.insertPlanDay(
                    WorkoutPlanDayEntity(planId = planId, dayNumber = dayIndex + 1, title = pattern.label)
                )

                val usedInDay = HashSet<Long>()
                var orderIndex = 0
                pattern.slots.forEachIndexed { slotIndex, slot ->
                    val slotCategory = parseEnum(slot.category, ExerciseCategory.GENERAL_FITNESS)
                    val pool = byCategory[slotCategory].orEmpty()
                    val picks = pickForSlot(
                        pool = pool,
                        slot = slot,
                        allowedLevels = allowedLevels,
                        equipment = equipment,
                        used = if (isFocusPlan) usedInDay else usedInDay + usedInPlan,
                        seedKey = "${plan.id}|$patternId|$slotIndex"
                    )
                    val timed = slotCategory in TIMED_CATEGORIES || slot.role in TIMED_ROLES

                    for (ref in picks) {
                        usedInPlan += ref.exerciseId
                        usedInDay += ref.exerciseId
                        allPlanExercises += WorkoutPlanExerciseEntity(
                            planDayId = dayId,
                            exerciseId = ref.exerciseId,
                            orderIndex = orderIndex++,
                            targetReps = if (timed) null else prescription.repMid,
                            targetDurationSec = if (timed) prescription.timedWorkSeconds else null,
                            restAfterSec = if (timed) prescription.timedRestSeconds else prescription.strengthRestSeconds
                        )
                    }
                }
            }
        }

        planDao.insertPlanExercises(allPlanExercises)
    }

    private fun pickForSlot(
        pool: List<ExerciseRef>,
        slot: PatternSlot,
        allowedLevels: Set<ExerciseLevel>,
        equipment: Set<String>,
        used: Set<Long>,
        seedKey: String
    ): List<ExerciseRef> {
        if (pool.isEmpty()) return emptyList()
        val slotForce = slot.force
        val slotMuscles = slot.primaryMuscles.toSet()

        fun filtered(useMuscle: Boolean, useForce: Boolean, useEquipment: Boolean): List<ExerciseRef> = pool.filter { ref ->
            ref.level in allowedLevels &&
                (!useEquipment || ref.equipment in equipment) &&
                (!useForce || slotForce == null || ref.force.name.equals(slotForce, ignoreCase = true)) &&
                (!useMuscle || slotMuscles.isEmpty() || ref.primaryMuscles.any { it in slotMuscles })
        }

        // Fallback ladder from docs/workout_plan_selection_guide.md §Step 4.
        val candidates = filtered(useMuscle = true, useForce = true, useEquipment = true)
            .ifTooFew(slot.count) { filtered(useMuscle = false, useForce = true, useEquipment = true) }
            .ifTooFew(slot.count) { filtered(useMuscle = false, useForce = false, useEquipment = true) }
            .ifTooFew(slot.count) { filtered(useMuscle = false, useForce = false, useEquipment = false) }
            .ifTooFew(slot.count) { pool.filter { it.level in allowedLevels } }
            .ifEmpty { pool }

        val ordered = candidates.sortedBy { "$seedKey|${it.exerciseId}".hashCode() }
        val fresh = ordered.filter { it.exerciseId !in used }
        val chosen = (fresh + ordered).distinctBy { it.exerciseId }
        return chosen.take(slot.count)
    }

    private inline fun List<ExerciseRef>.ifTooFew(target: Int, next: () -> List<ExerciseRef>): List<ExerciseRef> =
        if (size >= target) this else next()

    private fun describe(plan: CatalogPlan): String {
        val goalLabel = plan.goal.replace('_', ' ').replaceFirstChar { it.uppercase() }
        val levelLabel = plan.level.replaceFirstChar { it.uppercase() }
        return "$goalLabel · $levelLabel · ${plan.daysPerWeek} days/week · ${plan.minutesMin}–${plan.minutesMax} min per session."
    }

    private fun prescriptionKey(level: String): String = when (level) {
        "beginner", "intermediate", "expert" -> level
        else -> "intermediate"
    }

    private fun allowedLevels(level: String): Set<ExerciseLevel> = when (level) {
        "beginner" -> setOf(ExerciseLevel.BEGINNER)
        "expert" -> setOf(ExerciseLevel.BEGINNER, ExerciseLevel.INTERMEDIATE, ExerciseLevel.EXPERT)
        else -> setOf(ExerciseLevel.BEGINNER, ExerciseLevel.INTERMEDIATE)
    }

    private fun goalToWorkoutCategory(goal: String): WorkoutCategory = when (goal) {
        "build_muscle", "focus_area" -> WorkoutCategory.BUILD_MUSCLE
        "fat_loss" -> WorkoutCategory.BURN_FAT
        "mobility" -> WorkoutCategory.STRETCH_AND_WARM_UP
        else -> WorkoutCategory.KEEP_FIT
    }

    private fun levelToWorkoutLevel(level: String): WorkoutLevel = when (level) {
        "beginner" -> WorkoutLevel.BEGINNER
        "expert" -> WorkoutLevel.ADVANCED
        else -> WorkoutLevel.INTERMEDIATE
    }

    private inline fun <reified T : Enum<T>> parseEnum(raw: String?, fallback: T): T {
        val name = raw?.trim()?.uppercase().orEmpty()
        return enumValues<T>().firstOrNull { it.name == name } ?: fallback
    }
}
