package com.example.homeworkout.domain.repositories

import kotlinx.coroutines.flow.Flow

interface WorkoutSessionRepository {
    /** End timestamps (epoch millis) of completed workout sessions in [fromMillis, toMillis) for the single local user. */
    fun observeCompletedSessionTimestamps(fromMillis: Long, toMillis: Long): Flow<List<Long>>
}
