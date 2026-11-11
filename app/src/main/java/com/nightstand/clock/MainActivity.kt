package com.nightstand.clock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nightstand.clock.data.SettingsStore
import com.nightstand.clock.ui.ClockScreen
import com.nightstand.clock.ui.SettingsScreen
import com.nightstand.clock.ui.theme.AppTheme

/** App 內的頁面。頁面很少，不需要導覽函式庫。 */
private enum class Screen { CLOCK, SETTINGS }

class MainActivity : ComponentActivity() {

    private val store by lazy { SettingsStore.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hideSystemBars()
        setContent {
            val settings by store.settings.collectAsStateWithLifecycle()
            var screen by rememberSaveable { mutableStateOf(Screen.CLOCK) }
            BackHandler(enabled = screen != Screen.CLOCK) { screen = Screen.CLOCK }

            when (screen) {
                Screen.CLOCK -> ClockScreen(
                    settings = settings,
                    onUpdate = store::update,
                    interactive = true,
                    onOpenSettings = { screen = Screen.SETTINGS },
                )
                Screen.SETTINGS -> AppTheme(settings.nightMode) {
                    SettingsScreen(
                        settings = settings,
                        onUpdate = store::update,
                        onBack = { screen = Screen.CLOCK },
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // 從其他畫面回來時系統列可能又跑出來，重新隱藏。
        if (hasFocus) hideSystemBars()
    }

    /** 全螢幕：隱藏 Android 狀態列和導覽列，從邊緣滑動可暫時叫出。 */
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
