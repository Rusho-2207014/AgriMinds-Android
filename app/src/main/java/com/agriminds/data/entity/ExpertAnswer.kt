package com.agriminds.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expert_answers")
data class ExpertAnswer(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val questionId: Int,
    val expertId: Int,
    val answerText: String,
    val createdAt: Long = System.currentTimeMillis()
)
