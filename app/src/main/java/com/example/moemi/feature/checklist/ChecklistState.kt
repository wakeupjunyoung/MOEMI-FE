package com.example.moemi.feature.checklist

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.moemi.data.remote.AddItemRequest
import com.example.moemi.data.remote.ChecklistResponse
import com.example.moemi.data.remote.ItemSource
import com.example.moemi.data.remote.MoemiApi
import com.example.moemi.data.remote.OpenChecklistRequest
import com.example.moemi.data.remote.RecommendationStatus
import com.example.moemi.data.remote.UpdateItemRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/** id는 서버 체크리스트의 scheduleId로 그대로 보낸다. */
data class Schedule(val id: Long, val title: String, val time: String)

/** recommended면 앱이 생활수치로 추천한 항목, 아니면 사용자가 직접 추가·수정한 항목 */
data class CheckItem(
    val id: Long,
    val checklistId: Long,
    val scheduleId: Long,
    val name: String,
    val checked: Boolean,
    val recommended: Boolean,
    val reason: String?,
)

/**
 * 기능 2 체크리스트의 상태. 일정마다 서버 체크리스트(/api/checklists) 하나를 열어 두고,
 * 바꿀 때마다 서버가 돌려준 체크리스트 전체로 다시 그린다. 추천·삭제한 추천 항목 기억은 서버가 맡는다.
 */
class ChecklistState(val schedules: List<Schedule>, private val scope: CoroutineScope) {
    private val api get() = MoemiApi.service

    /** 일정 id → 그 일정의 체크리스트 */
    private var checklists by mutableStateOf<Map<Long, ChecklistResponse>>(emptyMap())

    val items: List<CheckItem> by derivedStateOf {
        schedules.flatMap { s ->
            val checklist = checklists[s.id] ?: return@flatMap emptyList()
            checklist.items.map {
                CheckItem(it.id, checklist.id, s.id, it.name, it.checked, it.source == ItemSource.RECOMMENDED, it.reason)
            }
        }
    }

    val loaded: Boolean get() = checklists.isNotEmpty()

    val recommendationFailed: Boolean
        get() = checklists.values.any { it.recommendationStatus == RecommendationStatus.FAILED }

    var loading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    // 요청을 순서대로 보내서, 늦게 도착한 응답이 최신 상태를 덮어쓰지 않게 한다.
    private val mutex = Mutex()
    private var date: LocalDate? = null
    private var regionCode: String? = null

    fun scheduleTitle(id: Long) = schedules.firstOrNull { it.id == id }?.title ?: "연결된 일정 없음"

    /** 날짜의 일정별 체크리스트를 연다. 처음 여는 일정이면 서버가 그 지역 생활수치로 추천 항목을 넣어 만든다. */
    fun load(date: LocalDate, regionCode: String) {
        this.date = date
        this.regionCode = regionCode
        request {
            loading = true
            try {
                checklists = schedules.associate { s ->
                    val opened = api.openChecklist(OpenChecklistRequest(date.toString(), s.id, regionCode))
                    // 예전에 다른 지역 기준으로 만든 체크리스트면 지금 지역으로 추천을 다시 받는다.
                    s.id to if (opened.regionCode != regionCode) api.refreshRecommendations(opened.id, regionCode) else opened
                }
            } finally {
                loading = false
            }
        }
    }

    fun reload() {
        load(date ?: return, regionCode ?: return)
    }

    fun add(scheduleId: Long, name: String) {
        val checklist = checklists[scheduleId]
        if (checklist == null) {
            error = "체크리스트를 아직 불러오지 못했어요."
            return
        }
        request { apply(api.addItem(checklist.id, AddItemRequest(name.trim()))) }
    }

    fun setChecked(ids: List<Long>, checked: Boolean) {
        // 체크는 바로 보이도록 먼저 그려 두고, 서버가 거절하면 되돌린다.
        val before = checklists
        checklists = before.mapValues { (_, c) ->
            c.copy(items = c.items.map { if (it.id in ids) it.copy(checked = checked) else it })
        }
        request(onError = { checklists = before }) {
            forEachItem(ids) { apply(api.updateItem(it.checklistId, it.id, UpdateItemRequest(checked = checked))) }
        }
    }

    /** 추천 항목 이름을 바꾸면 서버에서 사용자 항목으로 바뀌어, 추천 갱신 때 지워지지 않는다. */
    fun rename(ids: List<Long>, name: String) = request {
        forEachItem(ids) { apply(api.updateItem(it.checklistId, it.id, UpdateItemRequest(name = name.trim()))) }
    }

    /** 지운 추천 항목은 서버가 기억해서 같은 체크리스트에 다시 추천하지 않는다. */
    fun delete(ids: List<Long>) = request {
        forEachItem(ids) { apply(api.removeItem(it.checklistId, it.id)) }
    }

    private suspend fun forEachItem(ids: List<Long>, action: suspend (CheckItem) -> Unit) {
        items.filter { it.id in ids }.forEach { action(it) }
    }

    private fun apply(updated: ChecklistResponse) {
        checklists = checklists.mapValues { (_, c) -> if (c.id == updated.id) updated else c }
    }

    private fun request(onError: () -> Unit = {}, block: suspend () -> Unit) {
        scope.launch {
            mutex.withLock {
                try {
                    block()
                    error = null
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error = MoemiApi.messageOf(e)
                    onError()
                }
            }
        }
    }

    companion object {
        /** 디자인 시안의 예시 일정. 일정 API(기능 5)가 생기면 서버 일정으로 바꾼다. */
        val sampleSchedules = listOf(
            Schedule(1, "동아리 모임", "14:00"),
            Schedule(2, "학원", "18:30"),
        )
    }
}
