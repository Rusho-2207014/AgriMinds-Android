package com.agriminds.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_alerts")
data class WeatherAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String, // Storm, Flood, Heatwave, Drought
    val severity: String, // Low, Medium, High, Severe
    val message: String,
    val location: String,
    val startDate: Long,
    val endDate: Long? = null,
    val isActive: Boolean = true
)
