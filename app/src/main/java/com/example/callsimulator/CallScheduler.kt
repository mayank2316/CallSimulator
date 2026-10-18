package com.example.callsimulator

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object CallScheduler {
    private const val CHANNEL = "scheduled_simulated_calls"

    fun schedule(context: Context, call: SimCall) {
        CallStore.upsert(context, call)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CallAlarmReceiver::class.java).putExtra("call_id", call.id)
        val pi = PendingIntent.getBroadcast(context, call.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, call.timeMillis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, call.timeMillis, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, call.timeMillis, pi)
        }
    }

    fun cancel(context: Context, call: SimCall) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CallAlarmReceiver::class.java).putExtra("call_id", call.id)
        val pi = PendingIntent.getBroadcast(context, call.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.cancel(pi)
        CallStore.remove(context, call.id)
    }

    fun notifyCall(context: Context, call: SimCall) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Scheduled simulated calls", NotificationManager.IMPORTANCE_HIGH))
        }
        val open = Intent(context, CallActivity::class.java)
            .putExtra("call_id", call.id).putExtra("name", call.name)
            .putExtra("number", call.number).putExtra("ringtone", call.ringtoneUri)
            .putExtra("vibration", call.vibration)
        val pi = PendingIntent.getActivity(context, call.id.hashCode(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("Incoming simulated call")
            .setContentText(call.name)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try { NotificationManagerCompat.from(context).notify(call.id.hashCode(), notification) } catch (_: SecurityException) { }
    }
}

class CallAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("call_id") ?: return
        val call = CallStore.all(context).firstOrNull { it.id == id } ?: return
        if (call.status != "Scheduled") return
        CallStore.updateStatus(context, id, "Ringing")
        CallScheduler.notifyCall(context, call)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        CallStore.all(context).filter { it.status == "Scheduled" && it.timeMillis > System.currentTimeMillis() }
            .forEach { CallScheduler.schedule(context, it) }
    }
}
