package com.example.homeworkout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.homeworkout.data.local.entities.UserEntity
import com.example.homeworkout.data.local.entities.UserFitnessProfileEntity
import com.example.homeworkout.data.local.entities.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: Long): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun observeUser(userId: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT userId FROM users ORDER BY userId LIMIT 1")
    suspend fun getFirstUserId(): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM user_settings WHERE userId = :userId")
    fun observeUserSettings(userId: Long): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE userId = :userId")
    suspend fun getUserSettings(userId: Long): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserSettings(settings: UserSettingsEntity)

    @Query("SELECT * FROM user_fitness_profiles WHERE userId = :userId")
    fun observeFitnessProfile(userId: Long): Flow<UserFitnessProfileEntity?>

    @Query("SELECT * FROM user_fitness_profiles WHERE userId = :userId")
    suspend fun getFitnessProfile(userId: Long): UserFitnessProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFitnessProfile(profile: UserFitnessProfileEntity)
}
