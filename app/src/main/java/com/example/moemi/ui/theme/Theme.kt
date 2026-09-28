package com.example.moemi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 디자인이 라이트 모드 하나뿐이라 동적 색상/다크 모드는 쓰지 않는다.
private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    background = Background,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = Track,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceWhite,
    surfaceContainerHigh = SurfaceWhite,
    outline = Border,
)

@Composable
fun MoemiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}
