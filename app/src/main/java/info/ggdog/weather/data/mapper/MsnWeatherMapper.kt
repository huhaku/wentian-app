package info.ggdog.weather.data.mapper

import info.ggdog.weather.data.model.response.MsnAddress
import info.ggdog.weather.data.model.response.MsnCurrent
import info.ggdog.weather.data.model.response.MsnDaily
import info.ggdog.weather.data.model.response.MsnForecastDay
import info.ggdog.weather.data.model.response.MsnHourly
import info.ggdog.weather.data.model.response.MsnOverviewResponse
import info.ggdog.weather.data.model.response.MsnSuggestResponse
import info.ggdog.weather.domain.model.Aqi
import info.ggdog.weather.domain.model.CurrentWeather
import info.ggdog.weather.domain.model.Forecast
import info.ggdog.weather.domain.model.ForecastDay
import info.ggdog.weather.domain.model.HourlyForecast
import info.ggdog.weather.domain.model.HourlyForecastItem
import info.ggdog.weather.domain.model.Location
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * MSN 天气数据转换：将 weatherfalcon overview 响应映射为领域模型。
 *
 * MSN 的 `symbol` 字段格式为 `d210` / `n100`（d=白天 n=夜间），
 * 后缀与和风天气图标代码一致，去掉前缀后可直接复用现有图标映射。
 */
object MsnWeatherMapper {

