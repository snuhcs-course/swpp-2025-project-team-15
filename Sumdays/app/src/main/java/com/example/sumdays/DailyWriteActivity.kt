package com.example.sumdays

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class DailyWriteActivity : AppCompatActivity() {
    private lateinit var date: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_read)

        // CalendarActivity에서 넘어온 날짜 받기
        date = intent.getStringExtra("date") ?: "알 수 없는 날짜"

        val editDiary = findViewById<EditText>(R.id.editDiary)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // 저장 버튼 클릭
        btnSave.setOnClickListener {
            val text = editDiary.text.toString()
            DiaryRepository.saveDiary(date, text)

            // 저장 후 보기 화면으로 이동
            val intent = Intent(this, DailyReadActivity::class.java)
            intent.putExtra("date", date)
            startActivity(intent)
            finish() // 작성 화면 닫기
        }
    }
}
