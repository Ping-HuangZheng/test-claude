package com.nightstand.clock.alarm

import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nightstand.clock.data.ClockSettings
import com.nightstand.clock.data.SettingsStore
import com.nightstand.clock.data.rememberClockTime
import com.nightstand.clock.math.AlarmMath
import com.nightstand.clock.math.ClockMath
import com.nightstand.clock.ui.faces.DigitalFace
import com.nightstand.clock.ui.theme.AppTheme
import com.nightstand.clock.ui.theme.ClockColors
import kotlinx.coroutines.delay

/** 響鈴畫面：鎖定時也會顯示並點亮螢幕，有大大的「貪睡」和「停止」。 */
class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val store = SettingsStore.get(this)
        setContent {
            val settings by store.settings.collectAsStateWithLifecycle()
            val ringing by AlarmNotifier.ringing.collectAsStateWithLifecycle()

            // 在通知上按了停止／貪睡，或響太久自動停止時，關閉這個畫面。
            LaunchedEffect(ringing) { if (!ringing) finish() }
            LaunchedEffect(Unit) {
                delay(AlarmMath.RING_TIMEOUT_MINUTES * 60_000L)
                AlarmActions.dismiss(this@AlarmRingActivity)
            }

            AppTheme(settings.nightMode) {
                RingScreen(
                    settings = settings,
                    onSnooze = {
                        AlarmActions.snooze(this)
                        finish()
                    },
                    onDismiss = {
                        AlarmActions.dismiss(this)
                        finish()
                    },
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
fun RingScreen(settings: ClockSettings, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = ClockColors.of(settings.nightMode)
    val now by rememberClockTime(ClockMath.SECOND_MILLIS)
    val use24 = ClockMath.use24Hour(settings.hourFormat, DateFormat.is24HourFormat(context))

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("鬧鐘", color = colors.secondary, fontSize = 24.sp)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            DigitalFace(
                time = ClockMath.timeParts(now.hour, now.minute, now.second, use24, showSeconds = false),
                colors = colors,
                scale = 1f,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // 剛醒來要看得清楚：夜間模式也用主要顏色、粗外框。
            OutlinedButton(
                onClick = onSnooze,
                border = BorderStroke(2.dp, colors.primary),
                modifier = Modifier.weight(1f).height(72.dp),
            ) {
                Text("貪睡 ${AlarmMath.SNOOZE_MINUTES} 分鐘", fontSize = 20.sp, color = colors.primary)
            }
            Button(onClick = onDismiss, modifier = Modifier.weight(1f).height(72.dp)) {
                Text("停止", fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
