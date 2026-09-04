package com.example.homeworkout.domain.usecases.progression

import com.example.homeworkout.domain.models.enums.ProgressionBranch

/**
 * Static definition of one skill-tree tier. [exerciseTitle] is the exact `exercises.title`
 * (i.e. the free-exercise-db `name` field, see docs/architecture.md) this node maps onto — null
 * means no calisthenics-specific variant exists in the seeded library, so [GetProgressionTreeUseCase]
 * resolves this node as a fallback placeholder (Technical Acceptance Criteria #2) instead of
 * throwing. Exactly one of [targetReps]/[targetHoldSeconds] should be set.
 *
 * Mastery: [ProgressionCatalog]/[GetProgressionTreeUseCase] cannot read "N sets in one session"
 * from `workout_session_exercises` because the schema logs one rep/duration result per exercise
 * per session (no set-by-set breakdown - see WorkoutSessionExerciseEntity). We instead require the
 * node's target to be met or exceeded across [targetCompletions] separate completed sessions,
 * which is the closest honest signal the existing history actually supports.
 */
data class ProgressionNodeDef(
    val id: String,
    val order: Int,
    val name: String,
    val exerciseTitle: String?,
    val formTips: List<String>,
    val targetReps: Int? = null,
    val targetHoldSeconds: Int? = null,
    val targetCompletions: Int = 3,
    val badgeId: String? = null
)

object ProgressionCatalog {

    private val push = listOf(
        ProgressionNodeDef(
            id = "push_wall",
            order = 1,
            name = "Wall Push-up",
            exerciseTitle = null,
            formTips = listOf(
                "Stand an arm's length from a wall, hands shoulder-width apart.",
                "Keep your body in a straight line from head to heels as you bend your elbows.",
                "Push back to full arm extension without letting your hips sag."
            ),
            targetReps = 20
        ),
        ProgressionNodeDef(
            id = "push_incline",
            order = 2,
            name = "Incline Push-up",
            exerciseTitle = "Incline Push - Up",
            formTips = listOf(
                "Hands on a bench or step, body straight from shoulders to ankles.",
                "Lower your chest to the edge under control, elbows at ~45 degrees.",
                "Brace your core so your hips don't dip."
            ),
            targetReps = 18
        ),
        ProgressionNodeDef(
            id = "push_knee",
            order = 3,
            name = "Knee Push-up",
            exerciseTitle = null,
            formTips = listOf(
                "Knees down, straight line from head to knees.",
                "Lower until your chest nearly touches the floor.",
                "Keep elbows tracking back, not flared to 90 degrees."
            ),
            targetReps = 15
        ),
        ProgressionNodeDef(
            id = "push_standard",
            order = 4,
            name = "Standard Push-up",
            exerciseTitle = "Push-Up",
            formTips = listOf(
                "Full plank position, hands under shoulders.",
                "Lower your chest to an inch off the floor, elbows ~45 degrees.",
                "Drive back up without letting your hips pike or sag."
            ),
            targetReps = 12,
            badgeId = "skill_push_standard_pushup"
        ),
        ProgressionNodeDef(
            id = "push_diamond",
            order = 5,
            name = "Diamond Push-up",
            exerciseTitle = "Push - Up - Close Triceps Position",
            formTips = listOf(
                "Thumbs and index fingers touching under your chest.",
                "Elbows stay close to your ribs on the way down.",
                "Keep your body rigid - no hip sag."
            ),
            targetReps = 10
        ),
        ProgressionNodeDef(
            id = "push_archer",
            order = 6,
            name = "Archer Push-up",
            exerciseTitle = null,
            formTips = listOf(
                "Hands wide, shift your weight over one bent arm as you lower.",
                "The other arm stays straight, sliding out to the side.",
                "Push back to center, then repeat on the other side."
            ),
            targetReps = 8,
            targetCompletions = 2
        ),
        ProgressionNodeDef(
            id = "push_one_arm",
            order = 7,
            name = "One-Arm Push-up",
            exerciseTitle = "Single - Arm Push - Up",
            formTips = listOf(
                "Feet wide for a stable base, free hand behind your back.",
                "Lower under full control, chest staying square to the floor.",
                "Drive up without rotating your torso."
            ),
            targetReps = 3,
            targetCompletions = 1,
            badgeId = "skill_push_one_arm_pushup"
        )
    )

