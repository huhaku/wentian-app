package info.ggdog.weather.data.api

import info.ggdog.weather.data.model.response.MsnOverviewResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * MSN 天气数据接口（weatherfalcon）。
 *
 * MSN 天气网页（msn.com / msn.cn）使用的私有接口，内置公开的
 * appId / apikey（从网页 JS 中提取），无需用户申请。
 */
interface MsnWeatherApi {

    @GET("weatherfalcon/weather/overview")
    suspend fun getWeatherOverview(
        @Query("latLongList") latLongList: String,
        @Query("locale") locale: String = "zh-cn",
        @Query("units") units: String = "C",
        @Query("appId") appId: String = MSN_WEATHER_APP_ID,
        @Query("apikey") apiKey: String = MSN_API_KEY,
        @Query("ocid") ocid: String = "msftweather",
        @Query("cm") market: String = "zh-cn",
        @Query("it") it: String = "web",
        @Query("scn") scn: String = "ANON",
        @Query("wrapOData") wrapOData: String = "false"
    ): MsnOverviewResponse
}
