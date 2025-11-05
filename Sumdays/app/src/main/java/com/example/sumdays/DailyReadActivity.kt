package com.example.sumdays

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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.data.DailyEntry
import com.example.sumdays.data.viewModel.DailyEntryViewModel
import com.example.sumdays.daily.diary.AnalysisRepository
import com.example.sumdays.databinding.ActivityDailyReadBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

class DailyReadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyReadBinding
    private lateinit var currentDate: Calendar
    private val viewModel: DailyEntryViewModel by viewModels()
    private var currentLiveData: LiveData<DailyEntry?>? = null

    private lateinit var photoGalleryAdapter: PhotoGalleryAdapter

    private val repoKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormatter = SimpleDateFormat("MM-dd", Locale.getDefault())

    // (주석 처리됨)
    // private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyReadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigationBar()
        // (주석 처리됨)
        // initializeImagePicker()

        // ★★★ 변경점: initializeDate()를 observeEntry()보다 먼저 호출 ★★★
        initializeDate()
        setupPhotoGallery()
        setupClickListeners()
        observeEntry() // currentDate가 초기화된 후 호출
    }

    /* (주석 처리됨)
    private fun initializeImagePicker() {
        // ...
    }
    */

    private fun setupNavigationBar() {
        val btnCalendar = findViewById<ImageButton>(R.id.btnCalendar)
        val btnDaily = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnDaily)
        val btnInfo = findViewById<ImageButton>(R.id.btnInfo)

        btnCalendar.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }
        btnDaily.setOnClickListener {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
            val intent = Intent(this, DailyWriteActivity::class.java)
            intent.putExtra("date", today)
            startActivity(intent)
        }
        btnInfo.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
    private fun observeEntry() {
        // ★★★ 이제 currentDate가 초기화되었으므로 이 줄은 안전함 ★★★
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
        binding.dateText.text = "< ${displayFormatter.format(currentDate.time)} >"
        binding.diaryContentEditText.setText(entry?.diary ?: "")
        binding.diaryContentTextView.text = entry?.diary ?: ""
        binding.commentIcon.text = entry?.emotionIcon ?: "🤔"
        binding.keywordsText.text = entry?.keywords ?: ""

        binding.commentText.text = entry?.aiComment ?: "코멘트가 없습니다."
        val score = entry?.emotionScore ?: 0.0 // 점수 가져오기 (기본값 0.0)
        val thermometerResId = when {
            score > 0.5 -> R.drawable.ic_thermometer_high       // ( 0.5 ~  1.0] : 빨간색
            score > 0.0 -> R.drawable.ic_thermometer_medium     // ( 0.0 ~  0.5] : 주황색
            score > -0.5 -> R.drawable.ic_thermometer_low       // (-0.5 ~  0.0] : 하늘색
            else -> R.drawable.ic_thermometer_very_low          // [-1.0 ~ -0.5] : 파란색
        }
        binding.thermometerIcon.setImageResource(thermometerResId)

        // (주석 처리됨)
        // val photos = entry?.photoUris ?: emptyList()

        // --- (임시 테스트용 코드) ---
        val tempPhotos = listOf(
            "https://placehold.co/100x100/E26A2C/white?text=Img1",
            "https://placehold.co/100x100/3F51B5/white?text=Img2",
            "https://placehold.co/100x100/4CAF50/white?text=Img3"
        )
        // --- 임시 코드 끝 ---

        val photoItems = tempPhotos.map { GalleryItem.Photo(it) }
        val galleryItems = photoItems // 'Add' 버튼 없이 표시

        photoGalleryAdapter.submitList(galleryItems)
        if (galleryItems.isNotEmpty()) {
            binding.photoGalleryRecyclerView.visibility = View.VISIBLE
        } else {
            binding.photoGalleryRecyclerView.visibility = View.GONE
        }
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

    /**
     * 사진첩 RecyclerView 설정 함수 (수정됨)
     */
    private fun setupPhotoGallery() {
        photoGalleryAdapter = PhotoGalleryAdapter(
            onPhotoClick = { photoUrl ->
                showPhotoDialog(photoUrl)
            },
            onAddClick = {
                Toast.makeText(this, "사진 추가 기능은 현재 비활성화되어 있습니다.", Toast.LENGTH_SHORT).show()
            }
        )
        binding.photoGalleryRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DailyReadActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = photoGalleryAdapter
        }
    }

    private fun showPhotoDialog(photoUrl: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(this)
        imageView.setBackgroundColor(getColor(android.R.color.black))
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        imageView.setOnClickListener {
            Toast.makeText(this, "Clicked: $photoUrl", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.setContentView(imageView)
        dialog.show()
    }


    private fun setupClickListeners() {
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
            finish()
        }
    }

    private fun changeDate(amount: Int) {
        currentDate.add(Calendar.DAY_OF_MONTH, amount)
        observeEntry()
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


    /**
     * 사진첩 RecyclerView를 위한 어댑터
     */
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
                is GalleryItem.Photo -> (holder as PhotoViewHolder).bind(item.url, onPhotoClick, position)
                is GalleryItem.Add -> (holder as AddViewHolder).bind(onAddClick)
            }
        }

        class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageView: ImageView = itemView.findViewById(R.id.gallery_image)
            fun bind(url: String, onClick: (String) -> Unit, position: Int) {
                val colors = listOf(0xFFE26A2C.toInt(), 0xFF3F51B5.toInt(), 0xFF4CAF50.toInt())
                imageView.setBackgroundColor(colors[position % colors.size])
                itemView.setOnClickListener { onClick(url) }
            }
        }

        class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(onClick: () -> Unit) {
                itemView.setOnClickListener { onClick() }
            }
        }
    }

    /**
     * RecyclerView 아이템을 위한 Sealed Class
     */
    sealed class GalleryItem {
        data class Photo(val url: String) : GalleryItem()
        object Add : GalleryItem()
    }

    /**
     * ListAdapter를 위한 DiffUtil Callback
     */
    class GalleryDiffCallback : DiffUtil.ItemCallback<GalleryItem>() {
        override fun areItemsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean {
            return when {
                oldItem is GalleryItem.Photo && newItem is GalleryItem.Photo -> oldItem.url == newItem.url
                // ★★★ 오타 수정: GalleryM -> GalleryItem ★★★
                oldItem is GalleryItem.Add && newItem is GalleryItem.Add -> true
                else -> false
            }
        }
        override fun areContentsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean {
            return oldItem == newItem
        }
    }
}