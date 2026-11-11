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

    // ---- 日期 ----

    @Test
    fun dateText_formatsMonthDayAndWeekday() {
        assertEquals("11月11日 週三", ClockMath.dateText(11, 11, 3))
        assertEquals("1月1日 週日", ClockMath.dateText(1, 1, 7))
        assertEquals("12月31日 週一", ClockMath.dateText(12, 31, 1))
    }

    @Test
    fun dateText_rejectsInvalidWeekday() {
        assertFailsWith<IllegalArgumentException> { ClockMath.dateText(1, 1, 0) }
        assertFailsWith<IllegalArgumentException> { ClockMath.dateText(1, 1, 8) }
    }

    // ---- 電量 ----

    @Test
    fun batteryPercent_scalesAndRounds() {
        assertEquals(85, ClockMath.batteryPercent(85, 100))
        assertEquals(50, ClockMath.batteryPercent(128, 255)) // 50.2% → 50
        assertEquals(100, ClockMath.batteryPercent(255, 255))
        assertEquals(0, ClockMath.batteryPercent(0, 100))
    }

    @Test
    fun batteryPercent_invalidDataIsNull() {
        assertNull(ClockMath.batteryPercent(-1, 100))
        assertNull(ClockMath.batteryPercent(50, 0))
    }

    @Test
    fun batteryPercent_clampsAboveScale() {
        assertEquals(100, ClockMath.batteryPercent(120, 100))
    }

    @Test
    fun chargeState_unpluggedIsAlwaysDischarging() {
        assertEquals(ChargeState.DISCHARGING, ClockMath.chargeState(status = 5, plugged = 0))
        assertEquals(ChargeState.DISCHARGING, ClockMath.chargeState(status = 2, plugged = 0))
    }

    @Test
    fun chargeState_pluggedStates() {
        assertEquals(ChargeState.CHARGING, ClockMath.chargeState(status = 2, plugged = 1))
        assertEquals(ChargeState.FULL, ClockMath.chargeState(status = 5, plugged = 2))
        assertEquals(ChargeState.PLUGGED_NOT_CHARGING, ClockMath.chargeState(status = 4, plugged = 4))
    }

    @Test
    fun batteryLabel_textPerState() {
        assertEquals("85%", ClockMath.batteryLabel(85, ChargeState.DISCHARGING))
        assertEquals("充電中 85%", ClockMath.batteryLabel(85, ChargeState.CHARGING))
        assertEquals("已充飽 100%", ClockMath.batteryLabel(100, ChargeState.FULL))
        assertEquals("已接電源 80%", ClockMath.batteryLabel(80, ChargeState.PLUGGED_NOT_CHARGING))
        assertEquals("--%", ClockMath.batteryLabel(null, ChargeState.DISCHARGING))
    }

    // ---- 指針角度 ----

    private fun assertAngles(expected: HandAngles, actual: HandAngles) {
        assertEquals(expected.hour, actual.hour, 0.001f, "hour")
        assertEquals(expected.minute, actual.minute, 0.001f, "minute")
        assertEquals(expected.second, actual.second, 0.001f, "second")
    }

    @Test
    fun handAngles_midnightAndNoonPointUp() {
        assertAngles(HandAngles(0f, 0f, 0f), ClockMath.handAngles(0, 0, 0))
        assertAngles(HandAngles(0f, 0f, 0f), ClockMath.handAngles(12, 0, 0))
    }

    @Test
    fun handAngles_threeOClockIsNinetyDegrees() {
        assertAngles(HandAngles(90f, 0f, 0f), ClockMath.handAngles(15, 0, 0))
    }

    @Test
    fun handAngles_hourHandMovesWithMinutes() {
        // 9:30 → 時針在 9 和 10 中間（285 度），分針指向 6（180 度）
        assertAngles(HandAngles(285f, 180f, 0f), ClockMath.handAngles(9, 30, 0))
    }

    @Test
    fun handAngles_secondsMoveMinuteHand() {
        // 0:00:30 → 秒針 180 度、分針前進半格 3 度、時針前進 0.25 度
        assertAngles(HandAngles(0.25f, 3f, 180f), ClockMath.handAngles(0, 0, 30))
    }

    @Test
    fun handAngles_justBeforeMidnight() {
        val a = ClockMath.handAngles(23, 59, 59)
        assertTrue(a.hour < 360f && a.hour > 359.9f)
        assertTrue(a.minute < 360f && a.minute > 359.8f)
        assertEquals(354f, a.second, 0.001f)
    }

    // ---- 縮放 ----

    @Test
    fun clampScale_limitsRange() {
        assertEquals(0.5f, ClockMath.clampScale(0.1f))
        assertEquals(2f, ClockMath.clampScale(5f))
        assertEquals(1.3f, ClockMath.clampScale(1.3f))
    }

    // ---- 手勢 ----

    @Test
    fun isInEdgeZone_onlyNearLeftOrRightEdges() {
        assertTrue(ClockMath.isInEdgeZone(10f, 1000f))
        assertTrue(ClockMath.isInEdgeZone(990f, 1000f))
        assertFalse(ClockMath.isInEdgeZone(500f, 1000f))
        assertFalse(ClockMath.isInEdgeZone(150f, 1000f))
    }

    @Test
    fun classifyDrag_horizontalIsSwipeAnywhere() {
        assertEquals(DragKind.SWIPE, ClockMath.classifyDrag(500f, 1000f, dx = -40f, dy = 10f))
        assertEquals(DragKind.SWIPE, ClockMath.classifyDrag(20f, 1000f, dx = 40f, dy = -10f))
    }

    @Test
    fun classifyDrag_verticalAtEdgeIsBrightness() {
        assertEquals(DragKind.BRIGHTNESS, ClockMath.classifyDrag(20f, 1000f, dx = 5f, dy = -40f))
        assertEquals(DragKind.BRIGHTNESS, ClockMath.classifyDrag(980f, 1000f, dx = -5f, dy = 40f))
    }

    @Test
    fun classifyDrag_verticalInMiddleDoesNothing() {
        assertEquals(DragKind.NONE, ClockMath.classifyDrag(500f, 1000f, dx = 5f, dy = 40f))
    }

    @Test
    fun swipeDirection_needsEnoughDistance() {
        assertEquals(1, ClockMath.swipeDirection(-200f, 1000f))
        assertEquals(-1, ClockMath.swipeDirection(200f, 1000f))
        assertEquals(0, ClockMath.swipeDirection(-100f, 1000f))
        assertEquals(0, ClockMath.swipeDirection(119f, 1000f))
    }
}
