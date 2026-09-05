package com.example.homeworkout.domain.models.enums

/** Overall verdict Gemini assigns a form-check video, drives the result screen's status badge
 * color (green/amber/red). */
enum class FormCheckStatus {
    EXCELLENT,
    ACCEPTABLE,
    NEEDS_IMPROVEMENT
}

/** Optional exercise hint chip shown on the capture sheet. [AUTO_DETECT] lets Gemini identify the
 * exercise itself instead of taking the user's word for it - the system instruction always asks
 * it to confirm what it actually observes. */
enum class FormCheckExercise(val label: String) {
    AUTO_DETECT("Auto-Detect"),
    PUSH_UP("Push-up"),
    SQUAT("Squat"),
    PULL_UP("Pull-up"),
    PLANK("Plank")
}
