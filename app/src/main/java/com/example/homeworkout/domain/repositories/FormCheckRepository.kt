package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.FormAnalysis
import com.example.homeworkout.domain.models.enums.FormCheckExercise
import kotlinx.coroutines.flow.Flow

interface FormCheckRepository {
    /** Sends a chronologically-ordered sequence of JPEG-encoded frames sampled from one exercise
     * repetition to Gemini and returns its biomechanical evaluation. [exerciseHint] is the chip
     * the user picked on the capture sheet (may be [FormCheckExercise.AUTO_DETECT]). */
    suspend fun analyzeForm(frames: List<ByteArray>, exerciseHint: FormCheckExercise): FormAnalysis

    /** Persists a result the user chose to keep. Returns the new local id. */
    suspend fun saveResult(analysis: FormAnalysis): Long

    /** Saved results ("Save to History"), most recent first. */
    fun observeHistory(): Flow<List<FormAnalysis>>
}
