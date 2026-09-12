package info.ggdog.weather.presentation.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import info.ggdog.weather.domain.model.*
import info.ggdog.weather.presentation.viewmodel.LocationViewModel
import info.ggdog.weather.presentation.viewmodel.SettingsViewModel
import info.ggdog.weather.presentation.viewmodel.WeatherState
import info.ggdog.weather.presentation.viewmodel.WeatherViewModel
import info.ggdog.weather.widget.BaseWidgetProvider

@Composable
fun WeatherScreen(viewModel: WeatherViewModel) {
    val weatherState = viewModel.weatherState.collectAsStateWithLifecycle()
    var showLocationDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Log.d("WeatherScreen", "Current state: ${weatherState.value}")

    LaunchedEffect((weatherState.value as? WeatherState.Success)?.weather?.location?.id) {
        val state = weatherState.value
        if (state is WeatherState.Success) {
            try {
                val weather = state.weather
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

                // 使用 BackgroundTaskHelper 重启后台更新（WorkManager + AlarmManager 双重保障）
                val settingsPrefs = context.getSharedPreferences("weather_settings", android.content.Context.MODE_PRIVATE)
                val refreshInterval = settingsPrefs.getLong("refresh_interval", 30L)
                info.ggdog.weather.util.BackgroundTaskHelper.scheduleBackgroundUpdate(context, weather.location, refreshInterval)
                Log.d("WeatherScreen", "已更新桌面小部件并重启定时更新")
            } catch (e: Exception) {
                Log.e("WeatherScreen", "更新桌面小部件失败", e)
            }
        }
    }
    
    when (val state = weatherState.value) {
        is WeatherState.Loading -> {
            Log.d("WeatherScreen", "Rendering Loading state")
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is WeatherState.Success -> {
            Log.d("WeatherScreen", "Rendering Success state: ${state.weather.current.temp}°")
            
            if (showLocationDialog) {
                val locationViewModel = hiltViewModel<LocationViewModel>()
                LocationSelectionScreen(
                    viewModel = locationViewModel,
                    onLocationSelected = { location ->
                        viewModel.loadWeather(location)
                    },
                    onDismiss = { showLocationDialog = false }
                )
            }
            
            if (showSettingsDialog) {
                val settingsViewModel = hiltViewModel<info.ggdog.weather.presentation.viewmodel.SettingsViewModel>()
                SettingsDialog(
                    viewModel = settingsViewModel,
                    onDismiss = { showSettingsDialog = false }
                )
            }
            
            WeatherContent(
                weather = state.weather,
                onLocationClick = { showLocationDialog = true },
                onSettingsClick = { showSettingsDialog = true }
            )
        }
        is WeatherState.Error -> {
            Log.d("WeatherScreen", "Rendering Error state: ${state.message}")
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = state.message,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("错误信息", state.message)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "错误信息已复制", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制错误信息")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.refreshWeather()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "重试")
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherContent(weather: Weather, onLocationClick: () -> Unit, onSettingsClick: () -> Unit) {
    val locationName = listOf(weather.location.province, weather.location.name)
        .filter { it.isNotBlank() }
        .joinToString("")
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            WeatherHeaderSection(weather = weather, locationName = locationName, onLocationClick = onLocationClick, onSettingsClick = onSettingsClick)
        }
        
        item {
            EnvironmentCard(weather = weather)
        }
        
        item {
            HourlyForecastSection(hourlyForecast = weather.hourlyForecast)
        }
        
        item {
            DailyForecastSection(forecast = weather.forecast)
        }
    }
}

@Composable
fun WeatherHeaderSection(weather: Weather, locationName: String, onLocationClick: () -> Unit, onSettingsClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4A90D9),
                        Color(0xFF87CEEB)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左侧：位置信息（可点击切换城市）
                Row(
                    modifier = Modifier.clickable(onClick = onLocationClick),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "位置",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Text(
                        text = locationName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                // 右侧：设置按钮
                androidx.compose.material3.IconButton(onClick = onSettingsClick) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置",
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${weather.current.temp.toInt()}°",
                    color = Color.White,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Light
                )
                
                WeatherIcon(
                    symbol = weather.current.icon,
                    description = weather.current.condition,
                    size = 80.dp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherMetricItem(
                    label = "${weather.current.windDirection} ${weather.current.windScale}级",
                    icon = Icons.Default.Air
                )
                WeatherMetricItem(
                    label = "湿度 ${weather.current.humidity}%",
                    icon = Icons.Default.WaterDrop
                )
                WeatherMetricItem(
                    label = "气压 ${weather.current.pressure}Pa",
                    icon = Icons.Default.Speed
                )
            }
        }
    }
}

