package com.example.moemi.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.moemi.MainActivity
import com.example.moemi.R

/** 일정 준비 알림 채널과 알림 표시 */
object ReminderNotifications {

    const val CHANNEL_ID = "schedule_reminders"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "일정 준비 알림", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "일정 시작이나 출발 전에 준비할 시간을 알려 줍니다."
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** 알림을 보낼 수 있는지 (앱 알림이 꺼져 있거나 Android 13+ 권한이 없으면 false) */
    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(context: Context, id: String, title: String, body: String, target: String, scheduleId: Long, checklistId: Long) {
        if (!canPost(context)) return
        ensureChannel(context)

        // 알림을 누르면 앱을 열고 해당 일정 / 체크리스트로 이동한다.
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_REMINDER_TARGET, target)
            .putExtra(MainActivity.EXTRA_SCHEDULE_ID, scheduleId)
            .putExtra(MainActivity.EXTRA_CHECKLIST_ID, checklistId)
        val contentIntent = PendingIntent.getActivity(
            context, id.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
        } catch (e: SecurityException) {
            // 확인한 직후 사용자가 권한을 끈 경우. 앱 안의 예정 알림 목록으로 대신한다.
        }
    }
}
