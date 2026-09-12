package info.ggdog.weather.data.model.response

/**
 * MSN 天气 overview 接口（weatherfalcon）响应 DTO。
 *
 * 接口：GET https://api.msn.cn/weatherfalcon/weather/overview
 * 一次请求返回：当前天气 + 10 天逐日预报（含逐小时）+ 日出日落 + AQI。
 * 所有字段均为可空，缺失时由 Mapper 容错处理。
 */
data class MsnOverviewResponse(
    val responses: List<MsnResponse> = emptyList()
)

data class MsnResponse(
    val weather: List<MsnWeather> = emptyList()
)

data class MsnWeather(
    val current: MsnCurrent? = null,
    val forecast: MsnForecast? = null,
    val provider: MsnProvider? = null
)

data class MsnCurrent(
    val temp: Double? = null,
    val feels: Double? = null,
    val cap: String? = null,
    val pvdrCap: String? = null,
    val symbol: String? = null,
    val rh: Double? = null,
    val windSpd: Double? = null,
    val windGust: Double? = null,
    val baro: Double? = null,
    val vis: Double? = null,
    val uv: Double? = null,
    val uvDesc: String? = null,
    val cloudCover: Double? = null,
    val precip: Double? = null,
    val aqi: Double? = null,
    val pvdrWindDir: String? = null,
    val pvdrWindSpd: String? = null,
    val created: String? = null
)

data class MsnForecast(
    val days: List<MsnForecastDay> = emptyList()
)

data class MsnForecastDay(
    val daily: MsnDaily? = null,
    val hourly: List<MsnHourly> = emptyList(),
    val almanac: MsnAlmanac? = null
)

data class MsnDaily(
    val day: MsnDayPart? = null,
    val night: MsnDayPart? = null,
    val valid: String? = null,
    val symbol: String? = null,
    val pvdrCap: String? = null,
    val pvdrWindDir: String? = null,
    val pvdrWindSpd: String? = null,
    val precip: Double? = null,
    val windMax: Double? = null,
    val rh: Double? = null,
    val tempHi: Double? = null,
    val tempLo: Double? = null,
    val uv: Double? = null,
    val aqi: Double? = null,
    val rainAmount: Double? = null
)

data class MsnDayPart(
    val cap: String? = null,
    val symbol: String? = null,
    val precip: Double? = null
)

data class MsnHourly(
    val valid: String? = null,
    val temp: Double? = null,
    val feels: Double? = null,
    val cap: String? = null,
    val pvdrCap: String? = null,
    val symbol: String? = null,
    val rh: Double? = null,
    val windSpd: Double? = null,
    val pvdrWindDir: String? = null,
    val pvdrWindSpd: String? = null,
    val precip: Double? = null,
    val aqi: Double? = null,
    val uv: Double? = null,
    val baro: Double? = null,
    val vis: Double? = null,
    val cloudCover: Double? = null
)

data class MsnAlmanac(
    val sunrise: String? = null,
    val sunset: String? = null,
    val moonPhase: String? = null
)

data class MsnProvider(
    val name: String? = null,
    val url: String? = null
)
