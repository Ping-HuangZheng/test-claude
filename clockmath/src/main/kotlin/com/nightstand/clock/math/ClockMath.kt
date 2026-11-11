package com.nightstand.clock.math

/**
 * 和 Android 無關的時鐘計算。這裡的函式都是純函式，方便用 JVM 單元測試驗證。
 */
object ClockMath {

    /** 在 [count] 個項目間循環移動 [delta] 格，回傳新的索引（負數也會正確繞回）。 */
    fun cycleIndex(current: Int, delta: Int, count: Int): Int {
        require(count > 0) { "count 必須大於 0" }
        return Math.floorMod(current + delta, count)
    }
}
