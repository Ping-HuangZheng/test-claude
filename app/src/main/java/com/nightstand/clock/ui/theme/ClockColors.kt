package com.nightstand.clock.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** 時鐘畫面的配色。白天是黑底白字；夜間是黑底暗紅字，避免藍光刺眼。 */
@Immutable
data class ClockColors(
    /** 時間數字、指針。 */
    val primary: Color,
    /** 日期、電量、名言等次要文字。 */
    val secondary: Color,
    /** 秒針等強調色。 */
    val accent: Color,
    /** 翻頁卡片底色。 */
    val card: Color,
    val background: Color = Color.Black,
) {
    companion object {
        val Day = ClockColors(
            primary = Color(0xFFEDEDED),
            secondary = Color(0xFF9A9A9A),
            accent = Color(0xFFFF6E40),
            card = Color(0xFF1C1C1C),
        )
        val Night = ClockColors(
            primary = Color(0xFFC62828),
            secondary = Color(0xFF7F1D1D),
            accent = Color(0xFFE53935),
            card = Color(0xFF160404),
        )

        fun of(nightMode: Boolean) = if (nightMode) Night else Day
    }
}
