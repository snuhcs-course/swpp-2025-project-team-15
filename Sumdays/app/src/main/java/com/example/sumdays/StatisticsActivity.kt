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
import com.example.sumdays.statistics.EmotionAnalysis
import com.example.sumdays.statistics.Highlight
import com.example.sumdays.statistics.Insights
import com.example.sumdays.statistics.MonthStatistics
import com.example.sumdays.statistics.MonthSummary
import com.example.sumdays.statistics.StatisticsMonthAdapter
import com.example.sumdays.statistics.SummaryDetails
import com.example.sumdays.statistics.WeekSummary
import com.example.sumdays.statistics.WeekSummaryForMonth
import java.time.LocalDate

class StatisticsActivity : AppCompatActivity() {

    private lateinit var monthRecyclerView: RecyclerView

    // ⭐ 더미 데이터 생성 함수
    private fun createDummyData(): List<MonthStatistics> {

        // ==============================================
        // 1. 2025년 9월 데이터 (Week 2개 이상, MonthSummary 존재)
        // ==============================================

        // --- 주간 요약 Helper 객체들 ---
        val weekInsights9_1 = Insights("작은 성취를 기록하세요.", "단조로운 흐름")
        val weekSummaryDetails9_1 =
            SummaryDetails(listOf("개발", "테스팅"), "테스팅 환경 구축의 일주일", "테스팅 환경 구축 완료")
        val emotionAnalysis9_1 = EmotionAnalysis(
            mapOf("positive" to 3, "neutral" to 2, "negative" to 0),
            "😊",
            0.6f,
            "stable"
        )

        val weekInsights9_2 = Insights("협업 포인트를 명확히 하세요.", "긴장 -> 해소")
        val weekSummaryDetails9_2 = SummaryDetails(listOf("아키텍처", "DB연동"), "백엔드 통합 논의 완료", "아키텍처 확립")
        val emotionAnalysis9_2 = EmotionAnalysis(mapOf("positive" to 5, "neutral" to 1, "negative" to 1), "😄", 0.7f, "increasing")

        // --- WeekSummary 객체들 (나무 기둥 블록) ---
        val weekStatistics9_1 = WeekSummary(emotionAnalysis9_1, listOf(
            Highlight(
                "2025-09-03",
                "JUnit 4 설정 완료"
            )
        ), weekInsights9_1, weekSummaryDetails9_1)
        val weekStatistics9_2 = WeekSummary(emotionAnalysis9_2, listOf(Highlight("2025-09-10", "MVVM 구조 확정")), weekInsights9_2, weekSummaryDetails9_2)

        // Month Summary 객체들
        val monthInsights9 = Insights("9월의 안정감을 10월에도 유지하세요.", "긴장 -> 안정 -> 자신감")
        val monthSummaryDetails9 = SummaryDetails(listOf("안정", "계획", "테스팅"), "9월은 개발 기반을 다진 달", "개발 기반 확립의 9월")
        val monthWeeksForMonth9 = listOf(
            WeekSummaryForMonth(
                0.6f,
                "😊",
                listOf("개발", "계획"),
                "주간 요약 1",
                "안정적인 시작",
                "2025-09-01~2025-09-07"
            ),
            WeekSummaryForMonth(0.7f, "😄", listOf("통합", "구조"), "주간 요약 2", "구조 확정 주", "2025-09-08~2025-09-14")
        )

        val monthSummary9 = MonthSummary(monthInsights9, monthSummaryDetails9, monthWeeksForMonth9)


        val monthStatisticsSeptember = MonthStatistics(
            year = 2025,
            month = 9,
            monthTitle = "2025년 9월",
            weekSummaries = listOf(weekStatistics9_1, weekStatistics9_2), // 주간 블록
            monthSummary = monthSummary9 // 월간 요약 (포도)
        )

        // ==============================================
        // 2. 2025년 8월 데이터 (Week 2개 미만, MonthSummary 없음)
        // ==============================================

        val weekInsights8_1 = Insights("계속 아이디어를 구체화하세요.", "혼란")
        val weekSummaryDetails8_1 = SummaryDetails(listOf("아이디어", "회의"), "아이디어 구상의 일주일", "초기 아이디어 구상")
        val emotionAnalysis8_1 = EmotionAnalysis(mapOf("positive" to 1, "neutral" to 3, "negative" to 1), "🤔", 0.1f, "stable")

        val weekStatistics8_1 = WeekSummary(emotionAnalysis8_1, listOf(Highlight("2025-08-15", "프로젝트 주제 결정")), weekInsights8_1, weekSummaryDetails8_1)

        val monthStatisticsAugust = MonthStatistics(
            year = 2025,
            month = 8,
            monthTitle = "2025년 8월",
            weekSummaries = listOf(weekStatistics8_1), // 주간 블록 1개만
            monthSummary = null // 포도 없음
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