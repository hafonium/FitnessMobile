package com.example.homeworkout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.homeworkout.data.local.entities.FoodLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLog(log: FoodLogEntity): Long

    @Query("SELECT * FROM food_logs WHERE userId = :userId ORDER BY loggedAt DESC")
    fun observeFoodLogs(userId: Long): Flow<List<FoodLogEntity>>
}
