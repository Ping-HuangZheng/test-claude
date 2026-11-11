package com.nightstand.clock.math

/** 時間格式設定：跟隨系統、強制 12 小時制、強制 24 小時制。 */
enum class HourFormat { SYSTEM, H12, H24 }

/**
 * 已經格式化好、可以直接顯示的時間各部分。
 *
 * @property hour 24 小時制補零（"07"）；12 小時制不補零（"7"）。
 * @property second 不顯示秒數時為 null。
 * @property period 12 小時制時為「上午」或「下午」，24 小時制時為 null。
 */
data class TimeParts(
    val hour: String,
    val minute: String,
    val second: String?,
    val period: String?,
)

/** 充電狀態（已經把 Android 電池廣播的原始值整理過）。 */
enum class ChargeState {
    /** 沒接電源。 */
    DISCHARGING,
    /** 接著電源、正在充電。 */
    CHARGING,
    /** 接著電源、已充飽。 */
    FULL,
    /** 接著電源但沒有在充（例如手機的電池保護暫停在 80%）。 */
    PLUGGED_NOT_CHARGING,
}

/**
 * 和 Android 無關的時鐘計算。這裡的函式都是純函式，方便用 JVM 單元測試驗證。
 */
object ClockMath {

    const val SECOND_MILLIS = 1_000L
    const val MINUTE_MILLIS = 60_000L

    /** 在 [count] 個項目間循環移動 [delta] 格，回傳新的索引（負數也會正確繞回）。 */
    fun cycleIndex(current: Int, delta: Int, count: Int): Int {
        require(count > 0) { "count 必須大於 0" }
        return Math.floorMod(current + delta, count)
    }

    /** 依設定和系統偏好決定要不要用 24 小時制。 */
    fun use24Hour(format: HourFormat, systemIs24Hour: Boolean): Boolean = when (format) {
        HourFormat.SYSTEM -> systemIs24Hour
        HourFormat.H12 -> false
        HourFormat.H24 -> true
    }

    /**
     * 把 24 小時制的時、分、秒轉成要顯示的文字。
     * 12 小時制：0 點顯示為「上午 12」，12 點顯示為「下午 12」。
     */
    fun timeParts(
        hour24: Int,
        minute: Int,
        second: Int,
        use24Hour: Boolean,
        showSeconds: Boolean,
    ): TimeParts {
        require(hour24 in 0..23) { "hour24 超出範圍：$hour24" }
        require(minute in 0..59) { "minute 超出範圍：$minute" }
        require(second in 0..59) { "second 超出範圍：$second" }

        val hourText: String
        val period: String?
        if (use24Hour) {
            hourText = twoDigits(hour24)
            period = null
        } else {
            val h12 = if (hour24 % 12 == 0) 12 else hour24 % 12
            hourText = h12.toString()
            period = if (hour24 < 12) "上午" else "下午"
        }
        return TimeParts(
            hour = hourText,
            minute = twoDigits(minute),
            second = if (showSeconds) twoDigits(second) else null,
            period = period,
        )
    }

    /** 顯示秒數時每秒更新，否則每分鐘更新。 */
    fun tickIntervalMillis(showSeconds: Boolean): Long =
        if (showSeconds) SECOND_MILLIS else MINUTE_MILLIS

    /**
     * 距離下一個整秒或整分還有幾毫秒，讓畫面剛好在跳秒或跳分時更新。
     * 回傳值一定在 1..[intervalMillis] 之間。
     */
    fun millisUntilNextTick(nowMillis: Long, intervalMillis: Long): Long {
        require(intervalMillis > 0) { "intervalMillis 必須大於 0" }
        return intervalMillis - Math.floorMod(nowMillis, intervalMillis)
    }

    // ---- 日期 ----

    private val WEEKDAYS = listOf("週一", "週二", "週三", "週四", "週五", "週六", "週日")

    /** 「11月11日 週三」。[isoDayOfWeek]：1 = 週一 … 7 = 週日（和 java.time.DayOfWeek 相同）。 */
    fun dateText(month: Int, day: Int, isoDayOfWeek: Int): String {
        require(month in 1..12) { "month 超出範圍：$month" }
        require(day in 1..31) { "day 超出範圍：$day" }
        return "${month}月${day}日 ${weekdayName(isoDayOfWeek)}"
    }

    fun weekdayName(isoDayOfWeek: Int): String {
        require(isoDayOfWeek in 1..7) { "isoDayOfWeek 超出範圍：$isoDayOfWeek" }
        return WEEKDAYS[isoDayOfWeek - 1]
    }

    // ---- 電量 ----

    // 和 android.os.BatteryManager 的常數值相同，這裡複製一份以免依賴 Android。
    const val BATTERY_STATUS_CHARGING = 2
    const val BATTERY_STATUS_FULL = 5

    /** 電量百分比（四捨五入，0..100）；資料無效時回傳 null。 */
    fun batteryPercent(level: Int, scale: Int): Int? {
        if (level < 0 || scale <= 0) return null
        return Math.round(level * 100f / scale).coerceIn(0, 100)
    }

    /**
     * 由電池廣播的 status 和 plugged 判斷充電狀態。
     * 以「有沒有接電源」為準：拔掉電源後系統有時仍短暫回報 FULL，這種情況算放電。
     */
    fun chargeState(status: Int, plugged: Int): ChargeState = when {
        plugged == 0 -> ChargeState.DISCHARGING
        status == BATTERY_STATUS_CHARGING -> ChargeState.CHARGING
        status == BATTERY_STATUS_FULL -> ChargeState.FULL
        else -> ChargeState.PLUGGED_NOT_CHARGING
    }

    /** 右上角顯示的電量文字，例如「充電中 85%」。 */
    fun batteryLabel(percent: Int?, state: ChargeState): String {
        val pct = percent?.let { "$it%" } ?: "--%"
        return when (state) {
            ChargeState.DISCHARGING -> pct
            ChargeState.CHARGING -> "充電中 $pct"
            ChargeState.FULL -> "已充飽 $pct"
            ChargeState.PLUGGED_NOT_CHARGING -> "已接電源 $pct"
        }
    }

    private fun twoDigits(value: Int): String = value.toString().padStart(2, '0')
}
