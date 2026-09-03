# Workout plan catalog and selection guide

## 1. Data contract used by the planner

The supplied dataset was previously inspected as 515 exercise records. Every record uses these fields:

- `id`, `name`, `originalName`
- `category`, `equipment`, `level`, `force`
- `primaryMuscles[]`, `secondaryMuscles[]`
- `instructions[]`, `imageUrls[]`

The planner must not expect calories, repetitions, sets, duration, contraindications or movement-impact metadata from the exercise dataset. Those values belong to the workout-plan layer or to curated safety metadata.

Observed taxonomy:

- Categories: `abs_core`, `arms_shoulders`, `back_pull`, `cardio_hiit`, `chest_push`, `general_fitness`, `legs_glutes`, `stretching`.
- Levels: `beginner`, `intermediate`, `expert`.
- Force: `pull`, `push`, `static`.
- Equipment: `bands`, `bodyweight`, `dumbbell`, `exercise ball`, `foam roll`, `kettlebells`, `other`.
- Muscles: 17 values, including abdominals, chest, glutes, hamstrings, lats, quadriceps, shoulders, triceps and others.

## 2. Profile questions

### Required onboarding questions

| Question | Stored value | Why it is needed |
|---|---|---|
| What is your main goal? | `general_fitness`, `build_muscle`, `fat_loss`, `mobility`, `focus_area` | Selects the plan family. |
| What is your training experience? | `beginner`, `intermediate`, `expert` | Limits exercise complexity and volume. |
| How many days can you train each week? | 2-6 | Selects a schedule the user can actually follow. |
| How long can each session take? | 15, 20, 30, 45 or 60 minutes | Controls the number of exercise slots. |
| What equipment is available? | Multi-select from the seven dataset values | Prevents impossible exercise selections. Always include `bodyweight` when appropriate. |
| Which body areas are priorities? | Categories and/or muscles | Personalizes focused plans. |
| Do you have injuries, pain, movement restrictions or medical limitations? | Structured flags plus optional text | Hard safety exclusion; this must not be treated as a soft preference. |

### Useful optional questions

| Question | Use |
|---|---|
| Do you need quiet or small-space exercises? | Exclude jumping or space-heavy exercises once those tags are curated. |
| Do you want to avoid floor exercises? | Requires a curated `movement_tags` field; it cannot be inferred reliably from names. |
| Which exercises do you like or dislike? | Adds preferred and excluded exercise IDs. |
| How well have you recovered this week? | Low recovery reduces volume or substitutes a mobility day. |
| Which days of the week are available? | Places demanding sessions with recovery gaps. |
| What is your recent completion rate? | Prevents recommending a larger schedule than the user follows. |

Age, gender, height and weight should not decide whether an exercise is suitable by themselves. Weight may be used for progress reporting and calorie estimation, while safety and ability should be assessed from limitations, experience and actual performance.

## 3. System workout-plan catalog

The catalog contains 16 templates. A template references reusable day patterns; exercises are selected from the imported dataset at plan-generation time.

| ID | Plan | Goal | Level | Days | Session |
|---|---|---|---:|---:|---:|
| `general_beginner_3d` | Beginner Full Body Starter | General fitness | Beginner | 3 | 30-40 min |
| `general_intermediate_4d` | Intermediate Total Fitness | General fitness | Intermediate | 4 | 40-50 min |
| `general_expert_5d` | Advanced Athletic Week | General fitness | Expert | 5 | 45-60 min |
| `muscle_beginner_3d` | Beginner Strength Foundation | Build muscle | Beginner | 3 | 30-45 min |
| `muscle_intermediate_4d` | Intermediate Upper Lower Builder | Build muscle | Intermediate | 4 | 40-55 min |
| `muscle_expert_5d` | Expert Muscle Split | Build muscle | Expert | 5 | 50-60 min |
| `fatloss_beginner_3d` | Beginner Low Barrier Fat Burn | Fat loss | Beginner | 3 | 20-35 min |
| `fatloss_intermediate_4d` | Intermediate HIIT and Strength | Fat loss | Intermediate | 4 | 30-45 min |
| `fatloss_expert_5d` | Expert Conditioning | Fat loss | Expert | 5 | 35-50 min |
| `focus_abs_3d` | Abs and Core Focus | Focus area | Adaptive | 3 | 25-40 min |
| `focus_chest_3d` | Chest Push Focus | Focus area | Adaptive | 3 | 30-45 min |
| `focus_back_3d` | Back and Posture Focus | Focus area | Adaptive | 3 | 30-45 min |
| `focus_arms_shoulders_3d` | Arms and Shoulders Focus | Focus area | Adaptive | 3 | 30-45 min |
| `focus_legs_glutes_3d` | Legs and Glutes Focus | Focus area | Adaptive | 3 | 30-45 min |
| `mobility_5d` | Daily Mobility and Recovery | Mobility | Adaptive | 5 | 15-25 min |
| `travel_bodyweight_4d` | Travel Bodyweight Plan | General fitness | Adaptive | 4 | 20-30 min |

### Reusable day patterns

- `full_body_a`: quadriceps/glutes push, chest/triceps push, back pull and core.
- `full_body_b`: hamstrings/glutes, shoulders/arms, back and core.
- `full_body_c`: cardio plus one movement from each major category.
- `upper_push`: chest, shoulders and triceps.
- `upper_pull`: lats, middle back, traps, biceps and forearms.
- `lower_glutes`: glutes, hamstrings and core.
- `lower_quads`: quadriceps, calves, adductors, abductors and core.
- `hiit_full`: four cardio/HIIT slots plus legs and core.
- `cardio_core`: three cardio/HIIT and three core slots.
- `mixed_conditioning`: cardio plus all major movement categories.
- `abs_focus`, `chest_focus`, `back_focus`, `arms_shoulders_focus`: specialization days.
- `mobility`: static general-fitness and stretching slots.

