package info.ggdog.weather.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : ViewModel() {

    // 刷新间隔（分钟）
    private val _refreshInterval = MutableStateFlow(
        context.getSharedPreferences("weather_settings", Context.MODE_PRIVATE)
            .getLong("refresh_interval", 30L)
    )
    val refreshInterval: StateFlow<Long> = _refreshInterval.asStateFlow()

    // 可选的刷新间隔选项（分钟）
    val refreshIntervalOptions = listOf(15L, 30L, 60L, 120L, 360L)

    fun updateRefreshInterval(intervalMinutes: Long) {
        _refreshInterval.value = intervalMinutes
        context.getSharedPreferences("weather_settings", Context.MODE_PRIVATE)
            .edit().putLong("refresh_interval", intervalMinutes).apply()
    }

    /**
     * 格式化刷新间隔显示文本
     */
    fun formatRefreshInterval(minutes: Long): String {
        return when {
            minutes < 60 -> "${minutes}分钟"
            minutes < 1440 -> "${minutes / 60}小时"
            else -> "${minutes / 1440}天"
        }
    }
}
