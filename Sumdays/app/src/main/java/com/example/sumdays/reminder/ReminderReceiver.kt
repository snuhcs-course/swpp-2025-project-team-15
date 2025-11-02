package com.example.sumdays.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        // 1. click callback
        val activityIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. alarm builder
        val builder = NotificationCompat.Builder(context, "daily_reminder")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Sumdays")
            .setContentText("오늘의 하루, 잠깐만 메모해볼까요?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // 탭 시 실행할 동작
            .setAutoCancel(true)           // 탭하면 알림 사라짐
        val notificationManager = NotificationManagerCompat.from(context)


        // 3. check permission
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // 4. send alarm
        notificationManager.notify(1001, builder.build())
    }
}