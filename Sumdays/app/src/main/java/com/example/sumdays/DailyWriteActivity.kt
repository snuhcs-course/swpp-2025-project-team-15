package com.example.sumdays

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DailyWriteActivity : AppCompatActivity() {
    private lateinit var date: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var memoAdapter: MemoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_write)

        // CalendarActivity에서 넘어온 날짜 받기
        date = intent.getStringExtra("date") ?: "알 수 없는 날짜"

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
