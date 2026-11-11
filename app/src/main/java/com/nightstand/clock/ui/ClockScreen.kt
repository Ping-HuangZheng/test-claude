package com.nightstand.clock.ui

import android.content.Context
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.nightstand.clock.data.BatteryInfo
import com.nightstand.clock.data.ClockSettings
import com.nightstand.clock.data.FaceType
import com.nightstand.clock.data.rememberBatteryInfo
import com.nightstand.clock.data.rememberClockTime
import com.nightstand.clock.math.AlarmMath
import com.nightstand.clock.math.ClockMath
import com.nightstand.clock.math.Quotes
import com.nightstand.clock.ui.faces.AnalogFace
import com.nightstand.clock.ui.faces.DigitalFace
import com.nightstand.clock.ui.faces.FlipFace
import com.nightstand.clock.ui.theme.ClockColors
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

typealias SettingsUpdater = ((ClockSettings) -> ClockSettings) -> Unit

/** 控制列沒有操作多久後自動收起。 */
private const val CONTROLS_TIMEOUT_MILLIS = 5_000L

/**
 * 時鐘主畫面（有狀態的外層）：讀時間、電量，處理手勢和控制列。
 * [interactive] 為 false 時（螢幕保護程式）只顯示，不接手勢。
 */
@Composable
fun ClockScreen(
    settings: ClockSettings,
    onUpdate: SettingsUpdater,
    interactive: Boolean,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    onOpenAlarm: () -> Unit = {},
    /** 拖曳調亮度時即時套用到視窗；null 代表拖曳結束、回到設定值。 */
    onLiveBrightness: (Float?) -> Unit = {},
) {
    val context = LocalContext.current
    val now by rememberClockTime(ClockMath.tickIntervalMillis(settings.showSeconds))
    val battery by rememberBatteryInfo()
    val colors = ClockColors.of(settings.nightMode)

    // 縮放中先用暫存值，放開手指才寫入設定。
    var liveScale by remember { mutableStateOf<Float?>(null) }
    var swipeDirection by remember { mutableIntStateOf(1) }

    // 控制列：每次有操作就重新倒數 5 秒。
    var controlsVisible by remember { mutableStateOf(false) }
    var interactionCount by remember { mutableIntStateOf(0) }
    fun touched() {
        interactionCount++
    }
    LaunchedEffect(controlsVisible, interactionCount) {
        if (controlsVisible) {
            delay(CONTROLS_TIMEOUT_MILLIS)
            controlsVisible = false
        }
    }

    // 亮度調整：拖曳中即時預覽，放開才寫入設定（白天、夜間各存一個）。
    var dragStartBrightness by remember { mutableFloatStateOf(0.5f) }
    var liveBrightness by remember { mutableStateOf<Float?>(null) }
    var overlayBrightness by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(liveBrightness, overlayBrightness) {
        // 放開手指 1 秒後收起亮度提示。
        if (liveBrightness == null && overlayBrightness != null) {
            delay(1_000)
            overlayBrightness = null
        }
    }

    fun switchFace(dir: Int) {
        swipeDirection = dir
        liveScale = null
        onUpdate { s ->
            s.copy(face = FaceType.entries[ClockMath.cycleIndex(s.face.ordinal, dir, FaceType.entries.size)])
        }
        touched()
    }

    val gestures = ClockGestureCallbacks(
        onTap = {
            controlsVisible = !controlsVisible
            touched()
        },
        onSwipe = ::switchFace,
        onZoom = { factor ->
            liveScale = ClockMath.clampScale((liveScale ?: settings.faceScale) * factor)
            touched()
        },
        onZoomEnd = {
            liveScale?.let { scale ->
                onUpdate { s ->
                    s.copy(faceScales = s.faceScales.toMutableList().also { it[s.face.ordinal] = scale })
                }
            }
            liveScale = null
        },
        onBrightnessStart = {
            val current = ClockMath.windowBrightness(settings.nightMode, settings.dayBrightness, settings.nightBrightness)
            dragStartBrightness = if (current < 0f) systemBrightness(context) else current
            touched()
        },
        onBrightnessDrag = { fraction ->
            val b = ClockMath.brightnessAfterDrag(dragStartBrightness, fraction)
            liveBrightness = b
            overlayBrightness = b
            onLiveBrightness(b)
        },
        onBrightnessEnd = {
            liveBrightness?.let { b ->
                onUpdate { s -> if (s.nightMode) s.copy(nightBrightness = b) else s.copy(dayBrightness = b) }
            }
            liveBrightness = null
            onLiveBrightness(null)
            touched()
        },
    )

    Box(
        modifier
            .fillMaxSize()
            .then(if (interactive) Modifier.clockGestures(gestures) else Modifier),
    ) {
        ClockContent(
            now = now,
            battery = battery,
            settings = settings,
            systemIs24Hour = DateFormat.is24HourFormat(context),
            faceScale = liveScale ?: settings.faceScale,
            swipeDirection = swipeDirection,
            // 控制列出現時先把名言淡出，免得兩者疊在一起。
            quoteVisible = !controlsVisible,
        )

        if (interactive) {
            AnimatedVisibility(
                visible = overlayBrightness != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                // 淡出動畫期間 overlayBrightness 已經是 null，沿用最後的數值。
                var lastShown by remember { mutableFloatStateOf(0.5f) }
                overlayBrightness?.let { lastShown = it }
                BrightnessOverlay(lastShown, colors)
            }

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.displayCutout)
                    .padding(bottom = 24.dp),
            ) {
                ControlBar(
                    face = settings.face,
                    colors = colors,
                    onPreviousFace = { switchFace(-1) },
                    onNextFace = { switchFace(1) },
                    actions = listOf(
                        ControlAction("夜間模式", highlighted = settings.nightMode) {
                            onUpdate { it.copy(nightMode = !it.nightMode) }
                            touched()
                        },
                        ControlAction("鬧鐘", highlighted = settings.alarm.enabled) {
                            controlsVisible = false
                            onOpenAlarm()
                        },
                        ControlAction("設定") {
                            controlsVisible = false
                            onOpenSettings()
                        },
                    ),
                )
            }
        }
    }
}