The full machine-readable schedules and slot counts are in `workout_plan_catalog.json`.

## 4. Selecting a plan for a user

### Step 1: Safety gate

- Convert known restrictions into explicit exclusion tags or exercise IDs.
- Do not infer safety from an exercise name alone.
- If the user reports an acute injury, unexplained pain, pregnancy-related restriction, serious cardiovascular symptoms or a clinician-imposed restriction, do not auto-prescribe until appropriate clearance is recorded.

The current dataset does not contain contraindication, impact, joint-load, position or movement-pattern fields. Add curated tags such as `high_impact`, `jumping`, `overhead`, `deep_knee_flexion`, `floor`, `unilateral` and `requires_balance` before automating injury-specific substitutions.

### Step 2: Hard eligibility filters

Reject a template when:

- Its required equipment is unavailable.
- Its schedule cannot fit the user's available days.
- It conflicts with an explicit safety restriction.
- It cannot fill its required exercise slots after exclusions.

Exercise-level eligibility:

- Beginner user: beginner exercises only.
- Intermediate user: beginner and intermediate exercises.
- Expert user: all levels, preferring expert and intermediate.
- Exercise equipment must be `bodyweight` or one of the user's selected equipment values.
- Excluded exercise IDs are never selected.

### Step 3: Rank eligible templates

Use a normalized score:

```text
template_score =
    0.35 * goal_match
  + 0.20 * level_match
  + 0.15 * days_match
  + 0.10 * duration_fit
  + 0.10 * equipment_coverage
  + 0.10 * focus_match
```

Return the highest-scoring plan as the recommendation and the next two as alternatives. Do not add uncalibrated values to this score.

### Step 4: Fill every day-pattern slot

For each slot, filter by category, allowed level, available equipment and exclusions. Then rank remaining exercises:

```text
exercise_score =
    0.35 * category_match
  + 0.25 * primary_muscle_match
  + 0.10 * force_match
  + 0.10 * equipment_match
  + 0.10 * level_match
  + 0.10 * recent_novelty
```

Use a deterministic tie-break based on `user_id + ISO week + slot_index`. The same user therefore sees a stable weekly plan, while the next week can rotate exercises.

Fallback order when a slot has no candidate:

1. Remove the secondary-muscle preference.
2. Remove the force preference.
3. Allow an exercise one level below the preferred level.
4. Reduce that slot count by one.
5. Never relax equipment, explicit exclusions or safety constraints.

### Step 5: Apply volume and timer prescriptions

| Level | Working sets | Repetitions | Strength rest | Timed work/rest |
|---|---:|---:|---:|---:|
| Beginner | 2 | 8-12 | 60 sec | 30/30 sec |
| Intermediate | 3 | 8-15 | 45 sec | 40/20 sec |
| Expert | 4 | 6-15 | 60 sec | 45/15 sec |

The exercise dataset does not provide sets, repetitions or duration. These are plan prescriptions, not imported exercise attributes.

Scale session size by available time:

- 15 minutes: 1 warm-up, 3-4 main slots, 1 cool-down.
- 20-30 minutes: 1 warm-up, 5-6 main slots, 1 cool-down.
- 45 minutes: use the complete day pattern.
- 60 minutes: complete pattern plus one accessory slot; do not duplicate the same exercise.

### Step 6: Diversity and recovery checks

- Avoid repeating the same exercise within seven days unless it is intentionally part of a specialization plan.
- Avoid demanding work for the same primary muscle on consecutive days.
- Place at least 48 hours between two focus sessions for the same muscle group.
- Keep warm-up and cool-down slots for sessions lasting at least 20 minutes.
- When recovery is low, reduce one working set per exercise or replace the day with `mobility`.

## 5. Recommended profile tables

To support the selector, add these user-domain structures:

- `user_fitness_profiles`: goal, experience level, session minutes, available days and recovery preference.
- `user_available_equipment`: one row per available equipment type.
- `user_focus_preferences`: category or muscle plus priority.
- `user_exercise_preferences`: exercise ID plus `preferred` or `excluded` status.
- `user_limitations`: structured restriction tags, notes, status and optional clearance date.
- `exercise_movement_tags`: curated safety and environment tags not present in the imported dataset.

Keep the original onboarding answers and the chosen plan-template ID. This makes every recommendation explainable and reproducible.

## 6. Example selection

Example profile:

```json
{
  "primaryGoal": "build_muscle",
  "experienceLevel": "intermediate",
  "daysPerWeek": 4,
  "sessionMinutes": 45,
  "availableEquipment": ["bodyweight", "dumbbell", "bands"],
  "focusCategories": ["chest_push", "back_pull"],
  "focusMuscles": ["chest", "lats"],
  "excludedExerciseIds": [],
  "recoveryStatus": "normal"
}
```

Expected template: `muscle_intermediate_4d`.

Schedule:

1. Upper Push
2. Lower Body - Quads and Calves
3. Upper Pull
4. Lower Body - Glutes and Hamstrings

Each slot is then filled with intermediate-or-beginner exercises using only bodyweight, dumbbell or bands, with chest and lats used as ranking preferences rather than unsafe hard requirements.

## 7. Materializing plans in the database

When a user accepts a recommendation:

1. Create a custom or generated `workout_plans` row.
2. Create one `workout_plan_days` row for every scheduled day pattern.
3. Resolve every slot to an imported internal `exercise_id`.
4. Insert ordered `workout_plan_exercises` rows with repetitions, duration and rest.
5. Save the catalog version, profile snapshot and selector version used to create the plan.

Storing the generation version is important: a later algorithm or catalog update must not silently change an already accepted workout plan.
