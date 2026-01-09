package com.agriminds.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crop_diseases")
data class CropDisease(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val cropType: String,
    val symptoms: String, // Comma-separated list
    val treatment: String,
    val prevention: String,
    val severity: String, // Low, Medium, High
    val imageUrl: String? = null
)
