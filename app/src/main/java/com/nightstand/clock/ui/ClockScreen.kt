package com.nightstand.clock.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.nightstand.clock.data.ClockSettings
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

    val use24 = ClockMath.use24Hour(settings.hourFormat, DateFormat.is24HourFormat(context))
    val time = ClockMath.timeParts(now.hour, now.minute, now.second, use24, settings.showSeconds)

    Box(modifier.fillMaxSize().background(colors.background)) {
        DigitalFace(
            time = time,
            colors = colors,
            scale = settings.faceScale,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
