package com.agriminds.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String fullName;
    public String email;
    public String phone;
    public String password; // In production, hash this!
    public String userType; // "FARMER" or "EXPERT"

    // Farmer specific fields
    public String district;
    public String upazila;
    public String farmSize;
    public String farmingType; // e.g., Crops, Fisheries

    // Expert specific fields
    public String specialization;
    public String experience;
    public String organization;

    // Stats
    public int totalQuestionsAsked;
    public int totalAnswersReceived;
    public int totalChartsShared;
}
