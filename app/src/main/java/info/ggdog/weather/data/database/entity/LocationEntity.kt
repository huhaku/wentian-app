package info.ggdog.weather.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val country: String,
    val province: String,
    val lat: Double,
    val lon: Double,
    val isDefault: Boolean = false
)