    private val pull = listOf(
        ProgressionNodeDef(
            id = "pull_door_row",
            order = 1,
            name = "Door / Table Row",
            exerciseTitle = null,
            formTips = listOf(
                "Grip a sturdy table edge or anchored strap, lean back with straight arms.",
                "Pull your chest toward your hands, squeezing your shoulder blades.",
                "Lower back down under control."
            ),
            targetReps = 15
        ),
        ProgressionNodeDef(
            id = "pull_australian",
            order = 2,
            name = "Australian (Incline) Pull-up",
            exerciseTitle = null,
            formTips = listOf(
                "Bar at hip height, body straight, heels on the floor.",
                "Pull your chest to the bar, elbows close to your body.",
                "Lower with control to full arm extension."
            ),
            targetReps = 12
        ),
        ProgressionNodeDef(
            id = "pull_scapular",
            order = 3,
            name = "Scapular Pull-up",
            exerciseTitle = "Scapular Pull - Up",
            formTips = listOf(
                "Hang from the bar with straight arms.",
                "Without bending your elbows, shrug your shoulder blades down and together.",
                "Lift your body a few inches, then lower with control."
            ),
            targetReps = 10
        ),
        ProgressionNodeDef(
            id = "pull_negative",
            order = 4,
            name = "Negative Pull-up",
            exerciseTitle = null,
            formTips = listOf(
                "Jump or step up to chin over the bar.",
                "Lower yourself as slowly as possible to full extension.",
                "Aim for at least 5 seconds per rep."
            ),
            targetReps = 6
        ),
        ProgressionNodeDef(
            id = "pull_standard",
            order = 5,
            name = "Standard Chin-up / Pull-up",
            exerciseTitle = "Pull - Up",
            formTips = listOf(
                "Dead hang start, shoulders engaged.",
                "Pull until your chin clears the bar.",
                "Lower under control to a full dead hang."
            ),
            targetReps = 8,
            badgeId = "skill_pull_standard_pullup"
        ),
        ProgressionNodeDef(
            id = "pull_archer",
            order = 6,
            name = "Archer Pull-up",
            exerciseTitle = null,
            formTips = listOf(
                "Wide grip, pull toward one hand while the other arm stays extended.",
                "Keep your core tight to avoid swinging.",
                "Alternate sides between reps."
            ),
            targetReps = 6,
            targetCompletions = 2
        ),
        ProgressionNodeDef(
            id = "pull_muscle_up",
            order = 7,
            name = "Muscle-up",
            exerciseTitle = null,
            formTips = listOf(
                "Explosive pull that carries your chest above the bar.",
                "Transition your wrists over the bar as your elbows rise.",
                "Press out to full lockout on top."
            ),
            targetReps = 2,
            targetCompletions = 1,
            badgeId = "skill_pull_muscle_up"
        )
    )

    private val legs = listOf(
        ProgressionNodeDef(
            id = "legs_assisted_squat",
            order = 1,
            name = "Assisted Squat",
            exerciseTitle = null,
            formTips = listOf(
                "Hold a sturdy rail or door frame for balance.",
                "Sit your hips back and down, chest tall.",
                "Drive through your heels to stand."
            ),
            targetReps = 20
        ),
        ProgressionNodeDef(
            id = "legs_bodyweight_squat",
            order = 2,
            name = "Bodyweight Squat",
            exerciseTitle = "Bodyweight Squat",
            formTips = listOf(
                "Feet shoulder-width, toes slightly out.",
                "Sit back and down until thighs are at least parallel.",
                "Keep your chest up and knees tracking over your toes."
            ),
            targetReps = 20,
            badgeId = "skill_legs_bodyweight_squat"
        ),
        ProgressionNodeDef(
            id = "legs_close_stance_squat",
            order = 3,
            name = "Close-Stance Squat",
            exerciseTitle = null,
            formTips = listOf(
                "Feet together or nearly together.",
                "Squat down slowly - balance is the challenge, not depth.",
                "Keep your torso upright throughout."
            ),
            targetReps = 15
        ),
        ProgressionNodeDef(
            id = "legs_bulgarian_split_squat",
            order = 4,
            name = "Bulgarian Split Squat",
            exerciseTitle = "Split Squats",
            formTips = listOf(
                "Rear foot elevated on a bench or step behind you.",
                "Lower straight down until your rear knee nearly taps the floor.",
                "Drive through the front heel to stand."
            ),
            targetReps = 12,
            targetCompletions = 2
        ),
        ProgressionNodeDef(
            id = "legs_pistol_prep",
            order = 5,
            name = "Pistol Squat Prep (Box)",
            exerciseTitle = null,
            formTips = listOf(
                "Stand in front of a box or bench, one leg extended forward.",
                "Sit back onto the box on one leg, then stand back up.",
                "Keep the extended leg's heel off the floor throughout."
            ),
            targetReps = 8,
            targetCompletions = 2
        ),
        ProgressionNodeDef(
            id = "legs_pistol_squat",
            order = 6,
            name = "Full Pistol Squat",
            exerciseTitle = null,
            formTips = listOf(
                "One leg extended straight in front, arms forward for balance.",
                "Lower all the way down under control, hips to heel.",
                "Stand back up without touching your extended foot down."
            ),
            targetReps = 3,
            targetCompletions = 1,
            badgeId = "skill_legs_pistol_squat"
        )
    )

