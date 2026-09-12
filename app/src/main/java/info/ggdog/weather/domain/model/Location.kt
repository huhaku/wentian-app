package info.ggdog.weather.domain.model

data class Location(
    val id: String,
    val name: String,
    val country: String,
    val province: String,
    val lat: Double,
    val lon: Double,
    val isDefault: Boolean = false
)
