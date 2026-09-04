package com.example.homeworkout.data.repositories

import android.util.Log
import com.example.homeworkout.data.local.dao.ChatDao
import com.example.homeworkout.data.local.dao.UserDao
import com.example.homeworkout.data.local.entities.ChatMessageEntity
import com.example.homeworkout.data.local.entities.ChatSessionEntity
import com.example.homeworkout.data.local.entities.UserEntity
import com.example.homeworkout.data.local.seed.AppDatabaseSeeder
import com.example.homeworkout.data.remote.groq.GroqClient
import com.example.homeworkout.data.remote.groq.GroqPlanProposal
import com.example.homeworkout.domain.models.ExperienceLevel
import com.example.homeworkout.domain.models.FitnessProfile
import com.example.homeworkout.domain.models.PrimaryGoal
import com.example.homeworkout.domain.models.chat.ChatMessage
import com.example.homeworkout.domain.models.chat.ChatSession
import com.example.homeworkout.domain.models.chat.PlanProposal
import com.example.homeworkout.domain.models.enums.ChatMessageRole
import com.example.homeworkout.domain.models.enums.ExerciseCategory
import com.example.homeworkout.domain.repositories.ChatRepository
import com.example.homeworkout.domain.repositories.FitnessProfileRepository
import com.example.homeworkout.domain.repositories.WorkoutRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.math.abs

