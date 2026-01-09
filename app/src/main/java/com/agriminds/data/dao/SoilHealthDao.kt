package com.agriminds.data.dao

import androidx.room.*
import com.agriminds.data.entity.SoilHealth
import kotlinx.coroutines.flow.Flow

@Dao
interface SoilHealthDao {
    @Query("SELECT * FROM soil_health WHERE farmerId = :farmerId ORDER BY testDate DESC")
    fun getSoilHealthByFarmer(farmerId: Int): Flow<List<SoilHealth>>
    
    @Query("SELECT * FROM soil_health WHERE id = :id")
    suspend fun getSoilHealthById(id: Int): SoilHealth?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSoilHealth(soilHealth: SoilHealth): Long
    
    @Update
    suspend fun updateSoilHealth(soilHealth: SoilHealth)
    
    @Delete
    suspend fun deleteSoilHealth(soilHealth: SoilHealth)
    
    @Query("SELECT * FROM soil_health WHERE farmerId = :farmerId ORDER BY testDate DESC LIMIT 1")
    suspend fun getLatestSoilHealth(farmerId: Int): SoilHealth?
}
