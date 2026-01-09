package com.agriminds.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val farmerId: Int,
    val category: String, // Crop Diseases, Pest Control, Soil Management, Irrigation
    val questionText: String,
    val imageUrl: String? = null,
    val status: String = "Pending", // Pending, Answered
    val createdAt: Long = System.currentTimeMillis()
)
