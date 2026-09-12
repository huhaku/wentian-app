package info.ggdog.weather.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import info.ggdog.weather.data.database.entity.LocationEntity

@Dao
interface LocationDao {

    @Query("SELECT * FROM locations ORDER BY isDefault DESC, name ASC")
    suspend fun getAllLocations(): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultLocation(): LocationEntity?

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun getLocationById(id: String): LocationEntity?

    @Insert
    suspend fun insertLocation(location: LocationEntity)

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Delete
    suspend fun deleteLocation(location: LocationEntity)

    @Query("UPDATE locations SET isDefault = 0 WHERE isDefault = 1")
    suspend fun clearDefaultLocation()

    @Query("UPDATE locations SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultLocation(id: String)
}
