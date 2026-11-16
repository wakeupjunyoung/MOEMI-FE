package com.example.moemi.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 재부팅·앱 업데이트·시각 변경·정확한 알람 권한 변경 때 마지막으로 받은 목록으로 다시 예약한다.
 * (재부팅하면 AlarmManager 예약이 모두 사라진다.) 서버와의 정합성은 다음 앱 실행 때 다시 맞춘다.
 */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.apply(context, ReminderStore.reminders(context))
    }
}
