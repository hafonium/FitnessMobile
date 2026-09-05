package com.example.homeworkout.data.repositories

import com.example.homeworkout.data.local.dao.FormCheckResultDao
import com.example.homeworkout.data.local.dao.UserDao
import com.example.homeworkout.data.local.entities.FormCheckResultEntity
import com.example.homeworkout.data.local.entities.UserEntity
import com.example.homeworkout.data.local.seed.AppDatabaseSeeder
import com.example.homeworkout.data.remote.gemini.GeminiFormAnalysisDto
import com.example.homeworkout.data.remote.gemini.GeminiFormCheckApi
import com.example.homeworkout.domain.models.FormAnalysis
import com.example.homeworkout.domain.models.FormCheckObservation
import com.example.homeworkout.domain.models.enums.FormCheckExercise
import com.example.homeworkout.domain.models.enums.FormCheckStatus
import com.example.homeworkout.domain.repositories.FormCheckRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class FormCheckRepositoryImpl internal constructor(
    private val geminiFormCheckApi: GeminiFormCheckApi,
    private val formCheckResultDao: FormCheckResultDao,
    private val userDao: UserDao
) : FormCheckRepository {

    /** This app has a single local user; resolve (or lazily create) it by its seeded email. */
    private suspend fun currentUserId(): Long {
        userDao.getUserByEmail(AppDatabaseSeeder.DEFAULT_USER_EMAIL)?.let { return it.userId }
        return userDao.insertUser(UserEntity(email = AppDatabaseSeeder.DEFAULT_USER_EMAIL, passwordHash = "local-only"))
    }

    override suspend fun analyzeForm(frames: List<ByteArray>, exerciseHint: FormCheckExercise): FormAnalysis {
        val hint = exerciseHint.takeIf { it != FormCheckExercise.AUTO_DETECT }?.label
        return geminiFormCheckApi.analyzeFrames(frames, hint).toDomain()
    }

    override suspend fun saveResult(analysis: FormAnalysis): Long {
        val entity = FormCheckResultEntity(
            userId = currentUserId(),
            exerciseName = analysis.exerciseName,
            score = analysis.score,
            status = analysis.status,
            observationsJson = analysis.observations.toJson(),
            primaryCorrectionTip = analysis.primaryCorrectionTip,
            recordingTip = analysis.recordingTip,
            analyzedAt = analysis.analyzedAt
        )
        return formCheckResultDao.insertResult(entity)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeHistory(): Flow<List<FormAnalysis>> =
        flow { emit(currentUserId()) }.flatMapLatest { userId ->
            formCheckResultDao.observeResults(userId).map { entities -> entities.map { it.toDomain() } }
        }

    private fun GeminiFormAnalysisDto.toDomain(): FormAnalysis = FormAnalysis(
        exerciseName = exerciseName,
        score = score,
        status = FormCheckStatus.entries.firstOrNull { it.name == status } ?: FormCheckStatus.ACCEPTABLE,
        observations = observations.map { FormCheckObservation(it.jointArea, it.isCorrect, it.feedback) },
        primaryCorrectionTip = primaryCorrectionTip,
        recordingTip = recordingTip
    )

    private fun FormCheckResultEntity.toDomain(): FormAnalysis = FormAnalysis(
        id = resultId,
        exerciseName = exerciseName,
        score = score,
        status = status,
        observations = observationsJson.toObservations(),
        primaryCorrectionTip = primaryCorrectionTip,
        recordingTip = recordingTip,
        analyzedAt = analyzedAt
    )

    private fun List<FormCheckObservation>.toJson(): String {
        val array = JSONArray()
        forEach { observation ->
            array.put(
                JSONObject().apply {
                    put("jointArea", observation.jointArea)
                    put("isCorrect", observation.isCorrect)
                    put("feedback", observation.feedback)
                }
            )
        }
        return array.toString()
    }

    private fun String.toObservations(): List<FormCheckObservation> =
        runCatching {
            val array = JSONArray(this)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                FormCheckObservation(
                    jointArea = obj.optString("jointArea"),
                    isCorrect = obj.optBoolean("isCorrect", true),
                    feedback = obj.optString("feedback")
                )
            }
        }.getOrDefault(emptyList())
}
