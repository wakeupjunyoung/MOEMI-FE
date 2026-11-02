package com.example.moemi.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moemi.data.DailyLifeIndex
import com.example.moemi.data.DayAvailability
import com.example.moemi.data.IndexItem
import com.example.moemi.data.Level
import com.example.moemi.data.LifeIndexCalendar
import com.example.moemi.data.LifeIndexRepository
import com.example.moemi.data.Region
import com.example.moemi.data.remote.LifeIndexStatus
import com.example.moemi.data.remote.MoemiApi
import com.example.moemi.ui.components.AppCard
import com.example.moemi.ui.components.AppIcons
import com.example.moemi.ui.components.ClickableIcon
import com.example.moemi.ui.components.HorizontalLine
import com.example.moemi.ui.components.LevelBadge
import com.example.moemi.ui.components.StatusBadge
import com.example.moemi.ui.theme.Background
import com.example.moemi.ui.theme.Border
import com.example.moemi.ui.theme.CautionFg
import com.example.moemi.ui.theme.MoemiTheme
import com.example.moemi.ui.theme.Primary
import com.example.moemi.ui.theme.PrimarySoft
import com.example.moemi.ui.theme.Sunday
import com.example.moemi.ui.theme.TextPrimary
import com.example.moemi.ui.theme.TextSecondary
import com.example.moemi.ui.theme.TextTertiary
import com.example.moemi.ui.theme.Track
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class CalendarView(val label: String) { MONTH("월"), WEEK("주"), DAY("일") }

private val weekdayNames = listOf("일", "월", "화", "수", "목", "금", "토")
private val hourMinute = DateTimeFormatter.ofPattern("HH:mm")

/** 월=1 … 일=7 을 "월"…"일"로 */
private fun LocalDate.koreanDayOfWeek() = weekdayNames[dayOfWeek.value % 7]

/** 오늘이면 "HH:mm", 다른 날이면 "M/d HH:mm" */
private fun LocalDateTime.shortText(today: LocalDate) =
    if (toLocalDate() == today) format(hourMinute) else "$monthValue/$dayOfMonth ${format(hourMinute)}"

