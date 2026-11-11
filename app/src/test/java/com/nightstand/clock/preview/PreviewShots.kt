package com.nightstand.clock.preview

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.nightstand.clock.data.BatteryInfo
import com.nightstand.clock.data.ClockSettings
import com.nightstand.clock.data.FaceType
import com.nightstand.clock.math.ChargeState
import com.nightstand.clock.math.HourFormat
import com.nightstand.clock.ui.ClockContent
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
 * 執行：./gradlew :app:testDebugUnitTest -Ppreview
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
}
