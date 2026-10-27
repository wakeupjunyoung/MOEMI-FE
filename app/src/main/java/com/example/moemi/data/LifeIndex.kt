package com.example.moemi.data

import androidx.compose.ui.graphics.Color
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
import java.time.temporal.ChronoUnit

/** 사용자가 이해하기 쉬운 상태 단계 (기능 1). 서버 Level과 이름이 같고, 좋은 것부터 나쁜 순서다. */
@Serializable
enum class Level(val label: String, val dot: Color, val badgeBg: Color, val badgeFg: Color) {
    GOOD("양호", GoodDot, GoodBg, GoodFg),
    NORMAL("보통", NormalDot, NormalBg, NormalFg),
    CAUTION("주의", CautionDot, CautionBg, CautionFg),
}

/** 꽃가루·대기정체처럼 "낮음~매우높음"으로 오는 지수 */
enum class Grade(val label: String, val level: Level) {
    LOW("낮음", Level.GOOD),
    MEDIUM("보통", Level.NORMAL),
    HIGH("높음", Level.CAUTION),
    VERY_HIGH("매우높음", Level.CAUTION),
}

data class IndexItem(val name: String, val value: String, val level: Level)

/** 하루치 생활기상지수 */
data class DailyLifeIndex(
    val date: LocalDate,
    val feelsLike: Int,
    val uv: Int,
    val pollen: Grade,
    val stagnation: Grade,
    val issuedAt: String,
) {
    val uvLevel: Level
        get() = when {
            uv <= 2 -> Level.GOOD
            uv <= 5 -> Level.NORMAL
            else -> Level.CAUTION
        }

    private val uvGrade: String
        get() = when {
            uv <= 2 -> "낮음"
            uv <= 5 -> "보통"
            uv <= 7 -> "높음"
            uv <= 10 -> "매우높음"
            else -> "위험"
        }

    val feelsLevel: Level
        get() = when {
            feelsLike <= 0 || feelsLike >= 31 -> Level.CAUTION
            feelsLike >= 25 -> Level.NORMAL
            else -> Level.GOOD
        }

    val items: List<IndexItem>
        get() = listOf(
            IndexItem("자외선지수", "$uv · $uvGrade", uvLevel),
            IndexItem("체감온도", "${feelsLike}℃", feelsLevel),
            IndexItem("꽃가루농도위험지수", pollen.label, pollen.level),
            IndexItem("대기정체지수", stagnation.label, stagnation.level),
        )

    /** 캘린더 셀에 찍을 대표 상태 = 가장 나쁜 단계 */
    val overall: Level get() = items.maxOf { it.level }
}

@Serializable
enum class DayAvailability { PAST, AVAILABLE, NO_DATA }

/**
 * 기상청 생활기상지수 조회서비스를 대신하는 예시 저장소.
 * 나중에 공공데이터포털 API를 붙일 때 load()의 내용만 바꾸면 된다.
 */
object LifeIndexRepository {
    /** 오늘 포함 예보를 제공하는 일수 (이후 날짜는 "데이터 없음") */
    const val FORECAST_DAYS = 4

    val regions = listOf("광주 광산구", "광주 북구", "광주 서구", "광주 남구", "광주 동구")

    private data class Sample(val feels: Int, val uv: Int, val pollen: Grade, val stagnation: Grade)

    private val samples = listOf(
        Sample(27, 7, Grade.HIGH, Grade.LOW),
        Sample(25, 5, Grade.MEDIUM, Grade.LOW),
        Sample(23, 2, Grade.LOW, Grade.LOW),
        Sample(24, 5, Grade.MEDIUM, Grade.LOW),
    )
    private val regionShift = listOf(0, -1, 1, 2, -2)

    fun availability(date: LocalDate, today: LocalDate): DayAvailability = when {
        date.isBefore(today) -> DayAvailability.PAST
        ChronoUnit.DAYS.between(today, date) < FORECAST_DAYS -> DayAvailability.AVAILABLE
        else -> DayAvailability.NO_DATA
    }

    fun load(region: String, today: LocalDate, issuedAt: String): Map<LocalDate, DailyLifeIndex> {
        val shift = regionShift[regions.indexOf(region).coerceAtLeast(0)]
        return samples.mapIndexed { i, s ->
            val date = today.plusDays(i.toLong())
            date to DailyLifeIndex(
                date = date,
                feelsLike = s.feels + shift,
                uv = (s.uv + shift).coerceIn(0, 11),
                pollen = s.pollen,
                stagnation = s.stagnation,
                issuedAt = issuedAt,
            )
        }.toMap()
    }
}

/** 추천 준비물과 그 근거 (기능 2) */
data class Recommendation(val name: String, val reason: String)

/** 생활수치 → 준비물 매핑 규칙. day가 null이면 조회 실패로 본다. */
fun recommendationsFor(day: DailyLifeIndex?): List<Recommendation>? {
    if (day == null) return null
    return buildList {
        if (day.uvLevel == Level.CAUTION) {
            listOf("선크림", "모자", "선글라스").forEach { add(Recommendation(it, "자외선 높음")) }
        }
        if (day.feelsLike <= 0) {
            listOf("방한용품", "핫팩").forEach { add(Recommendation(it, "체감온도 낮음")) }
        }
        if (day.pollen.level == Level.CAUTION) add(Recommendation("마스크", "꽃가루 높음"))
        if (day.stagnation.level == Level.CAUTION && none { it.name == "마스크" }) {
            add(Recommendation("마스크", "대기정체 높음"))
        }
    }
}
