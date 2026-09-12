package info.ggdog.weather.presentation.ui.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import info.ggdog.weather.domain.model.Location
import info.ggdog.weather.presentation.ui.screen.WeatherScreen
import info.ggdog.weather.presentation.ui.theme.WeatherAppTheme
import info.ggdog.weather.util.BackgroundTaskHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化后台更新服务
        initializeBackgroundUpdates()

        setContent {
            WeatherAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: info.ggdog.weather.presentation.viewmodel.WeatherViewModel = hiltViewModel()
                    WeatherScreen(viewModel = viewModel)
                }
            }
        }
    }

    /**
     * 初始化后台更新服务
     * 从 SharedPreferences 读取用户保存的位置和刷新间隔
     */
    private fun initializeBackgroundUpdates() {
        try {
            Log.d("MainActivity", "初始化后台更新服务")

            // 读取刷新间隔
            val settingsPrefs = getSharedPreferences("weather_settings", MODE_PRIVATE)
            val refreshInterval = settingsPrefs.getLong("refresh_interval", 30L)

            // 从 SharedPreferences 读取保存的位置
            val locPrefs = getSharedPreferences("location_prefs", MODE_PRIVATE)
            val savedId = locPrefs.getString("loc_id", null)

            val location = if (savedId != null) {
                Location(
                    id = savedId,
                    name = locPrefs.getString("loc_name", "") ?: "",
                    country = "中国",
                    province = locPrefs.getString("loc_province", "") ?: "",
                    lat = locPrefs.getFloat("loc_lat", 0f).toDouble(),
                    lon = locPrefs.getFloat("loc_lon", 0f).toDouble(),
                    isDefault = true
                )
            } else {
                // 首次使用，使用默认城市
                Location(
                    id = "101280101",
                    name = "广州",
                    country = "中国",
                    province = "广东",
                    lat = 23.1291,
                    lon = 113.2644,
                    isDefault = true
                )
            }

            // 使用 BackgroundTaskHelper 启动双重保障（WorkManager + AlarmManager）
            BackgroundTaskHelper.scheduleBackgroundUpdate(this, location, refreshInterval)
            Log.d("MainActivity", "已启动后台更新：${location.name}，间隔 $refreshInterval 分钟")
        } catch (e: Exception) {
            Log.e("MainActivity", "初始化后台更新服务失败", e)
        }
    }
}
