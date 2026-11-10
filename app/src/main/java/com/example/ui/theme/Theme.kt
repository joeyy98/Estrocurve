package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.AppThemeMode

private val EstroDarkColorScheme = darkColorScheme(
    primary = EstradiolPink,
    onPrimary = Color.Black,
    primaryContainer = EstradiolPinkDark,
    onPrimaryContainer = Color.White,
    secondary = TestosteroneCyan,
    onSecondary = Color.Black,
    secondaryContainer = TestosteroneCyanDark,
    onSecondaryContainer = Color.White,
    tertiary = StatusSuppressed,
    onTertiary = Color.Black,
    background = Black,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorderLight
)

private val EstroLightColorScheme = lightColorScheme(
    primary = EstradiolPinkDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD8E4),
    onPrimaryContainer = Color(0xFF3B071E),
    secondary = TestosteroneCyanDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC4E7F8),
    onSecondaryContainer = Color(0xFF001F29),
    tertiary = StatusSuppressed,
    onTertiary = Color.White,
    background = Color(0xFFFBF8F9),
    onBackground = Color(0xFF1D1B1C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1B1C),
    surfaceVariant = Color(0xFFF0EAEF),
    onSurfaceVariant = Color(0xFF4C4549),
    outline = Color(0xFF7E7579),
    outlineVariant = Color(0xFFD0C3C9)
)

@Composable
fun EstroCurveTheme(
    themeMode: AppThemeMode = AppThemeMode.OLED_BLACK,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when (themeMode) {
        AppThemeMode.OLED_BLACK -> EstroDarkColorScheme
        AppThemeMode.DYNAMIC_SYSTEM -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDarkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            } else {
                if (isDarkTheme) EstroDarkColorScheme
                else EstroLightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    EstroCurveTheme(
        themeMode = if (dynamicColor) AppThemeMode.DYNAMIC_SYSTEM else AppThemeMode.OLED_BLACK,
        isDarkTheme = darkTheme,
        content = content
    )
}
