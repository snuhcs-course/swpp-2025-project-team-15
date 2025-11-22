package com.example.sumdays

import android.content.Intent
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.statistics.EmotionAnalysis
import com.example.sumdays.statistics.Highlight
import com.example.sumdays.statistics.Insights
import com.example.sumdays.statistics.SummaryDetails
import com.example.sumdays.statistics.WeekStatsDetailActivity
import com.example.sumdays.statistics.WeekSummary
import com.example.sumdays.ui.TreeTiledDrawable
import com.example.sumdays.utils.setupEdgeToEdge

class StatisticsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var lm: LinearLayoutManager
    private lateinit var treeDrawable: TreeTiledDrawable

    private var bgScrollY = 0f      // 배경 전환용
    private var treeScrollY = 0f    // 나무 줄기 타일용
    private var segmentScroll = 8000f  // 어느 정도 스크롤하면 완전히 bg2로 변할지

    private var backgrounds = listOf<Int>(
        R.drawable.statistics_background_morning,
        R.drawable.statistics_background_evening,
        R.drawable.statistics_background_stratosphere,
        R.drawable.statistics_background_space)

    // 전체 스크롤 범위 = (배경 개수 - 1) * segmentScroll
    private val maxScrollForTransition: Float
        get() = segmentScroll * (backgrounds.size - 1)

    // 현재 어떤 구간(배경 i ↔ i+1)을 쓰고 있는지
    private var currentSegmentIndex: Int = -1

    // ⭐️ WeekSummary 데이터 목록 (최대 인덱스 최신 데이터)
    private lateinit var weekSummaries: List<WeekSummary>

    private lateinit var adapter: LeafAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val bg1 = findViewById<ImageView>(R.id.statistics_background_1)
        val bg2 = findViewById<ImageView>(R.id.statistics_background_2)

        // 초기 배경은 리스트 첫 번째로
        if (backgrounds.isNotEmpty()) {
            bg1.setImageResource(backgrounds[0])
            bg2.setImageResource(backgrounds.getOrNull(1) ?: backgrounds[0])
            bg1.alpha = 1f
            bg2.alpha = 0f
            currentSegmentIndex = 0
        }

        // ⭐️ 더미 데이터 생성 및 저장 (60개 주간 데이터)
        val dummyCount = 60
        weekSummaries = createDummyWeekSummaries(dummyCount)

        recyclerView = findViewById(R.id.recyclerView)

        // 1) 레이아웃 매니저: 바닥에서 시작
        lm = LinearLayoutManager(this, RecyclerView.VERTICAL, false).apply {
            stackFromEnd = true   // ★ 아래가 "끝"
        }
        recyclerView.layoutManager = lm
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER

        // 2) 어댑터: 데이터 및 콜백 전달
        // adapter = LeafAdapter { index -> ... } // 기존
        adapter = LeafAdapter(
            weekSummaries = weekSummaries, // WeekSummary 데이터 전달
            currentStatsNumber = dummyCount
        )
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
                handleScrollForBackground(bg1, bg2, dy, rv.width)
                maybePrependMore()
            }
        })

        // 상태바, 네비게이션바 같은 색으로
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        setupEdgeToEdge(recyclerView)
    }

    private fun handleScrollForBackground(bg1: ImageView, bg2: ImageView, dy: Int, rvWidth: Int) {
        // ---------- 1) 배경 전환용 스크롤 (위로 올릴수록 값 증가) ----------
        // 위로 스크롤: dy < 0 → bgScrollY 증가
        // 아래로 스크롤: dy > 0 → bgScrollY 감소
        bgScrollY += -dy
        bgScrollY = bgScrollY.coerceIn(0f, maxScrollForTransition)

        if (backgrounds.size > 1) {
            val progress = bgScrollY / segmentScroll      // 0 ~ (N-1)
            val segmentIndex = progress.toInt().coerceIn(0, backgrounds.size - 2)
            val localRawT = (progress - segmentIndex).coerceIn(0f, 1f)

            if (segmentIndex != currentSegmentIndex) {
                bg1.setImageResource(backgrounds[segmentIndex])
                bg2.setImageResource(backgrounds[segmentIndex + 1])
                currentSegmentIndex = segmentIndex
            }

            // 각 세그먼트 내에서만 "짧은 전환"
            val transitionWidth = 0.2f
            val start = 0.5f - transitionWidth / 2f
            val end   = 0.5f + transitionWidth / 2f

            val localSharpT = when {
                localRawT <= start -> 0f
                localRawT >= end   -> 1f
                else -> (localRawT - start) / (end - start)
            }

            bg1.alpha = 1f - localSharpT
            bg2.alpha = localSharpT
        }

        // ---------- 2) 나무 줄기 스크롤 (RecyclerView와 같은 방향) ----------
        // RecyclerView는 dy<0 이면 "위로 스크롤" → 아이템들이 아래로 이동
        // 우리가 그 전에 잘 되던 때처럼, 나무도 dy를 그대로 누적시키면
        // 가지/잎이랑 같은 느낌으로 같이 움직여 보인다.
        treeScrollY += dy
        treeDrawable.setScroll(treeScrollY, rvWidth)
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

    // --- WeekSummary Dummy Data 생성 함수 ---

    private fun createDummyWeekSummary(index: Int): WeekSummary {
        // index 1이 가장 최신, index 60이 가장 오래된 더미 데이터
        val year = 2025
        val month = (index / 4) % 12 + 1 // 대략적인 월
        val day = (index % 4) * 7 + 1    // 대략적인 일

        val startDate = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

        val emotions = listOf("positive", "neutral", "negative")
        val dominantEmoji = when (index % 3) {
            0 -> "😊" // 긍정
            1 -> "😐" // 중립
            else -> "😠" // 부정
        }

        val topics = listOf("운동", "공부", "취미", "업무", "여행", "휴식", "식단")
        val topic = topics[index % topics.size]

        return WeekSummary(
            startDate = startDate,
            endDate = startDate, // 단순 더미 데이터이므로 시작일과 동일하게 설정
            diaryCount = 3 + (index % 4),
            emotionAnalysis = EmotionAnalysis(
                distribution = mapOf(
                    emotions[0] to 60 + index,
                    emotions[1] to 30 + (index % 10),
                    emotions[2] to 10 + (index % 5)
                ),
                dominantEmoji = dominantEmoji,
                emotionScore = 0.5f + (index % 10) * 0.05f,
                trend = if (index % 2 == 0) "increasing" else "decreasing"
            ),
            highlights = listOf(
                Highlight(date = startDate, summary = "이번 주는 $topic 주제로 열심히 살았습니다."),
                Highlight(date = startDate, summary = "마무리 일기 요약입니다.")
            ),
            insights = Insights(
                advice = "스트레스를 관리하며 $topic 을 꾸준히 하는 것이 중요합니다.",
                emotionCycle = if (index % 2 == 0) "주중 감정 기복이 적었습니다." else "주말 감정 기복이 컸습니다."
            ),
            summary = SummaryDetails(
                emergingTopics = listOf(topic, "성장", "회고"),
                overview = "주간 $index 째 요약입니다. $topic 에 집중한 한 주였습니다.",
                title = "$topic 라이프 - ${dominantEmoji} 주간 기록"
            )
        )
    }

    private fun createDummyWeekSummaries(count: Int): List<WeekSummary> {
        // 인덱스 1~count 만큼의 데이터를 생성하여 반환 (index 1이 list[0]에 해당)
        return (1..count).map { index ->
            createDummyWeekSummary(index)
        }
    }


    /** 어댑터 클래스: WeekSummary 데이터를 받도록 수정 */
    private class LeafAdapter(
        private val weekSummaries: List<WeekSummary>, // WeekSummary 데이터 목록 (index 1부터 순서대로)
        private val currentStatsNumber: Int
    ) : RecyclerView.Adapter<LeafAdapter.VH>() {

        private data class LeafItem(val index: Int)

        private val items = mutableListOf<LeafItem>()
        private var nextIndex: Int

        private var maxLeafIndex: Int

        init {
            // maxLeafIndex는 데이터 개수 + 여분 가지 (10개)
            maxLeafIndex = currentStatsNumber + 10

            // 맨 아래(가장 최신 기록)가 index 1이 되도록 세팅
            // items[0] = LeafItem(maxLeafIndex) (가장 오래된 가지)
            // items[maxLeafIndex - 1] = LeafItem(1) (가장 최신 기록)
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
            val leafIndex = items[position].index // 70(위) ~ 1(아래)

            // LeafItem의 index가 실제 통계 데이터 범위(1~60) 내에 있는지 확인
            val hasData = leafIndex <= currentStatsNumber && leafIndex >= 1
            val weekSummary = if (hasData) {
                // weekSummaries는 index 1부터 순서대로 저장되어 있으므로, index-1을 사용
                weekSummaries.getOrNull(leafIndex - 1)
            } else {
                null
            }

            // isOnlyBranch: 데이터가 없거나, 잎이 없는 단순 가지 (index > 60)
            val isOnlyBranch = !hasData

            val leafLP = holder.buttonWeeklyStats.layoutParams as FrameLayout.LayoutParams
            val foxLP = holder.foxOnBranchImage.layoutParams as FrameLayout.LayoutParams

            val isLeft = (leafIndex % 2 == 0)
            val isGrapeRow = (leafIndex % 5 == 0)

            // 여우 마스코트는 가장 최신 데이터(index 1) 위에 배치
           if (leafIndex == currentStatsNumber) {
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
            else { // isRight
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
            holder.foxOnBranchImage.layoutParams = foxLP

            // ⭐️ 버튼 클릭 리스너 업데이트 (데이터가 있을 때만 호출)
            if (hasData && weekSummary != null) {
                holder.buttonWeeklyStats.setOnClickListener {
                    val intent = Intent(holder.itemView.context, WeekStatsDetailActivity::class.java)

                    // ⭐ WeekSummary 객체를 Parcelable로 담아 전달
                    // WeekSummary 클래스가 Parcelable을 상속하고 있어야 이 코드가 정상 작동합니다.
                    intent.putExtra("week_summary", weekSummary)

                    holder.itemView.context.startActivity(intent)
                }
                holder.buttonWeeklyStats.isEnabled = true
            } else {
                holder.buttonWeeklyStats.setOnClickListener(null)
                holder.buttonWeeklyStats.isEnabled = false
            }
        }

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val buttonWeeklyStats: ImageButton = view.findViewById(R.id.btnWeeklyStats)
            val foxOnBranchImage: ImageView = view.findViewById(R.id.fox_on_branch)

            fun dp(v: Int): Int =
                (itemView.resources.displayMetrics.density * v + 0.5f).toInt()
        }
    }
}