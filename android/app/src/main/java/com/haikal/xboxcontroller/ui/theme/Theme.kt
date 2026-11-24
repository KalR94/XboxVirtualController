package com.haikal.xboxcontroller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val XboxDarkColorScheme = darkColorScheme(
    primary = XboxGreen,
    onPrimary = TextPrimary,
    primaryContainer = XboxGreenPressed,
    onPrimaryContainer = TextPrimary,
    secondary = XboxGreenBright,
    onSecondary = CharcoalDark,
    background = CharcoalDark,
    onBackground = TextPrimary,
    surface = CharcoalSurface,
    onSurface = TextPrimary,
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = CharcoalDark,
)

@Composable
fun XboxVirtualControllerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // The app is Xbox Elite style: always dark charcoal regardless of system setting.
    MaterialTheme(
        colorScheme = XboxDarkColorScheme,
        typography = XboxTypography,
        content = content,
    )
}
