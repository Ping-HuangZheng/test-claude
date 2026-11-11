package com.nightstand.clock.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightstand.clock.data.ClockSettings
import com.nightstand.clock.data.FaceType
import com.nightstand.clock.math.ClockMath
import com.nightstand.clock.math.HourFormat

/** 設定頁。所有改動立刻寫入設定，時鐘畫面和螢幕保護程式馬上跟著變。 */
@Composable
fun SettingsScreen(
    settings: ClockSettings,
    onUpdate: SettingsUpdater,
    onBack: () -> Unit,
    onOpenAlarm: () -> Unit = {},
    extraSections: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.displayCutout)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ 返回", fontSize = 18.sp) }
            Spacer(Modifier.padding(4.dp))
            Text("設定", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.height(8.dp))

        Section("錶面") {
            Choice(
                options = FaceType.entries.map { it to it.label },
                selected = settings.face,
                onSelect = { face -> onUpdate { it.copy(face = face) } },
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { onUpdate { it.copy(faceScales = it.faceScales.map { 1f }) } }) {
                Text("錶面大小全部還原")
            }
        }

        Section("時間") {
            Text("時間格式", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Choice(
                options = listOf(
                    HourFormat.SYSTEM to "跟隨系統",
                    HourFormat.H12 to "12 小時",
                    HourFormat.H24 to "24 小時",
                ),
                selected = settings.hourFormat,
                onSelect = { f -> onUpdate { it.copy(hourFormat = f) } },
            )
            SwitchRow("顯示秒數", settings.showSeconds) { v -> onUpdate { it.copy(showSeconds = v) } }
        }

        Section("顯示") {
            SwitchRow("夜間模式", settings.nightMode, "暗紅色文字＋低亮度，適合關燈後") { v ->
                onUpdate { it.copy(nightMode = v) }
            }
            SwitchRow("名言輪播", settings.showQuotes, "聖經、論語、老子共 60 句，每 10 分鐘換一句") { v ->
                onUpdate { it.copy(showQuotes = v) }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "在時鐘畫面的左右兩側上下滑動可以調亮度（只影響這個 App）。",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ValueRow(
                title = "白天亮度",
                value = if (settings.dayBrightness < 0f) "跟隨系統" else "${ClockMath.brightnessPercent(settings.dayBrightness)}%",
                actionLabel = "跟隨系統",
                actionEnabled = settings.dayBrightness >= 0f,
                onAction = { onUpdate { it.copy(dayBrightness = ClockSettings.BRIGHTNESS_FOLLOW_SYSTEM) } },
            )
            ValueRow(
                title = "夜間亮度",
                value = "${ClockMath.brightnessPercent(settings.nightBrightness)}%",
                actionLabel = "還原 5%",
                actionEnabled = settings.nightBrightness != ClockSettings.DEFAULT_NIGHT_BRIGHTNESS,
                onAction = { onUpdate { it.copy(nightBrightness = ClockSettings.DEFAULT_NIGHT_BRIGHTNESS) } },
            )
        }

        Section("鬧鐘") {
            OutlinedButton(onClick = onOpenAlarm) { Text("鬧鐘設定") }
        }

        Section("充電待機（螢幕保護程式）") {
            val context = LocalContext.current
            Text(
                "要在充電時自動顯示時鐘，請到系統設定選擇：\n" +
                    "設定 → 顯示 → 螢幕保護程式 → 選「待機時鐘」，啟動時機選「充電時」。\n" +
                    "螢幕保護程式會沿用這裡的錶面、夜間模式和亮度；碰一下螢幕就會結束。",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { openDreamSettings(context) }) {
                Text("開啟系統螢幕保護程式設定")
            }
        }

        extraSections()
    }
}

/** 打開系統的螢幕保護程式設定；有些廠牌拿掉了這頁，就退而打開顯示設定。 */
private fun openDreamSettings(context: Context) {
    val intents = listOf(
        Intent(Settings.ACTION_DREAM_SETTINGS),
        Intent(Settings.ACTION_DISPLAY_SETTINGS),
    )
    for (intent in intents) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            if (intent.action == Settings.ACTION_DISPLAY_SETTINGS) {
                Toast.makeText(context, "找不到螢幕保護程式設定，請在顯示設定裡找找看", Toast.LENGTH_LONG).show()
            }
            return
        } catch (e: ActivityNotFoundException) {
            // 試下一個
        }
    }
    Toast.makeText(context, "這支手機沒有螢幕保護程式設定", Toast.LENGTH_LONG).show()
}

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().widthIn(max = 640.dp).padding(vertical = 8.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))
        Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            subtitle?.let { Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun ValueRow(
    title: String,
    value: String,
    actionLabel: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = onAction, enabled = actionEnabled) { Text(actionLabel) }
    }
}

@Composable
fun <T> Choice(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label) }
        }
    }
}
