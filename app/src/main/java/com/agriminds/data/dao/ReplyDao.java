package com.agriminds.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.agriminds.data.entity.Reply;

import java.util.List;

@Dao
public interface ReplyDao {

    @Query("SELECT * FROM replies WHERE questionId = :questionId ORDER BY createdAt ASC")
    List<Reply> getRepliesByQuestion(int questionId);

    @Query("SELECT * FROM replies WHERE answerId = :answerId ORDER BY createdAt ASC")
    List<Reply> getRepliesByAnswer(int answerId);

    @Query("SELECT * FROM replies WHERE userId = :userId ORDER BY createdAt DESC")
    List<Reply> getRepliesByUser(int userId);

    @Query("SELECT * FROM replies WHERE id = :id")
    Reply getReplyById(int id);

    @Query("SELECT COUNT(*) FROM replies WHERE questionId = :questionId")
    int getReplyCountByQuestion(int questionId);

    @Query("SELECT COUNT(*) FROM replies WHERE answerId = :answerId")
    int getReplyCountByAnswer(int answerId);

    @Insert
    long insertReply(Reply reply);

    @Update
    void updateReply(Reply reply);

    @Delete
    void deleteReply(Reply reply);
}
