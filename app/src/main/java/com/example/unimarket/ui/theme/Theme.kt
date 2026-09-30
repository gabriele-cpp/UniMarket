package com.example.unimarket.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UniMarketColors = lightColorScheme(
    primary = Color(0xFF1A1C1A),
    onPrimary = Color.White,
    secondaryContainer = Color(0xFFE7EDE5),
    onSecondaryContainer = Color(0xFF222921),
    background = Color.White,
    surface = Color.White,
    onSurface = Color(0xFF1A1C1A),
    onSurfaceVariant = Color(0xFF60645E),
    outlineVariant = Color(0xFFDDE1DA),
)

@Composable
fun UniMarketTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = UniMarketColors, typography = Typography, content = content)
}
