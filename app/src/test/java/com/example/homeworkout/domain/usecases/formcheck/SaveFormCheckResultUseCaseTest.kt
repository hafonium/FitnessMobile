package com.example.homeworkout.domain.usecases.formcheck

import com.example.homeworkout.domain.models.FormAnalysis
import com.example.homeworkout.domain.models.enums.FormCheckExercise
import com.example.homeworkout.domain.models.enums.FormCheckStatus
import com.example.homeworkout.domain.repositories.FormCheckRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveFormCheckResultUseCaseTest {
    @Test
    fun `saves the analysis and returns the new id`() = runBlocking {
        val repository = FakeSaveFormCheckRepository()
        val useCase = SaveFormCheckResultUseCase(repository)
        val analysis = FormAnalysis(
            exerciseName = "Standard Squat",
            score = 91,
            status = FormCheckStatus.EXCELLENT,
            observations = emptyList(),
            primaryCorrectionTip = "Keep it up."
        )

        val id = useCase(analysis)

        assertEquals(analysis, repository.receivedAnalysis)
        assertEquals(42L, id)
    }
}

private class FakeSaveFormCheckRepository : FormCheckRepository {
    var receivedAnalysis: FormAnalysis? = null

    override suspend fun analyzeForm(frames: List<ByteArray>, exerciseHint: FormCheckExercise): FormAnalysis {
        throw NotImplementedError()
    }

    override suspend fun saveResult(analysis: FormAnalysis): Long {
        receivedAnalysis = analysis
        return 42L
    }

    override fun observeHistory(): Flow<List<FormAnalysis>> = emptyFlow()
}
