package com.nightstand.clock.ui.faces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.min
import com.nightstand.clock.math.HandAngles
import com.nightstand.clock.ui.theme.ClockColors
import kotlin.math.cos
import kotlin.math.sin

/** 指針錶面：60 個刻度、時針、分針，開啟秒數時加上秒針。 */
@Composable
fun AnalogFace(
    angles: HandAngles,
    showSeconds: Boolean,
    colors: ClockColors,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = min(maxWidth, maxHeight) * 0.92f * scale
        // requiredSize：放大超過可用空間時照樣畫，不被壓縮。
        Canvas(Modifier.requiredSize(side)) {
            val r = size.minDimension / 2f
            val c = center

            // 刻度
            for (i in 0 until 60) {
                val major = i % 5 == 0
                val outer = r * 0.98f
                val inner = if (major) r * 0.86f else r * 0.93f
                drawRadialLine(
                    color = if (major) colors.primary else colors.secondary.copy(alpha = 0.6f),
                    angleDeg = i * 6f,
                    from = inner,
                    to = outer,
                    width = if (major) r * 0.022f else r * 0.008f,
                    center = c,
                )
            }
            // 時針
            drawRadialLine(colors.primary, angles.hour, from = -r * 0.08f, to = r * 0.5f, width = r * 0.055f, center = c)
            // 分針
            drawRadialLine(colors.primary, angles.minute, from = -r * 0.08f, to = r * 0.78f, width = r * 0.035f, center = c)
            // 秒針
            if (showSeconds) {
                drawRadialLine(colors.accent, angles.second, from = -r * 0.16f, to = r * 0.88f, width = r * 0.012f, center = c)
            }
            drawCircle(if (showSeconds) colors.accent else colors.primary, radius = r * 0.035f, center = c)
            drawCircle(colors.background, radius = r * 0.012f, center = c)
        }
    }
}

/** 從圓心沿 [angleDeg]（12 點方向為 0、順時針）畫一段線，[from] 為負代表往反方向的尾巴。 */
private fun DrawScope.drawRadialLine(
    color: Color,
    angleDeg: Float,
    from: Float,
    to: Float,
    width: Float,
    center: Offset,
) {
    val rad = Math.toRadians(angleDeg.toDouble())
    val dx = sin(rad).toFloat()
    val dy = -cos(rad).toFloat()
    drawLine(
        color = color,
        start = Offset(center.x + dx * from, center.y + dy * from),
        end = Offset(center.x + dx * to, center.y + dy * to),
        strokeWidth = width,
        cap = StrokeCap.Round,
    )
}
