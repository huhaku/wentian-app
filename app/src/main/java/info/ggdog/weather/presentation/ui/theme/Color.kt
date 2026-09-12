package info.ggdog.weather.presentation.ui.theme

import androidx.compose.ui.graphics.Color

val lightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF1E88E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF005096),
    
    secondary = Color(0xFF5C6BC0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E5F8),
    onSecondaryContainer = Color(0xFF1B2975),
    
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF212121),
    
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFE0E0E0),
    
    error = Color(0xFFD32F2F),
    onError = Color.White,
    
    outline = Color(0xFFE0E0E0)
)

val darkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFF42A5F5),
    onPrimary = Color(0xFF002D5C),
    primaryContainer = Color(0xFF005096),
    onPrimaryContainer = Color(0xFFBBDEFB),
    
    secondary = Color(0xFF9FA8DA),
    onSecondary = Color(0xFF1B2975),
    secondaryContainer = Color(0xFF303F9F),
    onSecondaryContainer = Color(0xFFE3E5F8),
    
    background = Color(0xFF121212),
    onBackground = Color(0xFFFFFFFF),
    
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF303030),
    
    error = Color(0xFFEF5350),
    onError = Color.White,
    
    outline = Color(0xFF303030)
)

val weatherBackgrounds = mapOf(
    "sunny" to Pair(Color(0xFFFFB347), Color(0xFFFFCC33)),
    "cloudy" to Pair(Color(0xFF90CAF9), Color(0xFF64B5F6)),
    "overcast" to Pair(Color(0xFFBDBDBD), Color(0xFF9E9E9E)),
    "rain" to Pair(Color(0xFF64B5F6), Color(0xFF42A5F5)),
    "snow" to Pair(Color(0xFFE3F2FD), Color(0xFFBBDEFB)),
    "fog" to Pair(Color(0xFFCFD8DC), Color(0xFFB0BEC5))
)
