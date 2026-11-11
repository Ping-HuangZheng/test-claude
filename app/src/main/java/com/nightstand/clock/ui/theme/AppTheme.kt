package com.nightstand.clock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 設定頁等一般畫面用的深色主題；夜間模式時改成暗紅色系。 */
@Composable
fun AppTheme(nightMode: Boolean, content: @Composable () -> Unit) {
    val c = ClockColors.of(nightMode)
    val scheme = darkColorScheme(
        primary = c.accent,
        onPrimary = Color.Black,
        primaryContainer = c.card,
        onPrimaryContainer = c.primary,
        secondaryContainer = c.accent.copy(alpha = 0.35f),
        onSecondaryContainer = c.primary,
        background = Color.Black,
        onBackground = c.primary,
        surface = Color.Black,
        onSurface = c.primary,
        surfaceVariant = c.card,
        onSurfaceVariant = c.secondary,
        surfaceContainerHighest = c.card,
        outline = c.secondary,
        outlineVariant = c.card,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
