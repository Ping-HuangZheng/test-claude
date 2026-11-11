package com.nightstand.clock.robo

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.nightstand.clock.data.BatteryInfo
import com.nightstand.clock.data.ClockSettings
import com.nightstand.clock.data.FaceType
import com.nightstand.clock.math.ChargeState
import com.nightstand.clock.alarm.RingScreen
import com.nightstand.clock.math.AlarmConfig
import com.nightstand.clock.math.AlarmMath
import com.nightstand.clock.math.HourFormat
import com.nightstand.clock.ui.AlarmScreen
import com.nightstand.clock.ui.ClockContent
import com.nightstand.clock.ui.ClockScreen
import com.nightstand.clock.ui.SettingsScreen
import com.nightstand.clock.ui.theme.AppTheme
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 開發用的截圖預覽：把各畫面畫成 PNG 放到 build/preview/，方便在沒有手機時檢查版面。
 * 執行：./gradlew :app:testDebugUnitTest -Probo
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w800dp-h360dp-land-xhdpi")
class PreviewShots {

    @get:Rule
    val rule = createComposeRule()

    private val now = ZonedDateTime.of(2026, 11, 11, 22, 47, 36, 0, ZoneId.of("Asia/Taipei"))
    private val charging = BatteryInfo(85, ChargeState.CHARGING)

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.setContent(content)
        rule.waitForIdle()
        save(name)
    }

    private fun save(name: String) {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(System.getProperty("preview.outDir") ?: "build/preview").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun clock(settings: ClockSettings, battery: BatteryInfo = charging) = @Composable {
        ClockContent(
            now = now,
            battery = battery,
            settings = settings,
            systemIs24Hour = true,
            faceScale = settings.faceScale,
            swipeDirection = 1,
        )
    }

    @Test
    fun digital24() = shot("digital_24h", clock(ClockSettings(face = FaceType.DIGITAL)))

    @Test
    fun noQuotes() = shot("digital_no_quotes", clock(ClockSettings(face = FaceType.DIGITAL, showQuotes = false)))

    @Test
    fun digital12Seconds() = shot(
        "digital_12h_seconds",
        clock(ClockSettings(face = FaceType.DIGITAL, hourFormat = HourFormat.H12, showSeconds = true)),
    )

    @Test
    fun analogSeconds() = shot(
        "analog_seconds",
        clock(ClockSettings(face = FaceType.ANALOG, showSeconds = true), BatteryInfo(100, ChargeState.FULL)),
    )

    @Test
    fun flip24() = shot("flip_24h", clock(ClockSettings(face = FaceType.FLIP)))

    @Test
    fun flip12Seconds() = shot(
        "flip_12h_seconds",
        clock(ClockSettings(face = FaceType.FLIP, hourFormat = HourFormat.H12, showSeconds = true)),
    )

    @Test
    fun nightDigital() = shot(
        "night_digital",
        clock(ClockSettings(face = FaceType.DIGITAL, nightMode = true), BatteryInfo(42, ChargeState.DISCHARGING)),
    )

    @Test
    @Config(qualifiers = "w360dp-h780dp-port-xhdpi")
    fun portraitFlip() = shot("portrait_flip", clock(ClockSettings(face = FaceType.FLIP)))

    @Test
    @Config(qualifiers = "w360dp-h780dp-port-xhdpi")
    fun portraitAnalog() = shot("portrait_analog", clock(ClockSettings(face = FaceType.ANALOG)))

    /** 翻頁動畫進行到一半左右的樣子：47 → 48 分。 */
    @Test
    fun flipMidAnimation() {
        var minute by mutableStateOf(47)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            ClockContent(
                now = now.withMinute(minute),
                battery = charging,
                settings = ClockSettings(face = FaceType.FLIP),
                systemIs24Hour = true,
                faceScale = 1f,
                swipeDirection = 1,
            )
        }
        rule.mainClock.advanceTimeBy(1000)
        minute = 48
        rule.mainClock.advanceTimeBy(200)
        save("flip_anim_phase1")
        rule.mainClock.advanceTimeBy(200)
        save("flip_anim_phase2")
    }

    /** 點一下畫面後出現的控制列。 */
    @Test
    fun controlsVisible() {
        rule.setContent {
            ClockScreen(settings = ClockSettings(face = FaceType.DIGITAL), onUpdate = {}, interactive = true)
        }
        rule.onRoot().performTouchInput { click(center) }
        rule.waitForIdle()
        save("controls_visible")
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-port-xhdpi")
    fun settingsPage() = shot("settings") {
        AppTheme(nightMode = false) {
            SettingsScreen(settings = ClockSettings(showSeconds = true), onUpdate = {}, onBack = {})
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-port-xhdpi")
    fun settingsPageNight() = shot("settings_night") {
        AppTheme(nightMode = true) {
            SettingsScreen(settings = ClockSettings(nightMode = true), onUpdate = {}, onBack = {})
        }
    }

    /** 左側往上滑：應出現亮度提示，且亮度變亮。 */
    @Test
    fun brightnessSwipe() {
        var live: Float? = null
        rule.setContent {
            ClockScreen(
                settings = ClockSettings(face = FaceType.DIGITAL, dayBrightness = 0.2f),
                onUpdate = {},
                interactive = true,
                onLiveBrightness = { live = it },
            )
        }
        rule.onRoot().performTouchInput {
            down(Offset(width * 0.05f, height * 0.8f))
            repeat(10) { moveBy(Offset(0f, -height * 0.03f)) }
        }
        rule.waitForIdle()
        save("brightness_overlay")
        val b = live
        assertTrue("拖曳中應即時回報亮度，實際：$b", b != null && b > 0.2f)
    }

    /** 中間往左滑：應切到下一個錶面。 */
    @Test
    fun horizontalSwipeSwitchesFace() {
        var settings = ClockSettings(face = FaceType.DIGITAL)
        rule.setContent {
            ClockScreen(settings = settings, onUpdate = { f -> settings = f(settings) }, interactive = true)
        }
        rule.onRoot().performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertEquals(FaceType.ANALOG, settings.face)
    }

    /** 中間上下滑：不應改亮度、不應切錶面。 */
    @Test
    fun verticalSwipeInMiddleDoesNothing() {
        var settings = ClockSettings(face = FaceType.DIGITAL)
        var live: Float? = null
        rule.setContent {
            ClockScreen(
                settings = settings,
                onUpdate = { f -> settings = f(settings) },
                interactive = true,
                onLiveBrightness = { live = it },
            )
        }
        rule.onRoot().performTouchInput {
            swipe(Offset(width * 0.5f, height * 0.8f), Offset(width * 0.5f, height * 0.2f))
        }
        rule.waitForIdle()
        assertEquals(FaceType.DIGITAL, settings.face)
        assertEquals(null, live)
    }

    @Test
    fun statusWithAlarm() = shot(
        "status_with_alarm",
        clock(ClockSettings(face = FaceType.DIGITAL, alarm = AlarmConfig(enabled = true, hour = 6, minute = 30))),
    )

    @Test
    @Config(qualifiers = "w360dp-h780dp-port-xhdpi")
    fun alarmPage() = shot("alarm_page") {
        AppTheme(nightMode = false) {
            AlarmScreen(
                settings = ClockSettings(alarm = AlarmConfig(enabled = true, hour = 6, minute = 30, repeatMask = AlarmMath.WEEKDAYS)),
                onBack = {},
            )
        }
    }

    @Test
    fun ringScreen() = shot("ring_screen") {
        AppTheme(nightMode = true) {
            RingScreen(settings = ClockSettings(nightMode = true), onSnooze = {}, onDismiss = {})
        }
    }
}
