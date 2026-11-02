package com.example.moemi.data.remote

import com.example.moemi.data.DayAvailability
import com.example.moemi.data.Level
import kotlinx.serialization.Serializable

// MOEMI-BE 요청·응답 DTO. 이름과 필드는 백엔드 record와 같게 맞춘다.
// 날짜는 ISO 문자열(yyyy-MM-dd / yyyy-MM-ddTHH:mm:ss)로 오가므로 String으로 받고, 화면에 쓸 때 java.time으로 바꾼다.

/** 모든 엔드포인트 공통 에러 본문 */
@Serializable
data class ErrorResponse(val code: String, val message: String)

// ── 기능 1: 생활수치 캘린더 (/api/life-index) ──

@Serializable
data class RegionResponse(val code: String, val name: String)

@Serializable
enum class LifeIndexStatus { OK, STALE, FAILED }

@Serializable
data class LifeIndexCalendarResponse(
    val regionCode: String,
    val regionName: String,
    val status: LifeIndexStatus,
    val source: String,
    val fetchedAt: String?,
    val days: List<DayResponse>,
)

@Serializable
data class DayResponse(
    val date: String,
    val availability: DayAvailability,
    val overallLevel: Level?,
    val indices: List<IndexResponse>,
)

/** value는 그날 가장 나쁜 예보값. 정수면 소수점 없이 온다. */
@Serializable
data class IndexResponse(
    val type: String,
    val name: String,
    val value: Double,
    val unit: String?,
    val grade: String,
    val level: Level,
    val issuedAt: String,
)

// ── 기능 2: 외출 준비물 체크리스트 (/api/checklists, X-Device-Id 필요) ──

@Serializable
enum class RecommendationStatus { OK, NO_DATA, FAILED }

@Serializable
enum class ItemSource { RECOMMENDED, CUSTOM }

@Serializable
data class OpenChecklistRequest(val date: String, val scheduleId: Long?, val regionCode: String?)

@Serializable
data class AddItemRequest(val name: String)

/** 부분 수정: null인 필드는 보내지 않고, 서버도 바꾸지 않는다. */
@Serializable
data class UpdateItemRequest(val name: String? = null, val checked: Boolean? = null)

@Serializable
data class ChecklistResponse(
    val id: Long,
    val date: String,
    val scheduleId: Long?,
    val regionCode: String,
    val recommendationStatus: RecommendationStatus,
    val recommendedAt: String?,
    val totalCount: Int,
    val checkedCount: Int,
    val remainingCount: Int,
    val items: List<ChecklistItemResponse>,
)

@Serializable
data class ChecklistItemResponse(
    val id: Long,
    val name: String,
    val checked: Boolean,
    val source: ItemSource,
    val reason: String?,
)

// ── 기능 3: 주변 위험 알리미 (/api/hazards) ──

@Serializable
enum class HazardSearchStatus { OK, EMPTY, UNSUPPORTED_REGION, FAILED }

@Serializable
enum class HazardState { ACTIVE, SCHEDULED, ENDED }

@Serializable
data class HazardSearchResponse(
    val status: HazardSearchStatus,
    val source: String,
    val fetchedAt: String?,
    val radiusM: Int,
    val hazards: List<HazardResponse>,
)

/** type은 CONSTRUCTION / CONTROL / ACCIDENT / OTHER */
@Serializable
data class HazardResponse(
    val id: String,
    val type: String,
    val state: HazardState,
    val title: String,
    val description: String?,
    val lat: Double,
    val lng: Double,
    val distanceM: Int,
    val startAt: String?,
    val endAt: String?,
    val roadName: String?,
    val direction: String?,
    val blockType: String?,
    val blockedLanes: String?,
    val sourceEventType: String?,
    val sourceEventDetailType: String?,
)
