package com.example.homeworkout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.homeworkout.data.local.entities.FormCheckResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FormCheckResultDao {
    @Insert
    suspend fun insertResult(result: FormCheckResultEntity): Long

    @Query("SELECT * FROM form_check_results WHERE userId = :userId ORDER BY analyzedAt DESC")
    fun observeResults(userId: Long): Flow<List<FormCheckResultEntity>>
}
