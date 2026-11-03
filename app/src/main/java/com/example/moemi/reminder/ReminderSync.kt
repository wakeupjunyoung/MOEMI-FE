package com.example.moemi.reminder

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 서버의 예정 알림을 받아 휴대폰에 예약한다.
 * 앱을 열 때와 일정을 추가·수정·삭제한 뒤에 부른다. 서버에 못 붙으면 마지막으로 받은 목록을 그대로 둔다.
 */
object ReminderSync {

    data class State(
        val reminders: List<UpcomingReminder> = emptyList(),
        val syncedAt: LocalDateTime? = null,
        val syncing: Boolean = false,
        /** 마지막 시도가 실패했다면 그 이유 */
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    /** 저장해 둔 목록으로 화면 상태를 채운다 (네트워크 없이). */
    fun load(context: Context) {
        _state.value = _state.value.copy(
            reminders = ReminderStore.reminders(context),
            syncedAt = ReminderStore.syncedAt(context),
        )
    }

    suspend fun sync(context: Context) {
        val appContext = context.applicationContext
        _state.value = _state.value.copy(syncing = true)
        try {
            val reminders = ReminderApi.fetchUpcoming(ReminderStore.deviceId(appContext))
            val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
            ReminderStore.saveSynced(appContext, reminders, now)
            ReminderScheduler.apply(appContext, reminders)
            _state.value = State(reminders = reminders, syncedAt = now)
        } catch (e: IOException) {
            // 서버에 못 붙어도 예약해 둔 알림은 그대로 울린다.
            ReminderScheduler.apply(appContext, ReminderStore.reminders(appContext))
            _state.value = State(
                reminders = ReminderStore.reminders(appContext),
                syncedAt = ReminderStore.syncedAt(appContext),
                error = "서버에 연결하지 못했어요",
            )
        }
    }
}
