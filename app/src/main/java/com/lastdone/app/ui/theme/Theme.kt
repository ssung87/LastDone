package com.lastdone.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    secondary = Gray800,
    onSecondary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,
    outline = Gray300,
    outlineVariant = Gray200,
    error = StatusOverdue,
    onError = Paper
)

private val DarkColors = darkColorScheme(
    primary = Paper,
    onPrimary = Ink,
    secondary = Gray200,
    onSecondary = Ink,
    background = Color(0xFF000000),
    onBackground = Paper,
    surface = Color(0xFF0A0A0A),
    onSurface = Paper,
    surfaceVariant = Gray900,
    onSurfaceVariant = Gray400,
    outline = Gray800,
    outlineVariant = Gray900,
    error = StatusOverdue,
    onError = Paper
)

@Composable
fun LastDoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = LastDoneTypography,
        content = content
    )
}
