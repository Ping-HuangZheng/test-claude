package com.nightstand.clock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightstand.clock.data.FaceType
import com.nightstand.clock.ui.theme.ClockColors

/** 一個控制列按鈕。 */
class ControlAction(val label: String, val highlighted: Boolean = false, val onClick: () -> Unit)

/** 點一下畫面才出現的控制列：切換錶面、夜間模式、鬧鐘、設定。 */
@Composable
fun ControlBar(
    face: FaceType,
    colors: ClockColors,
    onPreviousFace: () -> Unit,
    onNextFace: () -> Unit,
    actions: List<ControlAction>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(colors.card.copy(alpha = 0.92f), RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarButton("‹", colors, onClick = onPreviousFace, big = true)
        Text(face.label, color = colors.primary, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
        BarButton("›", colors, onClick = onNextFace, big = true)
        actions.forEach { BarButton(it.label, colors, it.highlighted, onClick = it.onClick) }
    }
}

@Composable
private fun BarButton(
    label: String,
    colors: ClockColors,
    highlighted: Boolean = false,
    big: Boolean = false,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            color = if (highlighted) colors.accent else colors.primary,
            fontSize = if (big) 26.sp else 16.sp,
        )
    }
}
