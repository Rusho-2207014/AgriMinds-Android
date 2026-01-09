package com.agriminds.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crops")
data class Crop(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val season: String, // Boro, Aman, Rabi, Kharif
    val type: String, // Rice, Wheat, Vegetables, Jute
    val plantingDate: Long? = null,
    val expectedHarvestDate: Long? = null,
    val farmerId: Int
)
