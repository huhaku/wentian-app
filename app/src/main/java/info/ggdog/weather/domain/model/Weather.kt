package info.ggdog.weather.domain.model

data class Weather(
    val location: Location,
    val current: CurrentWeather,
    val forecast: Forecast,
    val hourlyForecast: HourlyForecast?,
    val aqi: Aqi?
)
