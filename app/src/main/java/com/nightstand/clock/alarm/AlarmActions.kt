package com.nightstand.clock.alarm

import android.content.Context
import android.text.format.DateFormat
import com.nightstand.clock.data.SettingsStore
import com.nightstand.clock.math.AlarmConfig
import com.nightstand.clock.math.AlarmMath
import com.nightstand.clock.math.ClockMath
import java.time.Instant
import java.time.ZonedDateTime

/** 鬧鐘的所有動作集中在這裡：改設定之後一定重新排程。 */
object AlarmActions {

    /** 修改鬧鐘設定並重新排程。 */
    fun update(context: Context, transform: (AlarmConfig) -> AlarmConfig): AlarmScheduler.Result {
        val store = SettingsStore.get(context)
        store.update { it.copy(alarm = transform(it.alarm)) }
        return AlarmScheduler.sync(context, store.current.alarm)
    }

    /** 依目前設定重新排程（開機、改時間、App 啟動時）。 */
    fun resync(context: Context): AlarmScheduler.Result =
        AlarmScheduler.sync(context, SettingsStore.get(context).current.alarm)

    /** 時間到了：更新設定（單次鬧鐘關閉、清掉貪睡）、排下一次，然後開始響。 */
    fun onFire(context: Context, isSnooze: Boolean) {
        update(context) { AlarmMath.afterRing(it, isSnooze) }
        AlarmNotifier.showRinging(context, ringTitle(context))
    }

    fun snooze(context: Context) {
        AlarmNotifier.cancel(context)
        // 和排程用同一個時間來源（java.time），避免兩邊的「現在」不一致。
        update(context) { AlarmMath.snooze(it, Instant.now().toEpochMilli()) }
    }

    fun dismiss(context: Context) {
        AlarmNotifier.cancel(context)
        update(context) { AlarmMath.dismiss(it) }
    }

    /** 不改設定、直接響一次，讓使用者確認音量和畫面。 */
    fun testRing(context: Context) {
        AlarmNotifier.showRinging(context, "試響：${ringTitle(context)}")
    }

    private fun ringTitle(context: Context): String {
        val settings = SettingsStore.get(context).current
        val use24 = ClockMath.use24Hour(settings.hourFormat, DateFormat.is24HourFormat(context))
        val now = ZonedDateTime.now()
        return "鬧鐘 ${AlarmMath.timeText(now.hour, now.minute, use24)}"
    }
}
