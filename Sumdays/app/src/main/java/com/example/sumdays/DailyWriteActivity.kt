package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyWriteActivity : AppCompatActivity() {

    private lateinit var date: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var memoAdapter: MemoAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_daily_write)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.write)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        // 네비게이션 바 기능 설정
        setupNavigationBar()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupNavigationBar() {
        val btnCalendar = findViewById<android.widget.Button>(R.id.btnCalendar)
        val btnDaily = findViewById<android.widget.Button>(R.id.btnDaily)
        val btnStats = findViewById<android.widget.Button>(R.id.btnStats)
        val btnInfo = findViewById<android.widget.Button>(R.id.btnInfo)

        btnCalendar.setOnClickListener {
            // CalendarActivity로 이동
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }

        btnDaily.setOnClickListener {
            val today = LocalDate.now().toString()
            val currentLoadedDate = intent.getStringExtra("date")

            // 현재 날짜와 화면에 로드된 날짜가 다를 경우에만 화면을 새로 로드
            if (today != currentLoadedDate) {
                val intent = Intent(this, DailyWriteActivity::class.java)
                intent.putExtra("date", today)
                // 새로운 액티비티를 열 때 기존 스택을 제거하여 중복 생성 방지
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
            } else {
                // 이미 오늘 날짜이므로 아무것도 하지 않음
                Toast.makeText(this, "이미 오늘 날짜입니다.", Toast.LENGTH_SHORT).show()
            }
        }
        btnStats.setOnClickListener {
            android.widget.Toast.makeText(this, "통계 화면 예정", android.widget.Toast.LENGTH_SHORT).show()
        }
        btnInfo.setOnClickListener {
            android.widget.Toast.makeText(this, "정보 화면 예정", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}