package com.example.moemi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moemi.data.Level
import com.example.moemi.ui.theme.Background
import com.example.moemi.ui.theme.Border
import com.example.moemi.ui.theme.Primary
import com.example.moemi.ui.theme.TextSecondary
import com.example.moemi.ui.theme.TextTertiary

/** 둥근 알약 모양 배지 (양호/보통/주의, 자외선 높음 등) */
@Composable
fun StatusBadge(text: String, background: Color, foreground: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(22.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(background)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = foreground)
    }
}

@Composable
fun LevelBadge(level: Level, modifier: Modifier = Modifier) =
    StatusBadge(level.label, level.badgeBg, level.badgeFg, modifier)

/** 필터/일정 선택용 칩 */
@Composable
fun AppChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(shape)
            .background(if (selected) Primary else Color.White)
            .then(if (selected) Modifier else Modifier.border(1.dp, Border, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else TextSecondary
        )
    }
}

/** 흰 배경 + 옅은 테두리 카드 */
@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Border, shape),
        content = content
    )
}

@Composable
fun HorizontalLine(modifier: Modifier = Modifier, color: Color = Border) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

/** 점선 테두리의 "+ ○○ 추가" 버튼 */
@Composable
fun DashedAddButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val color = if (enabled) Primary else TextTertiary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .drawBehind {
                val stroke = 1.2.dp.toPx()
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
                    )
                )
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun ClickableIcon(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Box(
        modifier = modifier
            .size(size + 12.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size))
    }
}

/** 하단 탭바 */
enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("홈", AppIcons.Home),
    CALENDAR("캘린더", AppIcons.Calendar),
    SCHEDULE("일정", AppIcons.Schedule),
    BUS("버스", AppIcons.Bus),
    MAP("지도", AppIcons.MapPin),
}

@Composable
fun AppTabBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
        HorizontalLine()
        Row(Modifier.fillMaxWidth().height(63.dp)) {
            AppTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val color = if (isSelected) Primary else TextTertiary
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = color
                    )
                }
            }
        }
    }
}

/** 아직 디자인이 없는 탭에 보여줄 화면 */
@Composable
fun ComingSoon(title: String) {
    Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        Text("$title 화면은 준비 중이에요", fontSize = 15.sp, color = TextSecondary)
    }
}
