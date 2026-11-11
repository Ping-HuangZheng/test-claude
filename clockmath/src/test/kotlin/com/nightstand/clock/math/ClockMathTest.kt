package com.nightstand.clock.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClockMathTest {

    // ---- cycleIndex ----

    @Test
    fun cycleIndex_wrapsForwardAndBackward() {
        assertEquals(1, ClockMath.cycleIndex(0, 1, 3))
        assertEquals(0, ClockMath.cycleIndex(2, 1, 3))
        assertEquals(2, ClockMath.cycleIndex(0, -1, 3))
        assertEquals(0, ClockMath.cycleIndex(1, -4, 3))
    }

    // ---- use24Hour ----

    @Test
    fun use24Hour_followsSystemOnlyWhenSetToSystem() {
        assertTrue(ClockMath.use24Hour(HourFormat.SYSTEM, systemIs24Hour = true))
        assertFalse(ClockMath.use24Hour(HourFormat.SYSTEM, systemIs24Hour = false))
        assertFalse(ClockMath.use24Hour(HourFormat.H12, systemIs24Hour = true))
        assertTrue(ClockMath.use24Hour(HourFormat.H24, systemIs24Hour = false))
    }

    // ---- timeParts ----

    @Test
    fun timeParts_24Hour_padsHourAndHasNoPeriod() {
        val p = ClockMath.timeParts(7, 5, 9, use24Hour = true, showSeconds = true)
        assertEquals(TimeParts("07", "05", "09", null), p)
    }

    @Test
    fun timeParts_hidesSecondsWhenDisabled() {
        assertNull(ClockMath.timeParts(23, 59, 59, use24Hour = true, showSeconds = false).second)
    }

    @Test
    fun timeParts_12Hour_midnightIsTwelveAm() {
        val p = ClockMath.timeParts(0, 30, 0, use24Hour = false, showSeconds = false)
        assertEquals("12", p.hour)
        assertEquals("上午", p.period)
    }

    @Test
    fun timeParts_12Hour_noonIsTwelvePm() {
        val p = ClockMath.timeParts(12, 0, 0, use24Hour = false, showSeconds = false)
        assertEquals("12", p.hour)
        assertEquals("下午", p.period)
    }

    @Test
    fun timeParts_12Hour_afternoonHasNoLeadingZero() {
        val p = ClockMath.timeParts(13, 7, 0, use24Hour = false, showSeconds = false)
        assertEquals("1", p.hour)
        assertEquals("07", p.minute)
        assertEquals("下午", p.period)
    }

    @Test
    fun timeParts_12Hour_lastMinuteBeforeNoonIsAm() {
        val p = ClockMath.timeParts(11, 59, 0, use24Hour = false, showSeconds = false)
        assertEquals("11", p.hour)
        assertEquals("上午", p.period)
    }

    @Test
    fun timeParts_rejectsOutOfRangeValues() {
        assertFailsWith<IllegalArgumentException> { ClockMath.timeParts(24, 0, 0, true, false) }
        assertFailsWith<IllegalArgumentException> { ClockMath.timeParts(0, 60, 0, true, false) }
    }

    // ---- ticks ----

    @Test
    fun tickInterval_dependsOnSeconds() {
        assertEquals(1_000L, ClockMath.tickIntervalMillis(showSeconds = true))
        assertEquals(60_000L, ClockMath.tickIntervalMillis(showSeconds = false))
    }

    @Test
    fun millisUntilNextTick_alignsToBoundary() {
        assertEquals(60_000L, ClockMath.millisUntilNextTick(120_000L, 60_000L))
        assertEquals(1L, ClockMath.millisUntilNextTick(119_999L, 60_000L))
        assertEquals(750L, ClockMath.millisUntilNextTick(10_250L, 1_000L))
    }
}
