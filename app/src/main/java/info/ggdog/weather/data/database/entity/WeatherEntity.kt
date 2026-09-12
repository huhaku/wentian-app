package info.ggdog.weather.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather")
data class WeatherEntity(
    @PrimaryKey val locationId: String,
    val currentTemp: Double,
    val currentFeelsLike: Double,
    val currentCondition: String,
    val currentIcon: String,
    val currentHumidity: Int,
    val currentWindSpeed: Double,
    val currentWindDirection: String,
    val currentPressure: Int,
    val currentVisibility: Int,
    val currentUvIndex: Int,
    val currentSunrise: String,
    val currentSunset: String,
    val currentUpdateTime: String,
    val forecastData: String,
    val aqiValue: Int?,
    val aqiLevel: String?,
    val aqiPrimaryPollutant: String?,
    val aqiPm25: Int?,
    val aqiPm10: Int?,
    val aqiSo2: Int?,
    val aqiNo2: Int?,
    val aqiCo: Double?,
    val aqiO3: Int?,
    val cacheTime: Long
)
