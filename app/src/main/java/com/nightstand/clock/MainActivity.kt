package com.nightstand.clock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nightstand.clock.data.SettingsStore
import com.nightstand.clock.ui.ClockScreen

class MainActivity : ComponentActivity() {

    private val store by lazy { SettingsStore.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hideSystemBars()
        setContent {
            val settings by store.settings.collectAsStateWithLifecycle()
            ClockScreen(
                settings = settings,
                onUpdate = store::update,
                interactive = true,
            )
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
