package com.example.moemi.feature.hazard

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moemi.ui.components.AppChip
import com.example.moemi.ui.components.AppIcons
import com.example.moemi.ui.components.StatusBadge
import com.example.moemi.ui.theme.AccidentBg
import com.example.moemi.ui.theme.AccidentFg
import com.example.moemi.ui.theme.AccidentPin
import com.example.moemi.ui.theme.Border
import com.example.moemi.ui.theme.CautionBg
import com.example.moemi.ui.theme.CautionDot
import com.example.moemi.ui.theme.CautionFg
import com.example.moemi.ui.theme.ConstructionBg
import com.example.moemi.ui.theme.ConstructionFg
import com.example.moemi.ui.theme.ConstructionPin
import com.example.moemi.ui.theme.EndedBg
import com.example.moemi.ui.theme.EndedFg
import com.example.moemi.ui.theme.EndedPin
import com.example.moemi.ui.theme.MapBase
import com.example.moemi.ui.theme.MapPark
import com.example.moemi.ui.theme.MapParkText
import com.example.moemi.ui.theme.MapRiver
import com.example.moemi.ui.theme.MapRoadText
import com.example.moemi.ui.theme.MoemiTheme
import com.example.moemi.ui.theme.Primary
import com.example.moemi.ui.theme.PrimarySoft
import com.example.moemi.ui.theme.SheetHandle
import com.example.moemi.ui.theme.TextPrimary
import com.example.moemi.ui.theme.TextSecondary
import com.example.moemi.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class HazardType(val label: String, val short: String, val pin: Color, val badgeBg: Color, val badgeFg: Color) {
    CONSTRUCTION("공사", "공", ConstructionPin, ConstructionBg, ConstructionFg),
    CONTROL("통제", "통", CautionDot, CautionBg, CautionFg),
    ACCIDENT("사고", "사", AccidentPin, AccidentBg, AccidentFg),
}

/** x, y는 디자인(390 폭) 기준 좌표로, 핀의 뾰족한 끝 위치 */
data class Hazard(
    val id: Int,
    val type: HazardType,
    val title: String,
    val distanceM: Int,
    val period: String,
    val ended: Boolean,
    val x: Float,
    val y: Float,
)

private val sampleHazards = listOf(
    Hazard(1, HazardType.CONSTRUCTION, "하남대로 도로 굴착 공사", 350, "9.15 ~ 9.30 · 진행 중", false, 122f, 262f),
    Hazard(2, HazardType.CONTROL, "광산구청 앞 차로 통제", 620, "오늘 10:00 ~ 18:00", false, 282f, 366f),
    Hazard(3, HazardType.ACCIDENT, "교차로 추돌 사고 처리 중", 900, "13:40 발생", false, 300f, 222f),
    Hazard(4, HazardType.CONSTRUCTION, "수완동 보도 정비", 980, "9.10 ~ 9.18 · 종료됨", true, 80f, 404f),
)

private val radiusOptions = listOf(500, 1000, 2000)
private const val DESIGN_WIDTH = 390f
private val ME = Offset(200f, 300f)

private fun formatDistance(m: Int) = if (m < 1000) "${m}m" else "%.1fkm".format(m / 1000f)

/** 기능 3 — 지도 기반 주변 위험 알리미 */
@Composable
fun HazardMapScreen(modifier: Modifier = Modifier) {
    var typeFilter by rememberSaveable { mutableStateOf<HazardType?>(null) }
    var radius by rememberSaveable { mutableStateOf(1000) }
    var selectedId by rememberSaveable { mutableStateOf<Int?>(1) }
    var query by rememberSaveable { mutableStateOf("") }
    var panTarget by remember { mutableStateOf(Offset.Zero) }
    val pan by animateOffsetAsState(panTarget, label = "pan")

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val visible = sampleHazards
        .filter { typeFilter == null || it.type == typeFilter }
        .filter { it.distanceM <= radius }
        .sortedWith(compareBy<Hazard> { it.ended }.thenBy { it.distanceM })
    val activeCount = visible.count { !it.ended }

    fun focusOn(h: Hazard) {
        selectedId = h.id
        // 선택한 핀이 지도 보이는 영역 가운데쯤 오도록 이동
        panTarget = Offset(195f - h.x, 250f - (h.y - 20f))
    }

    BoxWithConstraints(modifier.fillMaxSize().background(MapBase)) {
        val scale = constraints.maxWidth / DESIGN_WIDTH

        // 지도 (핀 + 반경 + 내 위치)
        Box(
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer {
                    translationX = pan.x * scale
                    translationY = pan.y * scale
                }
        ) {
            MapCanvas(scale, radius)
            visible.forEach { h ->
                HazardPin(
                    hazard = h,
                    selected = h.id == selectedId,
                    scale = scale,
                    onClick = {
                        selectedId = h.id
                        val index = visible.indexOf(h)
                        coroutineScope.launch { listState.animateScrollToItem(index) }
                    }
                )
            }
        }

        // 검색 + 필터
        Column(Modifier.statusBarsPadding().padding(top = 10.dp)) {
            SearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = { focusManager.clearFocus() },
                onLocate = {
                    panTarget = Offset.Zero
                    focusManager.clearFocus()
                }
            )
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppChip("전체", typeFilter == null, { typeFilter = null })
                HazardType.entries.forEach { t ->
                    AppChip(t.label, typeFilter == t, { typeFilter = t })
                }
                AppChip("반경 ${formatDistance(radius)}", false, {
                    radius = radiusOptions[(radiusOptions.indexOf(radius) + 1) % radiusOptions.size]
                })
            }
        }

        // 하단 목록 시트
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(340.dp)
                .shadow(12.dp, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Color.White)
        ) {
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .align(Alignment.CenterHorizontally)
                    .size(36.dp, 5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(SheetHandle)
            )
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("주변 위험 정보 ${activeCount}건", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("갱신 14:03", fontSize = 12.sp, color = TextTertiary)
            }
            LazyColumn(Modifier.weight(1f), state = listState) {
                if (visible.isEmpty()) {
                    item {
                        Text(
                            "주변에 등록된 정보가 없습니다",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = TextTertiary
                        )
                    }
                }
                itemsIndexed(visible, key = { _, h -> h.id }) { _, h ->
                    HazardRow(h, h.id == selectedId) { focusOn(h) }
                }
                item {
                    Text(
                        "출처: 광주광역시 열린데이터 · 원문 보기",
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit, onLocate: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Border, shape)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(AppIcons.Search, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("주소·장소 검색", fontSize = 15.sp, color = TextTertiary)
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(50)).clickable(onClick = onLocate),
            contentAlignment = Alignment.Center
        ) {
            Icon(AppIcons.Locate, "내 위치", tint = Primary, modifier = Modifier.size(22.dp))
        }
    }
}

