package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.daily.memo.Memo
import com.example.sumdays.daily.memo.MemoAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.LocalDate

class DailySumActivity : AppCompatActivity() {

    private lateinit var date: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var memoAdapter: MemoAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sum)

        // 다른 화면에서 넘어온 날짜를 받아오거나 오늘 날짜로 설정
        date = intent.getStringExtra("date") ?: LocalDate.now().toString()

        // 날짜 TextView 업데이트
        val dateTextView = findViewById<TextView>(R.id.date_text_view)
        dateTextView.text = date

        // 뒤로가기 및 건너뛰기 아이콘 리스너 설정
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val skipIcon = findViewById<ImageView>(R.id.skip_icon)

        backIcon.setOnClickListener {
            // DailyWriteActivity로 돌아가기 (날짜 데이터 포함)
            val intent = Intent(this, DailyWriteActivity::class.java)
            intent.putExtra("date", date)
            startActivity(intent)
            finish() // 현재 화면 종료
        }

        skipIcon.setOnClickListener {
            // DailyReadActivity로 이동 (날짜 데이터 포함)
            val intent = Intent(this, DailyReadActivity::class.java)
            intent.putExtra("date", date)
            startActivity(intent)
            finish() // 현재 화면 종료
        }

        // Room과 연결되지 않았으므로 더미 데이터로 임시 설정
        val dummyMemoList = listOf(
            Memo(0, "오늘은 소개원실 랩 수업을 들었다.", "21:05", "2025-10-12", 0),
            Memo(1, "점심을 다이어트를 위해 굶었다.", "22:09",  "2025-10-12", 1),
            Memo(2, "저녁은 집 가서 먹어야지~", "23:05", "2025-10-13", 0)
        )

        recyclerView = findViewById(R.id.memo_list_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // ListAdapter로 변경된 MemoAdapter에 맞게 수정
        memoAdapter = MemoAdapter()
        recyclerView.adapter = memoAdapter
        memoAdapter.submitList(dummyMemoList) // 임시 데이터 제출

        // 네비게이션 바 기능 설정
        setupNavigationBar()
    }

    // 하단 네비게이션 바의 버튼들 클릭 이벤트 처리
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupNavigationBar() {
        val btnCalendar: ImageButton = findViewById(R.id.btnCalendar)
        val btnDaily: FloatingActionButton = findViewById(R.id.btnDaily) // 타입을 FloatingActionButton으로 변경
        val btnInfo: ImageButton = findViewById(R.id.btnInfo)

        btnCalendar.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }
        btnDaily.setOnClickListener {
            val today = LocalDate.now().toString()
            val intent = Intent(this, DailyWriteActivity::class.java)
            intent.putExtra("date", today)
            startActivity(intent)
        }
        btnInfo.setOnClickListener {
            Toast.makeText(this, "정보 화면 예정", Toast.LENGTH_SHORT).show()
        }
    }
}