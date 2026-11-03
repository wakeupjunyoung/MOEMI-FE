package com.example.moemi.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 예정 알림을 휴대폰의 AlarmManager 에 예약한다. 예약 시각이 되면 [ReminderReceiver] 가 알림을 띄운다.
 * 인터넷이 끊겨도 이미 예약한 알림은 울린다.
 */
object ReminderScheduler {

    /** 서버 시각은 한국 시간이다. */
    private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    private const val ACTION_REMINDER = "com.example.moemi.action.REMINDER"

    /** [reminders] 대로 예약하고, 전에 예약했지만 목록에 없는 알림은 취소한다. */
    fun apply(context: Context, reminders: List<UpcomingReminder>, now: LocalDateTime = LocalDateTime.now(KST)) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val plan = ReminderPlan.plan(ReminderStore.scheduledIds(context), reminders, now)

        plan.toCancel.forEach { id ->
            existing(context, id)?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
        plan.toSchedule.forEach { reminder ->
            val triggerAt = reminder.fireAt.atZone(KST).toInstant().toEpochMilli()
            val operation = pendingIntent(context, reminder)
            val exact = canScheduleExact(context) && runCatching {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }.isSuccess
            if (!exact) {
                // 정확한 알람이 허용되지 않으면 시스템 사정에 따라 몇 분 늦게 울릴 수 있다.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }
        }
        ReminderStore.saveScheduledIds(context, plan.toSchedule.map { it.id }.toSet())
    }

    /** Android 12 이상에서 '알람 및 리마인더' 권한이 있는지. 없으면 설정 화면으로 안내한다. */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
    }

    private fun intent(context: Context, id: String) = Intent(context, ReminderReceiver::class.java)
        .setAction(ACTION_REMINDER)
        // data 가 다르면 서로 다른 PendingIntent 로 취급되어 알림마다 따로 예약·취소된다.
        .setData(Uri.parse("moemi://reminder/$id"))

    private fun pendingIntent(context: Context, reminder: UpcomingReminder): PendingIntent {
        val intent = intent(context, reminder.id)
            .putExtra(ReminderReceiver.EXTRA_ID, reminder.id)
            .putExtra(ReminderReceiver.EXTRA_TITLE, reminder.notificationTitle)
            .putExtra(ReminderReceiver.EXTRA_BODY, reminder.notificationBody)
            .putExtra(ReminderReceiver.EXTRA_TARGET, reminder.target.name)
            .putExtra(ReminderReceiver.EXTRA_SCHEDULE_ID, reminder.scheduleId)
            .putExtra(ReminderReceiver.EXTRA_CHECKLIST_ID, reminder.checklistId ?: -1L)
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun existing(context: Context, id: String): PendingIntent? = PendingIntent.getBroadcast(
        context, 0, intent(context, id), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
}
