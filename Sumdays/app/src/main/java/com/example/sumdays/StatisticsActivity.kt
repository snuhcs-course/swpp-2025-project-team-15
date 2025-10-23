package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageButton
import java.time.LocalDate

class StatisticsActivity : AppCompatActivity() {

    private lateinit var monthRecyclerView: RecyclerView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 임시로 레이아웃을 설정하거나, 토스트를 띄워 화면 전환 확인
        setContentView(R.layout.activity_statistics)
        // (통계 RecyclerView 초기화 로직은 여기에 추가될 예정)

        // ⭐ 내비게이션 바 설정 함수 호출
        setupNavigationBar()
    }
    private fun setupStatisticsRecyclerView() {
        monthRecyclerView = findViewById(R.id.month_statistics_recycler_view)

        // 1. 수평 LayoutManager를 코드로 설정
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        monthRecyclerView.layoutManager = layoutManager

        // 2. 어댑터 연결 (MonthAdapter 대신 StatisticsMonthAdapter를 사용해야 함)
        // monthRecyclerView.adapter = StatisticsMonthAdapter(...)
    }
    // ⭐ 하단 네비게이션 바의 버튼들 클릭 이벤트 처리 함수 추가
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupNavigationBar() {
        // include_nav_daily 레이아웃에서 뷰들을 찾습니다.
        val btnCalendar = findViewById<ImageButton>(R.id.btnCalendar)
        val btnDaily = findViewById<ImageButton>(R.id.btnDaily) // FloatingActionButton이라면 타입 수정 필요
        val btnInfo = findViewById<ImageButton>(R.id.btnInfo)

        // Calendar로 이동
        btnCalendar.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
            finish()
        }

        // DailyWrite (오늘의 일기)로 이동
        btnDaily.setOnClickListener {
            val today = LocalDate.now().toString()
            val intent = Intent(this, DailyWriteActivity::class.java)
            intent.putExtra("date", today)
            startActivity(intent)
            finish()
        }

        // Info 화면 (정보/설정)
        btnInfo.setOnClickListener {
            Toast.makeText(this, "정보 화면 예정", Toast.LENGTH_SHORT).show()
        }
    }
}