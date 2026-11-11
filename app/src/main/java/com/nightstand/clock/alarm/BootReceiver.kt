package com.nightstand.clock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 重開機、改時間或時區、App 更新、鬧鐘權限變更後，系統排程會失效或過時，重新排一次。 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmActions.resync(context)
    }
}
