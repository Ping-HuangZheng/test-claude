package com.nightstand.clock.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nightstand.clock.MainActivity
import com.nightstand.clock.math.AlarmConfig
import com.nightstand.clock.math.AlarmMath
import java.time.ZonedDateTime

/**
 * 用 AlarmManager.setAlarmClock 排定下一次響鈴。
 * setAlarmClock 是系統給「鬧鐘」用的 API：省電模式也會準時觸發，狀態列會出現鬧鐘圖示。
 * 同一時間只排一個（一般鬧鐘和貪睡取比較早的那個）。
 */
object AlarmScheduler {

    enum class Result { SCHEDULED, NOTHING_TO_SCHEDULE, NO_EXACT_ALARM_PERMISSION }

    private const val REQUEST_FIRE = 1
    private const val REQUEST_SHOW = 2

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun sync(context: Context, config: AlarmConfig, now: ZonedDateTime = ZonedDateTime.now()): Result {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val ring = AlarmMath.nextRing(config, now)
        val operation = firePendingIntent(context, isSnooze = ring?.isSnooze ?: false)
        alarmManager.cancel(operation)

        if (ring == null) return Result.NOTHING_TO_SCHEDULE
        if (!canScheduleExact(context)) return Result.NO_EXACT_ALARM_PERMISSION

        val showIntent = PendingIntent.getActivity(
            context,
            REQUEST_SHOW,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(ring.at.toInstant().toEpochMilli(), showIntent),
                operation,
            )
            Result.SCHEDULED
        } catch (e: SecurityException) {
            Result.NO_EXACT_ALARM_PERMISSION
        }
    }

    private fun firePendingIntent(context: Context, isSnooze: Boolean): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_FIRE,
            Intent(context, AlarmReceiver::class.java)
                .setAction(AlarmReceiver.ACTION_FIRE)
                .putExtra(AlarmReceiver.EXTRA_SNOOZE, isSnooze),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
