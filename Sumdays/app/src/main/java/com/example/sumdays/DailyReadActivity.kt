package com.example.sumdays

import android.app.DatePickerDialog
// import android.app.Dialog // 삭제
import android.content.Context
import android.content.Intent
// import android.net.Uri // 삭제
import android.os.Bundle
// import android.view.LayoutInflater // 삭제
import android.view.View
// import android.view.ViewGroup // 삭제
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
// ActivityResultLauncher 관련 import 모두 삭제
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
// RecyclerView/ListAdapter/DiffUtil 관련 import 모두 삭제
import com.example.sumdays.data.DailyEntry
import com.example.sumdays.data.viewModel.DailyEntryViewModel
import com.example.sumdays.daily.diary.AnalysisRepository
import com.example.sumdays.databinding.ActivityDailyReadBinding
import com.example.sumdays.utils.setupEdgeToEdge
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
// import java.time.LocalDate // (LocalDate 사용 부분 삭제됨)
import java.util.Calendar
import java.util.Locale

class DailyReadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyReadBinding
    private lateinit var currentDate: Calendar
    private val viewModel: DailyEntryViewModel by viewModels()
    private var currentLiveData: LiveData<DailyEntry?>? = null

    // private lateinit var photoGalleryAdapter: PhotoGalleryAdapter // 삭제

    private val repoKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // 연도 표시

    // private lateinit var pickImageLauncher: ... // 삭제

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyReadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigationBar()

        // initializeImagePicker() // 삭제

        initializeDate()
        // setupPhotoGallery() // 삭제
        setupClickListeners()
        observeEntry() // currentDate가 초기화된 후 호출

        // 상태바, 네비게이션 같은 색
        val rootView = findViewById<View>(R.id.main)
        setupEdgeToEdge(rootView)
    }

    private fun setupNavigationBar() {
        val btnCalendar = findViewById<ImageButton>(R.id.btnCalendar)
        val btnDaily = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnDaily)
        val btnInfo = findViewById<ImageButton>(R.id.btnInfo)

        btnCalendar.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        btnDaily.setOnClickListener {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
            val intent = Intent(this, DailyWriteActivity::class.java)
            intent.putExtra("date", today)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        btnInfo.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    // private fun initializeImagePicker() { ... } // 삭제


    private fun observeEntry() {
        val dateKey = repoKeyFormatter.format(currentDate.time)
        currentLiveData?.removeObservers(this)
        currentLiveData = viewModel.getEntry(dateKey)
        currentLiveData?.observe(this) { entry ->
            updateUI(entry)
        }
    }

    /**
     * UI 업데이트 함수 (수정됨)
     */
    private fun updateUI(entry: DailyEntry?) {

        // "오늘" 날짜 표시 로직
        val isToday = isSameDay(currentDate, Calendar.getInstance())

        if (isToday) {
            binding.dateText.text = "오늘"
            binding.nextDayButton.visibility = View.INVISIBLE
            binding.nextDayButton.isEnabled = false
        } else {
            binding.dateText.text = "${displayFormatter.format(currentDate.time)}"
            binding.nextDayButton.visibility = View.VISIBLE
            binding.nextDayButton.isEnabled = true
        }

        binding.diaryContentEditText.setText(entry?.diary ?: "")
        binding.diaryContentTextView.text = entry?.diary ?: ""
        binding.commentIcon.text = entry?.themeIcon ?: "🤔"
        binding.keywordsText.text = entry?.keywords ?: ""
        binding.commentText.text = entry?.aiComment ?: "코멘트가 없습니다."

        // 감정 점수 로직 수정 (유지)
        val score = entry?.emotionScore ?: 0.0 // 점수 가져오기 (기본값 0.0)

        // 1. 온도계 아이콘 설정
        val thermometerResId = when {
            score > 0.5 -> R.drawable.ic_thermometer_high       // ( 0.5 ~  1.0] : 빨간색
            score > 0.0 -> R.drawable.ic_thermometer_medium     // ( 0.0 ~  0.5] : 주황색
            score > -0.5 -> R.drawable.ic_thermometer_low       // (-0.5 ~  0.0] : 하늘색
            else -> R.drawable.ic_thermometer_very_low          // [-1.0 ~ -0.5] : 파란색
        }
        binding.thermometerIcon.setImageResource(thermometerResId)

        // 2. 온도 텍스트 설정 (score * 100)
        val temperature = score * 100
        binding.emotionScore.text = String.format(Locale.getDefault(), "감정 온도 %.0f°C", temperature)
        binding.emotionScore.visibility = View.VISIBLE // GONE이었던 것을 보이도록

        // 사진첩 관련 로직 모두 삭제
    }

    private fun initializeDate() {
        val dateString = intent.getStringExtra("date")
        currentDate = Calendar.getInstance()
        try {
            if (dateString != null) {
                repoKeyFormatter.parse(dateString)?.let { currentDate.time = it }
            }
        } catch (e: Exception) { /* ... */ }
    }

    // private fun setupPhotoGallery() { ... } // 삭제

    // private fun showPhotoDialog(photoUrl: String) { ... } // 삭제

    private fun setupClickListeners() {
        binding.dateText.setOnClickListener {
            showDatePickerDialog()
        }
        binding.prevDayButton.setOnClickListener { changeDate(-1) }
        binding.nextDayButton.setOnClickListener { changeDate(1) }
        binding.editInplaceButton.setOnClickListener { toggleEditMode(true) }
        binding.saveButton.setOnClickListener {
            val updatedContent = binding.diaryContentEditText.text.toString()
            showReanalysisDialog(updatedContent)
        }
        binding.editMemosButton.setOnClickListener {
            val intent = Intent(this, DailyWriteActivity::class.java)
            intent.putExtra("date", repoKeyFormatter.format(currentDate.time))
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }
    }
    private fun showDatePickerDialog() {
        val year = currentDate.get(Calendar.YEAR)
        val month = currentDate.get(Calendar.MONTH)
        val day = currentDate.get(Calendar.DAY_OF_MONTH)

        val dateSetListener = DatePickerDialog.OnDateSetListener { view, selectedYear, selectedMonth, selectedDayOfMonth ->
            currentDate.set(selectedYear, selectedMonth, selectedDayOfMonth)
            observeEntry()
        }

        val datePickerDialog = DatePickerDialog(this, dateSetListener, year, month, day)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        datePickerDialog.show()
    }

    private fun changeDate(amount: Int) {
        if (amount > 0) { // "다음 날" 버튼을 눌렀을 때
            if (isAfterToday(currentDate)) {
                return // 이미 오늘이거나 미래면 아무것도 하지 않음
            }
        }
        currentDate.add(Calendar.DAY_OF_MONTH, amount)
        observeEntry()
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    private fun isAfterToday(cal: Calendar): Boolean {
        val today = Calendar.getInstance()
        // 년, 월, 일만 비교하기 위해 시간 초기화
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val compareCal = cal.clone() as Calendar
        compareCal.set(Calendar.HOUR_OF_DAY, 0)
        compareCal.set(Calendar.MINUTE, 0)
        compareCal.set(Calendar.SECOND, 0)
        compareCal.set(Calendar.MILLISECOND, 0)

        // 오늘과 같거나(equals) 오늘보다 뒤(after)인지 확인
        return compareCal.equals(today) || compareCal.after(today)
    }

    private fun toggleEditMode(isEditing: Boolean) {
        if (isEditing) {
            binding.diaryContentEditText.setText(binding.diaryContentTextView.text)
            binding.diaryContentTextView.visibility = View.GONE
            binding.diaryContentEditText.visibility = View.VISIBLE
            binding.diaryContentEditText.requestFocus()
            showKeyboard(binding.diaryContentEditText)
            binding.editInplaceButton.visibility = View.GONE
            binding.saveButton.visibility = View.VISIBLE
        } else {
            binding.diaryContentTextView.text = binding.diaryContentEditText.text
            binding.diaryContentTextView.visibility = View.VISIBLE
            binding.diaryContentEditText.visibility = View.GONE
            hideKeyboard(binding.diaryContentEditText)
            binding.editInplaceButton.visibility = View.VISIBLE
            binding.saveButton.visibility = View.GONE
        }
    }

    private fun showReanalysisDialog(updatedContent: String) {
        val dateKey = repoKeyFormatter.format(currentDate.time)
        AlertDialog.Builder(this)
            .setTitle("AI 재분석")
            .setMessage("일기 내용을 수정했습니다. AI 코멘트와 분석 결과도 새로고침할까요?")
            .setPositiveButton("예 (새로 분석)") { dialog, _ ->
                lifecycleScope.launch {
                    AnalysisRepository.requestAnalysis(dateKey, updatedContent, viewModel)
                }
                toggleEditMode(false)
                dialog.dismiss()
            }
            .setNegativeButton("아니오 (텍스트만 저장)") { dialog, _ ->
                saveDiaryContent(updatedContent)
                toggleEditMode(false)
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun saveDiaryContent(updatedContent: String) {
        val dateKey = repoKeyFormatter.format(currentDate.time)
        viewModel.updateEntry(date = dateKey, diary = updatedContent)
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }


    // 사진첩 관련 Adapter, Sealed Class, DiffCallback 모두 삭제
}