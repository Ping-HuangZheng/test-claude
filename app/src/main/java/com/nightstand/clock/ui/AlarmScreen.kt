package com.nightstand.clock.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.nightstand.clock.alarm.AlarmActions
import com.nightstand.clock.alarm.AlarmNotifier
import com.nightstand.clock.alarm.AlarmScheduler
import com.nightstand.clock.data.ClockSettings
import com.nightstand.clock.data.rememberClockTime
import com.nightstand.clock.math.AlarmConfig
import com.nightstand.clock.math.AlarmMath
import com.nightstand.clock.math.ClockMath
import com.nightstand.clock.ui.faces.TabularNumbers

/** 鬧鐘設定頁：時間、開關、重複星期、權限檢查、試響。 */
@Composable
fun AlarmScreen(settings: ClockSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    val alarm = settings.alarm
    val use24 = ClockMath.use24Hour(settings.hourFormat, DateFormat.is24HourFormat(context))
    val now by rememberClockTime(ClockMath.MINUTE_MILLIS)
    var showPicker by remember { mutableStateOf(false) }

    // 從系統設定頁回來時重新檢查權限狀態。
    var permissionCheck by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permissionCheck++ }
    val notificationsOk = remember(permissionCheck) { AlarmNotifier.canPostNotifications(context) }
    val exactOk = remember(permissionCheck) { AlarmScheduler.canScheduleExact(context) }
    val fullScreenOk = remember(permissionCheck) { AlarmNotifier.canUseFullScreen(context) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionCheck++
        if (!granted) openNotificationSettings(context)
    }
    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openNotificationSettings(context)
        }
    }

    fun update(transform: (AlarmConfig) -> AlarmConfig) {
        AlarmActions.update(context, transform)
        permissionCheck++
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.displayCutout)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ 返回", fontSize = 18.sp) }
            Spacer(Modifier.padding(4.dp))
            Text("鬧鐘", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
        }

        // 大大的時間，點一下改
        Text(
            text = AlarmMath.timeText(alarm.hour, alarm.minute, use24),
            fontSize = 64.sp,
            style = TabularNumbers,
            color = if (alarm.enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .clickable { showPicker = true }
                .padding(8.dp),
        )
        Text("點時間可以修改", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        SwitchRow("開啟鬧鐘", alarm.enabled) { on ->
            update { it.copy(enabled = on) }
            if (on && !notificationsOk) requestNotifications()
        }

        val ring = AlarmMath.nextRing(alarm, now)
        Text(
            text = ring?.let {
                val kind = if (it.isSnooze) "貪睡後再響" else "下次響鈴"
                "$kind：${AlarmMath.ringText(it.at, now, use24)}（${AlarmMath.untilText(it.at, now)}）"
            } ?: "鬧鐘已關閉",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        if (ring?.isSnooze == true) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { AlarmActions.dismiss(context); permissionCheck++ }) { Text("取消貪睡") }
        }

        Section("重複") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (day in 1..7) {
                    FilterChip(
                        selected = AlarmMath.isDayEnabled(alarm.repeatMask, day),
                        onClick = { update { it.copy(repeatMask = AlarmMath.toggleDay(it.repeatMask, day)) } },
                        label = { Text(ClockMath.weekdayName(day).removePrefix("週")) },
                    )
                }
            }
            Text(AlarmMath.repeatText(alarm.repeatMask), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { update { it.copy(repeatMask = AlarmMath.WEEKDAYS) } }) { Text("平日") }
                OutlinedButton(onClick = { update { it.copy(repeatMask = AlarmMath.EVERY_DAY) } }) { Text("每天") }
                OutlinedButton(onClick = { update { it.copy(repeatMask = 0) } }) { Text("只響一次") }
            }
        }

        Section("響鈴") {
            Text(
                "使用系統預設的鬧鈴聲和鬧鐘音量，並會震動；貪睡 ${AlarmMath.SNOOZE_MINUTES} 分鐘；" +
                    "${AlarmMath.RING_TIMEOUT_MINUTES} 分鐘沒有關掉會自動停止。",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { AlarmActions.testRing(context) }) { Text("試響一次") }
        }

        if (!notificationsOk || !exactOk || !fullScreenOk) {
            Section("需要處理的權限") {
                if (!notificationsOk) {
                    Warning(
                        "通知被關閉：鬧鐘時間到時不會響、也不會跳出畫面。",
                        "開啟通知",
                        ::requestNotifications,
                    )
                }
                if (!exactOk) {
                    Warning(
                        "沒有「鬧鐘與提醒」權限：鬧鐘無法準時響。",
                        "前往設定",
                    ) { openExactAlarmSettings(context) }
                }
                if (!fullScreenOk) {
                    Warning(
                        "沒有「全螢幕通知」權限：螢幕關著時只會響鈴和出現通知，不會直接跳出鬧鐘畫面。",
                        "前往設定",
                    ) { openFullScreenSettings(context) }
                }
            }
        }
    }

    if (showPicker) {
        AlarmTimePicker(
            initialHour = alarm.hour,
            initialMinute = alarm.minute,
            is24Hour = use24,
            onConfirm = { h, m ->
                showPicker = false
                // 改時間時順便開啟鬧鐘，這通常就是使用者想要的。
                update { it.copy(hour = h, minute = m, enabled = true, snoozeUntilMillis = 0L) }
                if (!notificationsOk) requestNotifications()
            },
            onDismiss = { showPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmTimePicker(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour, initialMinute, is24Hour)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("確定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                TimePicker(state = state)
            }
        },
    )
}

@Composable
private fun Warning(message: String, actionLabel: String, onAction: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(message, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = onAction) { Text(actionLabel) }
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        appDetails(context)
    }
    startSafely(context, intent)
}

private fun openExactAlarmSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
    } else {
        appDetails(context)
    }
    startSafely(context, intent)
}

private fun openFullScreenSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, "package:${context.packageName}".toUri())
    } else {
        appDetails(context)
    }
    startSafely(context, intent)
}

private fun appDetails(context: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())

private fun startSafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(appDetails(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
