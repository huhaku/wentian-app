package info.ggdog.weather.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import info.ggdog.weather.domain.model.Location
import info.ggdog.weather.receiver.WeatherUpdateReceiver
import info.ggdog.weather.worker.WeatherUpdateWorker

/**
 * 后台任务辅助工具类
 * 提供跨版本的后台任务调度支持
 *
 * 策略：WorkManager 为主 + AlarmManager 为辅（兜底）
 * AlarmManager 使用 setExactAndAllowWhileIdle，每次触发后在 Receiver 中重新安排
 */
object BackgroundTaskHelper {

    private const val TAG = "BackgroundTaskHelper"
    private const val REQUEST_CODE_WEATHER_UPDATE = 1001

    /**
     * 安排后台更新任务（WorkManager + AlarmManager 双重保障）
     * @param context 上下文
     * @param location 位置信息
     * @param intervalMinutes 更新间隔（分钟）
     */
    fun scheduleBackgroundUpdate(
        context: Context,
        location: Location,
        intervalMinutes: Long = 30L
    ) {
        // 1. WorkManager（主方案）
        WeatherUpdateWorker.schedulePeriodicUpdate(context, location, intervalMinutes)

        // 2. AlarmManager（兜底方案）
        scheduleWithAlarmManager(context, location, intervalMinutes)

        Log.d(TAG, "已安排后台更新任务：${location.name}，间隔 $intervalMinutes 分钟")
    }

    /**
     * 使用 AlarmManager 安排后台任务（兜底方案）
     * setExactAndAllowWhileIdle 只执行一次，触发后在 Receiver 中调用 rescheduleAlarm 重新安排
     */
    private fun scheduleWithAlarmManager(
        context: Context,
        location: Location,
        intervalMinutes: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context, location, intervalMinutes)
        val triggerAtMillis = System.currentTimeMillis() + intervalMinutes * 60 * 1000

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 12+ (API 31) 需要检查是否有精确闹钟权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                        Log.d(TAG, "使用 setExactAndAllowWhileIdle 安排闹钟")
                    } else {
                        // 没有精确闹钟权限，使用不精确的闹钟
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                        Log.w(TAG, "无精确闹钟权限，使用 setAndAllowWhileIdle")
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "使用 setExactAndAllowWhileIdle 安排闹钟")
                }
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    intervalMinutes * 60 * 1000,
                    pendingIntent
                )
                Log.d(TAG, "使用 setRepeating 安排闹钟")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "安排闹钟失败（权限不足）", e)
        } catch (e: Exception) {
            Log.e(TAG, "安排闹钟失败", e)
        }
    }

    /**
     * 重新安排闹钟（在 Receiver 触发后调用）
     */
    fun rescheduleAlarm(
        context: Context,
        location: Location,
        intervalMinutes: Long
    ) {
        scheduleWithAlarmManager(context, location, intervalMinutes)
        Log.d(TAG, "已重新安排闹钟：${location.name}，$intervalMinutes 分钟后")
    }

    /**
     * 创建 AlarmManager 的 PendingIntent
     */
    private fun createPendingIntent(
        context: Context,
        location: Location,
        intervalMinutes: Long
    ): PendingIntent {
        val intent = Intent(context, WeatherUpdateReceiver::class.java).apply {
            action = WeatherUpdateReceiver.ACTION_UPDATE_WEATHER
            putExtra(WeatherUpdateReceiver.EXTRA_LOCATION_ID, location.id)
            putExtra(WeatherUpdateReceiver.EXTRA_LOCATION_NAME, location.name)
            putExtra(WeatherUpdateReceiver.EXTRA_LOCATION_PROVINCE, location.province)
            putExtra(WeatherUpdateReceiver.EXTRA_LOCATION_LAT, location.lat)
            putExtra(WeatherUpdateReceiver.EXTRA_LOCATION_LON, location.lon)
            putExtra(WeatherUpdateReceiver.EXTRA_INTERVAL, intervalMinutes)
        }

        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WEATHER_UPDATE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 取消所有后台更新任务
     */
    fun cancelBackgroundUpdate(context: Context) {
        // 取消 WorkManager 任务
        WeatherUpdateWorker.stopPeriodicUpdate(context)

        // 取消 AlarmManager 闹钟
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WeatherUpdateReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WEATHER_UPDATE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "已取消 AlarmManager 闹钟")
        } catch (e: Exception) {
            Log.e(TAG, "取消闹钟失败", e)
        }

        Log.d(TAG, "已取消所有后台更新任务")
    }

    /**
     * 检查后台任务是否正在运行
     */
    fun isBackgroundUpdateRunning(context: Context): Boolean {
        return try {
            val workManager = androidx.work.WorkManager.getInstance(context)
            val workInfos = workManager.getWorkInfosForUniqueWork(WeatherUpdateWorker.WORK_NAME).get()
            workInfos.any { workInfo ->
                workInfo.state == androidx.work.WorkInfo.State.RUNNING ||
                workInfo.state == androidx.work.WorkInfo.State.ENQUEUED
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查后台任务状态失败", e)
            false
        }
    }

    /**
     * 获取后台更新状态描述
     */
    fun getBackgroundUpdateStatus(context: Context): String {
        val isRunning = isBackgroundUpdateRunning(context)
        val isIgnoringBattery = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)

        return buildString {
            append("后台更新: ${if (isRunning) "运行中" else "已停止"}\n")
            append("电池优化: ${if (isIgnoringBattery) "已忽略" else "受限"}")
        }
    }
}
