package com.example.ricarica.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DarkGreen,
    secondary = LightGreen,
    surface = DarkGreen, // La barra è considerata una superficie nel tuo design
    onSurface = White,
    background = White
)

@Composable
fun RicaricaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}