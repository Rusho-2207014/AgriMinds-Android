package com.agriminds.data.repository

import com.agriminds.data.remote.ForecastResponse
import com.agriminds.data.remote.WeatherApiService
import com.agriminds.data.remote.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApiService: WeatherApiService
) {
    private val apiKey = "YOUR_WEATHER_API_KEY" // Get from weatherapi.com
    
    suspend fun getCurrentWeather(location: String): Result<WeatherResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = weatherApiService.getCurrentWeather(apiKey, location)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to fetch weather: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    suspend fun getForecast(location: String, days: Int = 7): Result<ForecastResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = weatherApiService.getForecast(apiKey, location, days)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to fetch forecast: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