class ChatRepositoryImpl(
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val groqClient: GroqClient,
    private val fitnessProfileRepository: FitnessProfileRepository,
    private val workoutRepository: WorkoutRepository
) : ChatRepository {

    /** This app has a single local user; resolve (or lazily create) it by its seeded email. */
    private suspend fun currentUserId(): Long {
        userDao.getUserByEmail(AppDatabaseSeeder.DEFAULT_USER_EMAIL)?.let { return it.userId }
        return userDao.insertUser(UserEntity(email = AppDatabaseSeeder.DEFAULT_USER_EMAIL, passwordHash = "local-only"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSessions(): Flow<List<ChatSession>> = flow { emit(currentUserId()) }
        .flatMapLatest { userId ->
            chatDao.observeSessions(userId).map { entities -> entities.map { it.toDomain() } }
        }

    override fun observeMessages(sessionId: Long): Flow<List<ChatMessage>> =
        chatDao.observeMessages(sessionId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun createSession(): Long {
        val now = System.currentTimeMillis()
        return chatDao.insertSession(
            ChatSessionEntity(
                userId = currentUserId(),
                title = DEFAULT_SESSION_TITLE,
                contextSummary = "",
                createdAt = now,
                updatedAt = now
            )
        )
    }

    override suspend fun sendMessage(sessionId: Long, text: String): PlanProposal? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        chatDao.insertMessage(
            ChatMessageEntity(sessionId = sessionId, role = ChatMessageRole.USER, content = trimmed, timestamp = System.currentTimeMillis())
        )

        val session = chatDao.getSession(sessionId)
        if (session != null && session.title == DEFAULT_SESSION_TITLE) {
            chatDao.updateSessionTitle(sessionId, trimmed.take(TITLE_PREVIEW_LENGTH))
        }

        return try {
            val result = groqClient.sendMessage(session?.contextSummary.orEmpty(), trimmed, buildAppContext())
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = ChatMessageRole.MODEL,
                    content = result.reply,
                    timestamp = System.currentTimeMillis()
                )
            )
            chatDao.updateSessionContext(sessionId, result.updatedContext, System.currentTimeMillis())
            result.planProposal?.toDomain()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.e("ChatRepository", "Groq request failed", error)
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = ChatMessageRole.MODEL,
                    content = FALLBACK_ERROR_MESSAGE,
                    timestamp = System.currentTimeMillis()
                )
            )
            null
        }
    }

    override suspend fun deleteSession(sessionId: Long) {
        chatDao.deleteSession(sessionId)
    }

    /** A compact snapshot of the app's data model plus this user's saved fitness profile and
     *  existing plans, given to Groq alongside every message so it can converse about — and when
     *  asked, help set up — real workout plans. See docs/chatbot-feature.md. */
    private suspend fun buildAppContext(): String = buildString {
        append(APP_TAXONOMY)
        append("\n\n")

        val profile = fitnessProfileRepository.getProfile()
        if (profile != null) {
            append("USER'S SAVED FITNESS PROFILE:\n")
            append("- Goal: ${profile.primaryGoal.key}\n")
            append("- Experience level: ${profile.experienceLevel.key}\n")
            append("- Days per week: ${profile.daysPerWeek}\n")
            append("- Session length: ${profile.sessionMinutes} min\n")
            append("- Equipment: ${(profile.availableEquipment + "bodyweight").joinToString(", ")}\n")
            if (profile.focusCategories.isNotEmpty()) {
                append("- Focus categories: ${profile.focusCategories.joinToString(", ") { it.name.lowercase() }}\n")
            }
            if (profile.focusMuscles.isNotEmpty()) {
                append("- Focus muscles: ${profile.focusMuscles.joinToString(", ")}\n")
            }
            if (profile.injuriesOrLimitations.isNotBlank()) {
                append("- Injuries/limitations: ${profile.injuriesOrLimitations}\n")
            }
        } else {
            append("USER'S SAVED FITNESS PROFILE: none saved yet.\n")
        }

        append("\n")
        val plans = try {
            workoutRepository.getWorkouts().first()
        } catch (e: Exception) {
            emptyList()
        }
        if (plans.isNotEmpty()) {
            append("USER'S EXISTING PLANS:\n")
            plans.forEach { plan ->
                append("- ${plan.title} (${plan.category.name.lowercase()}, ${plan.level.name.lowercase()}, ${plan.source.name.lowercase()})\n")
            }
        } else {
            append("USER'S EXISTING PLANS: none yet.\n")
        }
    }

    /** Validates and defaults the model's raw plan fields into a real [FitnessProfile] — never
     *  trust an LLM's structured output to exactly match the schema's enum/range constraints even
     *  under "strict" mode; see GroqClient.normalizeInlineBullets for another instance of this. */
    private fun GroqPlanProposal.toDomain(): PlanProposal {
        val goal = PrimaryGoal.fromKey(primaryGoal) ?: PrimaryGoal.GENERAL_FITNESS
        val level = ExperienceLevel.fromKey(experienceLevel) ?: ExperienceLevel.BEGINNER
        val allowedMinutes = listOf(15, 20, 30, 45, 60)
        val snappedMinutes = allowedMinutes.minByOrNull { abs(it - sessionMinutes) } ?: 30
        return PlanProposal(
            profile = FitnessProfile(
                primaryGoal = goal,
                experienceLevel = level,
                daysPerWeek = daysPerWeek.coerceIn(2, 6),
                sessionMinutes = snappedMinutes,
                availableEquipment = equipment.toSet(),
                focusCategories = focusCategories.mapNotNull { name ->
                    ExerciseCategory.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                }.toSet(),
                focusMuscles = focusMuscles.toSet(),
                injuriesOrLimitations = ""
            ),
            suggestedTitle = title,
            suggestedDescription = description
        )
    }

    companion object {
        private const val DEFAULT_SESSION_TITLE = "New chat"
        private const val TITLE_PREVIEW_LENGTH = 40
        private const val FALLBACK_ERROR_MESSAGE =
            "Sorry, I couldn't reach the assistant just now. Please check your connection and try again."

        private val APP_TAXONOMY = """
            APP DATA MODEL:
            - Goals: general_fitness, build_muscle, fat_loss, mobility, focus_area
            - Experience levels: beginner, intermediate, expert
            - Training days per week: 2-6. Session length: 15, 20, 30, 45 or 60 minutes.
            - Equipment: bodyweight, dumbbell, bands, kettlebells, exercise ball, foam roll, other (bodyweight is always available).
            - Exercise categories: abs_core, arms_shoulders, back_pull, cardio_hiit, chest_push, general_fitness, legs_glutes, stretching.
            - The app has 16 pre-built plan templates spanning every goal x level combination; the best match is auto-selected from the user's profile.
        """.trimIndent()
    }
}

private fun ChatSessionEntity.toDomain(): ChatSession = ChatSession(
    sessionId = sessionId,
    title = title,
    contextSummary = contextSummary,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
    messageId = messageId,
    sessionId = sessionId,
    role = role,
    content = content,
    timestamp = timestamp
)
