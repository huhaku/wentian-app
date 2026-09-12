package info.ggdog.weather.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import info.ggdog.weather.domain.model.Location
import info.ggdog.weather.util.BackgroundTaskHelper
import info.ggdog.weather.worker.WeatherUpdateWorker

/**
 * 天气更新广播接收器
 * 作为 AlarmManager 的兜底方案，每次触发后重新安排下一次闹钟
 */
class WeatherUpdateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WeatherUpdateReceiver"
        const val ACTION_UPDATE_WEATHER = "info.ggdog.weather.ACTION_UPDATE_WEATHER_ALARM"
        const val EXTRA_LOCATION_ID = "extra_location_id"
        const val EXTRA_LOCATION_NAME = "extra_location_name"
        const val EXTRA_LOCATION_PROVINCE = "extra_location_province"
        const val EXTRA_LOCATION_LAT = "extra_location_lat"
        const val EXTRA_LOCATION_LON = "extra_location_lon"
        const val EXTRA_INTERVAL = "extra_interval"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "收到广播：${intent.action}")

        when (intent.action) {
            ACTION_UPDATE_WEATHER -> {
                val locationId = intent.getStringExtra(EXTRA_LOCATION_ID) ?: return
                val locationName = intent.getStringExtra(EXTRA_LOCATION_NAME) ?: "未知"
                val locationProvince = intent.getStringExtra(EXTRA_LOCATION_PROVINCE) ?: ""
                val intervalMinutes = intent.getLongExtra(EXTRA_INTERVAL, 30L)

                val location = Location(
                    id = locationId,
                    name = locationName,
                    country = "中国",
                    province = locationProvince,
                    lat = intent.getDoubleExtra(EXTRA_LOCATION_LAT, 0.0),
                    lon = intent.getDoubleExtra(EXTRA_LOCATION_LON, 0.0),
                    isDefault = true
                )

                // 触发立即更新
                try {
                    WeatherUpdateWorker.executeImmediateUpdate(context, location)
                    Log.d(TAG, "已触发天气立即更新：$locationName")
                } catch (e: Exception) {
                    Log.e(TAG, "触发天气更新失败", e)
                }

                // 重新安排下一次闹钟（setExactAndAllowWhileIdle 只执行一次）
                BackgroundTaskHelper.rescheduleAlarm(context, location, intervalMinutes)
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "系统启动完成，重新安排后台更新")
                // 从 SharedPreferences 读取保存的位置
                val prefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
                val savedId = prefs.getString("loc_id", null)
                if (savedId != null) {
                    val location = Location(
                        id = savedId,
                        name = prefs.getString("loc_name", "") ?: "",
                        country = "中国",
                        province = prefs.getString("loc_province", "") ?: "",
                        lat = prefs.getFloat("loc_lat", 0f).toDouble(),
                        lon = prefs.getFloat("loc_lon", 0f).toDouble(),
                        isDefault = true
                    )
                    // 读取刷新间隔
                    val settingsPrefs = context.getSharedPreferences("weather_settings", Context.MODE_PRIVATE)
                    val interval = settingsPrefs.getLong("refresh_interval", 30L)
                    BackgroundTaskHelper.scheduleBackgroundUpdate(context, location, interval)
                    Log.d(TAG, "已重新安排后台更新：${location.name}，间隔 $interval 分钟")
                } else {
                    Log.w(TAG, "未找到保存的位置信息，跳过后台更新安排")
                }
            }
        }
    }
}
