package com.nightstand.clock.ui

import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * 把亮度套用到這個視窗（只影響本 App，不改系統設定，不需要權限）。
 * [brightness] 為 0..1；-1 代表跟隨系統。
 */
@Composable
fun ApplyWindowBrightness(window: Window, brightness: Float) {
    LaunchedEffect(window, brightness) {
        val attrs = window.attributes
        if (attrs.screenBrightness != brightness) {
            attrs.screenBrightness = brightness
            window.attributes = attrs
        }
    }
}
