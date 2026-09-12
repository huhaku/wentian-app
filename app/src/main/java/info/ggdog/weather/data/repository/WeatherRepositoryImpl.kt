package info.ggdog.weather.data.repository

import android.util.Log
import info.ggdog.weather.data.api.MsnWeatherApi
import info.ggdog.weather.data.mapper.MsnWeatherMapper
import info.ggdog.weather.domain.model.Location
import info.ggdog.weather.domain.model.Weather
import info.ggdog.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MSN 天气仓库实现：
 *
 * - 数据源为 MSN 天气（weatherfalcon）接口，数据提供方为中国天气网；
 * - 一次请求返回当前天气 + 10 天预报 + 逐小时 + AQI + 日出日落；
 * - 内置内存级缓存（30 分钟），预报 / 小时 / AQI 失败时不阻塞当前天气展示；
 * - 当前天气失败且无缓存时抛出 [WeatherException.NetworkError]。
 */
@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val api: MsnWeatherApi
) : WeatherRepository {

    private val ioDispatcher = Dispatchers.IO

    private val weatherCache = mutableMapOf<String, CacheEntry<Weather>>()
    private val cacheMutex = Mutex()

    private data class CacheEntry<T>(val data: T, val expiresAt: Long)

    override suspend fun getWeather(location: Location): Weather = withContext(ioDispatcher) {
        val locationKey = locationKey(location)

        // 1) 命中未过期缓存
        cacheMutex.withLock {
            val cached = weatherCache[locationKey]
            if (cached != null && cached.expiresAt > now()) cached.data else null
        }?.let { return@withContext it }

        // 2) 发起网络请求
        val data = try {
            fetchWeather(location, locationKey)
        } catch (e: Exception) {
            // 3) 失败时降级返回过期缓存
            val stale = cacheMutex.withLock { weatherCache[locationKey]?.data }
            if (stale != null) {
                Log.w(TAG, "天气请求失败，使用过期缓存: ${e.message}")
                stale
            } else {
                throw wrapException(e)
            }
        }

        cacheMutex.withLock {
            weatherCache[locationKey] = CacheEntry(data, now() + WEATHER_TTL_MS)
        }
        data
    }

    override suspend fun refreshWeather(location: Location): Weather = withContext(ioDispatcher) {
        // 强制刷新：清除该位置缓存后重新请求
        val locationKey = locationKey(location)
        cacheMutex.withLock { weatherCache.remove(locationKey) }
        getWeather(location)
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /** MSN 接口以经纬度定位，缓存 key 也使用经纬度 */
    private fun locationKey(location: Location): String = "${location.lat},${location.lon}"

    private suspend fun fetchWeather(location: Location, locationKey: String): Weather {
        val response = api.getWeatherOverview(locationKey)
        val parts = MsnWeatherMapper.mapToWeatherParts(response)
            ?: throw WeatherException.ParseError("MSN 天气数据解析失败")
        return Weather(
            location = location,
            current = parts.current,
            forecast = parts.forecast,
            hourlyForecast = parts.hourly,
            aqi = parts.aqi
        )
    }

    private fun wrapException(e: Exception): WeatherException {
        return when (e) {
            is IOException -> WeatherException.NetworkError("网络连接失败，请检查网络: ${e.message}")
            is HttpException -> {
                val code = e.code().toString()
                val msg = when (e.code()) {
                    401 -> "MSN 天气接口鉴权失败"
                    403 -> "无访问权限"
                    404 -> "未找到该城市"
                    429 -> "请求过于频繁，请稍后再试"
                    in 500..599 -> "MSN 天气服务异常，请稍后再试"
                    else -> "请求失败 (${e.code()})"
                }
                WeatherException.ApiError(code, msg)
            }
            else -> WeatherException.NetworkError(e.message ?: "未知错误")
        }
    }

    private fun now() = System.currentTimeMillis()

    companion object {
        private const val TAG = "WeatherRepository"
        private const val WEATHER_TTL_MS = 30L * 60 * 1000
    }
}

sealed class WeatherException(message: String) : Exception(message) {
    class NetworkError(message: String) : WeatherException(message)
    class ApiError(val code: String, message: String) : WeatherException("MSN 天气 API 错误 $code: $message")
    class ParseError(message: String) : WeatherException(message)
    class LocationNotFound(message: String) : WeatherException(message)
    class CacheError(message: String) : WeatherException(message)
    class ConfigError(message: String) : WeatherException(message)
}
