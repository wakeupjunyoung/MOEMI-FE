package com.example.moemi.reminder

import com.example.moemi.BuildConfig
import com.example.moemi.data.remote.DeviceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** 백엔드의 예정 알림 API. 라이브러리 없이 기본 HttpURLConnection 으로 부른다. */
object ReminderApi {

    // 배포 서버가 깨어나는 데 시간이 걸릴 수 있어 MoemiApi 와 같게 둔다.
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000

    /** @throws IOException 서버에 연결하지 못했거나 오류 응답일 때 */
    suspend fun fetchUpcoming(deviceId: String, days: Int = 7): List<UpcomingReminder> = withContext(Dispatchers.IO) {
        val url = URL(BuildConfig.BASE_URL.trimEnd('/') + "/api/reminders/upcoming?days=$days")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty(DeviceId.HEADER, deviceId)
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
