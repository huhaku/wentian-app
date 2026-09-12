package info.ggdog.weather.domain.model

data class HourlyForecast(
    val hours: List<HourlyForecastItem>
)

data class HourlyForecastItem(
    val time: String,
    val condition: String,
    val icon: String,
    val temp: Double,
    val windDirection: String,
    val windScale: String
)
