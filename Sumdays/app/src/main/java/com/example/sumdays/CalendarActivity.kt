package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.CalendarView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CalendarActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        val calendarView = findViewById<CalendarView>(R.id.calendarView)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // LocalDate를 사용해 날짜 객체 생성 (월은 0부터 시작하므로 +1)
            val localDate = LocalDate.of(year, month + 1, dayOfMonth)

            // ISO 8601 형식인 "yyyy-MM-dd"로 문자열 변환
            val date = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            val intent = Intent(this, DailyWriteActivity::class.java)
            intent.putExtra("date", date)
            startActivity(intent)
        }

        setupNavigationBar()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupNavigationBar() {
        val btnCalendar = findViewById<android.widget.Button>(R.id.btnCalendar)
        val btnDaily = findViewById<android.widget.Button>(R.id.btnDaily)
        val btnStats = findViewById<android.widget.Button>(R.id.btnStats)
        val btnInfo = findViewById<android.widget.Button>(R.id.btnInfo)

        btnCalendar.setOnClickListener {
            // 이미 캘린더 화면 → 아무 일도 안 함
        }
        btnDaily.setOnClickListener {
            val today = LocalDate.now().toString()
            val intent = Intent(this, DailyWriteActivity::class.java)
            intent.putExtra("date", today)
            startActivity(intent)
        }
        btnStats.setOnClickListener {
            android.widget.Toast.makeText(this, "통계 화면 예정", android.widget.Toast.LENGTH_SHORT).show()
        }
        btnInfo.setOnClickListener {
            android.widget.Toast.makeText(this, "정보 화면 예정", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
