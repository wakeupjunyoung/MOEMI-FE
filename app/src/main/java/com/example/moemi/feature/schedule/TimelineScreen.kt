package com.example.moemi.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moemi.ui.components.AppIcons
import com.example.moemi.ui.components.HorizontalLine
import com.example.moemi.ui.theme.Background
import com.example.moemi.ui.theme.BorderStrong
import com.example.moemi.ui.theme.CautionBg
import com.example.moemi.ui.theme.CautionDot
import com.example.moemi.ui.theme.CautionFg
import com.example.moemi.ui.theme.GoodBg
import com.example.moemi.ui.theme.GoodDot
import com.example.moemi.ui.theme.GoodFg
import com.example.moemi.ui.theme.MoemiTheme
import com.example.moemi.ui.theme.NormalDot
import com.example.moemi.ui.theme.Primary
import com.example.moemi.ui.theme.PrimarySoft
import com.example.moemi.ui.theme.Sunday
import com.example.moemi.ui.theme.TextPrimary
import com.example.moemi.ui.theme.TextSecondary
import com.example.moemi.ui.theme.TextTertiary
import com.example.moemi.ui.theme.Track
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val HourHeight = 70.dp
private val LabelWidth = 52.dp
private val PrepBg = Color(0xFFF1F3F6)
private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val DayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

/** 기능 5A — 하루 타임라인 */
@Composable
fun TimelineScreen(
    state: ScheduleState,
    onAdd: () -> Unit,
    onOpenSchedule: (ScheduleUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val date = state.selectedDate
    val schedules = state.schedulesOn(date)
    val conflict = state.conflictOn(date)

    Column(modifier.fillMaxSize().background(Background).statusBarsPadding()) {
        // 헤더
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("일정", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Primary).clickable(onClick = onAdd),
                contentAlignment = Alignment.Center
            ) {
                Icon(AppIcons.Plus, contentDescription = "일정 추가", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        WeekStrip(state)

        if (conflict != null) {
            val (late, early, minutes) = conflict
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CautionBg)
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(AppIcons.Warning, contentDescription = null, tint = CautionDot, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${late.title}이 ${early.title}과 ${minutes}분 겹쳐요",
                    fontSize = 13.sp,
                    color = CautionFg
                )
            }
        }

        DayTimeline(
            date = date,
            schedules = schedules,
            onOpenSchedule = onOpenSchedule,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun WeekStrip(state: ScheduleState) {
    val selected = state.selectedDate
    val sunday = selected.minusDays((selected.dayOfWeek.value % 7).toLong())
    val today = LocalDate.now()
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        (0..6).forEach { i ->
            val day = sunday.plusDays(i.toLong())
            val isSelected = day == selected
            val labelColor = when (day.dayOfWeek) {
                DayOfWeek.SUNDAY -> Sunday
                DayOfWeek.SATURDAY -> Primary
                else -> TextSecondary
            }
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { state.selectedDate = day }
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(DayLabels[i], fontSize = 12.sp, color = labelColor)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Primary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${day.dayOfMonth}",
                        fontSize = 16.sp,
                        fontWeight = if (isSelected || day == today) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextPrimary
                    )
                }
                Spacer(Modifier.height(6.dp))
                val dot = when (state.toneOf(day)) {
                    ScheduleTone.PRIMARY -> CautionDot
                    ScheduleTone.CAUTION -> NormalDot
                    ScheduleTone.GOOD -> GoodDot
                    null -> Color.Transparent
                }
                Box(Modifier.size(5.dp).clip(CircleShape).background(dot))
            }
        }
    }
}

/** One block placed on the timeline, with its column when blocks overlap. */
private data class Placed(val schedule: ScheduleUi, val column: Int, val columns: Int)

private fun place(schedules: List<ScheduleUi>): List<Placed> {
    val result = mutableListOf<Placed>()
    var group = mutableListOf<Pair<ScheduleUi, Int>>()
    var groupEnd = LocalTime.MIN
    fun flush() {
        val columns = (group.maxOfOrNull { it.second } ?: 0) + 1
        group.forEach { (s, c) -> result += Placed(s, c, columns) }
        group = mutableListOf()
    }
    schedules.sortedBy { it.start }.forEach { s ->
        if (group.isNotEmpty() && s.start >= groupEnd) flush()
        val taken = group.filter { it.first.end > s.start }.map { it.second }.toSet()
        val column = generateSequence(0) { it + 1 }.first { it !in taken }
        group += s to column
        groupEnd = maxOf(groupEnd, s.end)
    }
    if (group.isNotEmpty()) flush()
    return result
}

