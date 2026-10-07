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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError

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

    /**
     * 下拉刷新：强制刷新当前页面数据（绕过缓存），不将页面切换为 Loading，保留已有内容。
     */
    fun refreshWeather() {
        val location = _currentLocation.value
        if (location == null) {
            loadWeather()
            return
        }
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // 使用仓库的强制刷新，绕过 30 分钟内存缓存
                val weather = weatherRepository.refreshWeather(location)
                Log.d("WeatherViewModel", "Weather refreshed: ${weather.current.temp}°")
                _weatherState.value = WeatherState.Success(weather)
            } catch (e: Exception) {
                // 静默刷新失败时保留旧数据，并通过 Toast 提示用户
                Log.e("WeatherViewModel", "Error refreshing weather", e)
                _refreshError.value = e.message ?: "刷新失败"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearRefreshError() {
        _refreshError.value = null
    }
}
