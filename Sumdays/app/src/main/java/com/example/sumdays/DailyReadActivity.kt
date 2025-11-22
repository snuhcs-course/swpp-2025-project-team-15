package com.example.sumdays

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.example.sumdays.data.DailyEntry
import com.example.sumdays.data.viewModel.DailyEntryViewModel
import com.example.sumdays.daily.diary.AnalysisRepository
import com.example.sumdays.databinding.ActivityDailyReadBinding
import com.example.sumdays.utils.setupEdgeToEdge
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.recyclerview.widget.RecyclerView

class DailyReadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyReadBinding
    private lateinit var currentDate: Calendar
    private val viewModel: DailyEntryViewModel by viewModels()
    private var currentLiveData: LiveData<DailyEntry?>? = null

    private lateinit var photoGalleryAdapter: PhotoGalleryAdapter
    private val currentPhotoUris = mutableListOf<Uri>()
    private val repoKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // 연도 표시

    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyReadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigationBar()

        initializeImagePicker()

        initializeDate()
        setupPhotoGallery()
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

    private fun initializeImagePicker() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                // 1. 영구적인 읽기 권한 가져오기 (중요)
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, flag)

                addPhoto(uri)
            }
        }
    }
    private fun addPhoto(uri: Uri) {
        currentPhotoUris.add(uri)
        updatePhotoGallery() // 어댑터 갱신
        savePhotoUrls() // DB 저장
    }

    private fun updatePhotoGallery() {
        // 사진 리스트 뒤에 '추가' 버튼 아이템을 붙임 ([Photo] [Photo] ... [Add])
        val items = currentPhotoUris.map { GalleryItem.Photo(it.toString()) } + GalleryItem.Add
        photoGalleryAdapter.submitList(items)
    }

    private fun savePhotoUrls() {
        val dateKey = repoKeyFormatter.format(currentDate.time)
        val photoUrlsString = currentPhotoUris.joinToString(",") { it.toString() }
        viewModel.updateEntry(date = dateKey, photoUrls = photoUrlsString)
    }


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
        currentPhotoUris.clear()
        entry?.photoUrls?.let { urls ->
            if (urls.isNotEmpty()) {
                currentPhotoUris.addAll(urls.split(",").map { Uri.parse(it) })
            }
        }
        updatePhotoGallery() // 어댑터에 데이터 제출 (사진 + Add버튼)
        binding.photoGalleryRecyclerView.visibility = View.VISIBLE
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
    private fun setupPhotoGallery() {
        photoGalleryAdapter = PhotoGalleryAdapter(
            onPhotoClick = { photoUrl ->
                showPhotoDialog(photoUrl)
            },
            onAddClick = {
                // 추가 버튼 클릭 시 이미지 선택기 실행
                pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
        binding.photoGalleryRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DailyReadActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = photoGalleryAdapter
        }
    }

    // ★★★ 사진 확대 보기 다이얼로그 복구 ★★★
    private fun showPhotoDialog(photoUrl: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(this)
        imageView.setBackgroundColor(getColor(android.R.color.black))
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        Glide.with(this).load(photoUrl).into(imageView)

        imageView.setOnClickListener { dialog.dismiss() }
        dialog.setContentView(imageView)
        dialog.show()
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


    class PhotoGalleryAdapter(
        private val onPhotoClick: (String) -> Unit,
        private val onAddClick: () -> Unit
    ) : ListAdapter<GalleryItem, RecyclerView.ViewHolder>(GalleryDiffCallback()) {

        companion object {
            private const val VIEW_TYPE_PHOTO = 1
            private const val VIEW_TYPE_ADD = 2
        }

        override fun getItemViewType(position: Int): Int {
            return when (getItem(position)) {
                is GalleryItem.Photo -> VIEW_TYPE_PHOTO
                is GalleryItem.Add -> VIEW_TYPE_ADD
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                VIEW_TYPE_PHOTO -> {
                    val view = inflater.inflate(R.layout.item_photo_gallery, parent, false)
                    PhotoViewHolder(view)
                }
                VIEW_TYPE_ADD -> {
                    val view = inflater.inflate(R.layout.item_photo_gallery_add, parent, false)
                    AddViewHolder(view)
                }
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = getItem(position)) {
                is GalleryItem.Photo -> (holder as PhotoViewHolder).bind(item.url, onPhotoClick)
                is GalleryItem.Add -> (holder as AddViewHolder).bind(onAddClick)
            }
        }

        class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageView: ImageView = itemView.findViewById(R.id.gallery_image)
            fun bind(url: String, onClick: (String) -> Unit) {
                Glide.with(itemView.context).load(Uri.parse(url)).centerCrop().into(imageView)
                itemView.setOnClickListener { onClick(url) }
            }
        }

        class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(onClick: () -> Unit) {
                itemView.setOnClickListener { onClick() }
            }
        }
    }

    sealed class GalleryItem {
        data class Photo(val url: String) : GalleryItem()
        object Add : GalleryItem()
    }

    class GalleryDiffCallback : DiffUtil.ItemCallback<GalleryItem>() {
        override fun areItemsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean {
            return when {
                oldItem is GalleryItem.Photo && newItem is GalleryItem.Photo -> oldItem.url == newItem.url
                oldItem is GalleryItem.Add && newItem is GalleryItem.Add -> true
                else -> false
            }
        }
        override fun areContentsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean {
            return oldItem == newItem
        }
    }
}