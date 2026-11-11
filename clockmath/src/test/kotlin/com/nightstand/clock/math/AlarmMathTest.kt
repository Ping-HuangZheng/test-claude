package com.nightstand.clock.math

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlarmMathTest {

    private val taipei = ZoneId.of("Asia/Taipei")

    /** 2026-11-11 是週三。 */
    private fun at(day: Int, hour: Int, minute: Int, second: Int = 0) =
        ZonedDateTime.of(2026, 11, day, hour, minute, second, 0, taipei)

    private val wed = 3
    private val fri = 5
    private val mon = 1

    // ---- 星期遮罩 ----

    @Test
    fun dayMask_toggleAndCheck() {
        var mask = 0
        mask = AlarmMath.toggleDay(mask, mon)
        mask = AlarmMath.toggleDay(mask, fri)
        assertTrue(AlarmMath.isDayEnabled(mask, mon))
        assertTrue(AlarmMath.isDayEnabled(mask, fri))
        assertFalse(AlarmMath.isDayEnabled(mask, wed))
        mask = AlarmMath.toggleDay(mask, mon)
        assertFalse(AlarmMath.isDayEnabled(mask, mon))
    }

    @Test
    fun presetMasks() {
        assertTrue((1..5).all { AlarmMath.isDayEnabled(AlarmMath.WEEKDAYS, it) })
        assertFalse(AlarmMath.isDayEnabled(AlarmMath.WEEKDAYS, 6))
        assertTrue(AlarmMath.isDayEnabled(AlarmMath.WEEKEND, 7))
    }

    // ---- 下一次響鈴 ----

    @Test
    fun once_laterTodayRingsToday() {
        assertEquals(at(11, 7, 0), AlarmMath.nextTrigger(at(11, 6, 30), 7, 0, 0))
    }

    @Test
    fun once_alreadyPassedRingsTomorrow() {
        assertEquals(at(12, 7, 0), AlarmMath.nextTrigger(at(11, 7, 30), 7, 0, 0))
    }

    @Test
    fun once_exactlyNowRingsTomorrow() {
        assertEquals(at(12, 7, 0), AlarmMath.nextTrigger(at(11, 7, 0), 7, 0, 0))
    }

    @Test
    fun repeat_weekdaysFromFridayEveningIsMonday() {
        // 11/13 是週五；平日鬧鐘下一次是 11/16 週一
        assertEquals(at(16, 7, 0), AlarmMath.nextTrigger(at(13, 22, 0), 7, 0, AlarmMath.WEEKDAYS))
    }

    @Test
    fun repeat_onlyTodayButPassedIsNextWeek() {
        val mask = AlarmMath.dayBit(wed)
        assertEquals(at(18, 7, 0), AlarmMath.nextTrigger(at(11, 8, 0), 7, 0, mask))
    }

    @Test
    fun repeat_laterTodayWhenTodayEnabled() {
        val mask = AlarmMath.dayBit(wed) or AlarmMath.dayBit(fri)
        assertEquals(at(11, 23, 15), AlarmMath.nextTrigger(at(11, 23, 0), 23, 15, mask))
    }

    @Test
    fun nextTrigger_crossesMonthEnd() {
        val now = ZonedDateTime.of(2026, 11, 30, 23, 0, 0, 0, taipei)
        assertEquals(ZonedDateTime.of(2026, 12, 1, 6, 0, 0, 0, taipei), AlarmMath.nextTrigger(now, 6, 0, 0))
    }

    // ---- 貪睡與一般鬧鐘合併 ----

    @Test
    fun nextRing_disabledWithoutSnoozeIsNull() {
        assertNull(AlarmMath.nextRing(AlarmConfig(enabled = false), at(11, 6, 0)))
    }

    @Test
    fun nextRing_regularWhenNoSnooze() {
        val ring = AlarmMath.nextRing(AlarmConfig(enabled = true, hour = 7, minute = 0), at(11, 6, 0))
        assertEquals(AlarmRing(at(11, 7, 0), isSnooze = false), ring)
    }

    @Test
    fun nextRing_snoozeComesFirst() {
        val now = at(11, 7, 1)
        val config = AlarmMath.snooze(AlarmConfig(enabled = true, hour = 7, minute = 0, repeatMask = AlarmMath.EVERY_DAY), now.toInstant().toEpochMilli())
        val ring = AlarmMath.nextRing(config, now)!!
        assertTrue(ring.isSnooze)
        assertEquals(at(11, 7, 6), ring.at)
    }

    @Test
    fun nextRing_snoozeWorksEvenIfAlarmDisabled() {
        val now = at(11, 7, 0)
        val config = AlarmMath.snooze(AlarmConfig(enabled = false), now.toInstant().toEpochMilli())
        assertEquals(AlarmRing(at(11, 7, 5), isSnooze = true), AlarmMath.nextRing(config, now))
    }

    @Test
    fun nextRing_expiredSnoozeIsIgnored() {
        val config = AlarmConfig(enabled = true, hour = 7, minute = 0, snoozeUntilMillis = at(11, 7, 5).toInstant().toEpochMilli())
        assertEquals(AlarmRing(at(12, 7, 0), isSnooze = false), AlarmMath.nextRing(config, at(11, 7, 10)))
    }

    @Test
    fun afterRing_onceAlarmTurnsOffButRepeatingStays() {
        val once = AlarmConfig(enabled = true, repeatMask = 0, snoozeUntilMillis = 123L)
        assertEquals(AlarmConfig(enabled = false, repeatMask = 0), AlarmMath.afterRing(once, wasSnooze = false))
        val daily = AlarmConfig(enabled = true, repeatMask = AlarmMath.EVERY_DAY)
        assertTrue(AlarmMath.afterRing(daily, wasSnooze = false).enabled)
    }

    @Test
    fun afterRing_snoozeRingKeepsEnabledState() {
        val repeating = AlarmConfig(enabled = true, repeatMask = AlarmMath.WEEKDAYS, snoozeUntilMillis = 5L)
        assertEquals(repeating.copy(snoozeUntilMillis = 0L), AlarmMath.afterRing(repeating, wasSnooze = true))
    }

    @Test
    fun dismiss_clearsSnoozeOnly() {
        val c = AlarmConfig(enabled = true, hour = 6, snoozeUntilMillis = 99L)
        assertEquals(c.copy(snoozeUntilMillis = 0L), AlarmMath.dismiss(c))
    }

    // ---- 文字 ----

    @Test
    fun timeText_followsHourFormat() {
        assertEquals("07:05", AlarmMath.timeText(7, 5, use24Hour = true))
        assertEquals("上午 7:05", AlarmMath.timeText(7, 5, use24Hour = false))
        assertEquals("下午 12:00", AlarmMath.timeText(12, 0, use24Hour = false))
    }

    @Test
    fun repeatText_presetsAndCustom() {
        assertEquals("只響一次", AlarmMath.repeatText(0))
        assertEquals("每天", AlarmMath.repeatText(AlarmMath.EVERY_DAY))
        assertEquals("平日（週一到週五）", AlarmMath.repeatText(AlarmMath.WEEKDAYS))
        assertEquals("週末", AlarmMath.repeatText(AlarmMath.WEEKEND))
        assertEquals("週一、週三、週五", AlarmMath.repeatText(AlarmMath.dayBit(1) or AlarmMath.dayBit(3) or AlarmMath.dayBit(5)))
    }

    @Test
    fun ringText_todayTomorrowAndWeekday() {
        val now = at(11, 22, 0)
        assertEquals("今天 23:00", AlarmMath.ringText(at(11, 23, 0), now, true))
        assertEquals("明天 上午 7:00", AlarmMath.ringText(at(12, 7, 0), now, false))
        assertEquals("週五 07:00", AlarmMath.ringText(at(13, 7, 0), now, true))
    }

    @Test
    fun untilText_roundsUpToMinutes() {
        val now = at(11, 22, 0)
        assertEquals("9 小時後", AlarmMath.untilText(at(12, 7, 0), now))
        assertEquals("9 小時 15 分鐘後", AlarmMath.untilText(at(12, 7, 15), now))
        assertEquals("5 分鐘後", AlarmMath.untilText(at(11, 22, 5), now))
        assertEquals("1 分鐘後", AlarmMath.untilText(at(11, 22, 0, 20), now))
    }

    @Test
    fun statusText_addsWeekdayOnlyWhenFarAway() {
        val now = at(11, 22, 0)
        assertEquals("鬧鐘 07:00", AlarmMath.statusText(AlarmRing(at(12, 7, 0), false), now, true))
        assertEquals("貪睡 22:05", AlarmMath.statusText(AlarmRing(at(11, 22, 5), true), now, true))
        assertEquals("鬧鐘 週五 07:00", AlarmMath.statusText(AlarmRing(at(13, 7, 0), false), now, true))
    }
}
