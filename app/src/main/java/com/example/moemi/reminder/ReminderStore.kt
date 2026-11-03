package com.example.moemi.reminder

import android.content.Context
import org.json.JSONArray
import java.time.LocalDateTime
import java.util.UUID

/**
 * 기기에 남겨 두는 값들.
 * - 기기 식별자: 백엔드가 사용자별 데이터를 구분하는 X-Device-Id
 * - 마지막으로 받은 예정 알림: 재부팅 후 다시 예약하고, 서버에 못 붙을 때 앱 안에서 보여 준다
 * - 예약해 둔 알림 id: 사라진 알림을 취소하기 위해
 */
object ReminderStore {

    private const val PREFS = "moemi_reminders"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_REMINDERS = "reminders"
    private const val KEY_SCHEDULED_IDS = "scheduled_ids"
    private const val KEY_SYNCED_AT = "synced_at"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 설치마다 한 번 만들어 계속 쓴다. */
    fun deviceId(context: Context): String {
        val prefs = prefs(context)
        prefs.getString(KEY_DEVICE_ID, null)?.let { return it }
        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, created).apply()
        return created
    }

    fun reminders(context: Context): List<UpcomingReminder> {
        val raw = prefs(context).getString(KEY_REMINDERS, null) ?: return emptyList()
        return runCatching { ReminderJson.parseArray(JSONArray(raw)) }.getOrDefault(emptyList())
    }

    /** 마지막으로 서버에서 받아 온 시각. 한 번도 못 받았으면 null */
    fun syncedAt(context: Context): LocalDateTime? =
        prefs(context).getString(KEY_SYNCED_AT, null)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

    fun saveSynced(context: Context, reminders: List<UpcomingReminder>, syncedAt: LocalDateTime) {
        prefs(context).edit()
            .putString(KEY_REMINDERS, ReminderJson.toArray(reminders).toString())
            .putString(KEY_SYNCED_AT, syncedAt.toString())
            .apply()
    }

    fun scheduledIds(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_SCHEDULED_IDS, emptySet())?.toSet() ?: emptySet()

    fun saveScheduledIds(context: Context, ids: Set<String>) {
        prefs(context).edit().putStringSet(KEY_SCHEDULED_IDS, ids).apply()
    }
}
