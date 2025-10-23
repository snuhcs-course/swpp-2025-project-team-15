package com.example.sumdays.statistics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.R // R 파일 임포트

// 개별 '나무'를 담는 뷰 홀더
class MonthViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val monthTitle: TextView = itemView.findViewById(R.id.month_title)
    val grapeIcon: ImageView = itemView.findViewById(R.id.grape_icon) // 월간 요약 포도
    val treeStemLayout: LinearLayout = itemView.findViewById(R.id.tree_stem_layout) // 주간 블록을 쌓을 레이아웃
    val foxIcon: ImageView = itemView.findViewById(R.id.fox_icon)

    // 이외에 배경 나무 이미지, 여우 등의 뷰가 XML에 정의되어 있어야 함
}

class StatisticsMonthAdapter(private val monthList: List<MonthStatistics>) :
    RecyclerView.Adapter<MonthViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        // ⭐ 이 레이아웃 파일은 '나무 한 그루'를 표현하는 XML이 될 것입니다.
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_month_tree, parent, false)
        return MonthViewHolder(view)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        val monthStats = monthList[position]

        // 1. 월 제목 설정
        holder.monthTitle.text = monthStats.monthTitle

        // 2-1. 월간 요약 (포도 아이콘) 표시/숨김
        if (monthStats.monthSummary != null && monthStats.weekSummaries.size >= 2) {
            holder.grapeIcon.visibility = View.VISIBLE
            // TODO: 포도 아이콘 클릭 시 월간 요약 텍스트를 Toast 또는 Dialog로 표시
        } else {
            holder.grapeIcon.visibility = View.GONE
        }
        // ⭐ 2-2. 여우 아이콘 가시성 설정 로직 추가 ⭐
        if (position == 0) {
            // position이 0일 때 (가장 최근 월일 때)만 여우를 표시합니다.
            holder.foxIcon.visibility = View.VISIBLE
        } else {
            // 0이 아닐 때는 여우를 숨깁니다.
            holder.foxIcon.visibility = View.GONE
        }

        // 3. 주간 요약 블록 (초록색 블록) 동적 생성 및 배치
        // 기존 블록을 모두 제거하고 다시 그립니다.
        holder.treeStemLayout.removeAllViews()

        // 주간 요약은 밑에서부터 쌓여야 하므로, 리스트를 역순으로 순회하거나
        // 레이아웃에 리스트를 순서대로 추가하되, 레이아웃의 중력(gravity)을 bottom으로 설정해야 합니다.

        // 블록을 쌓을 때 사용되는 카운터
        var blockCount = 0
        val blockHeightPx = holder.itemView.context.resources.getDimensionPixelSize(R.dimen.week_summary_block_height)

        for (weekSummary in monthStats.weekSummaries) {
            val blockView = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.item_week_summary_block, holder.treeStemLayout, false)

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                blockHeightPx
            )

            // ⭐ 블록 사이의 간격 추가 (marginBottom)
            layoutParams.bottomMargin = holder.itemView.context.resources.getDimensionPixelSize(R.dimen.spacing_between_week_blocks)

            // ⭐ 홀수/짝수 번째에 따른 마진(정렬) 조정 로직
            // 리스트의 인덱스(0, 1, 2...)가 아닌, 생성되는 블록의 순서(1, 2, 3...)를 기준으로 합니다.
            if (blockCount % 2 == 0) { // 0, 2, 4... 번째 (첫 번째, 세 번째...) -> 왼쪽에 치우치게 (오른쪽에 마진)
                layoutParams.marginEnd = 60
                layoutParams.marginStart = 0
                // 텍스트는 블록 내에서 가운데 정렬이 유지되지만, 블록 자체가 나무 기둥의 왼쪽으로 이동합니다.
            } else { // 1, 3, 5... 번째 (두 번째, 네 번째...) -> 오른쪽에 치우치게 (왼쪽에 마진)
                layoutParams.marginStart = 60
                layoutParams.marginEnd = 0
            }

            blockView.layoutParams = layoutParams

            val summaryText: TextView = blockView.findViewById(R.id.week_summary_text)
            summaryText.text = weekSummary.summaryText

            blockView.setOnClickListener {
                Toast.makeText(holder.itemView.context, "주간 요약: ${weekSummary.summaryText}", Toast.LENGTH_SHORT).show()
            }

            holder.treeStemLayout.addView(blockView)
            blockCount++ // 블록 카운터 증가
        }
    }

    override fun getItemCount() = monthList.size
}