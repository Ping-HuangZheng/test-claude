package com.nightstand.clock

import android.service.dreams.DreamService
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.nightstand.clock.data.SettingsStore
import com.nightstand.clock.math.ClockMath
import com.nightstand.clock.ui.ApplyWindowBrightness
import com.nightstand.clock.ui.ClockScreen

/**
 * 系統螢幕保護程式（充電待機）。到「設定 → 顯示 → 螢幕保護程式」選「待機時鐘」即可。
 *
 * 和 App 共用同一份設定與錶面；這裡不接手勢（碰一下螢幕就結束，這是 Android 螢幕保護程式的標準行為）。
 * DreamService 不是 Activity，所以自己提供 Compose 需要的 Lifecycle 和 SavedState。
 */
class ClockDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = true

        val store = SettingsStore.get(this)
        val dreamWindow = window
        window.decorView.setViewTreeLifecycleOwner(this)
        window.decorView.setViewTreeSavedStateRegistryOwner(this)

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ClockDreamService)
            setViewTreeSavedStateRegistryOwner(this@ClockDreamService)
            setContent {
                val settings by store.settings.collectAsStateWithLifecycle()
                ApplyWindowBrightness(
                    window = dreamWindow,
                    brightness = ClockMath.windowBrightness(
                        settings.nightMode,
                        settings.dayBrightness,
                        settings.nightBrightness,
                    ),
                )
                ClockScreen(settings = settings, onUpdate = store::update, interactive = false)
            }
        }
        setContentView(view)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}
