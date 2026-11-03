package com.example.moemi.feature.bus

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moemi.ui.components.AppCard
import com.example.moemi.ui.components.AppIcons
import com.example.moemi.ui.components.DashedAddButton
import com.example.moemi.ui.components.HorizontalLine
import com.example.moemi.ui.theme.Background
import com.example.moemi.ui.theme.Border
import com.example.moemi.ui.theme.BorderStrong
import com.example.moemi.ui.theme.CautionDot
import com.example.moemi.ui.theme.EndedPin
import com.example.moemi.ui.theme.MoemiTheme
import com.example.moemi.ui.theme.NormalFg
import com.example.moemi.ui.theme.Primary
import com.example.moemi.ui.theme.TextPrimary
import com.example.moemi.ui.theme.TextSecondary
import com.example.moemi.ui.theme.TextTertiary
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TrunkGreen = Color(0xFF2E9E5B)
private val StarYellow = Color(0xFFF5B400)

/** Badge color of a bus route; ended routes are greyed out. */
enum class RouteColor(val color: Color) { TRUNK(TrunkGreen), BRANCH(Primary), ENDED(EndedPin) }

/** One bus arriving at a favorite stop. [minutes] is null when the route has finished for the day. */
data class BusArrivalUi(val routeNo: String, val color: RouteColor, val minutes: Int?, val stopsAway: Int?)

/** A favorite shown on the bus tab: a stop with its routes, or a single route in one direction. */
sealed interface BusFavoriteUi {
    val id: Long

    data class Stop(
        override val id: Long,
        val name: String,
        val description: String,
        val arrivals: List<BusArrivalUi>,
        val failed: Boolean = false,
    ) : BusFavoriteUi

    data class Route(
        override val id: Long,
        val routeNo: String,
        val color: RouteColor,
        val direction: String,
        val description: String,
        val baseStop: String,
        val nextMinutes: Int,
    ) : BusFavoriteUi
}

/** Sample favorites until the transit API is connected. */
val sampleBusFavorites = listOf(
    BusFavoriteUi.Stop(
        1, "광주소프트웨어마이스터고", "하남대로 방면 · 정류장 5123",
        listOf(
            BusArrivalUi("송정98", RouteColor.TRUNK, 3, 2),
            BusArrivalUi("첨단09", RouteColor.BRANCH, 12, 7),
            BusArrivalUi("1000", RouteColor.ENDED, null, null),
        )
    ),
    BusFavoriteUi.Route(2, "송정98", RouteColor.TRUNK, "첨단 방면", "광주송정역 → 첨단지구 · 배차 12분", "광산구청", 8),
    BusFavoriteUi.Stop(3, "수완지구 중앙", "우산동 방면 · 정류장 5210", emptyList(), failed = true),
)

private val ClockFormat = DateTimeFormatter.ofPattern("HH:mm:ss")

/** 기능 4 — 버스 노선·정류장 즐겨찾기 */
@Composable
fun BusScreen(modifier: Modifier = Modifier) {
    val favorites = remember { mutableStateListOf(*sampleBusFavorites.toTypedArray()) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var fetchedAt by remember { mutableStateOf(LocalTime.now()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        // 헤더
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, top = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("버스", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            Text(
                if (editing) "완료" else "편집",
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { editing = !editing }.padding(6.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Primary
            )
        }

        Spacer(Modifier.height(14.dp))
        SearchBox(query, onChange = { query = it })

        // 즐겨찾기 헤더
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, top = 22.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("즐겨찾기", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            Text(
                "${fetchedAt.format(ClockFormat)} 조회 · 30초마다 갱신",
                fontSize = 11.sp,
                color = TextTertiary
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                AppIcons.Refresh,
                contentDescription = "새로고침",
                tint = Primary,
                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(50)).clickable { fetchedAt = LocalTime.now() }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            favorites.forEach { favorite ->
                FavoriteCard(
                    favorite = favorite,
                    editing = editing,
                    onRemove = { favorites.remove(favorite) },
                    onRetry = { fetchedAt = LocalTime.now() },
                )
            }
            DashedAddButton("+ 노선·정류장 추가", onClick = {})
        }
    }
}

@Composable
private fun SearchBox(query: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Border, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(AppIcons.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("노선번호 또는 정류장 검색", fontSize = 15.sp, color = TextTertiary)
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FavoriteCard(favorite: BusFavoriteUi, editing: Boolean, onRemove: () -> Unit, onRetry: () -> Unit) {
    AppCard {
        Row(Modifier.padding(start = 8.dp, end = 16.dp, top = 14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                AppIcons.DragHandle,
                contentDescription = "순서 바꾸기",
                tint = BorderStrong,
                modifier = Modifier.padding(top = 6.dp).size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                when (favorite) {
                    is BusFavoriteUi.Stop -> Text(favorite.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    is BusFavoriteUi.Route -> Row(verticalAlignment = Alignment.CenterVertically) {
                        RouteBadge(favorite.routeNo, favorite.color)
                        Spacer(Modifier.width(8.dp))
                        Text(favorite.direction, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    when (favorite) {
                        is BusFavoriteUi.Stop -> favorite.description
                        is BusFavoriteUi.Route -> favorite.description
                    },
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            if (editing) {
                Icon(
                    AppIcons.Close,
                    contentDescription = "삭제",
                    tint = CautionDot,
                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(50)).clickable(onClick = onRemove)
                )
            } else {
                Icon(AppIcons.Star, contentDescription = "즐겨찾기", tint = StarYellow, modifier = Modifier.size(20.dp))
            }
        }
        HorizontalLine(Modifier.padding(start = 32.dp, end = 16.dp, top = 14.dp))

        Column(Modifier.padding(start = 32.dp, end = 16.dp, top = 6.dp, bottom = 10.dp)) {
            when (favorite) {
                is BusFavoriteUi.Stop ->
                    if (favorite.failed) FailedRow(onRetry)
                    else favorite.arrivals.forEach { ArrivalRow(it) }
                is BusFavoriteUi.Route -> Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("다음 차 · ${favorite.baseStop} 정류장 기준", fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.weight(1f))
                    Text("${favorite.nextMinutes}분", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun ArrivalRow(arrival: BusArrivalUi) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        RouteBadge(arrival.routeNo, arrival.color)
        Spacer(Modifier.weight(1f))
        if (arrival.minutes == null) {
            Text("운행 종료", fontSize = 13.sp, color = TextTertiary)
        } else {
            // Buses within 5 minutes are highlighted.
            Text(
                "${arrival.minutes}분",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (arrival.minutes <= 5) Primary else TextPrimary
            )
            Spacer(Modifier.width(8.dp))
            Text("${arrival.stopsAway}정거장 전", fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun FailedRow(onRetry: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(AppIcons.Warning, contentDescription = null, tint = NormalFg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text("실시간 정보를 불러올 수 없어요", fontSize = 13.sp, color = NormalFg, modifier = Modifier.weight(1f))
        Text(
            "다시 시도",
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onRetry).padding(4.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
    }
}

@Composable
fun RouteBadge(routeNo: String, color: RouteColor) {
    Box(
        Modifier
            .height(22.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(color.color)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(routeNo, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun BusScreenPreview() {
    MoemiTheme { BusScreen() }
}
