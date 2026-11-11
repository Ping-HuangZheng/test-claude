package com.nightstand.clock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightstand.clock.math.ClockMath
import com.nightstand.clock.ui.faces.TabularNumbers
import com.nightstand.clock.ui.theme.ClockColors

/** 調亮度時畫面中央顯示的百分比和橫條。 */
@Composable
fun BrightnessOverlay(brightness: Float, colors: ClockColors, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(colors.card, RoundedCornerShape(20.dp))
            .padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("亮度", color = colors.secondary, fontSize = 16.sp)
        Text(
            "${ClockMath.brightnessPercent(brightness)}%",
            color = colors.primary,
            fontSize = 36.sp,
            style = TabularNumbers,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .width(160.dp)
                .height(6.dp)
                .background(colors.secondary.copy(alpha = 0.3f), RoundedCornerShape(3.dp)),
        ) {
            // 橫條長度用滑桿位置（平方根），和手指移動的感覺一致。
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(ClockMath.brightnessToSlider(brightness))
                    .background(colors.accent, RoundedCornerShape(3.dp)),
            )
        }
    }
}
