package com.example.sumdays

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.text.style.LineBackgroundSpan
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate

class CalendarActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        setCalendarViewListener()
        setStatisticBtnListener()
        setupNavigationBar()
    }
    private fun setCalendarViewListener() {
        val calendarView = findViewById<MaterialCalendarView>(R.id.calendarView)

        // 날짜 선택 이벤트 핸들러
        calendarView.setOnDateChangedListener {widget, date, selected ->
            val dateInfo = date.date.toString()
            val intent = Intent(this, DailyReadActivity::class.java)
            intent.putExtra("date", dateInfo)
            startActivity(intent)
        }
        // 캘린더 커스텀
        calendarView.addDecorators(
            SaturdayDecorator(),
            SundayDecorator())
    }

    private fun setStatisticBtnListener() {
        val statisticButton = findViewById<android.widget.Button>(R.id.statistic_btn)

        statisticButton.setOnClickListener {
            android.widget.Toast.makeText(this, "통계 화면 예정", android.widget.Toast.LENGTH_SHORT).show()
        }
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

/* 토요일 색상 변경 */
private class SaturdayDecorator: DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay?): Boolean {
        val dayOfWeek = day?.date?.dayOfWeek
        return dayOfWeek == DayOfWeek.SATURDAY
    }

    override fun decorate(view: DayViewFacade?) {
        view?.addSpan(ForegroundColorSpan(Color.BLUE))
    }
}
/* 일요일 색상 변경 */
private class SundayDecorator: DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay?): Boolean {
        val dayOfWeek = day?.date?.dayOfWeek
        return dayOfWeek == DayOfWeek.SUNDAY
    }

    override fun decorate(view: DayViewFacade?) {
        view?.addSpan(ForegroundColorSpan(Color.RED))
    }
}
