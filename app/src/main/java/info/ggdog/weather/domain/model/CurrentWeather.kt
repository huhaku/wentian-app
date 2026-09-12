package info.ggdog.weather.domain.model

data class CurrentWeather(
    val temp: Double,
    val feelsLike: Double,
    val condition: String,
    val icon: String,
    val humidity: Int,
    val windSpeed: Double,
    val windScale: String,
    val windDirection: String,
    val pressure: Int,
    val visibility: Int,
    val precipitation: Double,
    val cloud: Int,
    val uvIndex: Int,
    val sunrise: String,
    val sunset: String,
    val updateTime: String
)
