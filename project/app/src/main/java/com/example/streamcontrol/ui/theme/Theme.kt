package com.example.streamcontrol.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = SurfaceLight,
    primaryContainer = Blue200,
    onPrimaryContainer = OnBackgroundLight,
    secondary = Teal700,
    onSecondary = SurfaceLight,
    secondaryContainer = Teal200,
    onSecondaryContainer = OnBackgroundLight,
    tertiary = Teal500,
    onTertiary = SurfaceLight,
    error = Red700,
    onError = SurfaceLight,
    errorContainer = Red200,
    onErrorContainer = OnBackgroundLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = OnSurfaceLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue700Dark,
    onPrimary = OnBackgroundLight,
    primaryContainer = Blue700,
    onPrimaryContainer = Blue200,
    secondary = Teal700Dark,
    onSecondary = OnBackgroundLight,
    secondaryContainer = Teal700,
    onSecondaryContainer = Teal200,
    tertiary = Teal500,
    onTertiary = OnBackgroundLight,
    error = Red700Dark,
    onError = OnBackgroundLight,
    errorContainer = Red700,
    onErrorContainer = Red200,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = OnSurfaceDark
)

@Composable
fun StreamControlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
