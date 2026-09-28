package com.example.moemi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Text()의 기본 스타일. 줄 높이를 글자 크기에 비례(em)하게 해서 작은 글자도 촘촘하게 배치된다.
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 1.4.em,
        letterSpacing = 0.sp,
        color = TextPrimary
    )
)
