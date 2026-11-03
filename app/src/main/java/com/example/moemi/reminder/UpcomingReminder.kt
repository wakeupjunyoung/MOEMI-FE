package com.example.moemi.reminder

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

/** 알림을 눌렀을 때 열 화면 */
enum class ReminderTarget { SCHEDULE, CHECKLIST }

/**
 * 서버(GET /api/reminders/upcoming)가 계산해 준 예정 알림 하나.
 * 시각은 모두 한국 시간이다.
 *
 * @param id 같은 일정·날짜·시점이면 항상 같다. 휴대폰 알림 예약의 키로 쓴다.
 */
data class UpcomingReminder(
    val id: String,
    val scheduleId: Long,
    val fireAt: LocalDateTime,
    val startAt: LocalDateTime,
    val title: String,
    val place: String?,
    val notificationTitle: String,
    val notificationBody: String,
    val target: ReminderTarget,
    val checklistId: Long?,
)

/** 서버 응답과 기기에 저장해 두는 목록이 같은 모양의 JSON 을 쓴다. */
object ReminderJson {

    /** 서버 응답 전체 `{"reminders":[...]}` 에서 알림 목록만 꺼낸다. */
    fun parseResponse(body: String): List<UpcomingReminder> =
        parseArray(JSONObject(body).optJSONArray("reminders") ?: JSONArray())

    fun parseArray(array: JSONArray): List<UpcomingReminder> =
        (0 until array.length()).map { parse(array.getJSONObject(it)) }

    fun toArray(reminders: List<UpcomingReminder>): JSONArray = JSONArray().apply {
        reminders.forEach { put(toJson(it)) }
    }

    private fun parse(json: JSONObject): UpcomingReminder {
        val checklist = json.optJSONObject("checklist")
        return UpcomingReminder(
            id = json.getString("id"),
            scheduleId = json.getLong("scheduleId"),
            fireAt = LocalDateTime.parse(json.getString("fireAt")),
            startAt = LocalDateTime.parse(json.getString("startAt")),
            title = json.getString("title"),
            place = json.optStringOrNull("place"),
            notificationTitle = json.getString("notificationTitle"),
            notificationBody = json.getString("notificationBody"),
            target = runCatching { ReminderTarget.valueOf(json.getString("target")) }.getOrDefault(ReminderTarget.SCHEDULE),
            checklistId = checklist?.let { if (it.isNull("checklistId")) null else it.getLong("checklistId") },
        )
    }

    private fun toJson(reminder: UpcomingReminder) = JSONObject().apply {
        put("id", reminder.id)
        put("scheduleId", reminder.scheduleId)
        put("fireAt", reminder.fireAt.toString())
        put("startAt", reminder.startAt.toString())
        put("title", reminder.title)
        put("place", reminder.place ?: JSONObject.NULL)
        put("notificationTitle", reminder.notificationTitle)
        put("notificationBody", reminder.notificationBody)
        put("target", reminder.target.name)
        put("checklist", reminder.checklistId?.let { JSONObject().put("checklistId", it) } ?: JSONObject.NULL)
    }

    private fun JSONObject.optStringOrNull(name: String): String? = if (isNull(name)) null else optString(name)
}
