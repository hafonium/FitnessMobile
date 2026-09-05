package com.example.homeworkout.domain.usecases.formcheck

import com.example.homeworkout.domain.models.FormAnalysis
import com.example.homeworkout.domain.models.FormCheckObservation
import com.example.homeworkout.domain.models.enums.FormCheckExercise
import com.example.homeworkout.domain.models.enums.FormCheckStatus
import com.example.homeworkout.domain.repositories.FormCheckRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AnalyzeFormVideoUseCaseTest {
    @Test
    fun `rejects an empty frame list`() {
        val useCase = AnalyzeFormVideoUseCase(FakeFormCheckRepository())

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(emptyList(), FormCheckExercise.AUTO_DETECT) }
        }
    }

    @Test
    fun `rejects too many frames`() {
        val useCase = AnalyzeFormVideoUseCase(FakeFormCheckRepository())
        val tooManyFrames = List(7) { byteArrayOf(1, 2, 3) }

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(tooManyFrames, FormCheckExercise.AUTO_DETECT) }
        }
    }

    @Test
    fun `rejects an oversized total payload`() {
        val useCase = AnalyzeFormVideoUseCase(FakeFormCheckRepository())
        val oversizedFrames = List(5) { ByteArray(4 * 1024 * 1024) }

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(oversizedFrames, FormCheckExercise.AUTO_DETECT) }
        }
    }

    @Test
    fun `passes frames and exercise hint to repository and returns analysis`() = runBlocking {
        val repository = FakeFormCheckRepository()
        val useCase = AnalyzeFormVideoUseCase(repository)
        val frames = listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))

        val result = useCase(frames, FormCheckExercise.PUSH_UP)

        assertEquals(frames, repository.receivedFrames)
        assertEquals(FormCheckExercise.PUSH_UP, repository.receivedHint)
        assertEquals("Standard Push-up", result.exerciseName)
        assertEquals(82, result.score)
    }
}

private class FakeFormCheckRepository : FormCheckRepository {
    var receivedFrames: List<ByteArray>? = null
    var receivedHint: FormCheckExercise? = null

    override suspend fun analyzeForm(frames: List<ByteArray>, exerciseHint: FormCheckExercise): FormAnalysis {
        receivedFrames = frames
        receivedHint = exerciseHint
        return FormAnalysis(
            exerciseName = "Standard Push-up",
            score = 82,
            status = FormCheckStatus.ACCEPTABLE,
            observations = listOf(
                FormCheckObservation("Elbows & Shoulders", true, "Elbow path stays tucked."),
                FormCheckObservation("Lumbar Spine & Hips", false, "Slight lumbar sagging observed.")
            ),
            primaryCorrectionTip = "Engage the glutes and brace the core.",
            recordingTip = "For best accuracy next time: ensure full-body visibility and adequate lighting."
        )
    }

    override suspend fun saveResult(analysis: FormAnalysis): Long = 1L

    override fun observeHistory(): Flow<List<FormAnalysis>> = emptyFlow()
}