/** 目前的系統亮度（0..1），讀不到時回傳 0.5。只讀取，不需要權限。 */
private fun systemBrightness(context: Context): Float = try {
    Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
} catch (e: Settings.SettingNotFoundException) {
    0.5f
}.let(ClockMath::clampBrightness)

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
    quoteVisible: Boolean = true,
) {
    val colors = ClockColors.of(settings.nightMode)
    val use24 = ClockMath.use24Hour(settings.hourFormat, systemIs24Hour)
    val time = ClockMath.timeParts(now.hour, now.minute, now.second, use24, settings.showSeconds)
    val date = ClockMath.dateText(now.monthValue, now.dayOfMonth, now.dayOfWeek.value)
    val alarmText = AlarmMath.nextRing(settings.alarm, now)?.let { AlarmMath.statusText(it, now, use24) }

    // 防烙印：每分鐘把整個畫面移一點，用 2 秒慢慢滑過去，看不出跳動。
    val shift = ClockMath.burnInShift(now.toEpochSecond() / 60)
    val shiftX by animateDpAsState(shift.x.dp, tween(2_000), label = "shiftX")
    val shiftY by animateDpAsState(shift.y.dp, tween(2_000), label = "shiftY")

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.displayCutout),
    ) {
        // 資訊文字大小跟著螢幕大小走：手機約 18dp，平板會大一些。
        val infoSize = (min(maxWidth, maxHeight) * 0.045f).coerceIn(14.dp, 30.dp)

        Box(
            Modifier
                .fillMaxSize()
                .padding(infoSize)
                .offset { IntOffset(shiftX.roundToPx(), shiftY.roundToPx()) },
        ) {
            StatusRow(
                date = date,
                extra = alarmText,
                battery = battery,
                color = colors.secondary,
                textSize = infoSize,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            if (settings.showQuotes) {
                AnimatedVisibility(
                    visible = quoteVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = infoSize * 2),
                ) {
                    QuoteLine(
                        quote = Quotes.at(now.toInstant().toEpochMilli()),
                        colors = colors,
                        textSize = infoSize,
                    )
                }
            }
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
                    .padding(top = infoSize * 2.5f, bottom = if (settings.showQuotes) infoSize * 4f else infoSize * 2.5f),
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
