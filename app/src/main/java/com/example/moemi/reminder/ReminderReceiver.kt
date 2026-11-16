package com.example.moemi.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 예약한 시각이 되면 AlarmManager 가 깨워 알림을 띄운다. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        ReminderNotifications.show(
            context = context,
            id = id,
            title = intent.getStringExtra(EXTRA_TITLE) ?: return,
            body = intent.getStringExtra(EXTRA_BODY).orEmpty(),
            target = intent.getStringExtra(EXTRA_TARGET) ?: ReminderTarget.SCHEDULE.name,
            scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L),
            checklistId = intent.getLongExtra(EXTRA_CHECKLIST_ID, -1L),
        )
        // 울린 알림은 더 이상 취소할 필요가 없다.
        ReminderStore.saveScheduledIds(context, ReminderStore.scheduledIds(context) - id)
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_BODY = "reminder_body"
        const val EXTRA_TARGET = "reminder_target"
        const val EXTRA_SCHEDULE_ID = "reminder_schedule_id"
        const val EXTRA_CHECKLIST_ID = "reminder_checklist_id"
    }
}
