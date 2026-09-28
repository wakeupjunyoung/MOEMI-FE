package com.example.moemi.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * 디자인 SVG에 있던 선 아이콘들을 그대로 옮긴 것.
 * 색은 Icon(tint = ...)으로 입힌다.
 */
object AppIcons {
    val Home by lazy { lineIcon("Home", listOf("M3 20V11L12 3L21 11V20Z")) }
    val Calendar by lazy {
        lineIcon("Calendar", listOf(roundRect(3f, 4f, 18f, 17f, 3f), "M3 9H21", "M8 1V6", "M16 1V6"))
    }
    val Schedule by lazy { lineIcon("Schedule", listOf("M4 6H20", "M4 12H20", "M4 18H20")) }
    val Bus by lazy {
        lineIcon(
            "Bus",
            strokes = listOf(roundRect(4f, 3f, 16f, 16f, 3.5f), "M4 10H20", "M7 20V22", "M17 20V22"),
            fills = listOf(circle(8f, 15f, 1.4f), circle(16f, 15f, 1.4f))
        )
    }
    val MapPin by lazy {
        lineIcon(
            "MapPin",
            listOf("M12 23C8 18 4 14 4 10A8 8 0 1 1 20 10C20 14 16 18 12 23Z", circle(12f, 10f, 2.6f))
        )
    }
    val ChevronDown by lazy { lineIcon("ChevronDown", listOf("M4 6L8 10L12 6"), strokeWidth = 1.6f, size = 16f) }
    val ChevronRight by lazy { lineIcon("ChevronRight", listOf("M6 4L10 8L6 12"), strokeWidth = 1.8f, size = 16f) }
    val Back by lazy { lineIcon("Back", listOf("M15 4L7 12L15 20"), strokeWidth = 2f) }
    val Forward by lazy { lineIcon("Forward", listOf("M9 4L17 12L9 20"), strokeWidth = 2f) }
    val Refresh by lazy {
        lineIcon("Refresh", listOf("M13 6A6.5 6.5 0 1 0 13.5 11", "M13.5 1V6.5H8"), strokeWidth = 1.6f, size = 16f)
    }
    val Search by lazy { lineIcon("Search", listOf(circle(10f, 10f, 6.5f), "M15 15L20 20")) }
    val Locate by lazy {
        lineIcon(
            "Locate",
            strokes = listOf(circle(12f, 12f, 6f), "M12 2V5", "M12 19V22", "M2 12H5", "M19 12H22"),
            fills = listOf(circle(12f, 12f, 2f))
        )
    }
    val Check by lazy { lineIcon("Check", listOf("M6 11.5L9.5 15L16.5 7.5"), strokeWidth = 2.2f, size = 22f) }
    val Close by lazy { lineIcon("Close", listOf("M7 7L17 17", "M17 7L7 17"), strokeWidth = 1.8f) }
}

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}A$r $r 0 1 0 ${cx + r} ${cy}A$r $r 0 1 0 ${cx - r} ${cy}Z"

private fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r} ${y}H${x + w - r}A$r $r 0 0 1 ${x + w} ${y + r}V${y + h - r}" +
        "A$r $r 0 0 1 ${x + w - r} ${y + h}H${x + r}A$r $r 0 0 1 $x ${y + h - r}V${y + r}" +
        "A$r $r 0 0 1 ${x + r} ${y}Z"

private fun lineIcon(
    name: String,
    strokes: List<String>,
    fills: List<String> = emptyList(),
    strokeWidth: Float = 1.8f,
    size: Float = 24f,
): ImageVector = ImageVector.Builder(name, size.dp, size.dp, size, size).apply {
    strokes.forEach {
        addPath(
            pathData = PathParser().parsePathString(it).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    fills.forEach {
        addPath(pathData = PathParser().parsePathString(it).toNodes(), fill = SolidColor(Color.Black))
    }
}.build()
