package com.example.sumdays

import android.Manifest // ⬅️ import 추가
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager // ⬅️ import 추가
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts // ⬅️ import 추가
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat // ⬅️ import 추가
import com.example.sumdays.auth.SessionManager
import com.example.sumdays.reminder.ReminderReceiver

class MainActivity : AppCompatActivity() {

    // 1. 알림 권한 요청을 위한 런처 등록
    //    이 코드가 알림 권한 팝업을 띄우고 그 결과를 처리합니다.
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // 권한이 승인되었을 때
                scheduleTestReminder()
            } else {
                // 권한이 거부되었을 때 (알림을 보낼 수 없음)
                // 이 경우에도 앱의 다음 단계는 진행합니다.
            }
            // 권한 요청 결과와 상관없이 로그인 확인 및 화면 이동
            checkLoginAndNavigate()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(applicationContext)
        createNotificationChannel()

        // 2. (가장 중요!) 알림 권한 확인 및 요청
        askForNotificationPermission()
    }

    private fun askForNotificationPermission() {
        // Android 13 (API 33) 이상에서만 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                // 3. 권한이 이미 승인된 경우
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    scheduleTestReminder()
                    checkLoginAndNavigate()
                }

                // 4. 권한이 없는 경우, 런처를 실행해 권한을 요청
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // 5. Android 13 미만은 권한이 필요 없으므로 바로 실행
            scheduleTestReminder()
            checkLoginAndNavigate()
        }
    }

    // 6. 로그인 확인 및 화면 이동 로직을 별도 함수로 분리
    //    (권한 요청 콜백에서도 호출해야 하므로)
    private fun checkLoginAndNavigate() {
        if (SessionManager.isLoggedIn()) {
            navigateToCalendar()
        } else {
            navigateToLogin()
        }
    }

    private fun navigateToCalendar() {
        val intent = Intent(this, CalendarActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "daily_reminder",
                "Daily Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "매일 일기 작성 알림"
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // --- ⬇️ 7. 10초짜리 일회성 테스트 알람 함수 ---
    private fun scheduleTestReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0, // 일회성이므로 0 사용
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 10초 뒤 시간 설정
        val triggerTime = System.currentTimeMillis() + 10_000 // 10초

        // (중요!) setExact...() 대신 set() 사용
        // 👉 setExactAndAllowWhileIdle()을 쓰면 Android 12에서 *충돌*합니다. (SecurityException)
        // 👉 set()은 부정확하지만 충돌하지 않습니다.
        //    (테스트 시 10초가 아니라 15~30초 뒤에 울릴 수도 있습니다.)
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }
}