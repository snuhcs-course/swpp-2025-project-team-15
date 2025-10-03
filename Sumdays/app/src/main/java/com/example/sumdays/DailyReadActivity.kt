package com.example.sumdays

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat // API 24 호환을 위해 SimpleDateFormat 사용
import java.util.Calendar // API 24 호환을 위해 Calendar 사용
import java.util.Locale

class DailyReadActivity : AppCompatActivity() {
    // Calendar 객체로 현재 날짜를 관리하여 이전 API 레벨과 호환성을 맞춥니다.
    private lateinit var currentDate: Calendar

    // 날짜 포맷을 미리 정의해둡니다.
    // DB/Repo와 통신할 때 사용할 키 포맷 (e.g., "2025-09-27")
    private val repoKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    // 화면에 보여줄 디스플레이 포맷 (e.g., "09-27")
    private val displayFormatter = SimpleDateFormat("MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_read)

        // Intent로부터 날짜 문자열을 받습니다. (e.g., "2025-09-27")
        val dateString = intent.getStringExtra("date")

        currentDate = Calendar.getInstance() // 기본값으로 오늘 날짜를 설정
        try {
            // 넘어온 날짜 문자열이 있다면, 파싱하여 Calendar 객체에 설정합니다.
            if (dateString != null) {
                val parsedDate = repoKeyFormatter.parse(dateString)
                if (parsedDate != null) {
                    currentDate.time = parsedDate
                }
            }
        } catch (e: Exception) {
            // 날짜 파싱에 실패하면, currentDate는 이미 오늘 날짜이므로 안전합니다.
        }

        // XML의 ID를 사용하여 뷰들을 찾습니다.
        val editButton = findViewById<ImageView>(R.id.edit_button)
        val prevDayButton = findViewById<ImageView>(R.id.prev_day_button)
        val nextDayButton = findViewById<ImageView>(R.id.next_day_button)

        // 초기 화면 업데이트
        updateUI()

        // 수정 버튼 클릭 리스너
        editButton.setOnClickListener {
            // DailyWriteActivity가 있다고 가정하고 이동합니다.
            // val intent = Intent(this, DailyWriteActivity::class.java)
            // intent.putExtra("date", repoKeyFormatter.format(currentDate.time))
            // startActivity(intent)
            // finish()
        }

        // 이전 날짜 버튼 클릭 리스너
        prevDayButton.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, -1) // 날짜를 하루 전으로 변경
            updateUI() // 변경된 날짜로 화면을 새로고침
        }

        // 다음 날짜 버튼 클릭 리스너
        nextDayButton.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, 1) // 날짜를 하루 뒤로 변경
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
        val dateStringForDisplay = displayFormatter.format(currentDate.time)
        dateText.text = "< $dateStringForDisplay >"

        // 2. 일기 내용 텍스트뷰 업데이트
        val dateStringForKey = repoKeyFormatter.format(currentDate.time)
        val diary = DiaryRepository.getDiary(dateStringForKey) ?: "저장된 일기가 없습니다."
        diaryContentText.text = diary
    }
}
