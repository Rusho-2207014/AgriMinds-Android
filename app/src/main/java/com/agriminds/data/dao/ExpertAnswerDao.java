package com.agriminds.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.agriminds.data.entity.ExpertAnswer;

import java.util.List;

@Dao
public interface ExpertAnswerDao {

    @Query("SELECT * FROM expert_answers WHERE questionId = :questionId ORDER BY createdAt DESC")
    List<ExpertAnswer> getAnswersByQuestion(int questionId);

    @Query("SELECT * FROM expert_answers WHERE expertId = :expertId ORDER BY createdAt DESC")
    List<ExpertAnswer> getAnswersByExpert(int expertId);

    @Query("SELECT * FROM expert_answers WHERE id = :id")
    ExpertAnswer getAnswerById(int id);

    @Insert
    long insertAnswer(ExpertAnswer answer);

    @Update
    void updateAnswer(ExpertAnswer answer);

    @Delete
    void deleteAnswer(ExpertAnswer answer);

    @Query("UPDATE expert_answers SET audioPath = :audioPath WHERE id = :answerId")
    void updateAnswerAudioPath(int answerId, String audioPath);
}
