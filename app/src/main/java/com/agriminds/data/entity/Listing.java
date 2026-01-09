package com.agriminds.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "listings")
public class Listing {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int farmerId;
    public String cropName;
    public double quantity; // in kg
    public double pricePerKg;
    public String location; // Optional specific location
    public String status; // "ACTIVE", "SOLD"
    public String datePosted;
}
