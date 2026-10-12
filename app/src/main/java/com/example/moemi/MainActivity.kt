package com.example.moemi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.moemi.data.LifeIndexRepository
import com.example.moemi.data.recommendationsFor
import com.example.moemi.feature.calendar.CalendarScreen
import com.example.moemi.feature.checklist.ChecklistScreen
import com.example.moemi.feature.checklist.ChecklistState
import com.example.moemi.feature.hazard.HazardMapScreen
import com.example.moemi.ui.components.AppTab
import com.example.moemi.ui.components.AppTabBar
import com.example.moemi.ui.components.ComingSoon
import com.example.moemi.ui.theme.Background
import com.example.moemi.ui.theme.MoemiTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 밝은 배경이므로 상태바/내비게이션바 아이콘을 어둡게
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = SystemBarStyle.light(transparent, transparent),
        )
        setContent {
            MoemiTheme {
                MoemiApp()
            }
        }
    }
}

@Composable
fun MoemiApp() {
    var tab by rememberSaveable { mutableStateOf(AppTab.CALENDAR) }

    // 체크리스트는 탭을 옮겨 다녀도 유지되도록 여기서 들고 있는다.
    val checklist = remember {
        val today = LocalDate.now()
        val day = LifeIndexRepository.load(LifeIndexRepository.regions.first(), today, "14:00")[today]
        ChecklistState.sample(recommendationsFor(day))
    }

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { AppTabBar(tab) { tab = it } }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                AppTab.CALENDAR -> CalendarScreen()
                AppTab.SCHEDULE -> ChecklistScreen(checklist, onBack = { tab = AppTab.CALENDAR })
                AppTab.MAP -> HazardMapScreen()
                AppTab.HOME, AppTab.BUS -> ComingSoon(tab.label)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun MoemiAppPreview() {
    MoemiTheme { MoemiApp() }
}
