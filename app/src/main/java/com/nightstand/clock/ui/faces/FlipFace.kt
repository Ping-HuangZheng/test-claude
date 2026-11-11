package com.nightstand.clock.ui.faces

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.min
import com.nightstand.clock.math.TimeParts
import com.nightstand.clock.ui.theme.ClockColors
import kotlin.math.PI
import kotlin.math.cos

private const val CARD_ASPECT = 1.4f          // 卡片高 / 寬
private const val DIGIT_GAP = 0.08f           // 同一組數字卡片間距（卡片寬的倍數）
private const val GROUP_GAP = 0.32f           // 時、分、秒之間的間距
private const val SECONDS_RATIO = 0.5f        // 秒數卡片相對大小
private const val FLIP_MILLIS = 600

/** 翻頁錶面：每個數字一張卡片，數字改變時上半張往下翻。 */
@Composable
fun FlipFace(
    time: TimeParts,
    colors: ClockColors,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val hourCards = time.hour.length
        val secondsCards = if (time.second != null) 2 else 0
        // 以「卡片寬」為單位算出總寬度，再依可用寬高決定卡片大小。
        // 卡片數 + 組內間距（時 hourCards-1 個、分 1 個）+ 組間距
        val unitsWide = hourCards + 2 +
            hourCards * DIGIT_GAP + GROUP_GAP +
            if (secondsCards > 0) GROUP_GAP + 2 * SECONDS_RATIO + DIGIT_GAP * SECONDS_RATIO else 0f
        val cardWidth: Dp = min(maxWidth * 0.92f / unitsWide, maxHeight * 0.7f / CARD_ASPECT) * scale
        val cardHeight = cardWidth * CARD_ASPECT
        val smallText = with(LocalDensity.current) { (cardWidth * 0.32f).toSp() }

        Column(
            modifier = Modifier.wrapContentSize(unbounded = true),
            horizontalAlignment = Alignment.Start,
        ) {
            time.period?.let {
                Text(it, color = colors.secondary, fontSize = smallText, maxLines = 1, softWrap = false)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                CardGroup(time.hour, cardWidth, cardHeight, colors, keyPrefix = "h")
                Spacer(Modifier.width(cardWidth * GROUP_GAP))
                CardGroup(time.minute, cardWidth, cardHeight, colors, keyPrefix = "m")
                time.second?.let {
                    Spacer(Modifier.width(cardWidth * GROUP_GAP))
                    CardGroup(it, cardWidth * SECONDS_RATIO, cardHeight * SECONDS_RATIO, colors, keyPrefix = "s")
                }
            }
        }
    }
}

@Composable
private fun CardGroup(digits: String, w: Dp, h: Dp, colors: ClockColors, keyPrefix: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(w * DIGIT_GAP)) {
        // 以「從右邊數第幾位」當 key：12 小時制 9 點變 10 點時，個位數的卡片狀態不會錯位。
        digits.forEachIndexed { i, ch ->
            key("$keyPrefix${digits.length - i}") {
                FlipCard(ch, w, h, colors)
            }
        }
    }
}

@Composable
private fun FlipCard(digit: Char, w: Dp, h: Dp, colors: ClockColors) {
    var shown by remember { mutableStateOf(digit) }
    var previous by remember { mutableStateOf(digit) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(digit) {
        if (digit != shown) {
            previous = shown
            shown = digit
            progress.snapTo(0f)
            progress.animateTo(1f, tween(FLIP_MILLIS, easing = FastOutSlowInEasing))
        }
    }

    val measurer = rememberTextMeasurer()
    val fontSize = with(LocalDensity.current) { (h * 0.72f).toSp() }
    val style = TextStyle(color = colors.primary, fontSize = fontSize, fontWeight = FontWeight.Medium)
    val newLayout = remember(shown, style) { measurer.measure(shown.toString(), style) }
    val oldLayout = remember(previous, style) { measurer.measure(previous.toString(), style) }

    Canvas(Modifier.size(w, h)) {
        val p = progress.value
        val mid = size.height / 2f
        val pivot = Offset(size.width / 2f, mid)

        // 上半：新數字；下半：翻完之前還是舊數字。
        clipRect(bottom = mid) { drawHalfCard(colors.card, newLayout) }
        clipRect(top = mid) { drawHalfCard(colors.card, if (p < 1f) oldLayout else newLayout) }

        if (p < 0.5f) {
            // 第一段：舊數字的上半張往下倒（垂直壓扁模擬 3D 翻轉），越倒越暗。
            val s = cos(p * PI).toFloat()
            scale(scaleX = 1f, scaleY = s, pivot = pivot) {
                clipRect(bottom = mid) {
                    drawHalfCard(colors.card, oldLayout)
                    drawRect(Color.Black.copy(alpha = (1f - s) * 0.5f))
                }
            }
        } else if (p < 1f) {
            // 第二段：新數字的下半張從中線展開蓋下來。
            val s = -cos(p * PI).toFloat()
            scale(scaleX = 1f, scaleY = s, pivot = pivot) {
                clipRect(top = mid) {
                    drawHalfCard(colors.card, newLayout)
                    drawRect(Color.Black.copy(alpha = (1f - s) * 0.5f))
                }
            }
        }
        // 中間的分隔縫
        drawLine(
            color = colors.background,
            start = Offset(0f, mid),
            end = Offset(size.width, mid),
            strokeWidth = size.height * 0.012f,
        )
    }
}

/** 畫整張卡片底色和置中的數字；呼叫端用 clipRect 決定只露出上半或下半。 */
private fun DrawScope.drawHalfCard(cardColor: Color, layout: TextLayoutResult) {
    drawRoundRect(cardColor, cornerRadius = CornerRadius(size.width * 0.1f))
    // 讓數字的視覺中心（基線往上約半個字高）對齊卡片中線。
    val capHalf = layout.layoutInput.style.fontSize.toPx() * 0.36f
    val top = size.height / 2f - (layout.firstBaseline - capHalf)
    drawText(layout, topLeft = Offset((size.width - layout.size.width) / 2f, top))
}
