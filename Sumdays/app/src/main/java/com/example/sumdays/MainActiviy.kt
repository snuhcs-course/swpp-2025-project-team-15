package com.example.sumdays

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sumdays.auth.SessionManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SessionManager 초기화 (앱의 첫 시작점이므로 여기서 해주는 것이 안전)
        SessionManager.init(applicationContext)

        // 로그인 상태 확인
        if (SessionManager.isLoggedIn()) {
            // 로그인 상태일 경우 CalendarActivity로 이동
            navigateToCalendar()
        } else {
            // 비로그인 상태일 경우 LoginActivity로 이동
            navigateToLogin()
        }
    }

    private fun navigateToCalendar() {
        // Intent를 사용하여 CalendarActivity로 이동
        val intent = Intent(this, CalendarActivity::class.java)
        startActivity(intent)
        // MainActivity를 스택에서 제거하여 뒤로 가기로 돌아오지 못하게 함
        finish()
    }

    private fun navigateToLogin() {
        // Intent를 사용하여 LoginActivity로 이동
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        // SplashActivity를 스택에서 제거
        finish()
    }
}
