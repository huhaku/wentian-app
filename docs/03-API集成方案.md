# Android 天气应用 API 集成方案

## 1. 当前接入结论

当前项目已接入 **和风天气 QWeather**，并已经从早期文档中的设计方案，落地为可运行的真实接口实现。

当前代码实际采用：
- **认证方式：API Key 查询参数**
- **天气接口与城市搜索接口分离**
- **城市搜索单独走 GeoAPI**
- **默认 API URL 与 API Key 已支持内置**

---

## 2. 当前接口划分

### 2.1 天气接口
由 [QWeatherApi.kt](file:///C:/Users/lenovo/Desktop/project/wentian-app/app/src/main/java/info/ggdog/weather/data/api/QWeatherApi.kt) 承载，包含：
- 实时天气
- 7 天预报
- 24 小时预报
- 空气质量
- 日出日落

### 2.2 城市搜索接口
由 [QWeatherGeoApi.kt](file:///C:/Users/lenovo/Desktop/project/wentian-app/app/src/main/java/info/ggdog/weather/data/api/QWeatherGeoApi.kt) 承载，包含：
- 城市搜索 `GET /geo/v2/city/lookup`

> 说明：这部分是当前实现中与早期文档差异最大的地方。城市搜索已经不再使用早期文档中的 `/location/search` 方案。

---

## 3. 当前认证方式

### 3.1 已采用方式
当前项目使用 **API Key 查询参数方式**。

实现方式：
- 在 OkHttp 拦截器中统一自动追加 `key`
- 页面和 Repository 不直接拼接 `key`

### 3.2 未采用方式
当前代码 **未使用 JWT / EdDSA / 私钥签名方案**。

原因：
- 当前项目以快速稳定接入真实接口为目标
- 旧版 API Key 模式更适合当前联调与测试场景

---

## 4. 当前 API 配置方案

### 4.1 配置来源
由 [ApiConfigProvider.kt](file:///C:/Users/lenovo/Desktop/project/wentian-app/app/src/main/java/info/ggdog/weather/data/api/ApiConfigProvider.kt) 统一管理：
- Base URL
- API Key
- 默认值
- 旧配置兼容迁移

### 4.2 当前行为
1. 首次启动可使用默认 URL / API Key
2. 用户可在设置页或错误页修改 URL / API Key
3. 如果用户保存过旧的 `devapi.qweather.com`，会自动迁移到当前可用地址
4. 如果用户输入 URL 未带协议，自动补全 `https://`

---

## 5. 当前接口清单

### 5.1 实时天气
```http
GET /weather/now?location={location}&lang=zh&key={key}
```

### 5.2 7 天预报
```http
GET /weather/7d?location={location}&lang=zh&key={key}
```

### 5.3 24 小时预报
```http
GET /weather/24h?location={location}&lang=zh&key={key}
```

### 5.4 空气质量
```http
GET /air/now?location={location}&lang=zh&key={key}
```

### 5.5 日出日落
```http
GET /astronomy/sun?location={location}&date={date}&key={key}
```

### 5.6 城市搜索（GeoAPI）
```http
GET /geo/v2/city/lookup?location={keyword}&lang=zh&range=cn&number=10&key={key}
```

---

## 6. 当前 Retrofit 设计

### 6.1 天气接口

```kotlin
interface QWeatherApi {
    @GET("weather/now")
    suspend fun getCurrentWeather(
        @Query("location") location: String,
        @Query("lang") lang: String = "zh"
    ): CurrentWeatherResponse

    @GET("weather/7d")
    suspend fun getForecast(
        @Query("location") location: String,
        @Query("lang") lang: String = "zh"
    ): ForecastResponse

    @GET("weather/24h")
    suspend fun getHourlyForecast(
        @Query("location") location: String,
        @Query("lang") lang: String = "zh"
    ): HourlyForecastResponse

    @GET("air/now")
    suspend fun getAqi(
        @Query("location") location: String,
        @Query("lang") lang: String = "zh"
    ): AqiResponse

    @GET("astronomy/sun")
    suspend fun getSunTime(
        @Query("location") location: String,
        @Query("date") date: String
    ): SunTimeResponse
}
```

### 6.2 城市搜索接口

```kotlin
interface QWeatherGeoApi {
    @GET("geo/v2/city/lookup")
    suspend fun searchCity(
        @Query("location") keyword: String,
        @Query("lang") lang: String = "zh",
        @Query("range") range: String = "cn",
        @Query("number") number: Int = 10
    ): SearchResponse
}
```

---

## 7. 当前 Repository 处理策略

### 7.1 WeatherRepositoryImpl
职责：
- 拉取天气数据
- 转换为领域模型
- 组织实时天气 / 预报 / AQI / 日出日落
- 向页面返回统一天气对象

### 7.2 LocationRepositoryImpl
职责：
- 查询默认/内置城市列表
- 调用 GeoAPI 搜索城市
- 对搜索异常做兜底处理

当前特殊处理：
- 当搜索接口返回 **HTTP 400 / 404** 时，按“无结果”处理，返回空列表
- 不再把无结果当作页面错误

---

## 8. 错误处理策略

### 8.1 天气页面
- API Key 未填写：提示用户填写正确配置
- API Key 错误：提示鉴权失败
- 网络异常：展示错误页，并支持复制错误信息
- 错误页支持填写 URL / API Key 后“保存并重试”

### 8.2 城市搜索
- 搜索无结果：显示空状态
- 400 / 404：转为空结果
- 其他 HTTP 错误：按错误态处理
- IOException：提示网络连接失败

---

## 9. 当前已解决的关键 API 问题

1. **Moshi Converter 创建失败**
   - 已通过加入 Kotlin 反射适配解决
2. **城市搜索持续 404**
   - 已拆出 `QWeatherGeoApi`
   - 使用官方 GeoAPI 路径 `/geo/v2/city/lookup`
3. **旧 host 配置导致接口错误**
   - 已在 `ApiConfigProvider` 中兼容迁移
4. **搜索无结果抛 400**
   - 已在仓库层按空结果处理

---

## 10. 当前与旧文档差异说明

| 旧文档设计 | 当前实际实现 |
|-----------|-------------|
| `key` 作为每个接口参数显式传入 | 统一由拦截器自动追加 |
| 城市搜索走 `/location/search` | 已改为 `/geo/v2/city/lookup` |
| 单一 Retrofit 处理所有接口 | 已拆分 weather / geo 两套 Retrofit |
| JWT 私钥签名 | 当前未使用 |
| 通过 `local.properties` 注入 Key | 当前已支持运行时页面配置 |

---

## 11. 后续建议

1. 增加接口回归测试清单
2. 补充天气接口与搜索接口的日志分级
3. 在正式发布前重新评估 API Key 暴露风险
4. 结合 Room 完善离线缓存与过期策略
