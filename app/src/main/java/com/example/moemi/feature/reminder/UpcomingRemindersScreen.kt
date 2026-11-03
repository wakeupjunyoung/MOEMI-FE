package com.example.moemi.feature.reminder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.moemi.BuildConfig
import com.example.moemi.reminder.ReminderNotifications
import com.example.moemi.reminder.ReminderScheduler
import com.example.moemi.reminder.ReminderStore
import com.example.moemi.reminder.ReminderSync
import com.example.moemi.reminder.UpcomingReminder
import com.example.moemi.ui.components.AppCard
import com.example.moemi.ui.theme.Background
import com.example.moemi.ui.theme.CautionBg
import com.example.moemi.ui.theme.CautionFg
import com.example.moemi.ui.theme.Primary
import com.example.moemi.ui.theme.PrimarySoft
import com.example.moemi.ui.theme.TextPrimary
import com.example.moemi.ui.theme.TextSecondary
import com.example.moemi.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FIRE_TIME = DateTimeFormatter.ofPattern("M/d (E) HH:mm", Locale.KOREAN)
private val SYNCED_TIME = DateTimeFormatter.ofPattern("M/d HH:mm", Locale.KOREAN)

/**
 * 기능 6: 앞으로 울릴 일정 준비 알림 목록.
 * 알림 권한이 꺼져 있어도 여기서 예정 알림을 확인할 수 있고, 권한 설정으로 바로 갈 수 있다.
 */
@Composable
fun UpcomingRemindersScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by ReminderSync.state.collectAsState()

    // 설정 화면에서 권한을 바꾸고 돌아오면 다시 확인하도록 화면이 보일 때마다 갱신한다.
    var permissionCheck by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            permissionCheck++
            ReminderScheduler.apply(context, ReminderSync.state.value.reminders)
        }
    }
    val canNotify = remember(permissionCheck) { ReminderNotifications.canPost(context) }
    val canExact = remember(permissionCheck) { ReminderScheduler.canScheduleExact(context) }

    val openNotificationSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionCheck++
        // 이미 거절했다면 시스템이 다시 묻지 않고 바로 거절되므로 앱 알림 설정을 연다.
        if (!granted) openNotificationSettings()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background).statusBarsPadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("예정 알림", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            val synced = state.syncedAt?.format(SYNCED_TIME)?.let { "$it 기준" } ?: "아직 받아 온 알림이 없어요"
            Text(
                if (state.syncing) "알림을 확인하는 중…" else synced,
                fontSize = 13.sp,
                color = TextTertiary
            )
            if (BuildConfig.DEBUG) {
                // 일정 화면이 생기기 전까지 Postman 으로 같은 X-Device-Id 의 일정을 만들어 시험한다.
                Text("X-Device-Id: ${ReminderStore.deviceId(context)}", fontSize = 11.sp, color = TextTertiary)
            }
        }

        if (!canNotify) {
            item {
                Banner(
                    title = "알림이 꺼져 있어요",
                    body = "휴대폰 알림은 오지 않지만, 예정 알림은 이 화면에서 확인할 수 있어요.",
                    action = "알림 켜기",
                ) {
                    val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    if (needsRuntimePermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // 권한은 있는데 앱 알림이 꺼져 있는 경우
                        openNotificationSettings()
                    }
                }
            }
        } else if (!canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            item {
                Banner(
                    title = "알림이 조금 늦게 올 수 있어요",
                    body = "'알람 및 리마인더'를 허용하면 정해 둔 시각에 맞춰 알려 드려요.",
                    action = "허용하기",
                ) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                    )
                }
            }
        }

        state.error?.let { error ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$error · 마지막으로 받은 알림을 보여 드려요", fontSize = 13.sp, color = CautionFg,
                        modifier = Modifier.weight(1f))
                    Text("다시 시도", fontSize = 13.sp, color = Primary, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { scope.launch { ReminderSync.sync(context) } }.padding(8.dp))
                }
            }
        }

        if (state.reminders.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text("앞으로 7일 동안 예정된 알림이 없어요", fontSize = 15.sp, color = TextSecondary)
                }
            }
        } else {
            items(state.reminders, key = { it.id }) { ReminderRow(it) }
        }
    }
}

@Composable
private fun ReminderRow(reminder: UpcomingReminder) {
    AppCard {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.clip(RoundedCornerShape(8.dp)).background(PrimarySoft).padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(reminder.fireAt.format(FIRE_TIME), fontSize = 12.sp, color = Primary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(reminder.notificationTitle, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(reminder.notificationBody, fontSize = 13.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun Banner(title: String, body: String, action: String, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CautionBg).padding(16.dp)
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = CautionFg)
        Spacer(Modifier.height(4.dp))
        Text(body, fontSize = 13.sp, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Text(action, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Primary,
            modifier = Modifier.clickable(onClick = onAction).padding(vertical = 4.dp))
    }
}
