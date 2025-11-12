

// MonthAdapter.kt
package com.example.sumdays.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.CalendarActivity
import com.example.sumdays.R
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth

class MonthAdapter(
    private val activity: CalendarActivity,
    // ★ 추가: 허용되는 최대 월(= 현재 월)과 오늘
    private val maxYearMonth: YearMonth = YearMonth.now(),
    private val today: LocalDate = LocalDate.now()
) : RecyclerView.Adapter<MonthAdapter.MonthViewHolder>() {

    override fun getItemCount(): Int = Int.MAX_VALUE

    private val START_POSITION = Int.MAX_VALUE / 2
    private val baseYearMonth = YearMonth.now()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_month_grid, parent, false)
        return MonthViewHolder(view)
    }

    /** 스크롤 위치를 바탕으로 그 달의 정보를 캘린더와 연결 */
    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        val monthDiff = position - START_POSITION
        val targetMonth = baseYearMonth.plusMonths(monthDiff.toLong())

        val monthData = getMonthData(targetMonth.year, targetMonth.monthValue)
        holder.bind(monthData, activity, today, maxYearMonth)
    }

    class MonthViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayGrid: RecyclerView = itemView.findViewById(R.id.rv_day_grid)

        fun bind(
            monthData: MonthData,
            activity: CalendarActivity,
            today: LocalDate,
            maxYearMonth: YearMonth
        ) {
            dayGrid.layoutManager = GridLayoutManager(itemView.context, 7)
            // ★ DayAdapter에 today와 maxYearMonth 전달
            val dayAdapter = DayAdapter(monthData.days, activity, today, maxYearMonth)
            dayGrid.adapter = dayAdapter
        }
    }
}