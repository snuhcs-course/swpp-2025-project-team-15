package com.example.sumdays

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.sumdays.auth.SessionManager


//로딩화면 역할도 하는 액티비티이고 로그인이 안 되어있으면 로그인화면으로, 로그인이 되어있으면 메모 위젯으로 이동.

// AppCompatActivity 대신 ComponentActivity를 사용해도 무방합니다. UI가 없기 때문입니다.
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SessionManager 초기화 (앱의 첫 시작점이므로 여기서 해주는 것이 안전)
        SessionManager.init(applicationContext)

        // 로그인 상태 확인
        if (SessionManager.isLoggedIn()) {
            // 로그인 상태일 경우 MainActivity로 이동
            navigateToMain()
        } else {
            // 비로그인 상태일 경우 LoginActivity로 이동
            navigateToLogin()
        }
    }

    private fun navigateToMain() {
        // Intent를 사용하여 MainActivity로 이동
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        // SplashActivity를 스택에서 제거하여 뒤로 가기로 돌아오지 못하게 함
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