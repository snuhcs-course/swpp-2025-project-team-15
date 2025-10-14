package com.example.sumdays.daily.memo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.R

// Room 데이터 변경을 효율적으로 처리하는 ListAdapter로 변경
class MemoAdapter : ListAdapter<Memo, MemoAdapter.MemoViewHolder>(MemoDiffCallback()) {

    // 뷰 홀더(ViewHolder) 클래스 정의
    class MemoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timestamp: TextView = view.findViewById(R.id.memo_time)
        val content: TextView = view.findViewById(R.id.memo_text)
    }

    // 뷰 홀더를 생성하는 함수
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memo, parent, false)
        return MemoViewHolder(view)
    }

    interface OnItemClickListener {
        fun onItemClick(memo: Memo)
    }

    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    // 데이터와 뷰를 연결하는 함수
    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        val currentMemo = getItem(position)
        holder.timestamp.text = currentMemo.timestamp
        holder.content.text = currentMemo.content

        // 아이템 클릭 리스너 설정
        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(currentMemo)
        }
    }

    // 아이템 위치를 직접 변경하는 함수
    fun moveItem(fromPosition: Int, toPosition: Int) {
        val newList = currentList.toMutableList()
        val movedItem = newList.removeAt(fromPosition)
        newList.add(toPosition, movedItem)
        submitList(newList)
    }
}

// 두 리스트의 차이를 계산하여 효율적으로 업데이트
class MemoDiffCallback : DiffUtil.ItemCallback<Memo>() {
    override fun areItemsTheSame(oldItem: Memo, newItem: Memo): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Memo, newItem: Memo): Boolean {
        return oldItem == newItem
    }
}