package info.ggdog.weather

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import info.ggdog.weather.domain.model.Location
import info.ggdog.weather.util.BatteryOptimizationHelper
import info.ggdog.weather.worker.WeatherUpdateWorker
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WeatherApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // 初始化后台更新服务（延迟到 MainActivity 中执行，避免 Hilt 注入时序问题）
        // initializeBackgroundUpdates()
    }

    /**
     * 初始化后台更新服务
     */
    private fun initializeBackgroundUpdates() {
        Timber.d("初始化后台更新服务")
        
        // 获取默认位置（实际应该从 SharedPreferences 或数据库读取）
        val defaultLocation = Location(
            id = "101210101",
            name = "杭州",
            country = "中国",
            province = "浙江",
            lat = 30.2741,
            lon = 120.1551,
            isDefault = true
        )
        
        // 启动定时天气更新（30分钟间隔）
        WeatherUpdateWorker.schedulePeriodicUpdate(this, defaultLocation, 30L)
        Timber.d("已启动定时天气更新，间隔30分钟")
        
        // 检查电池优化状态
        if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
            Timber.w("应用未加入电池优化白名单，可能影响后台更新准确性")
        }
    }
}
