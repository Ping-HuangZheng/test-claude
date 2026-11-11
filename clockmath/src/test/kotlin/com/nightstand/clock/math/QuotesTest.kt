package com.nightstand.clock.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class QuotesTest {

    private val tenMinutes = 10 * 60_000L

    @Test
    fun hasSixtyQuotesTwentyFromEachSource() {
        assertEquals(60, Quotes.all.size)
        assertEquals(20, Quotes.bible.size)
        assertEquals(20, Quotes.analects.size)
        assertEquals(20, Quotes.laozi.size)
    }

    @Test
    fun quotesAreNotBlankAndNotDuplicated() {
        Quotes.all.forEach {
            assertTrue(it.text.isNotBlank() && it.source.isNotBlank(), "空白名言：$it")
        }
        assertEquals(Quotes.all.size, Quotes.all.map { it.text }.toSet().size, "有重複的名言")
    }

    @Test
    fun sourcesAreLabelledCorrectly() {
        assertTrue(Quotes.bible.all { it.source.startsWith("聖經‧") })
        assertTrue(Quotes.analects.all { it.source.startsWith("《論語‧") })
        assertTrue(Quotes.laozi.all { it.source.startsWith("《老子》") })
    }

    @Test
    fun consecutiveQuotesComeFromDifferentBooks() {
        fun book(q: Quote) = q.source.take(3)
        for (i in 0 until Quotes.all.size) {
            val a = Quotes.all[i]
            val b = Quotes.all[(i + 1) % Quotes.all.size]
            assertNotEquals(book(a), book(b), "第 $i 句和下一句出自同一本書")
        }
    }

    @Test
    fun indexStaysTheSameWithinTenMinutes() {
        val start = 1_000L * tenMinutes
        val i = Quotes.indexAt(start)
        assertEquals(i, Quotes.indexAt(start + tenMinutes - 1))
    }

    @Test
    fun indexAdvancesEveryTenMinutesAndWraps() {
        val start = 0L
        assertEquals(0, Quotes.indexAt(start))
        assertEquals(1, Quotes.indexAt(start + tenMinutes))
        assertEquals(59, Quotes.indexAt(start + 59 * tenMinutes))
        assertEquals(0, Quotes.indexAt(start + 60 * tenMinutes))
    }

    @Test
    fun indexHandlesNegativeTimeAndCustomInterval() {
        assertEquals(59, Quotes.indexAt(-1L))
        assertEquals(1, Quotes.indexAt(5 * 60_000L, intervalMinutes = 5))
    }
}
