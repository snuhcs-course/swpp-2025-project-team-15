package com.example.sumdays.statistics

import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.sumdays.R
import com.example.sumdays.databinding.ActivityWeekStatsDetailBinding
import com.example.sumdays.utils.setupEdgeToEdge
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

// 주간 통계 상세 분석 화면 (보고서 형식)
class WeekStatsDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeekStatsDetailBinding
    private lateinit var weekSummary: WeekSummary

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Intent에서 데이터 받기
        weekSummary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("week_summary", WeekSummary::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("week_summary")
        } ?: run {
            // 데이터가 없을 경우 임시로 Activity를 종료하거나 오류 처리
            Toast.makeText(this, "통계 데이터를 불러올 수 없습니다.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding = ActivityWeekStatsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setupReportViews()

        // 상태바, 네비게이션바 같은 색으로
        val rootView = findViewById<View>(R.id.main_detail)
        setupEdgeToEdge(rootView)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupReportViews() {
        // 1. 기간 정보 및 제목 설정
        binding.weekRangeTextView.text = "${weekSummary.startDate} ~ ${weekSummary.endDate}"

        // 2. 요약 및 피드백 내용 설정 (GridLayout 내용)
        binding.summaryContentTextView.text = weekSummary.summary.overview

        binding.feedbackContentTextView.text = weekSummary.insights.advice


        // 3. 요일 표시 영역 (월-일) 동적 설정
        // API에서 받은 하이라이트 목록에서 일기가 작성된 날짜를 추출합니다.
        val highlightDates = weekSummary.highlights.map { LocalDate.parse(it.date) }

        // 주간 시작 날짜를 LocalDate 객체로 변환
        val startDate = LocalDate.parse(weekSummary.startDate)

        // 요일 헤더 레이아웃 참조
        val dayRow1 = binding.dayRow1
        val dayRow2 = binding.dayRow2
        dayRow1.removeAllViews()
        dayRow2.removeAllViews()

        // 요일 이름 (Kotlin의 DayOfWeek 순서를 따름)
        val dayNames = listOf("월", "화", "수", "목", "금", "토", "일")

        for (i in 0 until 7) {
            val date = startDate.plusDays(i.toLong())
            val dayOfWeek = date.dayOfWeek // DayOfWeek.MONDAY, TUESDAY 등

            // 일기가 작성된 날짜인지 확인
            val isDiaryWritten = highlightDates.contains(date)

            // 레이아웃 인플레이트 (item_day_name 대신 동적으로 TextView 생성)
            val dayLayout = LayoutInflater.from(this).inflate(R.layout.include_day_cell_static, null) as FrameLayout
            val dayTextView: TextView = dayLayout.findViewById(R.id.day_name_text) // 임시 TextView ID 가정

            // 요일 이름 설정
            dayTextView.text = dayNames[i % 7]

            // ⭐ 일기 작성 여부에 따라 테두리(여우 모양) 적용
            if (isDiaryWritten) {
                dayLayout.setBackgroundResource(R.drawable.shape_fox_orange_border)
            } else {
                dayLayout.setBackgroundResource(android.R.color.transparent) // 투명
            }

            // 레이아웃 파라미터 (5열과 2열에 맞게 가중치 조정 필요)
            val layoutParams = LinearLayout.LayoutParams(
                0,
                200,
                if (i < 5) 1.0f else 0.5f // 월-금은 1.0f, 토-일은 0.5f (공간을 나눠야 함)
            ).apply {
                // 간격 확보 (선택 사항)
                marginStart = 4.dp
                marginEnd = 4.dp
            }
            dayLayout.layoutParams = layoutParams

            // 월-금은 day_row_1에, 토-일은 day_row_2에 추가
            if (dayOfWeek <= DayOfWeek.FRIDAY) {
                dayRow1.addView(dayLayout)
            } else {
                dayRow2.addView(dayLayout)
            }
        }
    }

    private fun setupListeners() {
        // 뒤로가기 버튼
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    // DP를 Pixel로 변환하는 확장 함수 (Context 필요)
    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density + 0.5f).toInt()
}