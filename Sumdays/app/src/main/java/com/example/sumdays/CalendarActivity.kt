package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.sumdays.calendar.MonthAdapter
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter


class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarViewPager: ViewPager2
    private lateinit var tvMonthYear: TextView
    private lateinit var monthAdapter: MonthAdapter
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton


    // emojiMap: 이모지 데이터를 저장 <날짜, 이모지> -> TODO: 나중에 db에 맞춰 수정 필요
    // ex) "2025-10-10" -> "😊"
    private val emojiMap = mutableMapOf<String, String>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        calendarViewPager = findViewById(R.id.calendarViewPager)
        tvMonthYear = findViewById(R.id.tv_month_year)
        btnPrevMonth = findViewById(R.id.btn_prev_month)
        btnNextMonth = findViewById(R.id.btn_next_month)

        setCustomCalendar()
        loadEventData()
        setStatisticBtnListener()
        setNavigationBar()
    }

    private fun setStatisticBtnListener() {
        val btnStats = findViewById<android.widget.Button>(R.id.statistic_btn)

        btnStats.setOnClickListener {
            android.widget.Toast.makeText(this, "통계 화면 예정", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setNavigationBar() {
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

    /** setCustomCalendar: 달력의 스크롤, 버튼에 의한 달 전환과 현재 월을 표시하는 기능을 수행 */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setCustomCalendar() {
        monthAdapter = MonthAdapter(this)
        calendarViewPager.adapter = monthAdapter

        // 1. Scroll로 달 전환
            // ViewPager2의 무한 스크롤 시작 위치를 중앙으로 설정 (START_POSITION = Int.MAX_VALUE / 2)
        val startPosition = Int.MAX_VALUE / 2
        calendarViewPager.setCurrentItem(startPosition, false)
            // ViewPager2 페이지 전환시 달/년도 업데이트 되도록 함수 연결
        calendarViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateMonthYearTitle(position)
            }
        })

        // 2. 이전/다음 달 버튼으로 달 전환
        btnPrevMonth.setOnClickListener {
            val currentItem = calendarViewPager.currentItem
            calendarViewPager.setCurrentItem(currentItem - 1, true)
        }
        btnNextMonth.setOnClickListener {
            val currentItem = calendarViewPager.currentItem
            calendarViewPager.setCurrentItem(currentItem + 1, true)
        }

        // 현재 월 표시
        updateMonthYearTitle(startPosition)
    }

    /** 입력 받은 position 위치의 달/년도 계산하여 반환함 */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateMonthYearTitle(position: Int) {
        val baseYearMonth = YearMonth.now()
        val startPosition = Int.MAX_VALUE / 2
        val monthDiff = position - startPosition
        val targetMonth = baseYearMonth.plusMonths(monthDiff.toLong())

        val formatter = DateTimeFormatter.ofPattern(getString(R.string.month_year_format))
        tvMonthYear.text = targetMonth.format(formatter)
    }

    /** 이모지 데이터를 가져와 emojiMap에 저장함. 현재는 테스트용. TODO: 나중에 db에 맞춰 수정 필요 */
    private fun loadEventData() {
        val today = LocalDate.now().toString()
        val nextWeek = LocalDate.now().plusWeeks(1).toString()

        emojiMap[today] = "⭐"
        emojiMap[nextWeek] = "💻"
        emojiMap["2025-10-25"] = "🥳"
    }

    /** dayAdapter에서 호출하는 함수. 이모지 존재 유무에 따른 원 배경 설정을 위함 */
    fun getEventEmoji(dateString: String): String? {
        return emojiMap[dateString]
    }
}
