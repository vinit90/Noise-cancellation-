package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LabDarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = LabDarkVoid,
    primaryContainer = CyanGlow.copy(alpha = 0.25f),
    onPrimaryContainer = CyanNeon,
    secondary = AmberAntiPhase,
    onSecondary = LabDarkVoid,
    secondaryContainer = AmberAntiPhase.copy(alpha = 0.25f),
    onSecondaryContainer = OrangeGlow,
    tertiary = VioletMod,
    onTertiary = LabDarkVoid,
    background = LabDarkVoid,
    onBackground = TextPrimary,
    surface = LabDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = LabDarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = LabDarkBorder,
    error = CrimsonAlert,
    onError = TextPrimary
)

// For an acoustic lab tool, a dedicated dark interface prevents screen glare and highlights oscilloscope waveforms
private val LabLightColorScheme = LabDarkColorScheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LabDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = LabDarkVoid.toArgb()
                window.navigationBarColor = LabDarkVoid.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
