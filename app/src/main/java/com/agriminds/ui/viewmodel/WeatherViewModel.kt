package com.agriminds.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agriminds.data.remote.ForecastDay
import com.agriminds.data.remote.WeatherResponse
import com.agriminds.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _currentWeather = MutableStateFlow<WeatherResponse?>(null)
    val currentWeather: StateFlow<WeatherResponse?> = _currentWeather.asStateFlow()

    private val _forecast = MutableStateFlow<List<ForecastDay>>(emptyList())
    val forecast: StateFlow<List<ForecastDay>> = _forecast.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadWeather(location: String = "Dhaka, Bangladesh") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Load current weather
            weatherRepository.getCurrentWeather(location)
                .onSuccess { weather ->
                    _currentWeather.value = weather
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }

            // Load forecast
            weatherRepository.getForecast(location, 7)
                .onSuccess { forecastResponse ->
                    _forecast.value = forecastResponse.forecast.forecastday
                }
                .onFailure { exception ->
                    if (_error.value == null) {
                        _error.value = exception.message
                    }
                }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
