package com.nightstand.clock.ui.faces

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.min
import com.nightstand.clock.math.TimeParts
import com.nightstand.clock.ui.theme.ClockColors

/** 等寬數字，數字變化時寬度不會跳動。 */
internal val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")

/** 數位錶面：大字「時:分」，秒數和上午/下午用小字跟在旁邊。 */
@Composable
fun DigitalFace(
    time: TimeParts,
    colors: ClockColors,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // 「12:34」約 2.8 個字寬；有秒數時再多一些。依可用寬高算出字級，再乘上使用者縮放。
        val widthInEms = if (time.second != null) 3.4f else 2.9f
        val em: Dp = min(maxWidth * 0.9f / widthInEms, maxHeight * 0.55f) * scale
        val density = LocalDensity.current
        val big: TextUnit = with(density) { em.toSp() }
        val small: TextUnit = with(density) { (em * 0.3f).toSp() }

        Row(
            // 放大超過畫面時允許超出，不要被擠成兩行。
            modifier = Modifier.wrapContentSize(unbounded = true),
            verticalAlignment = Alignment.Bottom,
        ) {
            time.period?.let {
                Text(
                    text = it,
                    color = colors.secondary,
                    fontSize = small,
                    modifier = Modifier.alignByBaseline(),
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(Modifier.width(em * 0.1f))
            }
            Text(
                text = "${time.hour}:${time.minute}",
                color = colors.primary,
                fontSize = big,
                fontWeight = FontWeight.Light,
                style = TabularNumbers,
                modifier = Modifier.alignByBaseline(),
                maxLines = 1,
                softWrap = false,
            )
            time.second?.let {
                Spacer(Modifier.width(em * 0.08f))
                Text(
                    text = it,
                    color = colors.secondary,
                    fontSize = small,
                    style = TabularNumbers,
                    modifier = Modifier.alignByBaseline(),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
