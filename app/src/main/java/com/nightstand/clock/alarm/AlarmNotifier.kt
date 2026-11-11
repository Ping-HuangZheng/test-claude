package com.nightstand.clock.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nightstand.clock.R
import com.nightstand.clock.math.AlarmMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 響鈴方式：發一則「鬧鐘」類別的高優先通知。
 * - 聲音：系統預設鬧鈴聲，走鬧鐘音量（不受響鈴靜音影響），並設成重複播放直到停止。
 * - 螢幕關著或鎖定時：用全螢幕通知直接打開響鈴畫面；手機正在使用時則跳出可操作的通知。
 * - 10 分鐘沒人理會自動停止。
 */
object AlarmNotifier {

    private const val CHANNEL_ID = "alarm_ring"
    private const val NOTIFICATION_ID = 1001
    private const val REQUEST_RING_SCREEN = 10
    private const val REQUEST_SNOOZE = 11
    private const val REQUEST_DISMISS = 12
    private val VIBRATION = longArrayOf(0, 800, 600, 800, 600)

    private val _ringing = MutableStateFlow(false)

    /** 目前是否正在響（同一個行程內共用，響鈴畫面靠它知道何時關閉）。 */
    val ringing: StateFlow<Boolean> = _ringing.asStateFlow()

    fun canPostNotifications(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                )

    fun canUseFullScreen(context: Context): Boolean =
        NotificationManagerCompat.from(context).canUseFullScreenIntent()

    fun showRinging(context: Context, title: String) {
        ensureChannel(context)
        val ringScreen = PendingIntent.getActivity(
            context,
            REQUEST_RING_SCREEN,
            Intent(context, AlarmRingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snooze = receiverIntent(context, AlarmReceiver.ACTION_SNOOZE, REQUEST_SNOOZE)
        val dismiss = receiverIntent(context, AlarmReceiver.ACTION_DISMISS, REQUEST_DISMISS)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(title)
            .setContentText("點這裡打開鬧鐘畫面")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(ringScreen, true)
            .setContentIntent(ringScreen)
            .setDeleteIntent(dismiss)
            .setTimeoutAfter(AlarmMath.RING_TIMEOUT_MINUTES * 60_000L)
            // Android 7.x 沒有通知頻道，聲音和震動設在通知本身。
            .setSound(alarmSound(), AudioManager.STREAM_ALARM)
            .setVibrate(VIBRATION)
            .addAction(0, "貪睡 ${AlarmMath.SNOOZE_MINUTES} 分鐘", snooze)
            .addAction(0, "停止", dismiss)
            .build()
        // 重複播放聲音直到通知被取消。
        notification.flags = notification.flags or Notification.FLAG_INSISTENT

        _ringing.value = true
        if (canPostNotifications(context)) {
            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                // 權限在這一刻被收回，無法發通知。
            }
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        _ringing.value = false
    }

    private fun alarmSound(): Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, "鬧鐘響鈴", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "鬧鐘時間到時響鈴"
            setSound(
                alarmSound(),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = VIBRATION
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun receiverIntent(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
