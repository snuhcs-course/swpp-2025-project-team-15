package com.example.sumdays

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 앱 시작 시 바로 CalendarActivity로 이동
        val intent = Intent(this, CalendarActivity::class.java)
        startActivity(intent)

        // MainActivity는 필요 없으니 종료
        finish()
    }
}
