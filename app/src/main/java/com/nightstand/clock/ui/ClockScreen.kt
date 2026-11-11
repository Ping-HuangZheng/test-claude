package com.nightstand.clock.ui

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.nightstand.clock.data.BatteryInfo
import com.nightstand.clock.data.ClockSettings
import com.nightstand.clock.data.FaceType
import com.nightstand.clock.data.rememberBatteryInfo
import com.nightstand.clock.data.rememberClockTime
import com.nightstand.clock.math.ClockMath
import com.nightstand.clock.ui.faces.AnalogFace
import com.nightstand.clock.ui.faces.DigitalFace
import com.nightstand.clock.ui.faces.FlipFace
import com.nightstand.clock.ui.theme.ClockColors
import java.time.ZonedDateTime

typealias SettingsUpdater = ((ClockSettings) -> ClockSettings) -> Unit

/**
 * 時鐘主畫面（有狀態的外層）：讀時間、電量，處理手勢。
 * [interactive] 為 false 時（螢幕保護程式）只顯示，不接手勢。
 */
@Composable
fun ClockScreen(
    settings: ClockSettings,
    onUpdate: SettingsUpdater,
    interactive: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val now by rememberClockTime(ClockMath.tickIntervalMillis(settings.showSeconds))
    val battery by rememberBatteryInfo()

    // 縮放中先用暫存值，放開手指才寫入設定。
    var liveScale by remember { mutableStateOf<Float?>(null) }
    var swipeDirection by remember { mutableIntStateOf(1) }

    val gestures = ClockGestureCallbacks(
        onTap = {},
        onSwipe = { dir ->
            swipeDirection = dir
            liveScale = null
            onUpdate { s ->
                s.copy(face = FaceType.entries[ClockMath.cycleIndex(s.face.ordinal, dir, FaceType.entries.size)])
            }
        },
        onZoom = { factor ->
            liveScale = ClockMath.clampScale((liveScale ?: settings.faceScale) * factor)
        },
        onZoomEnd = {
            liveScale?.let { scale ->
                onUpdate { s ->
                    s.copy(faceScales = s.faceScales.toMutableList().also { it[s.face.ordinal] = scale })
                }
            }
            liveScale = null
        },
        onBrightnessStart = {},
        onBrightnessDrag = {},
        onBrightnessEnd = {},
    )

    ClockContent(
        now = now,
        battery = battery,
        settings = settings,
        systemIs24Hour = DateFormat.is24HourFormat(context),
        faceScale = liveScale ?: settings.faceScale,
        swipeDirection = swipeDirection,
        modifier = if (interactive) modifier.clockGestures(gestures) else modifier,
    )
}

/** 時鐘主畫面（純顯示的內層）：所有資料都由參數傳入，方便預覽和截圖測試。 */
@Composable
fun ClockContent(
    now: ZonedDateTime,
    battery: BatteryInfo,
    settings: ClockSettings,
    systemIs24Hour: Boolean,
    faceScale: Float,
    swipeDirection: Int,
    modifier: Modifier = Modifier,
) {
    val colors = ClockColors.of(settings.nightMode)
    val use24 = ClockMath.use24Hour(settings.hourFormat, systemIs24Hour)
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

        Box(Modifier.fillMaxSize().padding(infoSize)) {
            StatusRow(
                date = date,
                extra = null,
                battery = battery,
                color = colors.secondary,
                textSize = infoSize,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            AnimatedContent(
                targetState = settings.face,
                transitionSpec = {
                    val dir = swipeDirection
                    (slideInHorizontally { it * dir } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it * dir } + fadeOut())
                },
                label = "face",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = infoSize * 2.5f),
            ) { face ->
                val faceModifier = Modifier.fillMaxSize()
                when (face) {
                    FaceType.DIGITAL -> DigitalFace(time, colors, faceScale, faceModifier)
                    FaceType.ANALOG -> AnalogFace(
                        angles = ClockMath.handAngles(now.hour, now.minute, now.second),
                        showSeconds = settings.showSeconds,
                        colors = colors,
                        scale = faceScale,
                        modifier = faceModifier,
                    )
                    FaceType.FLIP -> FlipFace(time, colors, faceScale, faceModifier)
                }
            }
        }
    }
}
