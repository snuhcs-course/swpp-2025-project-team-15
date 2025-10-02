package com.example.sumdays

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView // ★★★ 변경점: Button -> ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.sumdays.DiaryRepository
import android.util.Log;
import java.time.LocalDate // 날짜 처리를 위한 java.time 라이브러리 (API 26+ 권장)
import java.time.format.DateTimeFormatter
class DailyReadActivity : AppCompatActivity() {
    // String이 아닌 LocalDate 객체로 현재 날짜를 관리하여 날짜 계산을 용이하게 합니다.
    private lateinit var currentDate: LocalDate
    // 날짜 포맷을 미리 정의해둡니다.
    // DB/Repo와 통신할 때 사용할 키 포맷 (e.g., "2025-09-27")
    private val repoKeyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    // 화면에 보여줄 디스플레이 포맷 (e.g., "09-27")
    private val displayFormatter = DateTimeFormatter.ofPattern("MM-dd")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_read)

        // WriteActivity에서 넘어온 날짜 받기
        val dateString = intent.getStringExtra("date")
        currentDate = try {
            LocalDate.parse(dateString, repoKeyFormatter)
        } catch (e: Exception) {
            LocalDate.now() // 기본값: 오늘 날짜
        }

        // XML의 ID를 사용하여 뷰들을 찾습니다.
        val editButton = findViewById<ImageView>(R.id.edit_button)
        val prevDayButton = findViewById<ImageView>(R.id.prev_day_button)
        val nextDayButton = findViewById<ImageView>(R.id.next_day_button)
        updateUI()
        // 수정 버튼(ImageView)에 클릭 리스너 설정
        editButton.setOnClickListener {
            // DailyWriteActivity가 있다고 가정하고 이동합니다.
            val intent = Intent(this, DailyWriteActivity::class.java)
            intent.putExtra("date", currentDate.toString())
            startActivity(intent)
            finish()
        }
        prevDayButton.setOnClickListener {
            currentDate = currentDate.minusDays(1) // 날짜를 하루 전으로 변경
            updateUI() // 변경된 날짜로 화면을 새로고침
        }

        // 다음 날짜 버튼 클릭 리스너
        nextDayButton.setOnClickListener {
            currentDate = currentDate.plusDays(1) // 날짜를 하루 뒤로 변경
            updateUI() // 변경된 날짜로 화면을 새로고침
        }
    }
    /**
     * 현재 'currentDate' 값을 기준으로 화면의 모든 뷰를 업데이트하는 함수
     */
    private fun updateUI() {
        val dateText = findViewById<TextView>(R.id.date_text)
        val diaryContentText = findViewById<TextView>(R.id.diary_content_text)

        // 1. 날짜 텍스트뷰 업데이트
        val dateStringForDisplay = currentDate.format(displayFormatter)
        dateText.text = "< $dateStringForDisplay >"

        // 2. 일기 내용 텍스트뷰 업데이트
        val dateStringForKey = currentDate.format(repoKeyFormatter)
        val diary = DiaryRepository.getDiary(dateStringForKey) ?: "저장된 일기가 없습니다."
        diaryContentText.text = diary
    }
}
