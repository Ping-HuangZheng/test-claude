package com.nightstand.clock.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.nightstand.clock.math.ClockMath
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

/**
 * 目前時間，剛好在整秒（或整分）時更新。
 *
 * - 畫面回到前景時立刻更新一次（螢幕關閉期間計時器可能停住）。
 * - 使用者改時間或時區時也立刻更新。
 */
@Composable
fun rememberClockTime(intervalMillis: Long): State<ZonedDateTime> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var timeChangeCount by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                timeChangeCount++
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    return produceState(ZonedDateTime.now(), intervalMillis, timeChangeCount, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = ZonedDateTime.now()
                delay(ClockMath.millisUntilNextTick(System.currentTimeMillis(), intervalMillis))
            }
        }
    }
}
