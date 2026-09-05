package com.example.homeworkout.domain.usecases.training

import com.example.homeworkout.domain.models.training.StructuredProgramProgress
import com.example.homeworkout.domain.models.training.StructuredTrainingProgram
import com.example.homeworkout.domain.models.training.StructuredTrainingSession
import com.example.homeworkout.domain.models.training.StructuredTrainingWeek
import com.example.homeworkout.domain.models.training.TrainingEnrollmentStatus
import com.example.homeworkout.domain.models.training.TrainingProgramKind
import com.example.homeworkout.domain.repositories.StructuredTrainingCatalogRepository
import com.example.homeworkout.domain.repositories.StructuredTrainingProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CompleteStructuredSessionUseCaseTest {
    @Test fun optionalSessionDoesNotBlockWeekCompletion() = runBlocking {
        val required = session("w1s1", optional = false)
        val optional = session("w1s2", optional = true)
        val program = program(listOf(week(1, required, optional), week(2, session("w2s1", false))))
        val progress = FakeProgressRepository()

        CompleteStructuredSessionUseCase(progress, FakeCatalog(program))(program.id, required.id)

        assertEquals(setOf(required.id), progress.requiredIds)
        assertFalse(optional.id in progress.requiredIds)
        assertEquals(2, progress.nextWeek)
    }

    private class FakeCatalog(private val program: StructuredTrainingProgram) : StructuredTrainingCatalogRepository {
        override suspend fun getProgram(programId: String) = program
    }

    private class FakeProgressRepository : StructuredTrainingProgressRepository {
        var requiredIds = emptySet<String>()
        var nextWeek: Int? = null
        override fun observeProgress(programId: String): Flow<StructuredProgramProgress> = flowOf(
            StructuredProgramProgress(programId, TrainingEnrollmentStatus.ACTIVE, 1, emptySet(), null)
        )
        override suspend fun enroll(programId: String) = Unit
        override suspend fun setActiveSession(programId: String, sessionId: String, weekNumber: Int) = Unit
        override suspend fun completeSession(programId: String, sessionId: String, weekNumber: Int, requiredSessionIds: Set<String>, nextWeekNumber: Int?, isLastWeek: Boolean, durationSeconds: Int?, distanceMeters: Double?) {
            requiredIds = requiredSessionIds
            nextWeek = nextWeekNumber
        }
        override suspend fun resetWeek(programId: String, weekNumber: Int) = Unit
    }

    private fun session(id: String, optional: Boolean) = StructuredTrainingSession(id, id.last().digitToInt(), id, "EASY", 20, 20, optional, emptyList())
    private fun week(number: Int, vararg sessions: StructuredTrainingSession) = StructuredTrainingWeek("w$number", number, "Week", 20, 20, null, sessions.toList(), null, null, null, null)
    private fun program(weeks: List<StructuredTrainingWeek>) = StructuredTrainingProgram(
        "test", TrainingProgramKind.RUNNING, "Test", null, weeks.size, "3", "Beginner", "Goal", null,
        emptyList(), emptyList(), weeks, "After", "Guide", emptyList(), emptyList()
    )
}
