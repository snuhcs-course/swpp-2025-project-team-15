package com.example.sumdays

// MemoActivity.kt 파일
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MemoActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var memoAdapter: MemoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memo)

        // 가상의 메모 데이터 리스트
        val dummyMemoList = listOf(
            Memo("오늘은 소개원실 랩 수업을 들었다.", "21:05"),
            Memo("점심을 다이어트를 위해 굶었다.", "22:09"),
            Memo("저녁은 집 가서 먹어야지~", "23:05")
        )

        recyclerView = findViewById(R.id.memo_list_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 어댑터 생성 및 리사이클러뷰에 연결
        memoAdapter = MemoAdapter(dummyMemoList)
        recyclerView.adapter = memoAdapter
    }
}