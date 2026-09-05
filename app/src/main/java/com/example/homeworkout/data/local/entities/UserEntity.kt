package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.homeworkout.domain.models.enums.UserGender

/**
 * Room table row for `users`. Mirrors docs/db_diagram.dbml one field at a time — mapping to
 * [com.example.homeworkout.domain.models.UserProfile] happens in data/repositories/UserRepositoryImpl.
 *
 * `updatedAt` must be maintained by the application whenever a row is updated.
 */
@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val userId: Long = 0,
    val email: String,
    val passwordHash: String,
    val fullName: String? = null,
    val gender: UserGender? = null,
    val heightCm: Double? = null,
    val ageYears: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
