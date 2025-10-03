package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate

class SumActivity : AppCompatActivity() {

    private lateinit var date: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var memoAdapter: MemoAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sum)

        // DailyWriteActivity에서 넘어온 날짜 받기
        date = intent.getStringExtra("date") ?: "알 수 없는 날짜"

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

        // 가상의 메모 데이터 리스트 (테스트용)
        val dummyMemoList = listOf(
            Memo("오늘은 소개원실 랩 수업을 들었다.", "21:05"),
            Memo("점심을 다이어트를 위해 굶었다.", "22:09"),
            Memo("저녁은 집 가서 먹어야지~", "23:05")
        )

        recyclerView = findViewById(R.id.memo_list_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        memoAdapter = MemoAdapter(dummyMemoList)
        recyclerView.adapter = memoAdapter

        setupNavigationBar()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupNavigationBar() {
        val btnCalendar = findViewById<android.widget.Button>(R.id.btnCalendar)
        val btnDaily = findViewById<android.widget.Button>(R.id.btnDaily)
        val btnStats = findViewById<android.widget.Button>(R.id.btnStats)
        val btnInfo = findViewById<android.widget.Button>(R.id.btnInfo)

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
        btnStats.setOnClickListener {
            android.widget.Toast.makeText(this, "통계 화면 예정", android.widget.Toast.LENGTH_SHORT).show()
        }
        btnInfo.setOnClickListener {
            android.widget.Toast.makeText(this, "정보 화면 예정", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}