package info.ggdog.weather.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import info.ggdog.weather.R

/**
 * 电池优化辅助工具类
 * 帮助应用申请电池优化白名单，提高后台存活率
 */
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptimization"

    /**
     * 检查应用是否在电池优化白名单中
     * @param context 上下文
     * @return true 表示已加入白名单，false 表示未加入
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
        // Android 6.0 以下默认返回 true（没有电池优化机制）
        return true
    }

    /**
     * 请求忽略电池优化
     * 会弹出系统对话框请求用户授权
     * @param context 上下文
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations(context)) {
                try {
                    Log.d(TAG, "请求加入电池优化白名单")
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "无法打开电池优化请求页面", e)
                    // 如果打开失败，跳转到电池优化设置页面
                    openBatteryOptimizationSettings(context)
                }
            } else {
                Log.d(TAG, "应用已在电池优化白名单中")
            }
        }
    }

    /**
     * 打开电池优化设置页面
     * @param context 上下文
     */
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            Log.d(TAG, "打开电池优化设置页面")
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "无法打开电池优化设置页面", e)
            // 如果失败，打开应用详情设置页面
            openAppSettings(context)
        }
    }

    /**
     * 打开应用设置页面
     * @param context 上下文
     */
    fun openAppSettings(context: Context) {
        try {
            Log.d(TAG, "打开应用设置页面")
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "无法打开应用设置页面", e)
        }
    }

    /**
     * 显示电池优化提示对话框
     * 引导用户将应用加入电池优化白名单
     * @param context 上下文
     */
    fun showBatteryOptimizationDialog(context: Context) {
        if (!isIgnoringBatteryOptimizations(context)) {
            AlertDialog.Builder(context)
                .setTitle(R.string.battery_optimization_title)
                .setMessage(R.string.battery_optimization_message)
                .setPositiveButton(R.string.go_to_settings) { dialog, _ ->
                    requestIgnoreBatteryOptimizations(context)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    /**
     * 检查并显示电池优化状态提示
     * 如果未加入白名单，显示提示对话框
     * @param context 上下文
     * @return true 表示需要提示，false 表示已在白名单中
     */
    fun checkAndShowOptimizationHint(context: Context): Boolean {
        val isIgnoring = isIgnoringBatteryOptimizations(context)
        if (!isIgnoring) {
            Log.w(TAG, "应用未加入电池优化白名单，可能影响后台更新")
            showBatteryOptimizationDialog(context)
        }
        return !isIgnoring
    }

    /**
     * 获取电池优化状态描述
     * @param context 上下文
     * @return 状态描述文本
     */
    fun getOptimizationStatusText(context: Context): String {
        return if (isIgnoringBatteryOptimizations(context)) {
            context.getString(R.string.battery_optimization_enabled)
        } else {
            context.getString(R.string.battery_optimization_disabled)
        }
    }
}