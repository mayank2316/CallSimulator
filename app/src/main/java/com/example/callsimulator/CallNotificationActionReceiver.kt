package com.example.callsimulator

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("call_id") ?: return
        if (intent.action == ACTION_DECLINE) {
            CallStore.updateStatus(context, id, "Rejected")
            val notificationId = id.hashCode()
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.cancel(notificationId)
        }
    }

    companion object {
        const val ACTION_DECLINE = "com.example.callsimulator.ACTION_DECLINE_CALL"
    }
}
