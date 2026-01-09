package com.agriminds.data.dao

import androidx.room.*
import com.agriminds.data.entity.Question
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY createdAt DESC")
    fun getAllQuestions(): Flow<List<Question>>
    
    @Query("SELECT * FROM questions WHERE farmerId = :farmerId ORDER BY createdAt DESC")
    fun getQuestionsByFarmer(farmerId: Int): Flow<List<Question>>
    
    @Query("SELECT * FROM questions WHERE status = :status ORDER BY createdAt DESC")
    fun getQuestionsByStatus(status: String): Flow<List<Question>>
    
    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: Int): Question?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: Question): Long
    
    @Update
    suspend fun updateQuestion(question: Question)
    
    @Delete
    suspend fun deleteQuestion(question: Question)
}
