package info.ggdog.weather.domain.repository

import info.ggdog.weather.domain.model.Weather
import info.ggdog.weather.domain.model.Location

interface WeatherRepository {
    suspend fun getWeather(location: Location): Weather
    suspend fun refreshWeather(location: Location): Weather
}
