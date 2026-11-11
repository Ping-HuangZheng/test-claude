package com.nightstand.clock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 接收 AlarmManager 的響鈴，以及通知上的「貪睡」「停止」。只接受本 App 自己發的 Intent。 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FIRE -> AlarmActions.onFire(context, intent.getBooleanExtra(EXTRA_SNOOZE, false))
            ACTION_SNOOZE -> AlarmActions.snooze(context)
            ACTION_DISMISS -> AlarmActions.dismiss(context)
        }
    }

    companion object {
        const val ACTION_FIRE = "com.nightstand.clock.action.ALARM_FIRE"
        const val ACTION_SNOOZE = "com.nightstand.clock.action.ALARM_SNOOZE"
        const val ACTION_DISMISS = "com.nightstand.clock.action.ALARM_DISMISS"
        const val EXTRA_SNOOZE = "snooze"
    }
}
