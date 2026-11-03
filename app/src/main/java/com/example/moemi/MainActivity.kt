package com.example.moemi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.moemi.data.LifeIndexRepository
import com.example.moemi.data.recommendationsFor
import com.example.moemi.feature.bus.BusScreen
import com.example.moemi.feature.calendar.CalendarScreen
import com.example.moemi.feature.checklist.ChecklistScreen
import com.example.moemi.feature.checklist.ChecklistState
import com.example.moemi.feature.hazard.HazardMapScreen
import com.example.moemi.feature.reminder.UpcomingRemindersScreen
import com.example.moemi.feature.schedule.NewScheduleScreen
import com.example.moemi.feature.schedule.ReminderSettingsScreen
import com.example.moemi.feature.schedule.ScheduleState
import com.example.moemi.feature.schedule.TimelineScreen
import com.example.moemi.reminder.ReminderNotifications
import com.example.moemi.reminder.ReminderSync
import com.example.moemi.ui.components.AppTab
import com.example.moemi.ui.components.AppTabBar
import com.example.moemi.ui.theme.Background
import com.example.moemi.ui.theme.MoemiTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    /** 알림을 눌러 들어왔을 때 열 탭. 같은 탭을 다시 요청해도 반응하도록 매번 새 값으로 바꾼다. */
    private var openRequest by mutableStateOf<OpenRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 밝은 배경이므로 상태바/내비게이션바 아이콘을 어둡게
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = SystemBarStyle.light(transparent, transparent),
        )
        ReminderNotifications.ensureChannel(this)
        ReminderSync.load(this)
        openRequest = openRequestOf(intent)
        setContent {
            MoemiTheme {
                MoemiApp(openRequest)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 앱을 열 때마다 서버의 예정 알림과 맞춘다. 지운 일정의 알림이 남아 있으면 여기서 취소된다.
        lifecycleScope.launch { ReminderSync.sync(applicationContext) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openRequestOf(intent)?.let { openRequest = it }
    }

    private fun openRequestOf(intent: Intent?): OpenRequest? {
        val target = intent?.getStringExtra(EXTRA_REMINDER_TARGET) ?: return null
        return OpenRequest(
            target = target,
            scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L).takeIf { it > 0 },
            checklistId = intent.getLongExtra(EXTRA_CHECKLIST_ID, -1L).takeIf { it > 0 },
        )
    }

    companion object {
        const val EXTRA_REMINDER_TARGET = "open_reminder_target"
        const val EXTRA_SCHEDULE_ID = "open_schedule_id"
        const val EXTRA_CHECKLIST_ID = "open_checklist_id"
    }
}

/** 알림을 눌러 열 화면. 일정 상세 화면이 생기면 scheduleId 로 바로 연다. */
class OpenRequest(val target: String, val scheduleId: Long?, val checklistId: Long?)

@Composable
fun MoemiApp(openRequest: OpenRequest? = null) {
    var tab by rememberSaveable { mutableStateOf(AppTab.CALENDAR) }

    // 일정 탭 안에서 열려 있는 화면 (타임라인 → 새 일정 → 알림 설정, 일정 → 준비물)
    var scheduleRoute by rememberSaveable { mutableStateOf(ScheduleRoute.TIMELINE) }

    // 알림에서 들어오면 일정 탭의 준비물 체크리스트로 이동한다.
    LaunchedEffect(openRequest) {
        if (openRequest != null) {
            tab = AppTab.SCHEDULE
            scheduleRoute = ScheduleRoute.CHECKLIST
        }
    }

    // 처음 실행할 때 알림 권한을 묻는다 (Android 13 이상). 거절해도 앱 안의 예정 알림은 볼 수 있다.
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // 체크리스트는 탭을 옮겨 다녀도 유지되도록 여기서 들고 있는다.
    val checklist = remember {
        val today = LocalDate.now()
        val day = LifeIndexRepository.load(LifeIndexRepository.regions.first(), today, "14:00")[today]
        ChecklistState.sample(recommendationsFor(day))
    }
    val schedule = remember { ScheduleState(LocalDate.now()) }

    BackHandler(enabled = tab == AppTab.SCHEDULE && scheduleRoute != ScheduleRoute.TIMELINE) {
        scheduleRoute = if (scheduleRoute == ScheduleRoute.REMINDER) ScheduleRoute.NEW else ScheduleRoute.TIMELINE
    }

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { AppTabBar(tab) { tab = it } }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                AppTab.CALENDAR -> CalendarScreen()
                AppTab.SCHEDULE -> when (scheduleRoute) {
                    ScheduleRoute.TIMELINE -> TimelineScreen(
                        schedule,
                        onAdd = { scheduleRoute = ScheduleRoute.NEW },
                        onOpenSchedule = { scheduleRoute = ScheduleRoute.CHECKLIST },
                    )
                    ScheduleRoute.NEW -> NewScheduleScreen(
                        schedule,
                        onBack = { scheduleRoute = ScheduleRoute.TIMELINE },
                        onOpenReminder = { scheduleRoute = ScheduleRoute.REMINDER },
                    )
                    ScheduleRoute.REMINDER -> ReminderSettingsScreen(schedule, onBack = { scheduleRoute = ScheduleRoute.NEW })
                    ScheduleRoute.CHECKLIST -> ChecklistScreen(checklist, onBack = { scheduleRoute = ScheduleRoute.TIMELINE })
                }
                AppTab.MAP -> HazardMapScreen()
                // 홈 화면이 정해지기 전까지 기능 6의 예정 알림 목록을 둔다.
                AppTab.HOME -> UpcomingRemindersScreen()
                AppTab.BUS -> BusScreen()
            }
        }
    }
}

private enum class ScheduleRoute { TIMELINE, NEW, REMINDER, CHECKLIST }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun MoemiAppPreview() {
    MoemiTheme { MoemiApp() }
}
