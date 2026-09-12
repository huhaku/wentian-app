package info.ggdog.weather.data.model.response

/**
 * MSN 城市搜索（Bing autosuggest）响应 DTO。
 *
 * 接口：GET https://assets.msn.cn/service/msn/v0/pages/weather/locations/autosuggest/
 */
data class MsnSuggestResponse(
    val `value`: List<MsnSuggestion> = emptyList()
)

data class MsnSuggestion(
    val name: String? = null,
    val geo: MsnGeo? = null,
    val address: MsnAddress? = null,
    val entityPresentationInfo: MsnEntityInfo? = null
)

data class MsnGeo(
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class MsnAddress(
    val addressLocality: String? = null,
    val addressSubregion: String? = null,
    val addressRegion: String? = null,
    val addressCountry: String? = null,
    val countryIso: String? = null,
    val text: String? = null
)

data class MsnEntityInfo(
    val entityScenario: String? = null,
    val entitySubTypeHints: List<String> = emptyList()
)
