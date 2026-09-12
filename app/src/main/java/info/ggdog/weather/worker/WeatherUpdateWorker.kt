package info.ggdog.weather.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import info.ggdog.weather.data.repository.WeatherRepositoryImpl
import info.ggdog.weather.domain.model.Location
import info.ggdog.weather.widget.BaseWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class WeatherUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val weatherRepository: WeatherRepositoryImpl
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "WeatherUpdateWorker"
        const val WORK_NAME = "weather_update_work"
        const val KEY_LOCATION_ID = "location_id"
        const val KEY_LOCATION_NAME = "location_name"
        const val KEY_LOCATION_PROVINCE = "location_province"
        const val KEY_LOCATION_LAT = "location_lat"
        const val KEY_LOCATION_LON = "location_lon"

        fun schedulePeriodicUpdate(
            context: Context,
            location: Location,
            intervalMinutes: Long = 30L
        ) {
            val inputData = workDataOf(
                KEY_LOCATION_ID to location.id,
                KEY_LOCATION_NAME to location.name,
                KEY_LOCATION_PROVINCE to location.province,
                KEY_LOCATION_LAT to location.lat,
                KEY_LOCATION_LON to location.lon
            )

            val periodicWorkRequest = PeriodicWorkRequestBuilder<WeatherUpdateWorker>(
                repeatInterval = intervalMinutes,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
                flexTimeInterval = 5L,
                flexTimeIntervalUnit = TimeUnit.MINUTES
            )
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(false)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(false)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest
            )

            Log.d(TAG, "已安排定时天气更新：${location.name}，间隔 $intervalMinutes 分钟")
        }

        fun executeImmediateUpdate(context: Context, location: Location) {
            val inputData = workDataOf(
                KEY_LOCATION_ID to location.id,
                KEY_LOCATION_NAME to location.name,
                KEY_LOCATION_PROVINCE to location.province,
                KEY_LOCATION_LAT to location.lat,
                KEY_LOCATION_LON to location.lon
            )

            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<WeatherUpdateWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)
            Log.d(TAG, "已执行立即天气更新：${location.name}")
        }

        fun stopPeriodicUpdate(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "已停止定时天气更新")
        }

        fun getWorkInfo(context: Context) =
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(WORK_NAME)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val locationId = inputData.getString(KEY_LOCATION_ID)
                ?: return@withContext Result.failure()
            val locationName = inputData.getString(KEY_LOCATION_NAME) ?: "未知"
            val locationProvince = inputData.getString(KEY_LOCATION_PROVINCE) ?: ""

            Log.d(TAG, "开始更新天气：$locationName (ID: $locationId)")

            val location = Location(
                id = locationId,
                name = locationName,
                country = "中国",
                province = locationProvince,
                lat = inputData.getDouble(KEY_LOCATION_LAT, 0.0),
                lon = inputData.getDouble(KEY_LOCATION_LON, 0.0),
                isDefault = true
            )

            val weather = weatherRepository.refreshWeather(location)

            Log.d(TAG, "天气数据已更新：${locationName}, 温度：${weather.current.temp}°C")

            updateWidgets(weather)

            saveUpdateTime(locationId)

            Log.d(TAG, "天气更新成功：$locationName")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "天气更新失败", e)

            if (runAttemptCount < 3) {
                Log.d(TAG, "将在稍后重试（第 ${runAttemptCount + 1} 次）")
                Result.retry()
            } else {
                Log.w(TAG, "已达到最大重试次数，更新失败")
                Result.failure()
            }
        }
    }

    private fun updateWidgets(weather: info.ggdog.weather.domain.model.Weather) {
        try {
            val tempHigh = weather.forecast.days.firstOrNull()?.highTemp?.toInt()?.toString() ?: "0"
            val tempLow = weather.forecast.days.firstOrNull()?.lowTemp?.toInt()?.toString() ?: "0"

            BaseWidgetProvider.saveWeatherData(
                context = context,
                city = weather.location.name,
                province = weather.location.province,
                temp = weather.current.temp.toInt().toString(),
                condition = weather.current.condition,
                icon = weather.current.icon,
                tempHigh = tempHigh,
                tempLow = tempLow,
                humidity = weather.current.humidity.toString(),
                wind = "${weather.current.windDirection} ${weather.current.windScale}级"
            )

            BaseWidgetProvider.updateAllWidgets(context)

            Log.d(TAG, "已更新桌面小部件")
        } catch (e: Exception) {
            Log.e(TAG, "更新小部件失败", e)
        }
    }

    private fun saveUpdateTime(locationId: String) {
        try {
            val prefs = context.getSharedPreferences("weather_updates", Context.MODE_PRIVATE)
            prefs.edit()
                .putLong("last_update_${locationId}", System.currentTimeMillis())
                .putString("last_location", locationId)
                .apply()
            Log.d(TAG, "已保存更新时间")
        } catch (e: Exception) {
            Log.e(TAG, "保存更新时间失败", e)
        }
    }

    /**
     * 根据风速（km/h）转换为蒲福风级
     */
    private fun getWindLevel(windSpeed: Double): Int {
        return when {
            windSpeed < 1 -> 0
            windSpeed < 6 -> 1
            windSpeed < 12 -> 2
            windSpeed < 20 -> 3
            windSpeed < 29 -> 4
            windSpeed < 39 -> 5
            windSpeed < 50 -> 6
            windSpeed < 62 -> 7
            windSpeed < 75 -> 8
            windSpeed < 89 -> 9
            windSpeed < 103 -> 10
            windSpeed < 118 -> 11
            else -> 12
        }
    }
}