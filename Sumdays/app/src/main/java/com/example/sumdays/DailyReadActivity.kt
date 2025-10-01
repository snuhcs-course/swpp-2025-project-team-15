package com.example.sumdays

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DailyReadActivity : AppCompatActivity() {
    private lateinit var date: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_write)

        // WriteActivity에서 넘어온 날짜 받기
        date = intent.getStringExtra("date") ?: "알 수 없는 날짜"

        val textDiary = findViewById<TextView>(R.id.textDiary)
        val btnEdit = findViewById<Button>(R.id.btnEdit)

        // 저장된 일기 불러오기
        val diary = DiaryRepository.getDiary(date) ?: "저장된 일기가 없습니다."
        textDiary.text = diary

        // 수정 버튼 → 다시 작성 화면으로 이동
        btnEdit.setOnClickListener {
            val intent = Intent(this, DailyWriteActivity::class.java)
            intent.putExtra("date", date)
            startActivity(intent)
            finish()
        }
    }
}
