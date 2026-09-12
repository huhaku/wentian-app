package info.ggdog.weather.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import info.ggdog.weather.data.database.entity.WeatherEntity

@Dao
interface WeatherDao {

    @Query("SELECT * FROM weather WHERE locationId = :locationId")
    suspend fun getWeatherByLocationId(locationId: String): WeatherEntity?

    @Insert
    suspend fun insertWeather(weather: WeatherEntity)

    @Update
    suspend fun updateWeather(weather: WeatherEntity)

    @Delete
    suspend fun deleteWeather(weather: WeatherEntity)

    @Query("DELETE FROM weather WHERE cacheTime < :timestamp")
    suspend fun deleteOldWeather(timestamp: Long)
}
