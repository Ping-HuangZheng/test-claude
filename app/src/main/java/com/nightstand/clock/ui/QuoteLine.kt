package com.nightstand.clock.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.nightstand.clock.math.Quote
import com.nightstand.clock.ui.theme.ClockColors

/** 時鐘下方的名言，換句時淡入淡出。 */
@Composable
fun QuoteLine(quote: Quote, colors: ClockColors, textSize: Dp, modifier: Modifier = Modifier) {
    val fontSize = with(LocalDensity.current) { textSize.toSp() }
    Crossfade(targetState = quote, animationSpec = tween(1_500), label = "quote", modifier = modifier) { q ->
        Text(
            text = "${q.text}　—— ${q.source}",
            color = colors.secondary,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
