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

/** 指針角度，單位是度，從 12 點方向順時針算。 */
data class HandAngles(val hour: Float, val minute: Float, val second: Float)

/** 防烙印的畫面位移（單位 dp）。 */
data class PixelShift(val x: Float, val y: Float)

/** 單指拖曳的用途。 */
enum class DragKind {
    /** 在左右兩側上下滑：調亮度。 */
    BRIGHTNESS,
    /** 水平滑動：切換錶面。 */
    SWIPE,
    /** 在中間上下滑：不做事。 */
    NONE,
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

    // ---- 指針角度 ----

    /** 時針會隨分秒緩慢前進；分針隨秒前進；秒針每秒 6 度。 */
    fun handAngles(hour24: Int, minute: Int, second: Int): HandAngles {
        val sec = second.toFloat()
        val min = minute + sec / 60f
        val hr = (hour24 % 12) + min / 60f
        return HandAngles(
            hour = hr * 30f,
            minute = min * 6f,
            second = sec * 6f,
        )
    }

    // ---- 縮放 ----

    const val MIN_FACE_SCALE = 0.5f
    const val MAX_FACE_SCALE = 2f

    fun clampScale(scale: Float): Float = scale.coerceIn(MIN_FACE_SCALE, MAX_FACE_SCALE)

    // ---- 手勢 ----

    /** 左右各佔畫面寬度 15% 的直條是亮度調整區。 */
    const val EDGE_ZONE_FRACTION = 0.15f

    /** 水平滑動超過畫面寬度 12% 才算切換錶面。 */
    const val SWIPE_THRESHOLD_FRACTION = 0.12f

    fun isInEdgeZone(x: Float, width: Float, edgeFraction: Float = EDGE_ZONE_FRACTION): Boolean =
        x < width * edgeFraction || x > width * (1f - edgeFraction)

    /**
     * 手指移動超過觸控門檻後，判斷這次拖曳要做什麼：
     * 以水平為主 → 切換錶面；以垂直為主且從兩側開始 → 調亮度；其餘不處理。
     */
    fun classifyDrag(
        startX: Float,
        width: Float,
        dx: Float,
        dy: Float,
        edgeFraction: Float = EDGE_ZONE_FRACTION,
    ): DragKind = when {
        kotlin.math.abs(dx) >= kotlin.math.abs(dy) -> DragKind.SWIPE
        isInEdgeZone(startX, width, edgeFraction) -> DragKind.BRIGHTNESS
        else -> DragKind.NONE
    }

    /** 往左滑（dx 為負）→ 下一個錶面 +1；往右滑 → 上一個 -1；距離不夠 → 0。 */
    fun swipeDirection(dx: Float, width: Float, threshold: Float = SWIPE_THRESHOLD_FRACTION): Int = when {
        dx <= -width * threshold -> 1
        dx >= width * threshold -> -1
        else -> 0
    }

    // ---- 防烙印 ----

    /** 防烙印位移的最大距離（dp）。 */
    const val BURN_IN_MAX_SHIFT_DP = 10f
    private const val BURN_IN_PERIOD_X = 17
    private const val BURN_IN_PERIOD_Y = 13

    /** 整個 x、y 位移路徑重複一次的分鐘數（17 × 13 = 221 分鐘）。 */
    const val BURN_IN_CYCLE_MINUTES = BURN_IN_PERIOD_X * BURN_IN_PERIOD_Y

    /**
     * 第 [minuteIndex] 分鐘時畫面要移多少。
     * x、y 用兩個互質的週期（17、13 分鐘）走正弦曲線，畫出類似 Lissajous 的路徑：
     * 每分鐘只移幾 dp（不明顯），但長時間下來會走遍整個 ±[maxShift] 的範圍，像素負擔被分散。
     */
    fun burnInShift(minuteIndex: Long, maxShift: Float = BURN_IN_MAX_SHIFT_DP): PixelShift {
        val ix = Math.floorMod(minuteIndex, BURN_IN_PERIOD_X.toLong()).toDouble()
        val iy = Math.floorMod(minuteIndex, BURN_IN_PERIOD_Y.toLong()).toDouble()
        return PixelShift(
            x = (maxShift * kotlin.math.sin(2 * Math.PI * ix / BURN_IN_PERIOD_X)).toFloat(),
            y = (maxShift * kotlin.math.sin(2 * Math.PI * iy / BURN_IN_PERIOD_Y)).toFloat(),
        )
    }

    // ---- 亮度 ----

    /** 亮度下限 1%，避免整個畫面全黑看不到。 */
    const val MIN_BRIGHTNESS = 0.01f
    const val MAX_BRIGHTNESS = 1f

    /** Android 視窗亮度「不覆寫、跟隨系統」的值（WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE）。 */
    const val BRIGHTNESS_SYSTEM = -1f

    fun clampBrightness(value: Float): Float = value.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)

    /**
     * 要套用到視窗的亮度：夜間模式用夜間亮度；白天若沒設定過（小於 0）就跟隨系統。
     */
    fun windowBrightness(nightMode: Boolean, dayBrightness: Float, nightBrightness: Float): Float = when {
        nightMode -> clampBrightness(nightBrightness)
        dayBrightness < 0f -> BRIGHTNESS_SYSTEM
        else -> clampBrightness(dayBrightness)
    }

    private fun twoDigits(value: Int): String = value.toString().padStart(2, '0')
}
