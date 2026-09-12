package info.ggdog.weather.data.api

import info.ggdog.weather.data.model.response.MsnSuggestResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * MSN 城市搜索接口（Bing autosuggest）。
 *
 * 返回结果含经纬度（geo.latitude / geo.longitude），
 * 可直接用于 [MsnWeatherApi.getWeatherOverview]。
 */
interface MsnLocationApi {

    @GET("locations/autosuggest/")
    suspend fun searchCity(
        @Query("q") query: String,
        @Query("count") count: Int = 10,
        /** 位置偏置，格式 "lat,lon,radius"，使行政区优先返回（MSN 网页同款行为） */
        @Query("localcircularview") localCircularView: String,
        @Query("appid") appid: String = MSN_SUGGEST_APP_ID,
        @Query("apikey") apiKey: String = MSN_API_KEY,
        @Query("setmkt") setmkt: String = "zh-cn",
        @Query("setlang") setlang: String = "zh-cn",
        @Query("types") types: String = "Place,Address,Business",
        @Query("abbrtext") abbrtext: String = "1",
        /** 返回结构化行政区划地址，缺省时只返回 POI/地标 */
        @Query("structuredaddress") structuredAddress: String = "true",
        @Query("strucaddrread") strucAddrRead: String = "1",
        @Query("ocid") ocid: String = "msftweather"
    ): MsnSuggestResponse
}
