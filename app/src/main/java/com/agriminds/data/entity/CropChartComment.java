package com.agriminds.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "crop_chart_comments")
public class CropChartComment {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private int cropChartId;
    private String expertId;
    private String expertName;
    private String comment;
    private long timestamp;

    public CropChartComment() {
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
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

    public String getExpertName() {
        return expertName;
    }

    public void setExpertName(String expertName) {
        this.expertName = expertName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
