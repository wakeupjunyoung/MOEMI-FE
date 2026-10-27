package com.example.moemi.data

import androidx.compose.ui.graphics.Color
import com.example.moemi.data.remote.IndexResponse
import com.example.moemi.data.remote.LifeIndexStatus
import com.example.moemi.data.remote.MoemiApi
import com.example.moemi.ui.theme.CautionBg
import com.example.moemi.ui.theme.CautionDot
import com.example.moemi.ui.theme.CautionFg
import com.example.moemi.ui.theme.GoodBg
import com.example.moemi.ui.theme.GoodDot
import com.example.moemi.ui.theme.GoodFg
import com.example.moemi.ui.theme.NormalBg
import com.example.moemi.ui.theme.NormalDot
import com.example.moemi.ui.theme.NormalFg
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** 사용자가 이해하기 쉬운 상태 단계 (기능 1). 서버 Level과 이름이 같고, 좋은 것부터 나쁜 순서다. */
@Serializable
enum class Level(val label: String, val dot: Color, val badgeBg: Color, val badgeFg: Color) {
    GOOD("양호", GoodDot, GoodBg, GoodFg),
    NORMAL("보통", NormalDot, NormalBg, NormalFg),
    CAUTION("주의", CautionDot, CautionBg, CautionFg),
}

@Serializable
enum class DayAvailability { PAST, AVAILABLE, NO_DATA }

/** 생활수치를 조회하는 지역. code는 기상청 지역번호 */
data class Region(val code: String, val name: String) : java.io.Serializable

data class IndexItem(val name: String, val value: String, val level: Level)

/** 하루치 생활기상지수 */
data class DailyLifeIndex(
    val date: LocalDate,
    val items: List<IndexItem>,
    /** 캘린더 셀에 찍을 대표 상태 = 가장 나쁜 단계 */
    val overall: Level,
    /** 기상청 발표 기준 시각 */
    val issuedAt: LocalDateTime,
)

/** 한 지역의 날짜별 생활기상지수 */
data class LifeIndexCalendar(
    val region: Region,
    val status: LifeIndexStatus,
    val source: String,
    /** 서버가 기상청에서 마지막으로 받아온 시각. FAILED면 null */
    val fetchedAt: LocalDateTime?,
    val availability: Map<LocalDate, DayAvailability>,
    val days: Map<LocalDate, DailyLifeIndex>,
)

/** 기상청 생활기상지수를 MOEMI-BE(/api/life-index)를 거쳐 가져온다. */
object LifeIndexRepository {
    /** 오늘 포함 예보를 제공하는 일수 (이후 날짜는 "데이터 없음") */
    const val FORECAST_DAYS = 4

    val DEFAULT_REGION = Region("1233000000", "광주 광산구")

    suspend fun regions(): List<Region> = MoemiApi.service.regions().map { Region(it.code, it.name) }

    suspend fun load(region: Region, refresh: Boolean = false): LifeIndexCalendar {
        val res = MoemiApi.service.lifeIndex(region.code, refresh = refresh)
        val days = res.days
            .filter { it.availability == DayAvailability.AVAILABLE && it.indices.isNotEmpty() }
            .associate { d ->
                val date = LocalDate.parse(d.date)
                val items = d.indices.map { IndexItem(it.name, it.displayValue(), it.level) }
                date to DailyLifeIndex(
                    date = date,
                    items = items,
                    overall = d.overallLevel ?: items.maxOf { it.level },
                    issuedAt = d.indices.maxOf { LocalDateTime.parse(it.issuedAt) },
                )
            }
        return LifeIndexCalendar(
            region = Region(res.regionCode, res.regionName),
            status = res.status,
            source = res.source,
            fetchedAt = res.fetchedAt?.let(LocalDateTime::parse),
            availability = res.days.associate { LocalDate.parse(it.date) to it.availability },
            days = days,
        )
    }

    /** 서버 응답에 있는 날짜는 서버 값을, 없는 날짜(다른 달 등)는 예보 일수로 판단한다. */
    fun availability(date: LocalDate, today: LocalDate, calendar: LifeIndexCalendar?): DayAvailability =
        calendar?.availability?.get(date) ?: when {
            date.isBefore(today) -> DayAvailability.PAST
            ChronoUnit.DAYS.between(today, date) < FORECAST_DAYS -> DayAvailability.AVAILABLE
            else -> DayAvailability.NO_DATA
        }
}

/** "7 · 높음"처럼 값과 기상청 등급을 함께 보여준다. 정수 값은 소수점 없이 쓴다. */
private fun IndexResponse.displayValue(): String {
    val number = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    return "$number${unit.orEmpty()} · $grade"
}
