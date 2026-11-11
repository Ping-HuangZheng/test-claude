package com.nightstand.clock.math

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * 鬧鐘設定（純資料，不依賴 Android）。
 *
 * @property repeatMask 重複的星期：bit0 = 週一 … bit6 = 週日；0 代表只響一次。
 * @property snoozeUntilMillis 貪睡到這個時間再響（epoch 毫秒）；0 代表沒有貪睡。
 */
data class AlarmConfig(
    val enabled: Boolean = false,
    val hour: Int = 7,
    val minute: Int = 0,
    val repeatMask: Int = 0,
    val snoozeUntilMillis: Long = 0L,
)

/** 下一次響鈴：時間，以及這次是不是貪睡後的再響。 */
data class AlarmRing(val at: ZonedDateTime, val isSnooze: Boolean)

/** 鬧鐘的時間計算。 */
object AlarmMath {

    const val SNOOZE_MINUTES = 5L
    const val RING_TIMEOUT_MINUTES = 10L

    const val EVERY_DAY = 0b1111111
    const val WEEKDAYS = 0b0011111
    const val WEEKEND = 0b1100000

    /** [isoDayOfWeek]：1 = 週一 … 7 = 週日。 */
    fun dayBit(isoDayOfWeek: Int): Int {
        require(isoDayOfWeek in 1..7) { "isoDayOfWeek 超出範圍：$isoDayOfWeek" }
        return 1 shl (isoDayOfWeek - 1)
    }

    fun isDayEnabled(mask: Int, isoDayOfWeek: Int): Boolean = mask and dayBit(isoDayOfWeek) != 0

    fun toggleDay(mask: Int, isoDayOfWeek: Int): Int = mask xor dayBit(isoDayOfWeek)

    /**
     * 下一次鬧鐘時間（一定晚於 [now]）。
     * 只響一次（mask = 0）：今天這個時間還沒到就是今天，否則明天。
     * 有重複：從今天起找第一個有勾選、而且時間還沒過的日子（最多找到下週同一天）。
     */
    fun nextTrigger(now: ZonedDateTime, hour: Int, minute: Int, repeatMask: Int): ZonedDateTime {
        require(hour in 0..23 && minute in 0..59) { "時間超出範圍：$hour:$minute" }
        val time = LocalTime.of(hour, minute)
        for (offset in 0L..7L) {
            val date = now.toLocalDate().plusDays(offset)
            if (repeatMask != 0 && !isDayEnabled(repeatMask, date.dayOfWeek.value)) continue
            val candidate = ZonedDateTime.of(date, time, now.zone)
            if (candidate.isAfter(now)) return candidate
        }
        error("repeatMask 無效：$repeatMask")
    }

    /** 綜合一般鬧鐘和貪睡，回傳最早的下一次響鈴；都沒有就回傳 null。 */
    fun nextRing(config: AlarmConfig, now: ZonedDateTime): AlarmRing? {
        val nowMillis = now.toInstant().toEpochMilli()
        val snooze = config.snoozeUntilMillis
            .takeIf { it > nowMillis }
            ?.let { ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), now.zone) }
        val regular = if (config.enabled) nextTrigger(now, config.hour, config.minute, config.repeatMask) else null
        return when {
            snooze != null && (regular == null || !regular.isBefore(snooze)) -> AlarmRing(snooze, isSnooze = true)
            regular != null -> AlarmRing(regular, isSnooze = false)
            else -> null
        }
    }

    /** 響鈴之後：清掉貪睡；只響一次的鬧鐘（不是貪睡那次）響完就自動關閉。 */
    fun afterRing(config: AlarmConfig, wasSnooze: Boolean): AlarmConfig = config.copy(
        snoozeUntilMillis = 0L,
        enabled = if (!wasSnooze && config.repeatMask == 0) false else config.enabled,
    )

    /** 按下貪睡：[SNOOZE_MINUTES] 分鐘後再響。 */
    fun snooze(config: AlarmConfig, nowMillis: Long): AlarmConfig =
        config.copy(snoozeUntilMillis = nowMillis + SNOOZE_MINUTES * 60_000L)

    /** 按下停止：取消貪睡（一般鬧鐘照原本設定繼續）。 */
    fun dismiss(config: AlarmConfig): AlarmConfig = config.copy(snoozeUntilMillis = 0L)

    // ---- 顯示文字 ----

    fun timeText(hour: Int, minute: Int, use24Hour: Boolean): String {
        val p = ClockMath.timeParts(hour, minute, 0, use24Hour, showSeconds = false)
        return listOfNotNull(p.period, "${p.hour}:${p.minute}").joinToString(" ")
    }

    fun repeatText(mask: Int): String = when (mask and EVERY_DAY) {
        0 -> "只響一次"
        EVERY_DAY -> "每天"
        WEEKDAYS -> "平日（週一到週五）"
        WEEKEND -> "週末"
        else -> (1..7).filter { isDayEnabled(mask, it) }.joinToString("、") { ClockMath.weekdayName(it) }
    }

    /** 「今天 07:00」「明天 上午 7:00」「週四 07:00」。 */
    fun ringText(ring: ZonedDateTime, now: ZonedDateTime, use24Hour: Boolean): String {
        val days = ChronoUnit.DAYS.between(now.toLocalDate(), ring.toLocalDate())
        val day = when (days) {
            0L -> "今天"
            1L -> "明天"
            else -> ClockMath.weekdayName(ring.dayOfWeek.value)
        }
        return "$day ${timeText(ring.hour, ring.minute, use24Hour)}"
    }

    /** 「7 小時 15 分鐘後」；不足一分鐘時無條件進位成 1 分鐘。 */
    fun untilText(ring: ZonedDateTime, now: ZonedDateTime): String {
        val seconds = Duration.between(now, ring).seconds.coerceAtLeast(0)
        val totalMinutes = (seconds + 59) / 60
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h == 0L -> "$m 分鐘後"
            m == 0L -> "$h 小時後"
            else -> "$h 小時 $m 分鐘後"
        }
    }

    /** 時鐘畫面左上角的提示：24 小時內只顯示時間，再遠就加上星期。 */
    fun statusText(ring: AlarmRing, now: ZonedDateTime, use24Hour: Boolean): String {
        val prefix = if (ring.isSnooze) "貪睡" else "鬧鐘"
        val time = timeText(ring.at.hour, ring.at.minute, use24Hour)
        val within24h = Duration.between(now, ring.at) <= Duration.ofHours(24)
        return if (within24h) "$prefix $time" else "$prefix ${ClockMath.weekdayName(ring.at.dayOfWeek.value)} $time"
    }
}
