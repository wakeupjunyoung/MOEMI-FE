package com.example.moemi.feature.checklist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moemi.ui.components.AppCard
import com.example.moemi.ui.components.AppChip
import com.example.moemi.ui.components.AppIcons
import com.example.moemi.ui.components.ClickableIcon
import com.example.moemi.ui.components.DashedAddButton
import com.example.moemi.ui.components.HorizontalLine
import com.example.moemi.ui.components.StatusBadge
import com.example.moemi.ui.theme.Background
import com.example.moemi.ui.theme.BorderStrong
import com.example.moemi.ui.theme.CautionBg
import com.example.moemi.ui.theme.CautionFg
import com.example.moemi.ui.theme.EndedBg
import com.example.moemi.ui.theme.MoemiTheme
import com.example.moemi.ui.theme.Primary
import com.example.moemi.ui.theme.TextPrimary
import com.example.moemi.ui.theme.TextSecondary
import com.example.moemi.ui.theme.TextTertiary
import com.example.moemi.ui.theme.Track

/** 통합 보기. 일정 id는 1부터라 겹치지 않는다. */
private const val ALL = -1L

/** 화면에 그릴 한 줄. 통합 보기에서는 여러 일정의 같은 추천 항목이 한 줄로 묶인다. */
private data class RowUi(
    val key: String,
    val name: String,
    val checked: Boolean,
    val reason: String?,
    val ids: List<Long>,
    val tag: String?,
)

private data class NameDialog(val title: String, val initial: String, val onConfirm: (String) -> Unit)

/** 기능 2 — 외출 전 준비물 체크리스트 */
@Composable
fun ChecklistScreen(state: ChecklistState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var scope by rememberSaveable { mutableStateOf(state.schedules.first().id) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<NameDialog?>(null) }

    val scoped = if (scope == ALL) state.items.toList() else state.items.filter { it.scheduleId == scope }
    val recommended = scoped.filter { it.recommended }.groupBy { it.name }.map { (name, group) ->
        RowUi("r-$name", name, group.all { it.checked }, group.first().reason, group.map { it.id }, null)
    }
    val custom = scoped.filter { !it.recommended }.map {
        RowUi("c-${it.id}", it.name, it.checked, null, listOf(it.id), if (scope == ALL) state.scheduleTitle(it.scheduleId) else null)
    }
    val total = recommended.size + custom.size
    val done = (recommended + custom).count { it.checked }

    val onToggle: (RowUi) -> Unit = { state.setChecked(it.ids, !it.checked) }
    val onDelete: (RowUi) -> Unit = { state.delete(it.ids) }
    val onRename: (RowUi) -> Unit = { row ->
        dialog = NameDialog("준비물 이름 수정", row.name) { state.rename(row.ids, it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // 헤더
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 20.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClickableIcon(AppIcons.Back, TextPrimary, onClick = onBack)
            Text("외출 준비물", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                if (editing) "완료" else "편집",
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { editing = !editing }.padding(6.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Primary
            )
        }

        // 일정 선택
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.schedules.forEach { s ->
                AppChip("${s.title} ${s.time}", scope == s.id, { scope = s.id })
            }
            AppChip("통합 보기", scope == ALL, { scope = ALL })
        }

        state.error?.let { message ->
            ErrorBanner(message, onRetry = state::reload, Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp))
        }

        // 진행률
        ProgressCard(done, total, Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp))

        // 추천 항목
        SectionTitle("추천 항목", "오늘 생활수치 기준")
        AppCard(Modifier.padding(horizontal = 16.dp)) {
            when {
                !state.loaded -> EmptyText(if (state.loading) "불러오는 중…" else "체크리스트를 불러오지 못했어요")
                state.recommendationFailed -> EmptyText("추천 정보를 불러오지 못했습니다.\n아래에서 준비물을 직접 추가할 수 있어요.")
                recommended.isEmpty() -> EmptyText("오늘은 추천할 준비물이 없어요")
                else -> recommended.forEachIndexed { i, row ->
                    if (i > 0) HorizontalLine(Modifier.padding(start = 50.dp, end = 16.dp))
                    ChecklistRow(row, editing, onToggle, onRename, onDelete)
                }
            }
        }

        // 직접 추가한 항목
        SectionTitle("직접 추가한 항목", "${custom.size}개")
        AppCard(Modifier.padding(horizontal = 16.dp)) {
            if (custom.isEmpty()) {
                EmptyText("직접 추가한 항목이 없어요")
            } else {
                custom.forEachIndexed { i, row ->
                    if (i > 0) HorizontalLine(Modifier.padding(start = 50.dp, end = 16.dp))
                    ChecklistRow(row, editing, onToggle, onRename, onDelete)
                }
            }
        }

        DashedAddButton(
            text = if (scope == ALL) "일정을 선택하면 준비물을 추가할 수 있어요" else "+ 준비물 추가",
            enabled = scope != ALL,
            onClick = {
                val target = scope
                dialog = NameDialog("준비물 추가", "") { state.add(target, it) }
            },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)
        )
    }

    dialog?.let { d ->
        NameInputDialog(d, onDismiss = { dialog = null })
    }
}

