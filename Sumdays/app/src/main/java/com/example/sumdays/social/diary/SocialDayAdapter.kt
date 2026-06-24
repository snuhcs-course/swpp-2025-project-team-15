package com.example.sumdays.social.diary


import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.DailyReadActivity
import com.example.sumdays.R
import com.example.sumdays.calendar.DateCell
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth

class SocialDayAdapter(
    private val days: List<DateCell>,
    private val activity: SocialCalendarActivity,
    private val today: LocalDate = LocalDate.now(),
    private val maxYearMonth: YearMonth = YearMonth.now()
) : RecyclerView.Adapter<SocialDayAdapter.DayViewHolder>() {

    override fun getItemCount(): Int = days.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day_cell, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val cell = days[position]
        holder.bind(cell, activity, today, maxYearMonth)
    }

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCircle: TextView = itemView.findViewById(R.id.tv_circle)
        private val tvDayNumber: TextView = itemView.findViewById(R.id.tv_day_number)
        private val tvEmoji: TextView = itemView.findViewById(R.id.tv_emoji)

        fun bind(
            cell: DateCell,
            activity: SocialCalendarActivity,
            today: LocalDate,
            maxYearMonth: YearMonth
        ) {
            activity.applyThemeModeSettings(itemView)

            if (cell.day <= 0 || cell.day > 31) {
                tvCircle.background = null
                tvDayNumber.text = ""
                tvEmoji.visibility = View.GONE
                itemView.isClickable = false
                return
            }

            tvDayNumber.text = cell.day.toString()
            itemView.isClickable = true

            val date = LocalDate.parse(cell.dateString)
            val isToday = date.isEqual(today)
            val isFutureDay = date.isAfter(today)

            val dayOfWeek = date.dayOfWeek
            val textColor = when {
                dayOfWeek == DayOfWeek.SUNDAY -> Color.RED
                dayOfWeek == DayOfWeek.SATURDAY -> Color.parseColor("#0095FF")
                else -> ContextCompat.getColor(itemView.context, R.color.textColor)
            }
            tvDayNumber.setTextColor(textColor)

            // 🌟 [변경] 액티비티가 연-월 최적화로 갈아끼워 준 currentMonthStatusMap을 직접 참조!
            val statusPair = activity.currentMonthStatusMap[cell.dateString]
            val hasDiary = statusPair?.first ?: false   // 친구가 일기를 작성했는가?
            val isAllowed = statusPair?.second ?: false // 나에게 열람 권한이 있는가?

            // 🌟 [변경] 형의 기획대로 완료 스킨(completed)은 빼버리고 오직 '오늘'만 특수 배경 처리!
            when {
                isToday -> {
                    tvCircle.background = ContextCompat.getDrawable(
                        itemView.context,
                        R.drawable.calendar_shape_fox_today
                    )
                    tvDayNumber.setTypeface(null, Typeface.BOLD)
                }

                else -> {
                    tvCircle.background = ContextCompat.getDrawable(
                        itemView.context,
                        R.drawable.calendar_shape_fox
                    )
                    tvDayNumber.setTypeface(null, Typeface.NORMAL)
                }
            }

            if (cell.isCurrentMonth && hasDiary) {
                tvEmoji.visibility = View.VISIBLE // 원래 안 보이던 에모지/아이콘 뷰를 켜주고!
                if (isAllowed) {
                    tvEmoji.text = "⭕"
                } else {
                    // Case 2: 작성되었으나 열람 비허용된 경우 잠긴 자물쇠
                    tvEmoji.text = "🔒"
                }
            } else {
                // 일기가 없거나 이번 달이 아니면 자물쇠 숨기기
                tvEmoji.visibility = View.GONE
            }

            if (!cell.isCurrentMonth) {
                tvEmoji.visibility = View.GONE
                tvDayNumber.alpha = 0.4f
                tvCircle.alpha = 0.4f
                tvDayNumber.setTextColor(Color.GRAY)
            } else {
                tvDayNumber.alpha = 1.0f
                tvCircle.alpha = 1.0f
            }

            if (isToday) {
                val density = itemView.context.resources.displayMetrics.density
                val width = (20 * density).toInt()
                val height = (17 * density).toInt()

                val params = tvDayNumber.layoutParams
                params.width = width
                params.height = height
                tvDayNumber.layoutParams = params

                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.WHITE)
                    cornerRadius = 5 * density
                }

                tvDayNumber.background = drawable
                tvDayNumber.setTextColor(Color.BLACK)
                tvDayNumber.setTypeface(null, Typeface.BOLD)
            } else {
                tvDayNumber.background = null
            }

            // 🌟 [변경] 친구 달력 전용 클릭 리스너 제어 (미래 디펜스 + 권한 분기)
            if (isFutureDay) {
                itemView.isClickable = false
                itemView.isFocusable = false
                tvEmoji.visibility = View.GONE
                tvDayNumber.setTypeface(null, Typeface.NORMAL)
                itemView.setOnClickListener {
                    Toast.makeText(activity, "미래의 일기로는 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (cell.dateString.isNotEmpty()) {
                    itemView.setOnClickListener {
                        when {
                            // 🟢 Case 1: 일기도 있고 나한테 권한도 준 날 -> 친구 전용 읽기창 빌드
                            hasDiary && isAllowed -> {
                                val intent = Intent(activity, DailyReadActivity::class.java).apply {
                                    putExtra("date", cell.dateString)
                                    putExtra("isFriendMode", true) // 친구 모드 플래그 주입
                                }
                                activity.startActivity(intent)
                            }

                            // 🔒 Case 2: 일기는 썼는데 나한테는 잠가둔 날 -> 토스트 디펜스
                            hasDiary && !isAllowed -> {
                                Toast.makeText(
                                    activity,
                                    "친구가 비공개로 설정한 일기입니다. 🔒",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            // ❌ Case 3: 친구가 아예 일기를 생략한 날
                            else -> {
                                Toast.makeText(
                                    activity,
                                    "친구가 일기를 작성하지 않은 날입니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }
}