package info.ggdog.weather.domain.repository

import info.ggdog.weather.domain.model.Location

interface LocationRepository {
    suspend fun getLocations(): List<Location>
    suspend fun getDefaultLocation(): Location?
    suspend fun saveLocation(location: Location)
    suspend fun deleteLocation(locationId: String)
    suspend fun setDefaultLocation(locationId: String)
    suspend fun searchCity(keyword: String): List<Location>
}
