package com.nightstand.clock.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.nightstand.clock.math.ChargeState
import com.nightstand.clock.math.ClockMath

@Immutable
data class BatteryInfo(val percent: Int?, val state: ChargeState) {
    val label: String get() = ClockMath.batteryLabel(percent, state)
    val isPlugged: Boolean get() = state != ChargeState.DISCHARGING

    companion object {
        val Unknown = BatteryInfo(null, ChargeState.DISCHARGING)

        fun from(intent: Intent): BatteryInfo = BatteryInfo(
            percent = ClockMath.batteryPercent(
                intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
                intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1),
            ),
            state = ClockMath.chargeState(
                intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1),
                intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
            ),
        )
    }
}

/** 監聽系統的電池廣播（不需要任何權限）。 */
@Composable
fun rememberBatteryInfo(): State<BatteryInfo> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(BatteryInfo.Unknown) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent?.let { state.value = BatteryInfo.from(it) }
            }
        }
        // 電池廣播是 sticky 的：註冊時會直接拿到最新一筆。
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        sticky?.let { state.value = BatteryInfo.from(it) }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}
