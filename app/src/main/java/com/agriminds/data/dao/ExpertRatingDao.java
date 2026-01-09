package com.agriminds.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.agriminds.data.entity.ExpertRating;

import java.util.List;

@Dao
public interface ExpertRatingDao {

    @Query("SELECT AVG(rating) FROM expert_ratings WHERE expertId = :expertId")
    Float getAverageRating(int expertId);

    @Query("SELECT AVG(rating) FROM expert_ratings WHERE answerId = :answerId")
    Float getAverageRatingForAnswer(int answerId);

    @Query("SELECT COUNT(*) FROM expert_ratings WHERE expertId = :expertId")
    int getRatingCount(int expertId);

    @Query("SELECT * FROM expert_ratings WHERE expertId = :expertId ORDER BY createdAt DESC")
    List<ExpertRating> getRatingsByExpert(int expertId);

    @Query("SELECT * FROM expert_ratings WHERE answerId = :answerId ORDER BY createdAt DESC")
    List<ExpertRating> getRatingsByAnswer(int answerId);

    @Query("SELECT * FROM expert_ratings WHERE answerId = :answerId AND farmerId = :farmerId LIMIT 1")
    ExpertRating getRatingByFarmerAndAnswer(int farmerId, int answerId);

    @Insert
    long insertRating(ExpertRating rating);

    @Update
    void updateRating(ExpertRating rating);

    @Delete
    void deleteRating(ExpertRating rating);
}
