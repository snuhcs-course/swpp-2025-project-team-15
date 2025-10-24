package com.example.sumdays

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.example.sumdays.databinding.ActivityDailyReadBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.sumdays.daily.diary.DiaryRepository
import com.example.sumdays.daily.diary.AnalysisRepository
import com.example.sumdays.daily.diary.AnalysisResponse
import android.util.Log
class DailyReadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyReadBinding
    private lateinit var currentDate: Calendar

    private val repoKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormatter = SimpleDateFormat("MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyReadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeDate()
        setupClickListeners()
        updateUI()
    }

    private fun initializeDate() {
        val dateString = intent.getStringExtra("date")
        currentDate = Calendar.getInstance()
        try {
            if (dateString != null) {
                repoKeyFormatter.parse(dateString)?.let { currentDate.time = it }
            }
        } catch (e: Exception) {
            // 파싱 실패 시 오늘 날짜로 유지
        }
    }

    private fun setupClickListeners() {
        // 이전/다음 날짜 버튼
        binding.prevDayButton.setOnClickListener { changeDate(-1) }
        binding.nextDayButton.setOnClickListener { changeDate(1) }

        // ★★★ 1번 버튼: 일기 수정 모드 진입 ★★★
        binding.editInplaceButton.setOnClickListener {
            toggleEditMode(true)
        }

        // 저장 버튼
        binding.saveButton.setOnClickListener {
            saveDiaryContent()
            toggleEditMode(false)
        }

        // ★★★ 2번 버튼: 메모 편집 화면으로 이동 ★★★
        binding.editMemosButton.setOnClickListener {
            val intent = Intent(this, DailyWriteActivity::class.java) // DailyWriteActivity로 가정
            intent.putExtra("date", repoKeyFormatter.format(currentDate.time))
            startActivity(intent)
            finish() // 현재 화면은 종료
        }
    }

    private fun changeDate(amount: Int) {
        currentDate.add(Calendar.DAY_OF_MONTH, amount)
        updateUI()
    }

    private fun updateUI() {
        binding.dateText.text = "< ${displayFormatter.format(currentDate.time)} >"
        val date = repoKeyFormatter.format(currentDate.time)
        val diary = DiaryRepository.getDiary(date)
        val analysis = AnalysisRepository.getAnalysis(date)
        val aiComment: String = analysis?.aiComment ?: ""
        binding.diaryContentEditText.setText(diary ?: "")
        binding.diaryContentTextView.setText(diary ?: "")
        binding.commentText.setText(aiComment ?: "")
        val emotionScore: Double = analysis?.analysis?.emotionScore ?: 0.0
        binding.emotionScore.setText("감정 점수: "+"$emotionScore")
        val keywords: List<String> = analysis?.analysis?.keywords ?: emptyList()
        val keywordsText = keywords.joinToString(", ")
        binding.keywords.setText("키워드: "+"$keywordsText")
        val icon : String = analysis?.icon ?: "\uD83E\uDD14"
        binding.commentIcon.setText("$icon")
    }

    private fun toggleEditMode(isEditing: Boolean) {
        if (isEditing) {
            // 1. TextView의 내용을 EditText로 복사
            binding.diaryContentEditText.setText(binding.diaryContentTextView.text)

            // 2. 뷰 전환
            binding.diaryContentTextView.visibility = View.GONE
            binding.diaryContentEditText.visibility = View.VISIBLE

            // 3. EditText에 포커스 주고 키보드 올리기
            binding.diaryContentEditText.requestFocus()
            showKeyboard(binding.diaryContentEditText)

            // 4. 버튼 전환
            binding.editInplaceButton.visibility = View.GONE
            binding.saveButton.visibility = View.VISIBLE
        } else {
            saveDiaryContent()
            // 1. EditText의 내용을 TextView로 업데이트
            binding.diaryContentTextView.text = binding.diaryContentEditText.text

            // 2. 뷰 전환
            binding.diaryContentTextView.visibility = View.VISIBLE
            binding.diaryContentEditText.visibility = View.GONE

            // 3. 키보드 내리기
            hideKeyboard(binding.diaryContentEditText)

            // 4. 버튼 전환
            binding.editInplaceButton.visibility = View.VISIBLE
            binding.saveButton.visibility = View.GONE
        }
    }
    private fun saveDiaryContent() {
        val updatedContent = binding.diaryContentEditText.text.toString()
        val dateKey = repoKeyFormatter.format(currentDate.time)
        DiaryRepository.saveDiary(dateKey, updatedContent)
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

