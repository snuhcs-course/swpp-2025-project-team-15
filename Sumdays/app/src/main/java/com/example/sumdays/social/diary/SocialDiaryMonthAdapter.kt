package com.example.sumdays.social.diary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.CalendarActivity
import com.example.sumdays.R
import com.example.sumdays.calendar.MonthData
import com.example.sumdays.calendar.getMonthData
import org.threeten.bp.YearMonth

class SocialDiaryMonthAdapter(private val activity: SocialDiaryActivity) :
    RecyclerView.Adapter<SocialDiaryMonthAdapter.MonthViewHolder>() {

    override fun getItemCount(): Int = Int.MAX_VALUE

    private val START_POSITION = Int.MAX_VALUE / 2
    private val baseYearMonth = YearMonth.now()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_month_grid, parent, false)

        return MonthViewHolder(view)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        val monthDiff = position - START_POSITION
        val targetMonth = baseYearMonth.plusMonths(monthDiff.toLong())
        val monthData = getMonthData(targetMonth.year, targetMonth.monthValue)

        holder.bind(monthData, activity)
    }

    class MonthViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayGrid: RecyclerView = itemView.findViewById(R.id.rv_day_grid)

        fun bind(monthData: MonthData, activity: SocialDiaryActivity) {
            dayGrid.layoutManager = GridLayoutManager(itemView.context, 7)

            val socialDiaryDayAdapter = SocialDiaryDayAdapter(monthData.days, activity)
            dayGrid.adapter = socialDiaryDayAdapter
        }
    }
}