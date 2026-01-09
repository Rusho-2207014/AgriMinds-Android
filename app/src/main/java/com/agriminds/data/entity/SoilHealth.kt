package com.agriminds.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "soil_health")
data class SoilHealth(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val farmerId: Int,
    val fieldName: String,
    val phLevel: Double,
    val nitrogen: Double, // Percentage
    val phosphorus: Double, // Percentage
    val potassium: Double, // Percentage
    val organicMatter: Double? = null,
    val testDate: Long = System.currentTimeMillis(),
    val recommendations: String? = null
)
