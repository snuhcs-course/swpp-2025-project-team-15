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
import androidx.activity.viewModels
import com.example.sumdays.data.viewModel.DailyEntryViewModel
import androidx.lifecycle.LiveData
import com.example.sumdays.data.DailyEntry

class DailyReadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyReadBinding
    private lateinit var currentDate: Calendar
    private val viewModel: DailyEntryViewModel by viewModels()
    private var currentLiveData: LiveData<DailyEntry?>? = null

    private val repoKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormatter = SimpleDateFormat("MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyReadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeDate()
        setupClickListeners()
        observeEntry()
    }


    private fun observeEntry() {
        val dateKey = repoKeyFormatter.format(currentDate.time)

        // ✅ 기존 옵저버 해제
        currentLiveData?.removeObservers(this)

        // ✅ 새로운 LiveData 구독 시작
        currentLiveData = viewModel.getEntry(dateKey)
        currentLiveData?.observe(this) { entry ->
            updateUI(entry)
        }
    }

    private fun updateUI(entry: DailyEntry?) {
        binding.dateText.text = "< ${displayFormatter.format(currentDate.time)} >"
        binding.diaryContentEditText.setText(entry?.diary ?: "")
        binding.diaryContentTextView.text = entry?.diary ?: ""
        binding.commentText.text = entry?.aiComment ?: ""
        binding.emotionScore.text = "감정 점수: ${entry?.emotionScore ?: 0.0}"
        binding.keywords.text = "키워드: ${entry?.keywords ?: ""}"
        binding.commentIcon.text = entry?.emotionIcon ?: "🤔"
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
        observeEntry()
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
            // 1. EditText의 내용을 TextView로 업데이트
            binding.diaryContentTextView.text = binding.diaryContentEditText.text
            saveDiaryContent()

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
        viewModel.updateEntry(date = dateKey, diary = updatedContent)
        // viewModel.updateEntry(date = dateKey, diary = updatedContent, themeIcon = "#") // test용
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