/** 기능 1 — 생활수치 캘린더 */
@Composable
fun CalendarScreen(region: Region, onRegionChange: (Region) -> Unit, modifier: Modifier = Modifier) {
    val today = remember { LocalDate.now() }
    var selected by rememberSaveable { mutableStateOf(today) }
    var view by rememberSaveable { mutableStateOf(CalendarView.MONTH) }
    var regions by remember { mutableStateOf(listOf(region)) }
    var calendar by remember { mutableStateOf<LifeIndexCalendar?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    // 새로고침을 누를 때마다 올려서 서버 캐시를 건너뛰고 다시 조회한다.
    var refreshCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        try {
            regions = LifeIndexRepository.regions()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 지역 목록을 못 받아도 현재 지역으로는 계속 조회할 수 있다.
        }
    }

    LaunchedEffect(region, refreshCount) {
        if (calendar?.region?.code != region.code) calendar = null
        refreshing = true
        try {
            calendar = LifeIndexRepository.load(region, refresh = refreshCount > 0)
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = MoemiApi.messageOf(e)
        }
        refreshing = false
    }

    fun refresh() {
        if (!refreshing) refreshCount++
    }

    val data = calendar?.days.orEmpty()
    val failure = when {
        calendar?.status == LifeIndexStatus.FAILED -> "기상청 생활수치를 불러오지 못했어요."
        calendar == null -> error
        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        // 제목 + 지역 선택
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("생활 캘린더", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            RegionSelector(region, regions, onRegionChange)
        }

        // 마지막 갱신 + 새로고침
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fetchedAt = calendar?.fetchedAt
            val updated = when {
                fetchedAt != null -> "마지막 갱신 " + (if (fetchedAt.toLocalDate() == today) "오늘 " else "") + fetchedAt.shortText(today)
                failure != null -> "조회 실패"
                else -> "불러오는 중…"
            }
            val warning = when {
                fetchedAt != null && error != null -> "새로고침 실패"
                calendar?.status == LifeIndexStatus.STALE -> "최신 데이터 아님"
                else -> null
            }
            Text(
                listOfNotNull(updated, warning).joinToString(" · "),
                fontSize = 12.sp,
                color = if (warning != null || failure != null) CautionFg else TextSecondary
            )
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable { refresh() }.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (refreshing) {
                    CircularProgressIndicator(Modifier.size(14.dp), color = Primary, strokeWidth = 1.6.dp)
                } else {
                    Icon(AppIcons.Refresh, null, tint = Primary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text("새로고침", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Primary)
            }
        }

        // 기간 이동 + 보기 전환
        val title = when (view) {
            CalendarView.DAY -> "${selected.monthValue}월 ${selected.dayOfMonth}일 (${selected.koreanDayOfWeek()})"
            else -> "${selected.year}년 ${selected.monthValue}월"
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(2.dp))
            ClickableIcon(AppIcons.Back, TextSecondary, size = 18.dp, onClick = {
                selected = when (view) {
                    CalendarView.MONTH -> selected.minusMonths(1)
                    CalendarView.WEEK -> selected.minusWeeks(1)
                    CalendarView.DAY -> selected.minusDays(1)
                }
            })
            ClickableIcon(AppIcons.Forward, TextSecondary, size = 18.dp, onClick = {
                selected = when (view) {
                    CalendarView.MONTH -> selected.plusMonths(1)
                    CalendarView.WEEK -> selected.plusWeeks(1)
                    CalendarView.DAY -> selected.plusDays(1)
                }
            })
            Spacer(Modifier.weight(1f))
            ViewToggle(view) { view = it }
        }

        if (view != CalendarView.DAY) {
            WeekdayHeader()
            val onSelect: (LocalDate) -> Unit = { selected = it }
            if (view == CalendarView.MONTH) {
                MonthGrid(selected, today, calendar, onSelect)
            } else {
                val weekStart = selected.minusDays((selected.dayOfWeek.value % 7).toLong())
                WeekRow(weekStart, selected.monthValue, selected, today, calendar, onSelect)
            }
            Legend()
        }

        DetailCard(
            date = selected,
            today = today,
            day = data[selected],
            availability = LifeIndexRepository.availability(selected, today, calendar),
            failure = failure,
            source = calendar?.source ?: "기상청 생활기상지수 조회서비스",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
        )
    }
}

