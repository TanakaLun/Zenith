package com.etrisad.zenith.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ZenithHeartbeatReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.etrisad.zenith.action.HEARTBEAT") {
            val serviceIntent = Intent(context, AppUsageMonitorService::class.java).apply {
                action = "com.etrisad.zenith.action.HEARTBEAT"
            }
            context.startService(serviceIntent)
        }
    }
}
