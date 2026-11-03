package com.example.moemi.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moemi.ui.components.AppCard
import com.example.moemi.ui.components.AppChip
import com.example.moemi.ui.components.AppIcons
import com.example.moemi.ui.components.ClickableIcon
import com.example.moemi.ui.components.DashedAddButton
import com.example.moemi.ui.components.HorizontalLine
import com.example.moemi.ui.theme.Background
import com.example.moemi.ui.theme.MoemiTheme
import com.example.moemi.ui.theme.Primary
import com.example.moemi.ui.theme.TextPrimary
import com.example.moemi.ui.theme.TextSecondary
import com.example.moemi.ui.theme.TextTertiary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PreviewBg = Color(0xFFEEF0F4)
private val ShortTime = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormat = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)

/** 기능 6 — 일정 준비 알림 설정 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderSettingsScreen(state: ScheduleState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val target = state.schedules.first()
    var customDialog by remember { mutableStateOf(false) }
    val firstOffset = state.reminderOffsets.firstOrNull()

    Column(
        modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // 헤더
        Row(Modifier.fillMaxWidth().padding(start = 6.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            ClickableIcon(AppIcons.Back, TextPrimary, onClick = onBack)
            Text("알림 설정", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            // 알림 미리보기
            Text(
                "알림 미리보기",
                modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 8.dp),
                fontSize = 12.sp,
                color = TextSecondary
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PreviewBg)
                    .padding(14.dp)
            ) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("생", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row {
                        Text("생활도우미", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                        Text("지금", fontSize = 12.sp, color = TextTertiary)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${firstOffset?.let { offsetLabel(it).removeSuffix(" 전") + " 뒤" } ?: "곧"} " +
                            "${target.title} (${target.start.format(ShortTime)})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("${target.place ?: "장소 미정"} · 준비물 4개 남음", fontSize = 12.sp, color = TextSecondary)
                    Text("경로에 도로 공사 1건이 있어요", fontSize = 12.sp, color = TextSecondary)
                }
            }

            // 이 일정의 알림
            SectionHeader("이 일정의 알림", "${target.title} · ${target.date.format(DateFormat)} ${target.start.format(ShortTime)}")
            AppCard {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("알림 받기", fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.reminderEnabled,
                        onCheckedChange = { state.reminderEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary, checkedThumbColor = Color.White)
                    )
                }
                HorizontalLine(Modifier.padding(horizontal = 16.dp))
                Column(Modifier.padding(16.dp)) {
                    Text("알림 시점 · 여러 개 선택할 수 있어요", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(10.dp))
                    val options = (ReminderPresets + state.reminderOffsets).distinct().sortedDescending()
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        options.forEach { minutes ->
                            AppChip(
                                offsetLabel(minutes),
                                selected = state.reminderEnabled && minutes in state.reminderOffsets,
                                onClick = { if (state.reminderEnabled) state.toggleOffset(minutes) }
                            )
                        }
                        DashedAddButton(
                            "+ 직접 입력",
                            onClick = { customDialog = true },
                            enabled = state.reminderEnabled,
                            modifier = Modifier.width(96.dp).height(32.dp)
                        )
                    }
                }
            }

            // 기본 알림
            SectionHeader("기본 알림", null)
            AppCard {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            val i = ReminderPresets.indexOf(state.defaultOffset)
                            state.defaultOffset = ReminderPresets[(i + 1) % ReminderPresets.size]
                        }
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("새 일정의 기본 알림", fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text(offsetLabel(state.defaultOffset), fontSize = 14.sp, color = TextSecondary)
                    Icon(AppIcons.ChevronRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                "각 일정에서 따로 바꿀 수 있어요",
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                fontSize = 11.sp,
                color = TextTertiary
            )

            // 예정된 알림
            SectionHeader("예정된 알림", null)
            AppCard {
                val planned = state.plannedReminders()
                if (planned.isEmpty()) {
                    Text("예정된 알림이 없어요", modifier = Modifier.padding(16.dp), fontSize = 14.sp, color = TextSecondary)
                }
                planned.forEachIndexed { i, reminder ->
                    if (i > 0) HorizontalLine(Modifier.padding(horizontal = 16.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(AppIcons.Bell, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            reminder.time.format(ShortTime),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("${reminder.title} · ${reminder.offsetLabel}", fontSize = 14.sp, color = TextPrimary)
                    }
                }
            }
            Text(
                "알림 권한이 꺼져 있어도 여기서 확인할 수 있어요",
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = TextTertiary
            )
        }
    }

    if (customDialog) {
        CustomOffsetDialog(
            onDismiss = { customDialog = false },
            onConfirm = { minutes ->
                if (minutes !in state.reminderOffsets) state.toggleOffset(minutes)
                customDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String?) {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
        trailing?.let { Text(it, fontSize = 11.sp, color = TextTertiary) }
    }
}

@Composable
private fun CustomOffsetDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val minutes = text.toIntOrNull()?.takeIf { it in 1..10080 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("알림 시점 직접 입력") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(5) },
                label = { Text("몇 분 전에 알릴까요?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(onClick = { minutes?.let(onConfirm) }, enabled = minutes != null) { Text("추가") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReminderSettingsPreview() {
    MoemiTheme { ReminderSettingsScreen(ScheduleState(LocalDate.now()), onBack = {}) }
}
