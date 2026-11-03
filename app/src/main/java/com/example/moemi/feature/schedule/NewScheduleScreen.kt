package com.example.moemi.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moemi.ui.components.AppCard
import com.example.moemi.ui.components.AppIcons
import com.example.moemi.ui.components.ClickableIcon
import com.example.moemi.ui.components.HorizontalLine
import com.example.moemi.ui.theme.Background
import com.example.moemi.ui.theme.Border
import com.example.moemi.ui.theme.BorderStrong
import com.example.moemi.ui.theme.MoemiTheme
import com.example.moemi.ui.theme.NormalBg
import com.example.moemi.ui.theme.NormalDot
import com.example.moemi.ui.theme.NormalFg
import com.example.moemi.ui.theme.Primary
import com.example.moemi.ui.theme.PrimarySoft
import com.example.moemi.ui.theme.TextPrimary
import com.example.moemi.ui.theme.TextSecondary
import com.example.moemi.ui.theme.TextTertiary
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TimeFormat = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
private val ShortTime = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormat = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)

/** Result the AI parser would return for a sentence. Fixed sample until the parse API is connected. */
private data class Draft(
    val title: String,
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime?,
    val place: String,
    val mode: TravelMode,
    val travelMinutes: Int,
    val prepMinutes: Int,
)

private fun sampleDraft() = Draft(
    "동아리 모임", LocalDate.now().plusDays(1), LocalTime.of(14, 0), null, "광주송정역", TravelMode.BUS, 40, 30
)

/** 기능 5B — 새 일정 (자연어 입력 → AI 정리) */
@Composable
fun NewScheduleScreen(
    state: ScheduleState,
    onBack: () -> Unit,
    onOpenReminder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sentence by rememberSaveable { mutableStateOf("내일 오후 2시에 광주송정역에서\n동아리 모임") }
    var draft by remember { mutableStateOf<Draft?>(sampleDraft()) }

    Column(modifier.fillMaxSize().background(Background).statusBarsPadding()) {
        // 헤더
        Row(
            Modifier.fillMaxWidth().padding(start = 6.dp, end = 20.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClickableIcon(AppIcons.Back, TextPrimary, onClick = onBack)
            Text("새 일정", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            Text(
                "취소",
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onBack).padding(6.dp),
                fontSize = 14.sp,
                color = TextSecondary
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "말로 입력하기",
                modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 8.dp),
                fontSize = 12.sp,
                color = TextSecondary
            )
            SentenceInput(sentence, onChange = { sentence = it }, onSend = { draft = sampleDraft() })

            draft?.let { d ->
                Spacer(Modifier.height(16.dp))
                DraftCard(
                    draft = d,
                    onFillEnd = { draft = d.copy(end = d.start.plusMinutes(90)) },
                    onEditTravel = {
                        draft = if (d.mode == TravelMode.BUS) d.copy(mode = TravelMode.WALK, travelMinutes = 55)
                        else d.copy(mode = TravelMode.BUS, travelMinutes = 40)
                    },
                    onEditPrep = { draft = d.copy(prepMinutes = if (d.prepMinutes >= 60) 10 else d.prepMinutes + 10) },
                )
            }

            Spacer(Modifier.height(12.dp))
            AppCard {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onOpenReminder).padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(AppIcons.Bell, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("알림", fontSize = 14.sp, color = TextPrimary)
                    Spacer(Modifier.weight(1f))
                    Text(state.reminderSummary(), fontSize = 13.sp, color = TextSecondary)
                    Icon(AppIcons.ChevronRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // 하단 버튼
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(shape)
                    .background(Color.White)
                    .border(1.dp, BorderStrong, shape)
                    .clickable { draft = null },
                contentAlignment = Alignment.Center
            ) {
                Text("직접 입력", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            val canSave = draft?.end != null
            Box(
                Modifier
                    .weight(1.65f)
                    .height(52.dp)
                    .clip(shape)
                    .background(if (canSave) Primary else Primary.copy(alpha = 0.4f))
                    .clickable(enabled = canSave) {
                        draft?.let { d ->
                            state.add(
                                ScheduleUi(
                                    id = 0, title = d.title, date = d.date, start = d.start, end = d.end!!,
                                    place = d.place, tone = ScheduleTone.PRIMARY,
                                    prepMinutes = d.prepMinutes, travelMinutes = d.travelMinutes,
                                    travelLabel = d.mode.label,
                                )
                            )
                        }
                        onBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("확인 후 저장", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun SentenceInput(value: String, onChange: (String) -> Unit, onSend: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.5.dp, Primary, shape)
            .padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, color = TextPrimary),
            modifier = Modifier.fillMaxWidth().padding(end = 70.dp, bottom = 22.dp)
        )
        Row(Modifier.align(Alignment.BottomEnd), verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Mic, contentDescription = "음성 입력", tint = TextTertiary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(Primary).clickable(onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                Icon(AppIcons.ArrowUp, contentDescription = "정리하기", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun DraftCard(draft: Draft, onFillEnd: () -> Unit, onEditTravel: () -> Unit, onEditPrep: () -> Unit) {
    AppCard {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Sparkle, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("AI가 이렇게 정리했어요", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Spacer(Modifier.height(4.dp))
            Text("저장하기 전에 확인하고 고칠 수 있어요", fontSize = 11.sp, color = TextSecondary)
            Spacer(Modifier.height(6.dp))

            DraftRow("제목") { Value(draft.title) }
            DraftRow("날짜") { Value(draft.date.format(DateFormat)) }
            DraftRow("시작") { Value(draft.start.format(TimeFormat)) }
            DraftRow("종료") {
                if (draft.end == null) MissingValue(onFillEnd) else Value(draft.end.format(TimeFormat))
            }
            DraftRow("장소") { Value(draft.place) }
            DraftRow("이동 시간", onEdit = onEditTravel) { Value("약 ${draft.travelMinutes}분 · ${draft.mode.label}") }
            DraftRow("준비 시간", onEdit = onEditPrep, divider = false) { Value("${draft.prepMinutes}분") }

            val depart = draft.start.minusMinutes(draft.travelMinutes.toLong())
            val prep = depart.minusMinutes(draft.prepMinutes.toLong())
            Text(
                "${prep.format(ShortTime)} 준비 시작 · ${depart.format(ShortTime)} 출발하면 늦지 않아요",
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimarySoft)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                fontSize = 12.sp,
                color = Primary
            )
        }
    }
}

@Composable
private fun DraftRow(
    label: String,
    onEdit: (() -> Unit)? = null,
    divider: Boolean = true,
    value: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 46.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(76.dp), fontSize = 12.sp, color = TextSecondary)
        Box(Modifier.weight(1f)) { value() }
        if (onEdit != null) {
            Text(
                "수정",
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onEdit).padding(4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
    }
    if (divider) HorizontalLine(color = Border)
}

@Composable
private fun Value(text: String) = Text(text, fontSize = 15.sp, color = TextPrimary)

@Composable
private fun MissingValue(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(NormalBg)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    color = NormalDot,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text("입력이 필요해요", fontSize = 14.sp, color = NormalFg)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NewSchedulePreview() {
    MoemiTheme { NewScheduleScreen(ScheduleState(LocalDate.now()), onBack = {}, onOpenReminder = {}) }
}
