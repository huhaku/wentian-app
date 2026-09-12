package info.ggdog.weather.domain.model

data class Settings(
    val themeMode: ThemeMode,
    val tempUnit: TempUnit,
    val windUnit: WindUnit,
    val updateInterval: Long
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class TempUnit { CELSIUS, FAHRENHEIT }
enum class WindUnit { KM_H, M_S, MPH, KNOTS }
