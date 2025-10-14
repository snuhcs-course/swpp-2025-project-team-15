// MemoMergeAdapter.kt
package com.example.sumdays.daily.memo

import android.content.ClipData
import android.os.Build
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.DragShadowBuilder
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.R

class MemoMergeAdapter(
    private val memoList: MutableList<Memo>
) : RecyclerView.Adapter<MemoMergeAdapter.VH>() {

    // 어댑터 상단 쪽에 추가
    private data class MergeRecord(
        val fromIndexBefore: Int,
        val toIndexBefore: Int,
        val fromMemo: Memo,
        val toMemoBefore: Memo
    )

    private val undoStack = ArrayDeque<MergeRecord>()


    /**
     * suppose that the timestamp is STABLE (since ID must be STABLE)
     */
    init { setHasStableIds(true) }
    override fun getItemId(position: Int): Long =
        (memoList[position].timestamp).hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memo, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val memo = memoList[position]
        holder.bind(memo)

        // 드래그 시작: 시작 index를 localState로 넘김
        holder.itemView.isLongClickable = true
        holder.itemView.setOnLongClickListener { v ->
            val fromIndex = holder.adapterPosition
            if (fromIndex == RecyclerView.NO_POSITION) return@setOnLongClickListener false

            val data = ClipData.newPlainText("memo", "drag")
            val shadow = DragShadowBuilder(v)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                v.startDragAndDrop(data, shadow, fromIndex, 0)
            } else {
                @Suppress("DEPRECATION") v.startDrag(data, shadow, fromIndex, 0)
            }
            v.alpha = 0.6f
            true // Completed the task successfully
        }

        // 타깃 리스너: STARTED에서 반드시 true 반환 (안 그러면 DROP 안 옴)
        holder.itemView.setOnDragListener { view, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENTERED -> { view.scaleX = 1.03f; view.scaleY = 1.03f; true }
                DragEvent.ACTION_DRAG_EXITED  -> { view.scaleX = 1f;    view.scaleY = 1f;    true }
                DragEvent.ACTION_DROP -> {
                    view.scaleX = 1f; view.scaleY = 1f
                    val fromIndex = event.localState as? Int ?: return@setOnDragListener false
                    val toIndex   = holder.adapterPosition
                    if (toIndex == RecyclerView.NO_POSITION) return@setOnDragListener false
                    if (fromIndex != toIndex) mergeByIndex(fromIndex, toIndex)
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> { holder.itemView.alpha = 1f; true }
                else -> false
            }
        }
    }

    override fun getItemCount(): Int = memoList.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val content: TextView = itemView.findViewById(R.id.memo_text)
        private val timestamp: TextView = itemView.findViewById(R.id.memo_time)
        fun bind(m: Memo) { content.text = m.content; timestamp.text = m.timestamp }
    }

    private fun mergeByIndex(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in memoList.indices || toIndex !in memoList.indices) return

        // 1) 되돌리기 레코드 저장 (변경 전 스냅샷)
        val record = MergeRecord(
            fromIndexBefore = fromIndex,
            toIndexBefore   = toIndex,
            fromMemo        = memoList[fromIndex],
            toMemoBefore    = memoList[toIndex]
        )

        // 2) 실제 머지 수행 (기존 네 로직)
        val from = memoList[fromIndex]
        val to   = memoList[toIndex]
        val mergedText = listOf(to.content, from.content)
            .filter { it.isNotBlank() }
            .joinToString("\n")

        // 타깃 갱신 (id가 있다면 id는 유지; 없다면 copy로도 OK)
        memoList[ toIndex ] = to.copy(content = mergedText)

        // 드래그 원본 제거 + 노티파이
        if (fromIndex < toIndex) {
            memoList.removeAt(fromIndex)
            notifyItemRemoved(fromIndex)
            notifyItemChanged(toIndex - 1)
        } else {
            memoList.removeAt(fromIndex)
            notifyItemRemoved(fromIndex)
            notifyItemChanged(toIndex)
        }

        // 3) 스택에 push (가장 마지막 머지부터 되돌리기)
        undoStack.addLast(record)

    }


    /** 마지막 머지를 되돌린다. 성공하면 true */
    fun undoLastMerge(): Boolean {
        val rec = undoStack.removeLastOrNull() ?: return false

        // 현재 리스트 상태에서 '타깃'이 있는 인덱스 계산
        // 머지 직후에는 fromIndex가 빠져서,
        // - from < to 였다면 현재 타깃 인덱스는 (to - 1)
        // - from > to 였다면 현재 타깃 인덱스는 to
        val toCurrent = if (rec.fromIndexBefore < rec.toIndexBefore)
            rec.toIndexBefore - 1
        else
            rec.toIndexBefore

        // 1) 타깃을 머지 전 내용으로 복구
        if (toCurrent in memoList.indices) {
            memoList[toCurrent] = rec.toMemoBefore
            notifyItemChanged(toCurrent)
        } else {
            // 인덱스가 틀어진 예외 상황 방지용: 안전 장치
            return false
        }

        // 2) from 메모를 원래 위치에 다시 삽입
        val insertIndex = rec.fromIndexBefore.coerceIn(0, memoList.size)
        memoList.add(insertIndex, rec.fromMemo)
        notifyItemInserted(insertIndex)

        return true
    }

}
