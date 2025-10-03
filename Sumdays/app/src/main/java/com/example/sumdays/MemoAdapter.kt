package com.example.sumdays

// MemoAdapter.kt 파일
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MemoAdapter(private val memoList: List<Memo>) :
    RecyclerView.Adapter<MemoAdapter.MemoViewHolder>() {

    // 뷰 홀더(ViewHolder) 클래스 정의: 아이템 뷰의 각 요소를 담는 역할
    class MemoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timestamp: TextView = view.findViewById(R.id.memo_timestamp)
        val content: TextView = view.findViewById(R.id.memo_content)
    }

    // 뷰 홀더를 생성하는 함수 (아이템 레이아웃을 인플레이트)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memo, parent, false)
        return MemoViewHolder(view)
    }

    // 데이터와 뷰를 연결하는 함수 (아이템에 데이터 설정)
    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        val memo = memoList[position]
        holder.timestamp.text = memo.timestamp
        holder.content.text = memo.content
    }

    // 전체 아이템 개수를 반환하는 함수
    override fun getItemCount(): Int {
        return memoList.size
    }
}