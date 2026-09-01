package com.example.homeworkout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.homeworkout.data.local.entities.UserWeightLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(log: UserWeightLogEntity): Long

    @Query("SELECT * FROM user_weight_logs WHERE userId = :userId ORDER BY loggedAt DESC")
    fun observeWeightLogs(userId: Long): Flow<List<UserWeightLogEntity>>

    @Query("SELECT * FROM user_weight_logs WHERE userId = :userId ORDER BY loggedAt DESC LIMIT 1")
    suspend fun getLatestWeightLog(userId: Long): UserWeightLogEntity?
}
