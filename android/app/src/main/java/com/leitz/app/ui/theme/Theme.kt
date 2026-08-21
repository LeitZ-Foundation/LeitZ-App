package com.leitz.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = LeitXPrimary,
    secondary = LeitXSecondary,
    background = LeitXBackground,
    surface = LeitXSurface,
    onPrimary = LeitXTextPrimary,
    onSecondary = LeitXTextPrimary,
    onBackground = LeitXTextPrimary,
    onSurface = LeitXTextPrimary,
    error = LeitXError
)

private val LightColorScheme = lightColorScheme(
    primary = LeitXPrimary,
    secondary = LeitXSecondary,
    background = LeitXTextPrimary,
    surface = LeitXSurface,
    onPrimary = LeitXTextPrimary,
    onSecondary = LeitXTextPrimary,
    onBackground = LeitXDark,
    onSurface = LeitXTextPrimary,
    error = LeitXError
)

@Composable
fun LeitXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LeitXTypography,
        content = content
    )
}
