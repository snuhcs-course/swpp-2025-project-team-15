package com.example.sumdays.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

object ReminderScheduler {

    private const val REQUEST_CODE_BASE = 100 // 알람 ID의 시작점 (0~99까지 사용 가능)

    /**
     * 특정 시간에 매일 반복되는 단일 알람을 등록합니다.
     * @param requestCode: 알람 취소를 위해 필요한 고유 ID (0 ~ 9)
     */
    private fun scheduleSingleReminder(context: Context, hour: Int, minute: Int, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + requestCode, // 고유 요청 코드로 PendingIntent 생성
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 1. 알람 시간 설정 (오늘의 설정된 시간)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            // 2. 만약 이미 지난 시간이라면 내일 알람으로 설정
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // 3. 매일 반복 알람 등록 (setRepeating 대신 setExactAndAllowWhileIdle + 재등록 권장)
        // 여기서는 매일 반복의 정확성을 위해 setExactAndAllowWhileIdle을 사용하고 리시버에서 다음날 알람을 재등록하는 방식이 권장되지만,
        // 단순 반복을 위해 setRepeating을 사용합니다.
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY, // 매일 반복
            pendingIntent
        )
        Log.d("Scheduler", "알람 등록 완료: $hour:$minute (Request Code: ${REQUEST_CODE_BASE + requestCode})")
    }

    /**
     * 모든 알람을 취소합니다. (OFF 기능 구현의 핵심)
     */
    fun cancelAllReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)

        // 최대 알람 개수만큼 반복하며 모든 PendingIntent를 취소합니다.
        for (i in 0 until 10) {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_BASE + i,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            // PendingIntent가 존재하면 취소합니다.
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                Log.d("Scheduler", "알람 취소 완료: Request Code: ${REQUEST_CODE_BASE + i}")
            }
        }
    }

    /**
     * 저장된 모든 시간 목록을 바탕으로 알람을 등록합니다.
     */
    fun scheduleAllReminders(context: Context) {
        // 1. 기존 알람 전체 취소 (중복 등록 방지)
        cancelAllReminders(context)

        val prefs = ReminderPrefs(context)
        val times = prefs.getAlarmTimes()

        if (!prefs.isMasterOn()) {
            Log.d("Scheduler", "마스터 스위치가 OFF 상태이므로 알람을 등록하지 않습니다.")
            return
        }

        // 2. 시간 목록을 순회하며 개별 알람 등록 (최대 10개)
        times.take(10).forEachIndexed { index, timeString ->
            // timeString ("HH:mm")을 파싱
            val parts = timeString.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: return@forEachIndexed
                val minute = parts[1].toIntOrNull() ?: return@forEachIndexed

                // 인덱스를 고유 requestCode로 사용 (0~9)
                scheduleSingleReminder(context, hour, minute, index)
            }
        }
    }
}