package com.example.homeworkout.domain.models

import com.example.homeworkout.domain.models.enums.FormCheckStatus

/** One evaluated body segment from a form-check result, e.g. "Lumbar Spine & Hips". */
data class FormCheckObservation(
    val jointArea: String,
    val isCorrect: Boolean,
    val feedback: String
)

/** Gemini's biomechanical evaluation of one exercise repetition, from
 * [com.example.homeworkout.domain.repositories.FormCheckRepository.analyzeForm]. [id] is 0 until
 * [com.example.homeworkout.domain.repositories.FormCheckRepository.saveResult] persists it. */
data class FormAnalysis(
    val id: Long = 0,
    val exerciseName: String,
    val score: Int,
    val status: FormCheckStatus,
    val observations: List<FormCheckObservation>,
    val primaryCorrectionTip: String,
    /** Advice for getting a better follow-up recording (lighting, framing, angle) - purely
     * informational, never a reason to refuse the current analysis. */
    val recordingTip: String = "",
    val analyzedAt: Long = System.currentTimeMillis()
)
