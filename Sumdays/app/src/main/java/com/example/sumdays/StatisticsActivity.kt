package com.example.sumdays

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.ui.TreeTiledDrawable

class StatisticsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var lm: LinearLayoutManager
    private lateinit var treeDrawable: TreeTiledDrawable
    private var totalScrollY = 0

    private var maxScrollForTransition = 10000f  // 어느 정도 스크롤하면 완전히 bg2로 변할지

    private var backgroundList = mutableListOf<Int>(
        R.drawable.statistics_background_morning,
        R.drawable.statistics_background_evening)

    private lateinit var adapter: LeafAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val bg1 = findViewById<ImageView>(R.id.statistics_background_1)
        val bg2 = findViewById<ImageView>(R.id.statistics_background_2)

        recyclerView = findViewById(R.id.recyclerView)

        // 1) 레이아웃 매니저: 바닥에서 시작
        lm = LinearLayoutManager(this, RecyclerView.VERTICAL, false).apply {
            stackFromEnd = true   // ★ 아래가 "끝"
        }
        recyclerView.layoutManager = lm
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER

        // 2) 어댑터: 처음엔 적당한 개수만 (예: 200개)
        adapter = LeafAdapter { index ->
            Toast.makeText(this, "Leaf $index clicked!", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter
        // stackFromEnd=true 덕분에 setAdapter 후 자동으로 "바닥"에 붙음

        // 3) 배경: 무한 타일
        val bmp = BitmapFactory.decodeResource(resources, R.drawable.tree_stem)
        treeDrawable = TreeTiledDrawable(
            bitmap = bmp
        )
        recyclerView.background = treeDrawable

        // 4) 스크롤 리스너: 위로 갈수록 prepend로 확장
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                totalScrollY += -dy

                if (totalScrollY < 0) totalScrollY = 0
                if (totalScrollY > maxScrollForTransition) totalScrollY = maxScrollForTransition.toInt()
                // 0 ~ maxScrollForTransition 범위로 clamp
                val t = (totalScrollY / maxScrollForTransition).coerceIn(0f, 1f)

                // 배경 알파 조절
                bg1.alpha = 1f - t   // 점점 사라짐
                bg2.alpha = t        // 점점 나타남

                treeDrawable.setScroll(totalScrollY.toFloat(), rv.width)
                maybePrependMore()
            }
        })
    }

    /** 리스트 상단 가까이 오면 앞쪽으로 아이템을 붙여 위로 무한 확장 */
    private fun maybePrependMore() {
        val firstPos = lm.findFirstVisibleItemPosition()
        if (firstPos <= 50) { // 상단 임계치
            val firstView = lm.findViewByPosition(firstPos)
            val offsetTop = firstView?.top ?: 0

            val request = 800 // 한 번에 시도할 개수
            val added = adapter.prepend(request)   // 🔴 실제로 얼마나 붙었는지 받기

            if (added > 0) {
                // prepend 전 보던 아이템이 동일 위치로 오도록 보정
                lm.scrollToPositionWithOffset(firstPos + added, offsetTop)
            }
            // added == 0 이면: 더 이상 위에 붙일 잎이 없으므로
            // 그냥 아무 것도 안 하고 놔두면 됨 → 리스트 최상단에서 막힘
        }
    }


    private class LeafAdapter(
        private val onLeafClick: (Int) -> Unit
    ) : RecyclerView.Adapter<LeafAdapter.VH>() {

        // 각 아이템이 "자기 번호"를 갖고 있게
        private data class LeafItem(val index: Int)

        private val items = mutableListOf<LeafItem>()
        private var nextIndex: Int

        private var currentWeeklyStatsNumber: Int = 30
        private var maxLeafIndex: Int

        init {
            maxLeafIndex = currentWeeklyStatsNumber + 10
            // 맨 아래가 1번이 되도록 세팅:
            // position: 0(맨 위) -> index 큰 값
            // position: last(맨 아래) -> index 1
            for (i in maxLeafIndex downTo 1) {
                items.add(LeafItem(i))
            }
            nextIndex = maxLeafIndex + 1
        }

        /** 위로 스크롤하다가 더 필요할 때, 위쪽에 잎 추가 */
        fun prepend(requestCount: Int): Int {
            if (requestCount <= 0) return 0

            // 아직 만들 수 있는 잎 개수
            val remaining = maxLeafIndex - (nextIndex - 1)
            if (remaining <= 0) return 0   // 🔴 한계 도달 → 더 이상 안 붙임

            val toAdd = minOf(requestCount, remaining)

            val newItems = (nextIndex + toAdd - 1 downTo nextIndex).map { LeafItem(it) }
            items.addAll(0, newItems)
            nextIndex += toAdd

            notifyItemRangeInserted(0, toAdd)
            return toAdd
        }

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_leaf, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val leafIndex = items[position].index

            val leafLP = holder.buttonWeeklyStats.layoutParams as FrameLayout.LayoutParams

            val foxLP = holder.foxOnBranchImage.layoutParams as FrameLayout.LayoutParams

            val isLeft = (leafIndex % 2 == 0)

            val isGrapeRow = (leafIndex % 5 == 0)

            val isOnlyBranch = (leafIndex > currentWeeklyStatsNumber)

            if (leafIndex == currentWeeklyStatsNumber){
                holder.foxOnBranchImage.visibility = View.VISIBLE
            }
            else {
                holder.foxOnBranchImage.visibility = View.GONE
            }

            if (isLeft) {
                leafLP.gravity = Gravity.START
                foxLP.gravity = Gravity.START
                if (isOnlyBranch){
                    holder.buttonWeeklyStats.setImageResource(R.drawable.branch_left)
                    holder.buttonWeeklyStats.isEnabled = false
                }
                else if (isGrapeRow) {
                    holder.buttonWeeklyStats.setImageResource(R.drawable.grape_with_branch_left)
                    holder.buttonWeeklyStats.isEnabled = true
                }
                else {
                    holder.buttonWeeklyStats.setImageResource(R.drawable.leaf_left)
                    holder.buttonWeeklyStats.isEnabled = true
                }
            }
            else {
                leafLP.gravity = Gravity.END
                foxLP.gravity = Gravity.END
                if (isOnlyBranch){
                    holder.buttonWeeklyStats.setImageResource(R.drawable.branch_right)
                    holder.buttonWeeklyStats.isEnabled = false
                }
                else if (isGrapeRow) {
                    holder.buttonWeeklyStats.setImageResource(R.drawable.grape_with_branch_right)
                    holder.buttonWeeklyStats.isEnabled = true
                }
                else {
                    holder.buttonWeeklyStats.setImageResource(R.drawable.leaf_right)
                    holder.buttonWeeklyStats.isEnabled = true
                }
            }
            holder.buttonWeeklyStats.layoutParams = leafLP

            holder.buttonWeeklyStats.setOnClickListener { onLeafClick(leafIndex) }
        }

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val buttonWeeklyStats: ImageButton = view.findViewById(R.id.btnWeeklyStats)
            val foxOnBranchImage: ImageView = view.findViewById(R.id.fox_on_branch)

            fun dp(v: Int): Int =
                (itemView.resources.displayMetrics.density * v + 0.5f).toInt()
        }
    }
}
