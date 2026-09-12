package info.ggdog.weather.domain.model

data class ForecastDay(
    val date: String,
    val condition: String,
    val icon: String,
    val highTemp: Double,
    val lowTemp: Double,
    val humidity: Int,
    val windSpeed: Double,
    val precipitation: Double
)
