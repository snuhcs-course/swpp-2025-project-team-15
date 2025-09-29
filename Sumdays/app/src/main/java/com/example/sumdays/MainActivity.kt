package com.example.sumdays

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import com.example.sumdays.auth.SessionManager
import com.example.sumdays.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // SplashActivity가 로그인 상태 확인을 마쳤다고 가정하므로,
        // 여기서는 지체없이 UI를 설정합니다. 이렇게 하면 lateinit 오류가 발생하지 않습니다.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SessionManager 초기화는 앱의 진입점(SplashActivity 또는 Application 클래스)에서
        // 한 번만 수행하는 것이 가장 이상적입니다.
        SessionManager.init(applicationContext)

        // 로그아웃 버튼에 클릭 리스너를 설정합니다.
        binding.logoutButton.setOnClickListener {
            handleLogout()
        }
    }

    /**
     * 로그아웃을 처리하는 함수
     */
    private fun handleLogout() {
        // 1. SessionManager를 사용하여 저장된 세션 정보(토큰, 사용자 ID)를 삭제합니다.
        SessionManager.clearSession()

        // 2. LoginActivity로 이동하는 Intent를 생성합니다.
        val intent = Intent(this, LoginActivity::class.java)

        // 3. 이전의 모든 액티비티를 스택에서 제거하는 플래그를 추가합니다.
        //    이렇게 해야 로그아웃 후 뒤로가기 버튼을 눌렀을 때 다시 메인 화면으로 돌아오는 것을 막을 수 있습니다.
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        // 4. LoginActivity를 시작하고 현재 MainActivity는 종료합니다.
        startActivity(intent)
        finish()
    }
}