package com.nightstand.clock.math

import kotlin.test.Test
import kotlin.test.assertEquals

class ClockMathTest {

    @Test
    fun cycleIndex_wrapsForwardAndBackward() {
        assertEquals(1, ClockMath.cycleIndex(0, 1, 3))
        assertEquals(0, ClockMath.cycleIndex(2, 1, 3))
        assertEquals(2, ClockMath.cycleIndex(0, -1, 3))
        assertEquals(0, ClockMath.cycleIndex(1, -4, 3))
    }
}
