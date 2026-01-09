package com.agriminds.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.agriminds.data.entity.AnswerView;

@Dao
public interface AnswerViewDao {

    @Query("SELECT COUNT(*) FROM answer_views WHERE farmerId = :farmerId AND viewed = 0")
    int getUnviewedAnswerCount(int farmerId);

    @Query("SELECT COUNT(*) FROM answer_views av INNER JOIN expert_answers ea ON av.answerId = ea.id WHERE ea.questionId = :questionId AND av.farmerId = :farmerId AND av.viewed = 0")
    int getUnviewedAnswerCountForQuestion(int questionId, int farmerId);

    @Query("SELECT * FROM answer_views WHERE answerId = :answerId AND farmerId = :farmerId LIMIT 1")
    AnswerView getAnswerView(int answerId, int farmerId);

    @Query("SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM answer_views WHERE answerId = :answerId AND farmerId = :farmerId AND viewed = 1")
    boolean isAnswerViewed(int answerId, int farmerId);

    @Insert
    long insertAnswerView(AnswerView answerView);

    @Update
    void updateAnswerView(AnswerView answerView);

    @Query("UPDATE answer_views SET viewed = 1, viewedAt = :viewedAt WHERE answerId = :answerId AND farmerId = :farmerId")
    void markAsViewed(int answerId, int farmerId, long viewedAt);
}
