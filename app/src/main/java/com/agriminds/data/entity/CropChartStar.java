package com.agriminds.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "crop_chart_stars")
public class CropChartStar {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private int cropChartId;
    private String expertId;
    private float stars; // 1-5
    private long createdAt;

    public CropChartStar() {
        this.createdAt = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCropChartId() {
        return cropChartId;
    }

    public void setCropChartId(int cropChartId) {
        this.cropChartId = cropChartId;
    }

    public String getExpertId() {
        return expertId;
    }

    public void setExpertId(String expertId) {
        this.expertId = expertId;
    }

    public float getStars() {
        return stars;
    }

    public void setStars(float stars) {
        this.stars = stars;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
