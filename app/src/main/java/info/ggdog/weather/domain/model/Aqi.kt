package info.ggdog.weather.domain.model

data class Aqi(
    val value: Int,
    val level: String,
    val primaryPollutant: String,
    val pm25: Int,
    val pm10: Int,
    val so2: Int,
    val no2: Int,
    val co: Double,
    val o3: Int
)
