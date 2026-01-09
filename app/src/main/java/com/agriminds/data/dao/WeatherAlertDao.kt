package com.agriminds.data.dao

import androidx.room.*
import com.agriminds.data.entity.WeatherAlert
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherAlertDao {
    @Query("SELECT * FROM weather_alerts WHERE isActive = 1 ORDER BY startDate DESC")
    fun getActiveAlerts(): Flow<List<WeatherAlert>>
    
    @Query("SELECT * FROM weather_alerts WHERE location = :location AND isActive = 1")
    fun getAlertsByLocation(location: String): Flow<List<WeatherAlert>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: WeatherAlert): Long
    
    @Update
    suspend fun updateAlert(alert: WeatherAlert)
    
    @Delete
    suspend fun deleteAlert(alert: WeatherAlert)
    
    @Query("UPDATE weather_alerts SET isActive = 0 WHERE endDate < :currentTime")
    suspend fun deactivateExpiredAlerts(currentTime: Long)
}
