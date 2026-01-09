package com.agriminds.data.dao

import androidx.room.*
import com.agriminds.data.entity.ExpertAnswer
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpertAnswerDao {
    @Query("SELECT * FROM expert_answers WHERE questionId = :questionId")
    fun getAnswersByQuestion(questionId: Int): Flow<List<ExpertAnswer>>
    
    @Query("SELECT * FROM expert_answers WHERE expertId = :expertId ORDER BY createdAt DESC")
    fun getAnswersByExpert(expertId: Int): Flow<List<ExpertAnswer>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswer(answer: ExpertAnswer): Long
    
    @Update
    suspend fun updateAnswer(answer: ExpertAnswer)
    
    @Delete
    suspend fun deleteAnswer(answer: ExpertAnswer)
}