@Composable
private fun RegionSelector(region: Region, regions: List<Region>, onChange: (Region) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        val shape = RoundedCornerShape(16.dp)
        Row(
            Modifier
                .height(32.dp)
                .clip(shape)
                .background(Color.White)
                .border(1.dp, Border, shape)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(AppIcons.MapPin, null, tint = Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(region.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(4.dp))
            Icon(AppIcons.ChevronDown, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            regions.forEach { r ->
                DropdownMenuItem(
                    text = {
                        Text(
                            r.name,
                            fontSize = 14.sp,
                            fontWeight = if (r == region) FontWeight.Bold else FontWeight.Normal,
                            color = if (r == region) Primary else TextPrimary
                        )
                    },
                    onClick = {
                        expanded = false
                        if (r != region) onChange(r)
                    }
                )
            }
        }
    }
}

@Composable
private fun ViewToggle(view: CalendarView, onChange: (CalendarView) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(9.dp)).background(Track).padding(3.dp)
    ) {
        CalendarView.entries.forEach { v ->
            val isSelected = v == view
            Box(
                Modifier
                    .size(width = 34.dp, height = 26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable { onChange(v) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    v.label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) TextPrimary else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp)) {
        weekdayNames.forEachIndexed { i, name ->
            Text(
                name,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = when (i) {
                    0 -> Sunday
                    6 -> Primary
                    else -> TextSecondary
                }
            )
        }
    }
}

@Composable
private fun MonthGrid(
    selected: LocalDate,
    today: LocalDate,
    calendar: LifeIndexCalendar?,
    onSelect: (LocalDate) -> Unit,
) {
    val first = selected.withDayOfMonth(1)
    val gridStart = first.minusDays((first.dayOfWeek.value % 7).toLong())
    val cells = (first.dayOfWeek.value % 7) + first.lengthOfMonth()
    val rows = (cells + 6) / 7
    Column {
        repeat(rows) { r ->
            WeekRow(gridStart.plusWeeks(r.toLong()), selected.monthValue, selected, today, calendar, onSelect)
        }
    }
}

@Composable
private fun WeekRow(
    start: LocalDate,
    month: Int,
    selected: LocalDate,
    today: LocalDate,
    calendar: LifeIndexCalendar?,
    onSelect: (LocalDate) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        repeat(7) { i ->
            val date = start.plusDays(i.toLong())
            if (date.monthValue == month) {
                val availability = LifeIndexRepository.availability(date, today, calendar)
                DayCell(date, date == today, date == selected, calendar?.days?.get(date), availability) {
                    onSelect(date)
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    day: DailyLifeIndex?,
    availability: DayAvailability,
    onClick: () -> Unit,
) {
    val numberColor = when {
        isToday -> Color.White
        availability == DayAvailability.NO_DATA -> TextTertiary
        isSelected -> Primary
        date.dayOfWeek.value == 7 -> Sunday
        date.dayOfWeek.value == 6 -> Primary
        else -> TextPrimary
    }
    Column(
        Modifier
            .weight(1f)
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isToday -> Primary
                        isSelected -> PrimarySoft
                        else -> Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${date.dayOfMonth}",
                fontSize = 14.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = numberColor
            )
        }
        when {
            day != null -> {
                Spacer(Modifier.height(3.dp))
                Box(Modifier.size(7.dp).clip(CircleShape).background(day.overall.dot))
            }
            availability == DayAvailability.NO_DATA -> {
                Text("–", fontSize = 12.sp, color = TextTertiary)
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Level.entries.forEach { level ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(level.dot))
                Spacer(Modifier.width(5.dp))
                Text(level.label, fontSize = 11.sp, color = TextSecondary)
            }
        }
        Text("–  데이터 없음 (API 제공 범위 밖)", fontSize = 11.sp, color = TextTertiary)
    }
}

@Composable
private fun DetailCard(
    date: LocalDate,
    today: LocalDate,
    day: DailyLifeIndex?,
    availability: DayAvailability,
    failure: String?,
    source: String,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${date.monthValue}월 ${date.dayOfMonth}일 (${date.koreanDayOfWeek()})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            if (date == today) {
                Spacer(Modifier.width(8.dp))
                StatusBadge("오늘", PrimarySoft, Primary)
            }
            Spacer(Modifier.weight(1f))
            if (day != null) {
                Text("${day.issuedAt.shortText(today)} 발표 기준", fontSize = 11.sp, color = TextTertiary)
            }
        }

        if (day != null) {
            day.items.forEachIndexed { i, item ->
                if (i > 0) HorizontalLine(Modifier.padding(horizontal = 16.dp))
                IndexRow(item)
            }
        } else {
            val message = when {
                availability == DayAvailability.PAST -> "지난 날짜의 생활수치는 제공되지 않아요"
                failure != null -> "$failure\n새로고침을 눌러 다시 시도해 주세요"
                availability == DayAvailability.AVAILABLE -> "불러오는 중…"
                else -> "데이터 없음 · 기상청은 오늘부터 ${LifeIndexRepository.FORECAST_DAYS}일까지만 예보를 제공해요"
            }
            Text(
                message,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 28.dp),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = TextTertiary
            )
        }

        Text(
            "출처: $source",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
            fontSize = 11.sp,
            color = TextTertiary
        )
    }
}

@Composable
private fun IndexRow(item: IndexItem) {
    Row(
        Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.name, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text(item.value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(10.dp))
        LevelBadge(item.level)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun CalendarScreenPreview() {
    MoemiTheme { CalendarScreen(LifeIndexRepository.DEFAULT_REGION, onRegionChange = {}) }
}
