package com.example.moemi.reminder

import java.time.LocalDateTime

/**
 * 새 예정 알림 목록과 지금까지 예약해 둔 알림을 맞춰 본다.
 * 목록에서 사라진 알림(일정 삭제·수정)은 취소하고, 이미 지난 알림은 예약하지 않는다.
 */
object ReminderPlan {

    data class Plan(val toCancel: Set<String>, val toSchedule: List<UpcomingReminder>)

    fun plan(previouslyScheduled: Set<String>, reminders: List<UpcomingReminder>, now: LocalDateTime): Plan {
        val future = reminders.filter { it.fireAt.isAfter(now) }.distinctBy { it.id }
        val keep = future.map { it.id }.toSet()
        return Plan(toCancel = previouslyScheduled - keep, toSchedule = future)
    }
}
