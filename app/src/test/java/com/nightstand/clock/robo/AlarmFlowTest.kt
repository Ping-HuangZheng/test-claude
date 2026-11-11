package com.nightstand.clock.robo

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import com.nightstand.clock.alarm.AlarmActions
import com.nightstand.clock.alarm.AlarmNotifier
import com.nightstand.clock.alarm.AlarmReceiver
import com.nightstand.clock.alarm.BootReceiver
import com.nightstand.clock.data.ClockSettings
import com.nightstand.clock.data.SettingsStore
import com.nightstand.clock.math.AlarmConfig
import com.nightstand.clock.math.AlarmMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.ZonedDateTime
import kotlin.math.abs

/** 鬧鐘流程：排程 → 響鈴 → 貪睡／停止 → 重開機重排。在模擬的 Android 環境跑。 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlarmFlowTest {

    private val app = RuntimeEnvironment.getApplication()
    private val store get() = SettingsStore.get(app)
    private val alarms get() = shadowOf(app.getSystemService(AlarmManager::class.java))
    private val notifications get() = shadowOf(app.getSystemService(NotificationManager::class.java))

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        // 真機上宣告了 USE_EXACT_ALARM 就會允許；模擬環境預設不允許，要手動打開。
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        // SettingsStore 是單例，測試之間要清乾淨。
        AlarmNotifier.cancel(app)
        AlarmActions.update(app) { AlarmConfig() }
        store.update { ClockSettings() }
    }

    private fun fire(snooze: Boolean = false) = AlarmReceiver().onReceive(
        app,
        Intent(app, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE, snooze),
    )

    private fun send(action: String) = AlarmReceiver().onReceive(app, Intent(app, AlarmReceiver::class.java).setAction(action))

    @Test
    fun enablingAlarmSchedulesAlarmClockAtNextTime() {
        AlarmActions.update(app) { AlarmConfig(enabled = true, hour = 7, minute = 30) }
        val scheduled = alarms.peekNextScheduledAlarm()
        assertNotNull("應該有排程", scheduled)
        assertNotNull("要用 setAlarmClock（有 AlarmClockInfo）", scheduled!!.alarmClockInfo)
        val expected = AlarmMath.nextTrigger(ZonedDateTime.now(), 7, 30, 0).toInstant().toEpochMilli()
        assertEquals(expected, scheduled.triggerAtMs)
    }

    @Test
    fun disablingAlarmCancelsSchedule() {
        AlarmActions.update(app) { AlarmConfig(enabled = true, hour = 7, minute = 30) }
        AlarmActions.update(app) { it.copy(enabled = false) }
        assertNull(alarms.peekNextScheduledAlarm())
    }

    @Test
    fun firingOnceAlarmRingsAndTurnsItselfOff() {
        AlarmActions.update(app) { AlarmConfig(enabled = true, hour = 7, minute = 0, repeatMask = 0) }
        fire()

        assertEquals(1, notifications.size())
        val n = notifications.allNotifications.single()
        assertEquals(Notification.CATEGORY_ALARM, n.category)
        assertNotNull("要有全螢幕響鈴畫面", n.fullScreenIntent)
        assertTrue("聲音要重複播放", n.flags and Notification.FLAG_INSISTENT != 0)
        assertEquals(2, n.actions.size)
        assertTrue(AlarmNotifier.ringing.value)

        assertFalse("只響一次的鬧鐘響完要自動關閉", store.current.alarm.enabled)
        assertNull("沒有下一次了", alarms.peekNextScheduledAlarm())
    }

    @Test
    fun snoozeStopsRingingAndRingsAgainInFiveMinutes() {
        AlarmActions.update(app) { AlarmConfig(enabled = true, hour = 7, minute = 0, repeatMask = AlarmMath.EVERY_DAY) }
        fire()
        send(AlarmReceiver.ACTION_SNOOZE)

        assertEquals(0, notifications.size())
        assertFalse(AlarmNotifier.ringing.value)
        val scheduled = alarms.peekNextScheduledAlarm()!!
        val expected = java.time.Instant.now().toEpochMilli() + 5 * 60_000L
        assertTrue("貪睡要排在 5 分鐘後，實際差 ${scheduled.triggerAtMs - expected}ms", abs(scheduled.triggerAtMs - expected) < 5_000)
        assertTrue(store.current.alarm.snoozeUntilMillis > 0)
    }

    @Test
    fun snoozeRingThenDismissKeepsRepeatingAlarm() {
        AlarmActions.update(app) { AlarmConfig(enabled = true, hour = 7, minute = 0, repeatMask = AlarmMath.EVERY_DAY) }
        fire()
        send(AlarmReceiver.ACTION_SNOOZE)
        fire(snooze = true)
        assertEquals(1, notifications.size())
        send(AlarmReceiver.ACTION_DISMISS)

        assertEquals(0, notifications.size())
        assertTrue("每天的鬧鐘要保持開啟", store.current.alarm.enabled)
        assertEquals(0L, store.current.alarm.snoozeUntilMillis)
        val expected = AlarmMath.nextTrigger(ZonedDateTime.now(), 7, 0, AlarmMath.EVERY_DAY).toInstant().toEpochMilli()
        assertEquals(expected, alarms.peekNextScheduledAlarm()!!.triggerAtMs)
    }

    @Test
    fun bootReschedulesFromSavedSettings() {
        // 模擬重開機：設定還在，但系統排程被清掉了。
        store.update { it.copy(alarm = AlarmConfig(enabled = true, hour = 6, minute = 15)) }
        com.nightstand.clock.alarm.AlarmScheduler.sync(app, AlarmConfig())
        assertNull(alarms.peekNextScheduledAlarm())

        BootReceiver().onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))

        val expected = AlarmMath.nextTrigger(ZonedDateTime.now(), 6, 15, 0).toInstant().toEpochMilli()
        assertEquals(expected, alarms.peekNextScheduledAlarm()!!.triggerAtMs)
    }

    @Test
    fun withoutExactAlarmPermissionReportsIt() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val result = AlarmActions.update(app) { AlarmConfig(enabled = true, hour = 7, minute = 0) }
        assertEquals(com.nightstand.clock.alarm.AlarmScheduler.Result.NO_EXACT_ALARM_PERMISSION, result)
        assertNull(alarms.peekNextScheduledAlarm())
    }

    @Test
    fun testRingDoesNotChangeSettings() {
        AlarmActions.update(app) { AlarmConfig(enabled = true, hour = 7, minute = 0) }
        val before = store.current.alarm
        AlarmActions.testRing(app)
        assertEquals(1, notifications.size())
        assertEquals(before, store.current.alarm)
        send(AlarmReceiver.ACTION_DISMISS)
        assertEquals(0, notifications.size())
    }
}
