package com.agriminds.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    
    @GET("v1/current.json")
    suspend fun getCurrentWeather(
        @Query("key") apiKey: String,
        @Query("q") location: String,
        @Query("aqi") aqi: String = "no"
    ): Response<WeatherResponse>
    
    @GET("v1/forecast.json")
    suspend fun getForecast(
        @Query("key") apiKey: String,
        @Query("q") location: String,
        @Query("days") days: Int = 7,
        @Query("aqi") aqi: String = "no"
    ): Response<ForecastResponse>
}

data class WeatherResponse(
    val location: Location,
    val current: Current
)

data class ForecastResponse(
    val location: Location,
    val current: Current,
    val forecast: Forecast
)

data class Location(
    val name: String,
    val region: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val localtime: String
)

data class Current(
    val temp_c: Double,
    val temp_f: Double,
    val is_day: Int,
    val condition: Condition,
    val wind_kph: Double,
    val precip_mm: Double,
    val humidity: Int,
    val cloud: Int,
    val feelslike_c: Double,
    val uv: Double
)

data class Condition(
    val text: String,
    val icon: String,
    val code: Int
)

data class Forecast(
    val forecastday: List<ForecastDay>
)

data class ForecastDay(
    val date: String,
    val day: Day,
    val hour: List<Hour>
)

data class Day(
    val maxtemp_c: Double,
    val mintemp_c: Double,
    val avgtemp_c: Double,
    val maxwind_kph: Double,
    val totalprecip_mm: Double,
    val avghumidity: Int,
    val daily_chance_of_rain: Int,
    val condition: Condition
)

data class Hour(
    val time: String,
    val temp_c: Double,
    val condition: Condition,
    val chance_of_rain: Int
)
