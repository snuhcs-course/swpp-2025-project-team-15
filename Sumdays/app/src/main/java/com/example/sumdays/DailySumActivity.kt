package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.daily.memo.Memo
import com.example.sumdays.daily.memo.MemoMergeAdapter
import java.time.LocalDate

class DailySumActivity : AppCompatActivity() {

    private lateinit var date: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var memoMergeAdapter: MemoMergeAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sum)

        date = intent.getStringExtra("date") ?: "알 수 없는 날짜"
        findViewById<TextView>(R.id.date_text_view).text = date

        findViewById<ImageView>(R.id.back_icon).setOnClickListener {
            startActivity(Intent(this, DailyWriteActivity::class.java).putExtra("date", date))
            finish()
        }
        findViewById<ImageView>(R.id.skip_icon).setOnClickListener {
            startActivity(Intent(this, DailyReadActivity::class.java).putExtra("date", date))
            finish()
        }
        findViewById<ImageButton>(R.id.undo_button).setOnClickListener {
            memoMergeAdapter.undoLastMerge()
        }

        // ✅ MutableList 로 변경
        val dummyMemoList = mutableListOf(
            Memo("오늘은 소개원실 랩 수업을 들었다.", "21:05"),
            Memo("점심을 다이어트를 위해 굶었다.", "22:09"),
            Memo("저녁은 집 가서 먹어야지~", "23:05"),
        )

        recyclerView = findViewById(R.id.memo_list_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // ✅ 드래그-머지 지원 어댑터로 교체
        memoMergeAdapter = MemoMergeAdapter(dummyMemoList)
        recyclerView.adapter = memoMergeAdapter

        setupNavigationBar()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupNavigationBar() {
        val btnCalendar = findViewById<ImageButton>(R.id.btnCalendar)
        val btnDaily = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnDaily)
        val btnInfo = findViewById<ImageButton>(R.id.btnInfo)

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
            android.widget.Toast.makeText(this, "정보 화면 예정", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}