/** 실제 지도 SDK 대신 디자인의 지도 그림을 그린다. */
@Composable
private fun MapCanvas(scale: Float, radiusM: Int) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(Modifier.fillMaxSize()) {
        fun p(x: Float, y: Float) = Offset(x * scale, y * scale)
        val big = 2000f

        // 화면 밖까지 넉넉히 칠해서 지도를 움직여도 빈 곳이 안 보이게
        drawRect(MapBase, topLeft = p(-big, -big), size = Size(big * 3 * scale, big * 3 * scale))
        drawRoundRect(MapPark, topLeft = p(236f, 160f), size = Size(120f * scale, 76f * scale), cornerRadius = CornerRadius(18f * scale))

        val river = Path().apply {
            moveTo(-big * scale, 392f * scale)
            lineTo(-10f * scale, 392f * scale)
            cubicTo(120f * scale, 368f * scale, 250f * scale, 432f * scale, 400f * scale, 380f * scale)
            lineTo(big * scale, 380f * scale)
        }
        drawPath(river, MapRiver, style = Stroke(26f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

        listOf(280f to 14f, 196f to 7f, 440f to 7f).forEach { (y, w) ->
            drawLine(Color.White, p(-big, y), p(big, y), strokeWidth = w * scale)
        }
        listOf(205f to 14f, 92f to 7f, 318f to 7f).forEach { (x, w) ->
            drawLine(Color.White, p(x, -big), p(x, big), strokeWidth = w * scale)
        }

        drawText(textMeasurer, "하남대로", topLeft = p(22f, 263f),
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MapRoadText))
        drawText(textMeasurer, "수완공원", topLeft = p(248f, 217f),
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MapParkText))

        // 조회 반경 (1km = 132)
        val r = 132f * radiusM / 1000f * scale
        val me = p(ME.x, ME.y)
        drawCircle(Primary.copy(alpha = 0.07f), r, me)
        drawCircle(
            Primary, r, me,
            style = Stroke(1.2f * scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f * scale, 5f * scale)))
        )

        // 내 위치
        drawCircle(Primary.copy(alpha = 0.18f), 18f * scale, me)
        drawCircle(Color.White, 10f * scale, me)
        drawCircle(Primary, 7f * scale, me)
    }
}

@Composable
private fun HazardPin(hazard: Hazard, selected: Boolean, scale: Float, onClick: () -> Unit) {
    val radius = if (selected) 17.5f else 14f
    val widthDp = radius * 2 + 2
    val heightDp = radius * 2.571f + 2
    val color = if (hazard.ended) EndedPin else hazard.type.pin
    val letter = if (hazard.ended) "종" else hazard.type.short

    Box(
        Modifier
            .offset {
                IntOffset(
                    (hazard.x * scale - widthDp.dp.toPx() / 2).roundToInt(),
                    (hazard.y * scale - heightDp.dp.toPx()).roundToInt()
                )
            }
            .size(widthDp.dp, heightDp.dp)
            .graphicsLayer { alpha = if (hazard.ended) 0.7f else 1f }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val border = 1.dp.toPx()
            val r = size.width / 2 - border
            val cx = size.width / 2
            val cy = r + border
            val tipY = size.height - border
            val k = tipY - cy
            val path = Path().apply {
                moveTo(cx, tipY)
                cubicTo(cx - r * 0.2857f, cy + k * 0.727f, cx - r, cy + k * 0.35f, cx - r, cy)
                arcTo(Rect(cx - r, cy - r, cx + r, cy + r), 180f, 180f, false)
                cubicTo(cx + r, cy + k * 0.35f, cx + r * 0.2857f, cy + k * 0.727f, cx, tipY)
                close()
            }
            drawPath(path, color)
            drawPath(path, Color.White, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
        }
        Box(Modifier.size(widthDp.dp), contentAlignment = Alignment.Center) {
            Text(letter, fontSize = if (selected) 15.sp else 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun HazardRow(hazard: Hazard, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) PrimarySoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hazard.ended) {
                StatusBadge("종료", EndedBg, EndedFg)
            } else {
                StatusBadge(hazard.type.label, hazard.type.badgeBg, hazard.type.badgeFg)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                hazard.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (hazard.ended) TextTertiary else TextPrimary,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${formatDistance(hazard.distanceM)} · ${hazard.period}",
            fontSize = 12.sp,
            color = if (hazard.ended) TextTertiary else TextSecondary
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun HazardMapScreenPreview() {
    MoemiTheme { HazardMapScreen() }
}
