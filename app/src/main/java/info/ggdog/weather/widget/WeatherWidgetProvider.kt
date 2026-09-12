package info.ggdog.weather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import info.ggdog.weather.R
import java.text.SimpleDateFormat
import java.util.*

class SmallWidgetProvider : BaseWidgetProvider() {
    override fun getLayoutId(): Int = R.layout.widget_small
}

class MediumWidgetProvider : BaseWidgetProvider() {
    override fun getLayoutId(): Int = R.layout.widget_medium
}

class LargeWidgetProvider : BaseWidgetProvider() {
    override fun getLayoutId(): Int = R.layout.widget_large
}

abstract class BaseWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_WEATHER = "info.ggdog.weather.ACTION_UPDATE_WEATHER"
        private const val ACTION_TICK = "info.ggdog.weather.ACTION_WIDGET_TICK"
        private const val TICK_REQUEST_CODE = 1001
        private const val PREFS_NAME = "weather_widget_prefs"
        private const val KEY_CITY = "widget_city"
        private const val KEY_PROVINCE = "widget_province"
        private const val KEY_TEMP = "widget_temp"
        private const val KEY_CONDITION = "widget_condition"
        private const val KEY_ICON = "widget_icon"
        private const val KEY_TEMP_HIGH = "widget_temp_high"
        private const val KEY_TEMP_LOW = "widget_temp_low"
        private const val KEY_HUMIDITY = "widget_humidity"
        private const val KEY_WIND = "widget_wind"

        fun saveWeatherData(
            context: Context,
            city: String,
            province: String,
            temp: String,
            condition: String,
            icon: String,
            tempHigh: String,
            tempLow: String,
            humidity: String,
            wind: String
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_CITY, city)
                .putString(KEY_PROVINCE, province)
                .putString(KEY_TEMP, temp)
                .putString(KEY_CONDITION, condition)
                .putString(KEY_ICON, icon)
                .putString(KEY_TEMP_HIGH, tempHigh)
                .putString(KEY_TEMP_LOW, tempLow)
                .putString(KEY_HUMIDITY, humidity)
                .putString(KEY_WIND, wind)
                .apply()
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            SmallWidgetProvider().updateAllWidgetsForProvider(context, appWidgetManager)
            MediumWidgetProvider().updateAllWidgetsForProvider(context, appWidgetManager)
            LargeWidgetProvider().updateAllWidgetsForProvider(context, appWidgetManager)
        }

        /** 是否还存在任意规格的小组件实例 */
        fun hasAnyWidget(context: Context): Boolean {
            val manager = AppWidgetManager.getInstance(context)
            return listOf(SmallWidgetProvider(), MediumWidgetProvider(), LargeWidgetProvider())
                .any { manager.getAppWidgetIds(ComponentName(context, it::class.java)).isNotEmpty() }
        }

        /**
         * 安排在下一个整分钟刷新小组件（用于更新时钟文本）。
         * 用 RTC 非唤醒闹钟：息屏期间不触发（自动顺延），亮屏/设备唤醒瞬间立即补发，
         * 时间随即刷新；息屏时不产生任何刷新开销。
         */
        fun scheduleNextMinuteTick(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                ?: return
            val pendingIntent = tickPendingIntent(context)
            val nextMinute = (System.currentTimeMillis() / 60000L + 1L) * 60000L
            alarmManager.set(android.app.AlarmManager.RTC, nextMinute, pendingIntent)
        }

        fun cancelMinuteTick(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                ?: return
            alarmManager.cancel(tickPendingIntent(context))
        }

        private fun tickPendingIntent(context: Context): android.app.PendingIntent {
            val intent = Intent(context, SmallWidgetProvider::class.java).setAction(ACTION_TICK)
            return android.app.PendingIntent.getBroadcast(
                context,
                TICK_REQUEST_CODE,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun BaseWidgetProvider.updateAllWidgetsForProvider(context: Context, appWidgetManager: AppWidgetManager) {
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, this::class.java))
            for (id in ids) {
                val views = getWidgetRemoteViews(context, getLayoutId())
                appWidgetManager.updateAppWidget(id, views)
            }
        }

        private fun getWidgetRemoteViews(context: Context, layoutId: Int): RemoteViews {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val city = prefs.getString(KEY_CITY, "杭州") ?: "杭州"
            val province = prefs.getString(KEY_PROVINCE, "") ?: ""
            val temp = prefs.getString(KEY_TEMP, "26") ?: "26"
            val condition = prefs.getString(KEY_CONDITION, "多云") ?: "多云"
            val icon = prefs.getString(KEY_ICON, "100") ?: "100"
            val tempHigh = prefs.getString(KEY_TEMP_HIGH, "28") ?: "28"
            val tempLow = prefs.getString(KEY_TEMP_LOW, "23") ?: "23"
            val humidity = prefs.getString(KEY_HUMIDITY, "65") ?: "65"
            val wind = prefs.getString(KEY_WIND, "") ?: ""

            val timeFormat = SimpleDateFormat("h:mm", Locale.CHINA)
            val ampmFormat = SimpleDateFormat("a", Locale.CHINA)
            val weekdayFormat = SimpleDateFormat("EEEE", Locale.CHINA)
            val shortDateFormat = SimpleDateFormat("MM/dd", Locale.CHINA)
            val dateFormat = SimpleDateFormat("E M月d日", Locale.getDefault())
            val currentTime = timeFormat.format(Date())
            val currentAmpm = ampmFormat.format(Date()).replace("AM", "上午").replace("PM", "下午")
            val currentWeekday = weekdayFormat.format(Date())
            val currentShortDate = shortDateFormat.format(Date())
            val currentDate = dateFormat.format(Date())

            val views = RemoteViews(context.packageName, layoutId)
            val weatherIconBitmap = getWeatherIconBitmap(context, icon)

            when (layoutId) {
                R.layout.widget_small -> {
                    views.setTextViewText(R.id.widget_time, currentTime)
                    views.setTextViewText(R.id.widget_ampm, currentAmpm)
                    views.setTextViewText(R.id.widget_city, city)
                    if (weatherIconBitmap != null) {
                        views.setImageViewBitmap(R.id.widget_icon, weatherIconBitmap)
                    }
                    views.setTextViewText(R.id.widget_temp, "${temp}°")
                    views.setTextViewText(R.id.widget_condition, condition)
                    views.setTextViewText(R.id.widget_weekday, currentWeekday)
                    views.setTextViewText(R.id.widget_date, currentShortDate)
                }
                R.layout.widget_medium -> {
                    views.setTextViewText(R.id.widget_time, currentTime)
                    views.setTextViewText(R.id.widget_ampm, currentAmpm)
                    views.setTextViewText(R.id.widget_city, city)
                    if (weatherIconBitmap != null) {
                        views.setImageViewBitmap(R.id.widget_icon, weatherIconBitmap)
                    }
                    views.setTextViewText(R.id.widget_temp, "${temp}°")
                    views.setTextViewText(R.id.widget_condition, condition)
                    views.setTextViewText(R.id.widget_weekday, currentWeekday)
                    views.setTextViewText(R.id.widget_date, currentShortDate)
                }
                R.layout.widget_large -> {
                    views.setTextViewText(R.id.widget_time, currentTime)
                    views.setTextViewText(R.id.widget_ampm, currentAmpm)
                    views.setTextViewText(R.id.widget_city, city)
                    if (weatherIconBitmap != null) {
                        views.setImageViewBitmap(R.id.widget_icon, weatherIconBitmap)
                    }
                    views.setTextViewText(R.id.widget_temp, "${temp}°")
                    views.setTextViewText(R.id.widget_condition, condition)
                    views.setTextViewText(R.id.widget_weekday, currentWeekday)
                    views.setTextViewText(R.id.widget_date, currentShortDate)
                }
            }

            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (intent != null) {
                val pendingIntent = android.app.PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(android.R.id.background, pendingIntent)
            }

            return views
        }

        /** 缓存已渲染的图标位图（symbol -> bitmap） */
        private val bitmapCache = mutableMapOf<String, Bitmap>()

        /**
         * 加载 MSN 官方 SVG 天气图标（assets/weather_icons/{symbol}.svg）并渲染为 Bitmap。
         * RemoteViews 不支持 SVG，Widget 需要位图；symbol 无效时回退 d000。
         *
         * 注意：必须用 AndroidSVG 同步渲染，禁止在此用 Coil 的 runBlocking——
         * onUpdate 在主线程执行，Coil 管线恢复需要主 Looper，会死锁（曾导致 Widget 空白）。
         */
        private fun getWeatherIconBitmap(context: Context, symbol: String): Bitmap? {
            val safeSymbol = if (symbol.matches(Regex("[dn]\\d{3}"))) symbol else "d000"
            synchronized(bitmapCache) { bitmapCache[safeSymbol]?.let { return it } }
            return try {
                val svg = com.caverock.androidsvg.SVG.getFromAsset(
                    context.assets, "weather_icons/$safeSymbol.svg"
                )
                val docW = svg.documentWidth.takeIf { it > 0f } ?: 100f
                val docH = svg.documentHeight.takeIf { it > 0f } ?: 100f
                val target = 144
                val scale = target / maxOf(docW, docH)
                val bitmap = Bitmap.createBitmap(
                    (docW * scale).toInt().coerceAtLeast(1),
                    (docH * scale).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)
                canvas.scale(scale, scale)
                svg.renderToCanvas(canvas)
                synchronized(bitmapCache) { bitmapCache[safeSymbol] = bitmap }
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }

    abstract fun getLayoutId(): Int

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = getWidgetRemoteViews(context, getLayoutId())
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        // 确保分钟刷新闹钟在运行（幂等，天气更新时也会续链）
        scheduleNextMinuteTick(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_UPDATE_WEATHER -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, this::class.java)
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
            // 每分钟刷新一次时间显示（由 SmallWidgetProvider 统一驱动全部三种规格）
            ACTION_TICK -> {
                // 屏幕熄灭时（设备清醒但未亮屏的边缘场景）跳过重绘，只续链
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                if (powerManager?.isInteractive != false) {
                    updateAllWidgets(context)
                }
                scheduleNextMinuteTick(context)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // 所有规格的小组件都被移除后停掉闹钟，避免空转耗电
        if (!hasAnyWidget(context)) {
            cancelMinuteTick(context)
        }
    }
}