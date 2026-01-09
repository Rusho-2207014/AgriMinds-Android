package com.agriminds.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fullName: String,
    val email: String,
    val phone: String,
    val password: String, // In production, hash this!
    val userType: String, // FARMER or EXPERT
    
    // Farmer specific fields
    val district: String? = null,
    val upazila: String? = null,
    val farmSize: String? = null,
    val farmingType: String? = null,
    
    // Expert specific fields
    val specialization: String? = null,
    val experience: String? = null,
    val organization: String? = null
)
