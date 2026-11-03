package com.example.moemi.reminder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class ReminderPlanTest {

    private val now = LocalDateTime.of(2026, 10, 29, 10, 0)

    @Test
    fun removedRemindersAreCancelledAndPastOnesAreNotScheduled() {
        val upcoming = reminder("s1-20261029-D60", now.plusHours(2))
        val past = reminder("s2-20261029-D60", now.minusMinutes(5))

        val plan = ReminderPlan.plan(setOf("s1-20261029-D60", "s9-20261029-D60"), listOf(upcoming, past), now)

        assertEquals(setOf("s9-20261029-D60"), plan.toCancel)
        assertEquals(listOf(upcoming), plan.toSchedule)
    }

    @Test
    fun duplicateIdsAreScheduledOnce() {
        val first = reminder("s1-20261030-D10", now.plusDays(1))

        val plan = ReminderPlan.plan(emptySet(), listOf(first, first.copy(title = "중복")), now)

        assertEquals(1, plan.toSchedule.size)
    }

    @Test
    fun parsesTheServerResponse() {
        val body = """
            {"generatedAt":"2026-10-29T10:00:00","until":"2026-11-05T10:00:00","reminders":[
             {"id":"s3-20261030-D60","scheduleId":3,"occurrenceDate":"2026-10-30","fireAt":"2026-10-30T12:20:00",
              "anchor":"DEPARTURE","minutesBefore":60,"title":"미팅","startAt":"2026-10-30T14:00:00","place":"강남",
              "prepStartAt":"2026-10-30T12:50:00","departAt":"2026-10-30T13:20:00",
              "checklist":{"checklistId":7,"totalCount":3,"remainingCount":2},"target":"CHECKLIST",
              "notificationTitle":"14:00 미팅 · 출발 1시간 전","notificationBody":"장소: 강남 · 13:20 출발 · 준비물 2개 남음"},
             {"id":"s4-20261030-S0","scheduleId":4,"fireAt":"2026-10-30T18:00:00","startAt":"2026-10-30T18:00:00",
              "title":"저녁","place":null,"checklist":null,"target":"SCHEDULE",
              "notificationTitle":"18:00 저녁 · 지금 시작","notificationBody":"18:00에 시작합니다."}]}
        """.trimIndent()

        val reminders = ReminderJson.parseResponse(body)

        assertEquals(2, reminders.size)
        assertEquals(LocalDateTime.of(2026, 10, 30, 12, 20), reminders[0].fireAt)
        assertEquals(ReminderTarget.CHECKLIST, reminders[0].target)
        assertEquals(7L, reminders[0].checklistId)
        assertEquals(null, reminders[1].place)
        assertEquals(null, reminders[1].checklistId)
        // 기기에 저장했다가 다시 읽어도 같다.
        assertEquals(reminders, ReminderJson.parseArray(ReminderJson.toArray(reminders)))
    }

    private fun reminder(id: String, fireAt: LocalDateTime) = UpcomingReminder(
        id = id,
        scheduleId = 1,
        fireAt = fireAt,
        startAt = fireAt.plusHours(1),
        title = "미팅",
        place = null,
        notificationTitle = "미팅",
        notificationBody = "",
        target = ReminderTarget.SCHEDULE,
        checklistId = null,
    )
}