@Composable
fun WeatherMetricItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun EnvironmentCard(weather: Weather) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text(
            text = "当前天气",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    EnvironmentItem(
                        label = "天气情况",
                        value = weather.current.condition
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    )
                    EnvironmentItem(
                        label = "体感温度",
                        value = "${weather.current.feelsLike.toInt()}°"
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    )
                    EnvironmentItem(
                        label = "空气质量",
                        value = weather.aqi?.value?.toString() ?: "--"
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    EnvironmentItem(
                        label = "降水量",
                        value = "${weather.forecast.days.firstOrNull()?.precipitation ?: weather.current.precipitation}mm"
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    )
                    EnvironmentItem(
                        label = "云量",
                        value = "${weather.current.cloud}%"
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    )
                    EnvironmentItem(
                        label = "能见度",
                        value = "${weather.current.visibility}km"
                    )
                }
            }
        }
    }
}

@Composable
fun EnvironmentItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun HourlyForecastSection(hourlyForecast: HourlyForecast?) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "24小时预报",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                hourlyForecast?.hours?.let { hours ->
                    items(hours) { hour ->
                        HourlyForecastItem(hour = hour)
                    }
                }
            }
        }
    }
}

@Composable
fun HourlyForecastItem(hour: HourlyForecastItem) {
    Column(
        modifier = Modifier.width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = hour.time,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        WeatherIcon(
            symbol = hour.icon,
            description = hour.condition,
            size = 32.dp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "${hour.temp.toInt()}°",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "${hour.windDirection} ${hour.windScale}级",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun DailyForecastSection(forecast: Forecast) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text(
            text = "7天预报",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                forecast.days.forEachIndexed { index, day ->
                    DailyForecastItem(day = day, isFirst = index == 0)
                    if (index < forecast.days.size - 1) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyForecastItem(day: ForecastDay, isFirst: Boolean) {
    val dateParts = day.date.split("-")
    val monthDay = if (dateParts.size >= 3) {
        "${dateParts[1].toIntOrNull() ?: 0}-${dateParts[2].toIntOrNull() ?: 0}"
    } else {
        day.date
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 日期（左侧）
        Text(
            text = if (isFirst) "今天" else getWeekDay(day.date),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.width(50.dp)
        )
        
        Text(
            text = monthDay,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.width(45.dp)
        )
        
        // 天气图标（居中）
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            WeatherIcon(
                symbol = day.icon,
                description = day.condition,
                size = 32.dp
            )
        }
        
        // 天气状况
        Text(
            text = day.condition,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.width(60.dp)
        )
        
        // 温度（右侧）
        Text(
            text = "${day.highTemp.toInt()}° / ${day.lowTemp.toInt()}°",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(70.dp),
            textAlign = TextAlign.End
        )
    }
}

fun getWeekDay(date: String): String {
    return try {
        val localDate = java.time.LocalDate.parse(date)
        when (localDate.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> "周一"
            java.time.DayOfWeek.TUESDAY -> "周二"
            java.time.DayOfWeek.WEDNESDAY -> "周三"
            java.time.DayOfWeek.THURSDAY -> "周四"
            java.time.DayOfWeek.FRIDAY -> "周五"
            java.time.DayOfWeek.SATURDAY -> "周六"
            java.time.DayOfWeek.SUNDAY -> "周日"
            null -> "--"
        }
    } catch (_: Exception) {
        "--"
    }
}

fun getWindLevel(windSpeedKmh: Double): Int {
    return when {
        windSpeedKmh < 1 -> 0
        windSpeedKmh < 6 -> 1
        windSpeedKmh < 12 -> 2
        windSpeedKmh < 20 -> 3
        windSpeedKmh < 29 -> 4
        windSpeedKmh < 39 -> 5
        windSpeedKmh < 50 -> 6
        windSpeedKmh < 62 -> 7
        windSpeedKmh < 75 -> 8
        windSpeedKmh < 89 -> 9
        windSpeedKmh < 103 -> 10
        windSpeedKmh < 118 -> 11
        else -> 12
    }
}

/**
 * MSN 官方天气图标（assets/weather_icons/{symbol}.svg）。
 * symbol 如 "d210"，由 Mapper 规范化，未知代码已在 Mapper 回退为 d000/n000。
 */
@Composable
fun WeatherIcon(symbol: String, description: String, size: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    val svgLoader = remember {
        coil.ImageLoader.Builder(context)
            .components { add(coil.decode.SvgDecoder.Factory()) }
            .build()
    }
    val request = remember(symbol) {
        coil.request.ImageRequest.Builder(context)
            .data("file:///android_asset/weather_icons/$symbol.svg")
            .build()
    }
    coil.compose.AsyncImage(
        model = request,
        imageLoader = svgLoader,
        contentDescription = description,
        modifier = Modifier.size(size),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit
    )
}
