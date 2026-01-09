package com.agriminds.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.agriminds.data.entity.Question;

import java.util.List;

@Dao
public interface QuestionDao {

    @Query("SELECT * FROM questions ORDER BY createdAt DESC")
    List<Question> getAllQuestions();

    @Query("SELECT * FROM questions WHERE farmerId = :farmerId ORDER BY createdAt DESC")
    List<Question> getQuestionsByFarmer(int farmerId);

    @Query("SELECT * FROM questions WHERE status = :status ORDER BY createdAt DESC")
    List<Question> getQuestionsByStatus(String status);

    @Query("SELECT * FROM questions WHERE id = :id")
    Question getQuestionById(int id);

    @Insert
    long insertQuestion(Question question);

    @Update
    void updateQuestion(Question question);

    @Delete
    void deleteQuestion(Question question);

    @Query("UPDATE questions SET answerCount = answerCount + 1, status = 'Answered' WHERE id = :questionId")
    void incrementAnswerCount(int questionId);
}
