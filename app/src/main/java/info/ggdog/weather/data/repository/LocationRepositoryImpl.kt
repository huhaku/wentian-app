package info.ggdog.weather.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import info.ggdog.weather.data.api.MsnLocationApi
import info.ggdog.weather.data.mapper.MsnWeatherMapper
import info.ggdog.weather.domain.model.Location
import info.ggdog.weather.domain.repository.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val api: MsnLocationApi,
    @ApplicationContext private val context: Context
) : LocationRepository {

    companion object {
        private const val TAG = "LocationRepository"
        private const val PREFS_NAME = "location_prefs"
        private const val KEY_LOC_ID = "loc_id"
        private const val KEY_LOC_NAME = "loc_name"
        private const val KEY_LOC_PROVINCE = "loc_province"
        private const val KEY_LOC_LAT = "loc_lat"
        private const val KEY_LOC_LON = "loc_lon"
        /** 无已保存位置时的默认偏置中心（广州） */
        private const val DEFAULT_LAT = 23.1291
        private const val DEFAULT_LON = 113.2644
    }

    override suspend fun getLocations(): List<Location> = withContext(Dispatchers.IO) {
        fallbackLocations()
    }

    override suspend fun getDefaultLocation(): Location? = withContext(Dispatchers.IO) {
        // 优先从 SharedPreferences 读取用户选择的位置
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getString(KEY_LOC_ID, null)
        if (savedId != null) {
            Location(
                id = savedId,
                name = prefs.getString(KEY_LOC_NAME, "") ?: "",
                country = "中国",
                province = prefs.getString(KEY_LOC_PROVINCE, "") ?: "",
                lat = prefs.getFloat(KEY_LOC_LAT, 0f).toDouble(),
                lon = prefs.getFloat(KEY_LOC_LON, 0f).toDouble(),
                isDefault = true
            )
        } else {
            fallbackLocations().firstOrNull { it.isDefault }
        }
    }

    override suspend fun saveLocation(location: Location) {
        Log.d(TAG, "saveLocation: ${location.name}")
        // 持久化保存到 SharedPreferences
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_LOC_ID, location.id)
            .putString(KEY_LOC_NAME, location.name)
            .putString(KEY_LOC_PROVINCE, location.province)
            .putFloat(KEY_LOC_LAT, location.lat.toFloat())
            .putFloat(KEY_LOC_LON, location.lon.toFloat())
            .apply()
    }

    override suspend fun deleteLocation(locationId: String) {
        Log.d(TAG, "deleteLocation: $locationId")
    }

    override suspend fun setDefaultLocation(locationId: String) {
        Log.d(TAG, "setDefaultLocation: $locationId")
    }

    override suspend fun searchCity(keyword: String): List<Location> = withContext(Dispatchers.IO) {
        if (keyword.isBlank()) return@withContext emptyList()

        try {
            // 用当前位置做搜索偏置（"lat,lon,radius"），使附近的行政区（区/县级）优先返回
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lat = if (prefs.contains(KEY_LOC_LAT)) prefs.getFloat(KEY_LOC_LAT, 0f).toDouble() else DEFAULT_LAT
            val lon = if (prefs.contains(KEY_LOC_LON)) prefs.getFloat(KEY_LOC_LON, 0f).toDouble() else DEFAULT_LON
            val bias = "$lat,$lon,100"

            val response = api.searchCity(keyword.trim(), localCircularView = bias)
            MsnWeatherMapper.mapSuggest(response)
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP错误: ${e.code()}, ${e.message()}", e)
            if (e.code() == 400 || e.code() == 404) {
                return@withContext emptyList()
            }
            throw Exception("网络请求失败: HTTP ${e.code()}")
        } catch (e: IOException) {
            Log.e(TAG, "网络错误: ${e.message}", e)
            throw Exception("网络连接失败: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "搜索异常: ${e.message}", e)
            throw e
        }
    }

    private fun fallbackLocations(): List<Location> = listOf(
        Location("23.1291,113.2644", "广州", "中国", "广东", 23.1291, 113.2644, true),
        Location("39.9042,116.4074", "北京", "中国", "北京", 39.9042, 116.4074, false),
        Location("31.2304,121.4737", "上海", "中国", "上海", 31.2304, 121.4737, false),
        Location("30.2741,120.1551", "杭州", "中国", "浙江", 30.2741, 120.1551, false),
        Location("22.5431,114.0579", "深圳", "中国", "广东", 22.5431, 114.0579, false)
    )
}
