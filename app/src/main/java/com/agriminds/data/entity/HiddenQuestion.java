package com.agriminds.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "hidden_questions")
public class HiddenQuestion {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int expertId;
    private int questionId;
    private long hiddenAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getExpertId() {
        return expertId;
    }

    public void setExpertId(int expertId) {
        this.expertId = expertId;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public long getHiddenAt() {
        return hiddenAt;
    }

    public void setHiddenAt(long hiddenAt) {
        this.hiddenAt = hiddenAt;
    }
}