    private val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.CHINA)
    private val outputTimeFormat = SimpleDateFormat("HH:mm", Locale.CHINA)

    /**
     * 将 overview 响应完整映射为领域模型三元组。
     * 响应无效（缺 current）时返回 null。
     */
    fun mapToWeatherParts(response: MsnOverviewResponse): MappedWeather? {
        val current = response.responses.firstOrNull()
            ?.weather?.firstOrNull()?.current
            ?: return null
        val forecastDay = response.responses.firstOrNull()
            ?.weather?.firstOrNull()?.forecast?.days

        val firstDay = forecastDay?.firstOrNull()
        val mappedCurrent = mapCurrent(current)
        val withSun = firstDay?.almanac?.let {
            mappedCurrent.copy(
                sunrise = formatTime(it.sunrise),
                sunset = formatTime(it.sunset)
            )
        } ?: mappedCurrent

        return MappedWeather(
            current = withSun,
            forecast = Forecast(days = forecastDay?.map(::mapForecastDay) ?: emptyList()),
            hourly = HourlyForecast(
                hours = forecastDay
                    ?.flatMap { it.hourly }
                    ?.map(::mapHourly)
                    ?: emptyList()
            ),
            aqi = current.aqi?.let { mapAqi(it.toInt()) }
        )
    }

    data class MappedWeather(
        val current: CurrentWeather,
        val forecast: Forecast,
        val hourly: HourlyForecast,
        val aqi: Aqi?
    )

    fun mapCurrent(current: MsnCurrent): CurrentWeather {
        return CurrentWeather(
            temp = current.temp ?: 0.0,
            feelsLike = current.feels ?: 0.0,
            condition = current.cap ?: current.pvdrCap ?: "",
            icon = stripSymbol(current.symbol),
            humidity = current.rh?.toInt() ?: 0,
            windSpeed = current.windSpd ?: 0.0,
            windScale = stripWindScale(current.pvdrWindSpd),
            windDirection = current.pvdrWindDir ?: "",
            pressure = current.baro?.toInt() ?: 0,
            visibility = current.vis?.toInt() ?: 0,
            precipitation = current.precip ?: 0.0,
            cloud = current.cloudCover?.toInt() ?: 0,
            uvIndex = current.uv?.toInt() ?: 0,
            sunrise = "",
            sunset = "",
            updateTime = current.created ?: ""
        )
    }

    fun mapForecastDay(day: MsnForecastDay): ForecastDay {
        val daily = day.daily
        return ForecastDay(
            date = daily?.valid?.take(10) ?: "",
            condition = daily?.day?.cap ?: daily?.pvdrCap ?: "",
            icon = stripSymbol(daily?.day?.symbol ?: daily?.symbol),
            highTemp = daily?.tempHi ?: 0.0,
            lowTemp = daily?.tempLo ?: 0.0,
            humidity = daily?.rh?.toInt() ?: 0,
            windSpeed = daily?.windMax ?: 0.0,
            precipitation = daily?.precip ?: 0.0
        )
    }

    fun mapHourly(hour: MsnHourly): HourlyForecastItem {
        return HourlyForecastItem(
            time = formatTime(hour.valid),
            condition = hour.cap ?: hour.pvdrCap ?: "",
            icon = stripSymbol(hour.symbol),
            temp = hour.temp ?: 0.0,
            windDirection = hour.pvdrWindDir ?: "",
            windScale = stripWindScale(hour.pvdrWindSpd)
        )
    }

    /**
     * MSN 只提供 AQI 数值，等级按国内标准计算，
     * 其他污染物明细不可用（填 0）。
     */
    fun mapAqi(value: Int): Aqi {
        return Aqi(
            value = value,
            level = aqiLevel(value),
            primaryPollutant = "",
            pm25 = 0,
            pm10 = 0,
            so2 = 0,
            no2 = 0,
            co = 0.0,
            o3 = 0
        )
    }

    fun mapSuggest(response: MsnSuggestResponse): List<Location> {
        val all = response.value.mapNotNull { item ->
            val lat = item.geo?.latitude ?: return@mapNotNull null
            val lon = item.geo.longitude ?: return@mapNotNull null
            val address = item.address
            val name = item.name
                ?: address?.addressLocality
                ?: address?.text
                ?: return@mapNotNull null
            val hints = item.entityPresentationInfo?.entitySubTypeHints.orEmpty()
            // 行政区划条目（市/区/县）hints 为 PopulatedPlace 或 Other；POI/邮编类噪音过滤掉
            val isAdministrative = "PopulatedPlace" in hints || "Other" in hints
            if (!isAdministrative || name.matches(Regex("\\d+"))) return@mapNotNull null
            Location(
                id = "$lat,$lon",
                name = name,
                country = countryDisplay(address?.countryIso),
                province = fullRegionChain(address, name),
                lat = lat,
                lon = lon,
                isDefault = false
            )
        }
        // 过滤后为空时回退到全量结果，避免误杀
        return if (all.isNotEmpty()) all else response.value.mapNotNull { item ->
            val lat = item.geo?.latitude ?: return@mapNotNull null
            val lon = item.geo.longitude ?: return@mapNotNull null
            val name = item.name ?: item.address?.addressLocality ?: item.address?.text
            ?: return@mapNotNull null
            Location(
                id = "$lat,$lon",
                name = name,
                country = countryDisplay(item.address?.countryIso),
                province = fullRegionChain(item.address, name),
                lat = lat,
                lon = lon,
                isDefault = false
            )
        }
    }

    /**
     * ISO 国家代码 → 显示名（接口只返回 "cn" 等小写代码）。
     */
    private fun countryDisplay(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return if (iso.equals("cn", ignoreCase = true)) "中国" else iso
    }

    /**
     * 拼接完整区域链「省市区」（如 广东省广州市花都区）。
     * 若末级与地点名相同（如搜「花都区」），去掉末级避免头部重复显示。
     */
    private fun fullRegionChain(address: MsnAddress?, name: String): String {
        address ?: return ""
        val parts = listOfNotNull(
            address.addressRegion?.takeIf { it.isNotBlank() },
            address.addressSubregion?.takeIf { it.isNotBlank() },
            address.addressLocality?.takeIf { it.isNotBlank() }
        ).distinct()
        val chain = if (parts.lastOrNull() == name) parts.dropLast(1) else parts
        return chain.joinToString("")
    }

    /**
     * MSN symbol 规范化（如 `d210` / `n100`）。
     *
     * 返回值直接对应 assets/weather_icons/ 里的 MSN 官方 SVG 图标文件名；
     * 未收录的代码回退到昼/夜默认图标（d000 / n000）。
     */
    fun stripSymbol(symbol: String?): String {
        if (symbol.isNullOrBlank()) return "d000"
        val lower = symbol.lowercase(Locale.ROOT)
        val normalized = when {
            lower.startsWith("d") || lower.startsWith("n") -> lower
            else -> "d$lower"
        }
        return if (KNOWN_SYMBOLS.contains(normalized)) normalized
        else if (normalized.startsWith("n")) "n000" else "d000"
    }

    /** 已下载图标覆盖的 symbol 集合，与 assets/weather_icons 目录下的 svg 文件一一对应 */
    val KNOWN_SYMBOLS: Set<String> = setOf(
        "d000", "d100", "d200", "d210", "d211", "d212", "d220", "d221", "d222", "d240",
        "d300", "d310", "d311", "d312", "d320", "d321", "d322", "d340",
        "d400", "d410", "d411", "d412", "d420", "d421", "d422", "d430", "d431", "d432", "d440",
        "d500", "d600", "d605", "d705", "d905", "d900", "d907",
        "n000", "n100", "n200", "n210", "n211", "n212", "n220", "n221", "n222", "n240",
        "n300", "n310", "n311", "n312", "n320", "n321", "n322", "n340",
        "n400", "n410", "n411", "n412", "n420", "n421", "n422", "n430", "n431", "n432", "n440",
        "n500", "n600", "n605", "n705", "n905", "n900", "n907"
    )

    /**
     * MSN 风级描述（如 `2级` / `微风`）→ 风级数字（UI 统一拼接"级"字）。
     */
    private fun stripWindScale(scale: String?): String {
        return scale?.removeSuffix("级") ?: ""
    }

    /**
     * AQI 数值 → 国内等级描述。
     */
    private fun aqiLevel(value: Int): String = when {
        value <= 50 -> "优"
        value <= 100 -> "良"
        value <= 150 -> "轻度污染"
        value <= 200 -> "中度污染"
        value <= 300 -> "重度污染"
        else -> "严重污染"
    }

    /**
     * 将 MSN 时间（如 `2024-05-29T20:00:00+08:00`）格式化为 `HH:mm`。
     */
    private fun formatTime(isoTime: String?): String {
        if (isoTime.isNullOrBlank()) return ""
        return try {
            val date = inputFormat.parse(isoTime) ?: return ""
            outputTimeFormat.format(date)
        } catch (e: Exception) {
            // 容错：解析失败时直接截取时分
            isoTime.substringAfter('T').take(5).ifEmpty { isoTime }
        }
    }
}