    private val core = listOf(
        ProgressionNodeDef(
            id = "core_tuck_plank",
            order = 1,
            name = "Tuck Plank",
            exerciseTitle = null,
            formTips = listOf(
                "Forearm plank with knees tucked toward your chest.",
                "Keep your lower back flat - no arching.",
                "Hold the tuck rather than resting on your knees."
            ),
            targetHoldSeconds = 30
        ),
        ProgressionNodeDef(
            id = "core_knee_to_elbow",
            order = 2,
            name = "Knee-to-Elbow",
            exerciseTitle = "Elbow to Knee",
            formTips = listOf(
                "Standing or plank position, drive one knee toward the opposite elbow.",
                "Keep your core braced, not just swinging the limb.",
                "Alternate sides with control."
            ),
            targetReps = 20
        ),
        ProgressionNodeDef(
            id = "core_forearm_plank",
            order = 3,
            name = "Standard Forearm Plank",
            exerciseTitle = "Plank",
            formTips = listOf(
                "Forearms and toes on the floor, elbows under shoulders.",
                "One straight line from head to heels - no sagging or piking.",
                "Breathe steadily; don't hold your breath."
            ),
            targetHoldSeconds = 45,
            badgeId = "skill_core_plank"
        ),
        ProgressionNodeDef(
            id = "core_knee_raise",
            order = 4,
            name = "Hanging / Lying Knee Raise",
            exerciseTitle = "Hanging Leg Raise",
            formTips = listOf(
                "Hang from a bar or lie flat, legs extended.",
                "Curl your knees up toward your chest without swinging.",
                "Lower with control back to the start."
            ),
            targetReps = 15
        ),
        ProgressionNodeDef(
            id = "core_leg_raise",
            order = 5,
            name = "Lying Leg Raise",
            exerciseTitle = "Flat Bench Lying Leg Raise",
            formTips = listOf(
                "Lie flat, legs straight and together.",
                "Raise your legs to vertical without lifting your lower back off the floor.",
                "Lower slowly, stopping just above the floor."
            ),
            targetReps = 15,
            targetCompletions = 2
        ),
        ProgressionNodeDef(
            id = "core_l_sit",
            order = 6,
            name = "L-Sit",
            exerciseTitle = null,
            formTips = listOf(
                "Support yourself on parallettes, blocks, or the floor.",
                "Raise both legs straight out in front, parallel to the ground.",
                "Keep your shoulders pressed down and elbows locked."
            ),
            targetHoldSeconds = 15,
            targetCompletions = 2
        ),
        ProgressionNodeDef(
            id = "core_dragon_flag",
            order = 7,
            name = "Dragon Flag",
            exerciseTitle = null,
            formTips = listOf(
                "Lie on a bench, hold behind your head for anchoring.",
                "Raise your whole body to a straight line, pivoting at your shoulders.",
                "Lower as slowly as possible without letting your hips sag."
            ),
            targetReps = 3,
            targetCompletions = 1,
            badgeId = "skill_core_dragon_flag"
        )
    )

    private val byBranch: Map<ProgressionBranch, List<ProgressionNodeDef>> = mapOf(
        ProgressionBranch.PUSH to push,
        ProgressionBranch.PULL to pull,
        ProgressionBranch.LEGS to legs,
        ProgressionBranch.CORE to core
    )

    fun nodesFor(branch: ProgressionBranch): List<ProgressionNodeDef> =
        byBranch.getValue(branch).sortedBy { it.order }

    fun allNodes(): List<Pair<ProgressionBranch, ProgressionNodeDef>> =
        byBranch.flatMap { (branch, defs) -> defs.map { branch to it } }
}
