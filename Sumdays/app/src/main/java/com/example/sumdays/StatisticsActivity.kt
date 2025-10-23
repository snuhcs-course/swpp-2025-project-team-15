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
import com.example.sumdays.statistics.MonthStatistics
import com.example.sumdays.statistics.MonthSummary
import com.example.sumdays.statistics.StatisticsMonthAdapter
import com.example.sumdays.statistics.WeekSummary
import java.time.LocalDate

class StatisticsActivity : AppCompatActivity() {

    private lateinit var monthRecyclerView: RecyclerView

    // ⭐ 더미 데이터 생성 함수
    private fun createDummyData(): List<MonthStatistics> {
        val week1 = WeekSummary("이번 주 테스팅 계획을 수립함.", "2025-09-01", "2025-09-07", 5)
        val week2 = WeekSummary("프론트/백엔드 아키텍처 정리 완료.", "2025-09-08", "2025-09-14", 3)
        val week3 = WeekSummary("새로운 통계 화면 UI/UX 논의 완료.", "2025-09-15", "2025-09-21", 7)

        // Month Summary (포도) 생성 조건: Week Summary가 2개 이상일 때
        val monthSummary9 = MonthSummary("9월은 아키텍처 확립과 테스팅 계획을 수립하며 개발의 기틀을 다진 달.", "2025-09-30")

        val monthStatisticsSeptember = MonthStatistics(
            year = 2025,
            month = 9,
            monthTitle = "2025년 9월",
            weekSummaries = listOf(week1, week2, week3),
            monthSummary = monthSummary9 // 3개이므로 포도 존재
        )

        // Week Summary가 2개 미만인 달 (포도 없음)
        val week4 = WeekSummary("8월 프로젝트 아이디어 구상 완료.", "2025-08-01", "2025-08-07", 2)
        val monthStatisticsAugust = MonthStatistics(
            year = 2025,
            month = 8,
            monthTitle = "2025년 8월",
            weekSummaries = listOf(week4),
            monthSummary = null // 1개이므로 포도 없음
        )

        return listOf(monthStatisticsSeptember, monthStatisticsAugust) // 9월(최근)이 앞에 위치
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 임시로 레이아웃을 설정하거나, 토스트를 띄워 화면 전환 확인
        setContentView(R.layout.activity_statistics)
        // 통계 RecyclerView 초기화 로직
        setupStatisticsRecyclerView()
        // ⭐ 내비게이션 바 설정 함수 호출
        setupNavigationBar()
    }
    private fun setupStatisticsRecyclerView() {
        monthRecyclerView = findViewById(R.id.month_statistics_recycler_view)

        // 1. 수평 LayoutManager를 코드로 설정
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        monthRecyclerView.layoutManager = layoutManager

        // ⭐ 더미 데이터를 어댑터에 넘겨 초기화
        val dummyData = createDummyData()
        val adapter = StatisticsMonthAdapter(dummyData)
        monthRecyclerView.adapter = adapter
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