@Composable
private fun ProgressCard(done: Int, total: Int, modifier: Modifier = Modifier) {
    val fraction by animateFloatAsState(if (total == 0) 0f else done.toFloat() / total, label = "progress")
    val left = total - done
    AppCard(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("준비 완료 $done / $total", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                if (total > 0 && left == 0) "모두 챙겼어요" else "${left}개 남음",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
        Box(
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 18.dp)
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Track)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Primary)
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CautionBg)
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, modifier = Modifier.weight(1f), fontSize = 13.sp, color = CautionFg)
        Text(
            "다시 시도",
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onRetry).padding(8.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = CautionFg
        )
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(trailing, fontSize = 12.sp, color = TextTertiary)
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp),
        textAlign = TextAlign.Center,
        fontSize = 13.sp,
        color = TextTertiary
    )
}

@Composable
private fun ChecklistRow(
    row: RowUi,
    editing: Boolean,
    onToggle: (RowUi) -> Unit,
    onRename: (RowUi) -> Unit,
    onDelete: (RowUi) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { if (editing) onRename(row) else onToggle(row) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckBox(row.checked)
        Spacer(Modifier.width(12.dp))
        Text(
            row.name,
            modifier = Modifier.weight(1f, fill = false),
            fontSize = 15.sp,
            color = if (row.checked) TextTertiary else TextPrimary,
            textDecoration = if (row.checked) TextDecoration.LineThrough else null,
            maxLines = 1
        )
        if (editing) {
            Spacer(Modifier.width(6.dp))
            Text("수정", fontSize = 12.sp, color = TextTertiary)
        }
        Spacer(Modifier.weight(1f))
        row.tag?.let { StatusBadge(it, EndedBg, TextSecondary) }
        row.reason?.let { StatusBadge(it, CautionBg, CautionFg) }
        if (editing) {
            ClickableIcon(AppIcons.Close, TextTertiary, onClick = { onDelete(row) }, size = 20.dp)
        }
    }
}

@Composable
private fun CheckBox(checked: Boolean) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .size(22.dp)
            .clip(shape)
            .background(if (checked) Primary else Color.White)
            .then(if (checked) Modifier else Modifier.border(1.6.dp, BorderStrong, shape)),
        contentAlignment = Alignment.Center
    ) {
        if (checked) Icon(AppIcons.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun NameInputDialog(dialog: NameDialog, onDismiss: () -> Unit) {
    var text by remember(dialog) { mutableStateOf(dialog.initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(dialog.title, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text("예: 우산", color = TextTertiary) }
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = {
                    dialog.onConfirm(text)
                    onDismiss()
                }
            ) { Text("저장", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = TextSecondary) }
        }
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun ChecklistScreenPreview() {
    val scope = rememberCoroutineScope()
    val state = remember { ChecklistState(ChecklistState.sampleSchedules, scope) }
    MoemiTheme { ChecklistScreen(state, onBack = {}) }
}
