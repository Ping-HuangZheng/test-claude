package com.nightstand.clock.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.nightstand.clock.data.ClockSettings
import com.nightstand.clock.data.rememberBatteryInfo
import com.nightstand.clock.data.rememberClockTime
import com.nightstand.clock.math.ClockMath
import com.nightstand.clock.ui.faces.DigitalFace
import com.nightstand.clock.ui.theme.ClockColors

/** 時鐘主畫面。App 和螢幕保護程式都用它。 */
@Composable
fun ClockScreen(settings: ClockSettings, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = ClockColors.of(settings.nightMode)
    val now by rememberClockTime(ClockMath.tickIntervalMillis(settings.showSeconds))
    val battery by rememberBatteryInfo()

    val use24 = ClockMath.use24Hour(settings.hourFormat, DateFormat.is24HourFormat(context))
    val time = ClockMath.timeParts(now.hour, now.minute, now.second, use24, settings.showSeconds)
    val date = ClockMath.dateText(now.monthValue, now.dayOfMonth, now.dayOfWeek.value)

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.displayCutout),
    ) {
        // 資訊文字大小跟著螢幕大小走：手機約 18dp，平板會大一些。
        val infoSize = (min(maxWidth, maxHeight) * 0.045f).coerceIn(14.dp, 30.dp)
        val edge = infoSize

        Box(Modifier.fillMaxSize().padding(edge)) {
            StatusRow(
                date = date,
                extra = null,
                battery = battery,
                color = colors.secondary,
                textSize = infoSize,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            DigitalFace(
                time = time,
                colors = colors,
                scale = settings.faceScale,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = infoSize * 2.5f),
            )
        }
    }
}
