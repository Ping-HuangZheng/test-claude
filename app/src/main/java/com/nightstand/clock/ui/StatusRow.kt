package com.nightstand.clock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.nightstand.clock.data.BatteryInfo
import com.nightstand.clock.math.ChargeState
import com.nightstand.clock.ui.faces.TabularNumbers

/** 上方資訊列：左邊日期（和下一次鬧鐘），右邊電量。 */
@Composable
fun StatusRow(
    date: String,
    extra: String?,
    battery: BatteryInfo,
    color: Color,
    textSize: Dp,
    modifier: Modifier = Modifier,
) {
    val fontSize = with(LocalDensity.current) { textSize.toSp() }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(date, color = color, fontSize = fontSize, maxLines = 1)
            if (extra != null) {
                Spacer(Modifier.width(textSize))
                Text(extra, color = color, fontSize = fontSize, style = TabularNumbers, maxLines = 1)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BatteryIcon(battery, color, Modifier.size(width = textSize * 1.6f, height = textSize * 0.85f))
            Spacer(Modifier.width(textSize * 0.4f))
            Text(battery.label, color = color, fontSize = fontSize, style = TabularNumbers, maxLines = 1)
        }
    }
}

/** 小電池圖示：外框、依電量填滿，接著電源時畫閃電。 */
@Composable
private fun BatteryIcon(battery: BatteryInfo, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = size.height * 0.12f
        val nubWidth = size.width * 0.08f
        val bodyWidth = size.width - nubWidth
        // 外框
        drawRoundRect(
            color = color,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(bodyWidth - stroke, size.height - stroke),
            cornerRadius = CornerRadius(stroke * 1.5f),
            style = Stroke(stroke),
        )
        // 正極凸點
        drawRect(
            color = color,
            topLeft = Offset(bodyWidth, size.height * 0.3f),
            size = Size(nubWidth, size.height * 0.4f),
        )
        // 電量
        val fraction = (battery.percent ?: 0) / 100f
        val inset = stroke * 2
        drawRect(
            color = color.copy(alpha = 0.6f),
            topLeft = Offset(inset, inset),
            size = Size((bodyWidth - inset * 2) * fraction, size.height - inset * 2),
        )
        // 充電中：閃電
        if (battery.state == ChargeState.CHARGING) {
            val w = bodyWidth
            val h = size.height
            val bolt = Path().apply {
                moveTo(w * 0.56f, h * 0.08f)
                lineTo(w * 0.34f, h * 0.56f)
                lineTo(w * 0.5f, h * 0.56f)
                lineTo(w * 0.44f, h * 0.92f)
                lineTo(w * 0.68f, h * 0.42f)
                lineTo(w * 0.52f, h * 0.42f)
                close()
            }
            drawPath(bolt, Color.Black)
            drawPath(bolt, color, style = Stroke(stroke * 0.6f))
        }
    }
}
