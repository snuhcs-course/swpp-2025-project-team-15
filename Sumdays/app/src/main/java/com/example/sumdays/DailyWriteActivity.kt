package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate

class DailyWriteActivity : AppCompatActivity() {

    private lateinit var date: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var memoAdapter: MemoAdapter
    private lateinit var dateTextView: TextView // 날짜 텍스트뷰 추가

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

        // 뷰 초기화
        dateTextView = findViewById(R.id.date_text_view)
        recyclerView = findViewById(R.id.memo_list_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 초기 데이터 설정 (onCreate에서만 호출)
        handleIntent(intent)

        // 가상의 메모 데이터 리스트
        val dummyMemoList = listOf(
            Memo("오늘은 소개원실 랩 수업을 들었다.", "21:05"),
            Memo("점심을 다이어트를 위해 굶었다.", "22:09"),
            Memo("저녁은 집 가서 먹어야지~", "23:05")
        )

        // 어댑터 생성 및 리사이클러뷰에 연결
        memoAdapter = MemoAdapter(dummyMemoList)
        recyclerView.adapter = memoAdapter

        // "일기 보기" 버튼 기능 설정
        val readDiaryButton = findViewById<Button>(R.id.read_diary_button)
        readDiaryButton.setOnClickListener {
            // DailyReadActivity로 이동
            val intent = Intent(this, DailyReadActivity::class.java)
            intent.putExtra("date", date)
            startActivity(intent)
        }

        // 네비게이션 바 기능 설정
        setupNavigationBar()
    }

    // onNewIntent() 메서드 추가
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // 새로운 인텐트를 현재 액티비티의 인텐트로 설정
        handleIntent(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleIntent(intent: Intent?) {
        date = intent?.getStringExtra("date") ?: LocalDate.now().toString()
        dateTextView.text = date
        // TODO: 받은 날짜에 따라 리사이클러뷰 데이터를 업데이트하는 로직 추가
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
            val currentLoadedDate = this.date // 여기서 this.date를 사용

            if (today != currentLoadedDate) {
                val intent = Intent(this, DailyWriteActivity::class.java)
                intent.putExtra("date", today)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
            } else {
                Toast.makeText(this, "이미 오늘 날짜입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        btnStats.setOnClickListener {
            Toast.makeText(this, "통계 화면 예정", Toast.LENGTH_SHORT).show()
        }
        btnInfo.setOnClickListener {
            Toast.makeText(this, "정보 화면 예정", Toast.LENGTH_SHORT).show()
        }
    }
}