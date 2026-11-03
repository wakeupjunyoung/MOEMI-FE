package com.example.moemi.reminder

import com.example.moemi.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** 백엔드의 예정 알림 API. 라이브러리 없이 기본 HttpURLConnection 으로 부른다. */
object ReminderApi {

    private const val TIMEOUT_MS = 5_000

    /** @throws IOException 서버에 연결하지 못했거나 오류 응답일 때 */
    suspend fun fetchUpcoming(deviceId: String, days: Int = 7): List<UpcomingReminder> = withContext(Dispatchers.IO) {
        val url = URL(BuildConfig.BASE_URL.trimEnd('/') + "/api/reminders/upcoming?days=$days")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("X-Device-Id", deviceId)
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("예정 알림 조회 실패: HTTP $code")
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            runCatching { ReminderJson.parseResponse(body) }
                .getOrElse { throw IOException("예정 알림 응답을 읽지 못했습니다", it) }
        } finally {
            connection.disconnect()
        }
    }
}
