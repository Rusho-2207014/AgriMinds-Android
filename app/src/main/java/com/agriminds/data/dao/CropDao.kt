package com.agriminds.data.dao

import androidx.room.*
import com.agriminds.data.entity.Crop
import kotlinx.coroutines.flow.Flow

@Dao
interface CropDao {
    @Query("SELECT * FROM crops WHERE farmerId = :farmerId")
    fun getCropsByFarmer(farmerId: Int): Flow<List<Crop>>
    
    @Query("SELECT * FROM crops WHERE id = :cropId")
    suspend fun getCropById(cropId: Int): Crop?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrop(crop: Crop): Long
    
    @Update
    suspend fun updateCrop(crop: Crop)
    
    @Delete
    suspend fun deleteCrop(crop: Crop)
    
    @Query("SELECT * FROM crops WHERE farmerId = :farmerId AND season = :season")
    fun getCropsBySeason(farmerId: Int, season: String): Flow<List<Crop>>
}
