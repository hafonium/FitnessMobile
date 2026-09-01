package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room table row for `equipment_types` — one row per distinct `equipment` value found in
 * data/data.json (bodyweight, dumbbell, barbell, machine, cable, kettlebells, bands, etc.).
 */
@Entity(tableName = "equipment_types", indices = [Index(value = ["name"], unique = true)])
data class EquipmentTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val equipmentId: Long = 0,
    val name: String
)
