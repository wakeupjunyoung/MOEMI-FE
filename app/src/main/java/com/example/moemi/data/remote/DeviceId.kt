package com.example.moemi.data.remote

import android.content.Context
import java.util.UUID

/**
 * 로그인 기능이 생기기 전까지 백엔드가 사용자별 데이터(체크리스트·일정·알림)를 구분하는 X-Device-Id 값.
 * 설치마다 한 번만 만들고, 모든 API 호출이 이 값 하나를 같이 쓴다.
 */
object DeviceId {
    const val HEADER = "X-Device-Id"

    private const val PREFS = "moemi"
    private const val KEY = "device_id"

    // 예전에는 알림 쪽(ReminderStore)이 여기에 값을 따로 만들었다.
    private const val LEGACY_PREFS = "moemi_reminders"

    @Volatile
    private var cached: String? = null

    @Synchronized
    fun get(context: Context): String {
        cached?.let { return it }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        // 둘 다 있으면 체크리스트가 쓰던 값을 남긴다. 알림 쪽 값만 있으면 그 값을 옮겨 온다.
        val id = prefs.getString(KEY, null)
            ?: legacy.getString(KEY, null)
            ?: UUID.randomUUID().toString()
        prefs.edit().putString(KEY, id).apply()
        legacy.edit().remove(KEY).apply()
        cached = id
        return id
    }
}
