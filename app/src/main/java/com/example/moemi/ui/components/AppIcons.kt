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
    val Plus by lazy { lineIcon("Plus", listOf("M12 5V19", "M5 12H19"), strokeWidth = 2.2f) }
    val Star by lazy {
        lineIcon(
            "Star",
            strokes = emptyList(),
            fills = listOf("M12 2.5L14.8 8.6L21.5 9.3L16.5 13.8L17.9 20.5L12 17.1L6.1 20.5L7.5 13.8L2.5 9.3L9.2 8.6Z")
        )
    }
    val Warning by lazy {
        lineIcon(
            "Warning",
            strokes = listOf("M12 3.5L21.5 20H2.5Z", "M12 9.5V14"),
            fills = listOf(circle(12f, 17f, 1f)),
            strokeWidth = 1.6f
        )
    }
    val Bell by lazy {
        lineIcon("Bell", listOf("M6 16V11A6 6 0 0 1 18 11V16L20 18H4Z", "M10 21H14"), strokeWidth = 1.6f)
    }
    val Mic by lazy {
        lineIcon("Mic", listOf(roundRect(9f, 3f, 6f, 11f, 3f), "M5.5 11A6.5 6.5 0 0 0 18.5 11", "M12 17.5V21"), strokeWidth = 1.6f)
    }
    val ArrowUp by lazy { lineIcon("ArrowUp", listOf("M12 19V5", "M6 11L12 5L18 11"), strokeWidth = 2.2f) }
    val Sparkle by lazy {
        lineIcon(
            "Sparkle",
            strokes = emptyList(),
            fills = listOf("M12 2C13 8 16 11 22 12C16 13 13 16 12 22C11 16 8 13 2 12C8 11 11 8 12 2Z")
        )
    }
    val DragHandle by lazy {
        lineIcon(
            "DragHandle",
            strokes = emptyList(),
            fills = listOf(
                circle(9f, 6f, 1.5f), circle(15f, 6f, 1.5f),
                circle(9f, 12f, 1.5f), circle(15f, 12f, 1.5f),
                circle(9f, 18f, 1.5f), circle(15f, 18f, 1.5f),
            )
        )
    }
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
