package com.agriminds.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "crop_diseases")
public class CropDisease {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String diseaseName;
    public String symptoms;
    public String treatment;
    public String prevention;
    public String imageUrl; // asset path or url
}
