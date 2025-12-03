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
import com.example.sumdays.image.PhotoGalleryAdapter
import com.example.sumdays.image.GalleryItem
import com.example.sumdays.image.GalleryDiffCallback
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
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

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

        val rootView = findViewById<View>(R.id.main)
        setupEdgeToEdge(rootView)
    }

    private fun setupNavigationBar() {
        val btnCalendar = findViewById<ImageButton>(R.id.btnCalendar)
        val btnDaily =
            findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnDaily)
        val btnInfo = findViewById<ImageButton>(R.id.btnInfo)

        btnCalendar.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        btnDaily.setOnClickListener {
            val today = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            ).format(Calendar.getInstance().time)
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
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    saveImageToAppStorageAndAdd(uri)
                }
            }
    }

    private fun saveImageToAppStorageAndAdd(sourceUri: Uri) {
        try {
            val fileName = "diary_img_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
            val destinationFile = File(filesDir, fileName)
            contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(
                        outputStream
                    )
                }
            }
            addPhoto(Uri.fromFile(destinationFile))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "이미지 저장 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addPhoto(uri: Uri) {
        currentPhotoUris.add(uri)
        updatePhotoGalleryUI()
        savePhotoUrls()
    }

    private fun updatePhotoGalleryUI() {
        val items = currentPhotoUris.map { GalleryItem.Photo(it.toString()) } + GalleryItem.Add
        photoGalleryAdapter.submitList(items)
        binding.photoGalleryRecyclerView.visibility = View.VISIBLE
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
     * UI 업데이트 함수
     */
    private fun updateUI(entry: DailyEntry?) {

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
        val diaryText = entry?.diary ?: ""
        binding.diaryContentEditText.setText(diaryText)
        binding.diaryContentTextView.text = diaryText
        binding.commentIcon.text = entry?.themeIcon ?: "🤔"
        binding.keywordsText.text = entry?.keywords?.replace(";", ", ")
        binding.commentText.text = entry?.aiComment ?: ""

        val score = entry?.emotionScore ?: 0.0

        val FoxFaceResId = when {
            score >= 0.6 -> R.drawable.fox_face_level_5
            score >= 0.2 -> R.drawable.fox_face_level_4
            score >= -0.2 -> R.drawable.fox_face_level_3
            score >= -0.6 -> R.drawable.fox_face_level_2
            else -> R.drawable.fox_face_level_1
        }
        binding.foxFaceImage.setImageResource(FoxFaceResId)

        currentPhotoUris.clear()
        entry?.photoUrls?.let { urls ->
            if (urls.isNotEmpty()) {
                currentPhotoUris.addAll(urls.split(",").map { Uri.parse(it) })
            }
        }
        updatePhotoGalleryUI()
    }

    private fun initializeDate() {
        val dateString = intent.getStringExtra("date")
        currentDate = Calendar.getInstance()
        try {
            if (dateString != null) {
                repoKeyFormatter.parse(dateString)?.let { currentDate.time = it }
            }
        } catch (e: Exception) { /* ... */
        }
    }

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
            onPhotoLongClick = { position -> // ★ 롱 클릭 시 삭제 다이얼로그 호출
                showDeleteConfirmDialog(position)
            },
            onAddClick = {
                pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
        binding.photoGalleryRecyclerView.apply {
            layoutManager =
                LinearLayoutManager(this@DailyReadActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = photoGalleryAdapter
        }
    }

    private fun showDeleteConfirmDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("사진 삭제")
            .setMessage("이 사진을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { dialog, _ ->
                deletePhoto(position)
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deletePhoto(position: Int) {
        if (position in currentPhotoUris.indices) {
            val uriToDelete = currentPhotoUris[position]

            // 1. 앱 내부 저장소 파일이면 실제 파일 삭제 시도
            if (uriToDelete.scheme == "file") {
                val file = File(uriToDelete.path!!)
                if (file.exists()) {
                    file.delete()
                }
            }

            // 2. 리스트에서 제거 및 UI/DB 업데이트
            currentPhotoUris.removeAt(position)
            updatePhotoGalleryUI()
            savePhotoUrls()

            Toast.makeText(this, "사진이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }
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

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { view, selectedYear, selectedMonth, selectedDayOfMonth ->
                currentDate.set(selectedYear, selectedMonth, selectedDayOfMonth)
                observeEntry()
            }

        val datePickerDialog = DatePickerDialog(this, dateSetListener, year, month, day)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        datePickerDialog.show()
    }

    private fun changeDate(amount: Int) {
        if (amount > 0) {
            if (isAfterToday(currentDate)) {
                return
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
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val compareCal = cal.clone() as Calendar
        compareCal.set(Calendar.HOUR_OF_DAY, 0)
        compareCal.set(Calendar.MINUTE, 0)
        compareCal.set(Calendar.SECOND, 0)
        compareCal.set(Calendar.MILLISECOND, 0)

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

}