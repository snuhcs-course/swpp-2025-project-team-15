package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.daily.memo.Memo
import com.example.sumdays.daily.memo.MemoAdapter
import java.time.LocalDate

// 일기 작성 화면을 담당하는 액티비티
class DailyWriteActivity : AppCompatActivity() {

    // 오늘 날짜를 저장하는 변수
    private lateinit var date: String
    // 메모 목록을 표시하는 어댑터
    private lateinit var memoAdapter: MemoAdapter

    // UI 뷰 변수들
    private lateinit var dateTextView: TextView
    private lateinit var memoListView: RecyclerView

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
        initViews()

        // 다른 화면에서 전달된 날짜를 처리
        handleIntent(intent)

        // 가상의 메모 데이터를 설정
        setupDummyData()

        // 버튼 클릭 리스너 설정
        setupClickListeners()

        // 하단 네비게이션 바 설정
        setupNavigationBar()
    }

    // 액티비티가 재사용될 때 새로운 인텐트 처리
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    // UI 뷰 변수들을 레이아웃 ID와 연결
    private fun initViews() {
        dateTextView = findViewById(R.id.date_text_view)
        memoListView = findViewById(R.id.memo_list_view)
        memoListView.layoutManager = LinearLayoutManager(this)
    }

    // 인텐트에서 날짜 데이터를 가져와 화면에 표시
    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleIntent(intent: Intent?) {
        date = intent?.getStringExtra("date") ?: LocalDate.now().toString()
        dateTextView.text = date
        // TODO: 받은 날짜에 따라 리사이클러뷰 데이터를 업데이트하는 로직 추가
    }

    // 더미 데이터를 생성하고 어댑터를 리사이클러뷰에 연결
    private fun setupDummyData() {
        val dummyMemoList = listOf(
            Memo("오늘은 소개원실 랩 수업을 들었다.", "21:05"),
            Memo("점심을 다이어트를 위해 굶었다.", "22:09"),
            Memo("저녁은 집 가서 먹어야지~", "23:05")
        )

        memoAdapter = MemoAdapter(dummyMemoList)
        memoListView.adapter = memoAdapter
    }

    // "일기 보기" 버튼 클릭 시 다른 화면으로 이동
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupClickListeners() {
        val readDiaryButton: Button = findViewById(R.id.read_diary_button)
        readDiaryButton.setOnClickListener {
            val intent = Intent(this, DailyReadActivity::class.java)
            intent.putExtra("date", date)
            startActivity(intent)
        }
    }

    // 하단 내비게이션 바의 버튼들 클릭 이벤트 처리
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupNavigationBar() {
        val btnCalendar: ImageButton = findViewById(R.id.btnCalendar)
        val btnDaily: ImageButton = findViewById(R.id.btnDaily)
        val btnInfo: ImageButton = findViewById(R.id.btnInfo)
        val btnSum: ImageButton = findViewById(R.id.btnSum)

        btnSum.setOnClickListener {
            val intent = Intent(this, DailySumActivity::class.java)
            intent.putExtra("date", date)
            startActivity(intent)
        }

        btnCalendar.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }

        btnDaily.setOnClickListener {
            val today = LocalDate.now().toString()
            val currentLoadedDate = this.date

            if (today != currentLoadedDate) {
                val intent = Intent(this, DailyWriteActivity::class.java)
                intent.putExtra("date", today)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
            } else {
                Toast.makeText(this, "이미 오늘 날짜입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        btnInfo.setOnClickListener {
            Toast.makeText(this, "정보 화면 예정", Toast.LENGTH_SHORT).show()
        }
    }
}