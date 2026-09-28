package com.example.moemi.feature.checklist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.moemi.data.Recommendation

data class Schedule(val id: String, val title: String, val time: String)

/** reason이 있으면 앱이 추천한 항목, 없으면 사용자가 직접 추가한 항목 */
data class CheckItem(
    val id: Long,
    val scheduleId: String,
    val name: String,
    val checked: Boolean = false,
    val reason: String? = null,
) {
    val recommended: Boolean get() = reason != null
}

/** 기능 2 체크리스트의 상태와 규칙 */
class ChecklistState(val schedules: List<Schedule>) {
    val items = mutableStateListOf<CheckItem>()

    var recommendationFailed by mutableStateOf(false)
        private set

    // 사용자가 지운 추천 항목 (일정 id, 이름) — 같은 조건에서 다시 자동 추가하지 않는다.
    private val dismissed = mutableSetOf<Pair<String, String>>()
    private var nextId = 1L

    fun scheduleTitle(id: String) = schedules.firstOrNull { it.id == id }?.title ?: "연결된 일정 없음"

    /** 생활수치가 바뀌면 추천 항목만 갱신한다. 직접 추가·수정한 항목은 건드리지 않는다. */
    fun applyRecommendations(recs: List<Recommendation>?) {
        if (recs == null) {
            recommendationFailed = true
            return
        }
        recommendationFailed = false
        val names = recs.map { it.name }.toSet()
        items.removeAll { it.recommended && it.name !in names }
        schedules.forEach { s ->
            recs.forEach { r ->
                val exists = items.any { it.scheduleId == s.id && it.recommended && it.name == r.name }
                if (!exists && (s.id to r.name) !in dismissed) {
                    items.add(CheckItem(nextId++, s.id, r.name, reason = r.reason))
                }
            }
        }
    }

    fun add(scheduleId: String, name: String) {
        items.add(CheckItem(nextId++, scheduleId, name.trim()))
    }

    fun setChecked(ids: List<Long>, checked: Boolean) = update(ids) { it.copy(checked = checked) }

    /** 추천 항목 이름을 바꾸면 사용자 항목으로 바뀌어, 추천 갱신 때 지워지지 않는다. */
    fun rename(ids: List<Long>, name: String) {
        items.filter { it.id in ids && it.recommended }.forEach { dismissed += it.scheduleId to it.name }
        update(ids) { it.copy(name = name.trim(), reason = null) }
    }

    fun delete(ids: List<Long>) {
        items.filter { it.id in ids && it.recommended }.forEach { dismissed += it.scheduleId to it.name }
        items.removeAll { it.id in ids }
    }

    private fun update(ids: List<Long>, transform: (CheckItem) -> CheckItem) {
        for (i in items.indices) {
            if (items[i].id in ids) items[i] = transform(items[i])
        }
    }

    companion object {
        /** 디자인 시안과 같은 예시 데이터 */
        fun sample(recs: List<Recommendation>?) = ChecklistState(
            listOf(
                Schedule("club", "동아리 모임", "14:00"),
                Schedule("academy", "학원", "18:30"),
            )
        ).apply {
            applyRecommendations(recs)
            listOf("노트북", "충전기", "교통카드").forEach { add("club", it) }
            listOf("교재", "필통").forEach { add("academy", it) }
            setChecked(
                items.filter { it.scheduleId == "club" && it.name in setOf("선크림", "모자", "노트북") }.map { it.id },
                true
            )
        }
    }
}