@Composable
private fun DayTimeline(
    date: LocalDate,
    schedules: List<ScheduleUi>,
    onOpenSchedule: (ScheduleUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val firstHour = schedules.minOfOrNull { it.prepStartAt.hour } ?: 9
    LaunchedEffect(date) {
        with(density) { scroll.scrollTo((HourHeight * (firstHour - 1).coerceAtLeast(0)).roundToPx()) }
    }

    fun y(time: LocalTime): Dp = HourHeight * (time.hour + time.minute / 60f)

    Box(modifier.fillMaxWidth().verticalScroll(scroll)) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(HourHeight * 24 + 20.dp)
                .padding(top = 10.dp, end = 16.dp)
        ) {
            val laneWidth = maxWidth - LabelWidth - 4.dp

            // 시간 눈금
            (0..23).forEach { hour ->
                Row(Modifier.offset(y = HourHeight * hour - 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "%02d:00".format(hour),
                        modifier = Modifier.width(LabelWidth).padding(start = 20.dp),
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                    HorizontalLine(Modifier.padding(start = 4.dp), color = Track)
                }
            }

            // 준비·이동 블록
            schedules.filter { it.prepMinutes > 0 || it.travelMinutes > 0 }.forEach { s ->
                if (s.prepMinutes > 0) {
                    BufferBlock(
                        "준비 ${s.prepMinutes}분",
                        background = PrepBg,
                        border = BorderStrong,
                        textColor = TextSecondary,
                        modifier = Modifier
                            .offset(x = LabelWidth + 4.dp, y = y(s.prepStartAt))
                            .width(laneWidth)
                            .height(y(s.departAt) - y(s.prepStartAt))
                    )
                }
                if (s.travelMinutes > 0) {
                    BufferBlock(
                        "이동 ${s.travelMinutes}분" + (s.travelLabel?.let { " · $it" } ?: ""),
                        background = PrimarySoft,
                        border = Primary.copy(alpha = 0.45f),
                        textColor = Primary,
                        modifier = Modifier
                            .offset(x = LabelWidth + 4.dp, y = y(s.departAt))
                            .width(laneWidth)
                            .height(y(s.start) - y(s.departAt))
                    )
                }
            }

            // 일정 블록
            place(schedules).forEach { (s, column, columns) ->
                val gap = 6.dp
                val width = (laneWidth - gap * (columns - 1)) / columns
                ScheduleBlock(
                    s,
                    onClick = { onOpenSchedule(s) },
                    modifier = Modifier
                        .offset(x = LabelWidth + 4.dp + (width + gap) * column, y = y(s.start) + 1.dp)
                        .width(width)
                        .height(y(s.end) - y(s.start) - 2.dp)
                )
            }

            // 현재 시각
            if (date == LocalDate.now()) {
                val now = y(LocalTime.now())
                Box(
                    Modifier
                        .offset(x = LabelWidth + 4.dp, y = now)
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(CautionDot)
                )
                Box(
                    Modifier
                        .offset(x = LabelWidth, y = now - 3.5.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CautionDot)
                )
            }
        }
    }
}

@Composable
private fun BufferBlock(text: String, background: Color, border: Color, textColor: Color, modifier: Modifier) {
    Box(
        modifier
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    color = border,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
                )
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text, fontSize = 12.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ScheduleBlock(schedule: ScheduleUi, onClick: () -> Unit, modifier: Modifier) {
    val (bg, border, title, sub) = when (schedule.tone) {
        ScheduleTone.PRIMARY -> listOf(Primary, Primary, Color.White, Color.White.copy(alpha = 0.85f))
        ScheduleTone.CAUTION -> listOf(CautionBg, CautionDot, CautionFg, CautionFg)
        ScheduleTone.GOOD -> listOf(GoodBg, GoodDot.copy(alpha = 0.5f), GoodFg, GoodFg)
    }
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(schedule.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = title, maxLines = 1)
        Text(
            "${schedule.start.format(TimeFormat)} – ${schedule.end.format(TimeFormat)}",
            fontSize = 12.sp,
            color = sub,
            maxLines = 1
        )
        schedule.place?.let { Text(it, fontSize = 12.sp, color = sub, maxLines = 1) }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun TimelinePreview() {
    MoemiTheme { TimelineScreen(ScheduleState(LocalDate.now()), onAdd = {}, onOpenSchedule = {}) }
}
