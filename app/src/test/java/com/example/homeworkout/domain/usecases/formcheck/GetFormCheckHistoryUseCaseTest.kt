package com.example.homeworkout.domain.usecases.formcheck

import com.example.homeworkout.domain.models.FormAnalysis
import com.example.homeworkout.domain.models.enums.FormCheckExercise
import com.example.homeworkout.domain.models.enums.FormCheckStatus
import com.example.homeworkout.domain.repositories.FormCheckRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetFormCheckHistoryUseCaseTest {
    @Test
    fun `returns the repository's saved history`() = runBlocking {
        val saved = listOf(
            FormAnalysis(
                id = 1L,
                exerciseName = "Standard Squat",
                score = 91,
                status = FormCheckStatus.EXCELLENT,
                observations = emptyList(),
                primaryCorrectionTip = "Keep it up."
            )
        )
        val useCase = GetFormCheckHistoryUseCase(FakeHistoryFormCheckRepository(saved))

        assertEquals(saved, useCase().first())
    }
}

private class FakeHistoryFormCheckRepository(private val saved: List<FormAnalysis>) : FormCheckRepository {
    override suspend fun analyzeForm(frames: List<ByteArray>, exerciseHint: FormCheckExercise): FormAnalysis {
        throw NotImplementedError()
    }

    override suspend fun saveResult(analysis: FormAnalysis): Long = throw NotImplementedError()

    override fun observeHistory(): Flow<List<FormAnalysis>> = flowOf(saved)
}
