package com.example.sumdays

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private lateinit var adapter: LeafAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        recyclerView = findViewById(R.id.recyclerView)

        // 1) 레이아웃 매니저: 바닥에서 시작
        lm = LinearLayoutManager(this, RecyclerView.VERTICAL, false).apply {
            stackFromEnd = true   // ★ 아래가 "끝"
        }
        recyclerView.layoutManager = lm
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER

        // 2) 어댑터: 처음엔 적당한 개수만 (예: 200개)
        adapter = LeafAdapter(initialCount = 100) { index ->
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
                totalScrollY += dy
                treeDrawable.setScroll(totalScrollY.toFloat(), rv.width)
                maybePrependMore()
            }
        })
    }

    /** 리스트 상단 가까이 오면 앞쪽으로 아이템을 붙여 위로 무한 확장 */
    private fun maybePrependMore() {
        val firstPos = lm.findFirstVisibleItemPosition()
        if (firstPos <= 50) { // 상단 임계치
            // prepend 전 현재 첫번째 보이는 뷰의 top을 저장해서 점프 방지
            val firstView = lm.findViewByPosition(firstPos)
            val offsetTop = firstView?.top ?: 0

            val added = 800 // 한 번에 넉넉히 추가 (필요에 맞춰 조절)
            adapter.prepend(added)

            // 화면 보정: prepend 전 보던 아이템이 동일 위치로 오도록
            lm.scrollToPositionWithOffset(firstPos + added, offsetTop)
        }
    }

    private class LeafAdapter(
        initialCount: Int,
        private val onLeafClick: (Int) -> Unit
    ) : RecyclerView.Adapter<LeafAdapter.VH>() {

        // 각 아이템이 "자기 번호"를 갖고 있게
        private data class LeafItem(val index: Int)

        private val items = mutableListOf<LeafItem>()
        private var nextIndex: Int

        init {
            // 맨 아래가 1번이 되도록 세팅:
            // position: 0(맨 위) -> index 큰 값
            // position: last(맨 아래) -> index 1
            for (i in initialCount downTo 1) {
                items.add(LeafItem(i))
            }
            nextIndex = initialCount + 1
        }

        /** 위로 스크롤하다가 더 필요할 때, 위쪽에 잎 추가 */
        fun prepend(n: Int) {
            if (n <= 0) return

            // 새 잎들은 기존 것보다 번호가 더 큰 애들
            val newItems = (nextIndex + n - 1 downTo nextIndex).map { idx ->
                LeafItem(idx)
            }
            items.addAll(0, newItems)
            nextIndex += n

            notifyItemRangeInserted(0, n)
        }

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_leaf, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val leafIndex = items[position].index

            val leafLP = holder.buttonLeaf.layoutParams as FrameLayout.LayoutParams
            val grapeLP = holder.imgGrape.layoutParams as FrameLayout.LayoutParams

            // 좌/우 번갈아: (parityBase + position) % 2
            val isLeft = (leafIndex % 2 == 0)

            if (isLeft) {
                // grape is at right branch
                leafLP.gravity = Gravity.START
                grapeLP.gravity = Gravity.END
                holder.buttonLeaf.setImageResource(R.drawable.leaf_left)
                holder.imgGrape.setImageResource(R.drawable.grape_with_branch_right)
                holder.imgGrape.translationX = 300.toFloat()
            }
            else {
                // grape is at left branch
                leafLP.gravity = Gravity.END
                grapeLP.gravity = Gravity.START
                holder.buttonLeaf.setImageResource(R.drawable.leaf_right)
                holder.imgGrape.setImageResource(R.drawable.grape_with_branch_left)
                holder.imgGrape.translationX = -300.toFloat()
            }
            holder.buttonLeaf.layoutParams = leafLP

            holder.buttonLeaf.setOnClickListener { onLeafClick(leafIndex) }

            val isGrapeRow = ((leafIndex % 5 == 1) && (leafIndex != 1))
            holder.imgGrape.visibility = if (isGrapeRow) View.VISIBLE else View.GONE
        }

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val buttonLeaf: ImageButton = view.findViewById(R.id.btnLeaf)
            val imgGrape: ImageView = view.findViewById(R.id.imgGrape)
            fun dp(v: Int): Int =
                (itemView.resources.displayMetrics.density * v + 0.5f).toInt()
        }
    }
}
