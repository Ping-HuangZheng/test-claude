package com.nightstand.clock.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import com.nightstand.clock.math.ClockMath
import com.nightstand.clock.math.DragKind

/** 時鐘畫面的手勢回呼。 */
class ClockGestureCallbacks(
    val onTap: () -> Unit,
    /** +1 = 下一個錶面，-1 = 上一個。 */
    val onSwipe: (direction: Int) -> Unit,
    /** 兩指縮放，[factor] 是這一小段移動的倍率。 */
    val onZoom: (factor: Float) -> Unit,
    val onZoomEnd: () -> Unit,
    val onBrightnessStart: () -> Unit,
    /** [dragFraction]：從起點算起的垂直移動量 / 畫面高度，往上為負。 */
    val onBrightnessDrag: (dragFraction: Float) -> Unit,
    val onBrightnessEnd: () -> Unit,
)

private enum class GestureMode { UNDECIDED, PINCH, SWIPE, BRIGHTNESS, IGNORED }

/**
 * 把所有手勢集中在一個 detector，避免互相搶：
 * - 兩指 → 縮放
 * - 單指水平滑 → 切換錶面
 * - 單指在左右兩側上下滑 → 調亮度
 * - 沒有移動就放開 → 點一下
 */
@Composable
fun Modifier.clockGestures(callbacks: ClockGestureCallbacks): Modifier {
    val cb by rememberUpdatedState(callbacks)
    return pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val start = down.position
            var mode = GestureMode.UNDECIDED
            var total = Offset.Zero
            val slop = viewConfiguration.touchSlop
            val width = size.width.toFloat()
            val height = size.height.toFloat()

            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.count { it.pressed }
                if (pressed == 0) break

                if (pressed >= 2 && (mode == GestureMode.UNDECIDED || mode == GestureMode.IGNORED)) {
                    mode = GestureMode.PINCH
                }
                val primary = event.changes.firstOrNull { it.id == down.id } ?: event.changes.first()

                when (mode) {
                    GestureMode.PINCH -> {
                        val zoom = event.calculateZoom()
                        if (zoom != 1f) cb.onZoom(zoom)
                        event.changes.forEach { it.consume() }
                    }
                    GestureMode.UNDECIDED -> {
                        total += primary.positionChange()
                        if (total.getDistance() > slop) {
                            mode = when (ClockMath.classifyDrag(start.x, width, total.x, total.y)) {
                                DragKind.SWIPE -> GestureMode.SWIPE
                                DragKind.BRIGHTNESS -> GestureMode.BRIGHTNESS.also { cb.onBrightnessStart() }
                                DragKind.NONE -> GestureMode.IGNORED
                            }
                        }
                    }
                    GestureMode.SWIPE -> {
                        total += primary.positionChange()
                        primary.consume()
                    }
                    GestureMode.BRIGHTNESS -> {
                        total += primary.positionChange()
                        cb.onBrightnessDrag(total.y / height)
                        primary.consume()
                    }
                    GestureMode.IGNORED -> Unit
                }
            }

            when (mode) {
                GestureMode.UNDECIDED -> cb.onTap()
                GestureMode.SWIPE -> ClockMath.swipeDirection(total.x, width).let { if (it != 0) cb.onSwipe(it) }
                GestureMode.PINCH -> cb.onZoomEnd()
                GestureMode.BRIGHTNESS -> cb.onBrightnessEnd()
                GestureMode.IGNORED -> Unit
            }
        }
    }
}
