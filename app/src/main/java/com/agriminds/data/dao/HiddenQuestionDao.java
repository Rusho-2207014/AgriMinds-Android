package com.agriminds.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.agriminds.data.entity.HiddenQuestion;

import java.util.List;

@Dao
public interface HiddenQuestionDao {

    @Query("SELECT * FROM hidden_questions WHERE expertId = :expertId")
    List<HiddenQuestion> getHiddenQuestionsByExpert(int expertId);

    @Query("SELECT questionId FROM hidden_questions WHERE expertId = :expertId")
    List<Integer> getHiddenQuestionIdsByExpert(int expertId);

    @Insert
    long insertHiddenQuestion(HiddenQuestion hiddenQuestion);

    @Query("DELETE FROM hidden_questions WHERE expertId = :expertId AND questionId = :questionId")
    void unhideQuestion(int expertId, int questionId);

    @Query("DELETE FROM hidden_questions WHERE expertId = :expertId")
    void unhideAllQuestions(int expertId);
}
