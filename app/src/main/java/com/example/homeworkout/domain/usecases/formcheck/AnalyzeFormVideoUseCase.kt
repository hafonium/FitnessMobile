package com.example.homeworkout.domain.usecases.formcheck

import com.example.homeworkout.domain.models.FormAnalysis
import com.example.homeworkout.domain.models.enums.FormCheckExercise
import com.example.homeworkout.domain.repositories.FormCheckRepository

class AnalyzeFormVideoUseCase(
    private val repository: FormCheckRepository
) {
    suspend operator fun invoke(frames: List<ByteArray>, exerciseHint: FormCheckExercise): FormAnalysis {
        require(frames.isNotEmpty()) { "Record or choose a video before analyzing." }
        require(frames.size <= MAX_FRAMES) { "Too many frames were extracted from this video." }
        require(frames.sumOf { it.size } <= MAX_TOTAL_BYTES) {
            "This video is too large to analyze. Trim it to 5-8 seconds and try again."
        }
        return repository.analyzeForm(frames, exerciseHint)
    }

    private companion object {
        // The capture sheet extracts 4-6 evenly-spaced, 720px-wide JPEG frames per video (see
        // FormCheckScreen.extractFrames) rather than uploading the raw clip, so both bounds here
        // are generous safety nets rather than expected limits in normal use.
        const val MAX_FRAMES = 6
        const val MAX_TOTAL_BYTES = 15 * 1024 * 1024
    }
}
