package info.ggdog.weather.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.ggdog.weather.domain.model.Weather
import info.ggdog.weather.domain.model.Location
import info.ggdog.weather.domain.repository.WeatherRepository
import info.ggdog.weather.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WeatherState {
    data object Loading : WeatherState
    data class Success(val weather: Weather) : WeatherState
    data class Error(val message: String) : WeatherState
}

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val weatherState: StateFlow<WeatherState> = _weatherState

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    init {
        loadWeather()
    }

    fun loadWeather(location: Location? = null) {
        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading
            try {
                Log.d("WeatherViewModel", "Loading weather data...")
                val targetLocation = location ?: locationRepository.getDefaultLocation()
                if (targetLocation == null) {
                    Log.e("WeatherViewModel", "No default location found")
                    _weatherState.value = WeatherState.Error("暂无位置，请添加城市")
                    return@launch
                }
                Log.d("WeatherViewModel", "Location: ${targetLocation.name}")
                _currentLocation.value = targetLocation
                // 持久化保存当前选择的位置，供后台任务使用
                locationRepository.saveLocation(targetLocation)
                val weather = weatherRepository.getWeather(targetLocation)
                Log.d("WeatherViewModel", "Weather loaded: ${weather.current.temp}°")
                _weatherState.value = WeatherState.Success(weather)
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error loading weather", e)
                _weatherState.value = WeatherState.Error(e.message ?: "获取天气失败")
            }
        }
    }

    fun refreshWeather() {
        _currentLocation.value?.let {
            loadWeather(it)
        }
    }
}
