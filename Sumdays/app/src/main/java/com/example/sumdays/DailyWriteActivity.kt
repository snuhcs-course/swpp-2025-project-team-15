package com.example.sumdays

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.daily.memo.Memo
import com.example.sumdays.daily.memo.MemoAdapter
import com.example.sumdays.daily.memo.MemoViewModel
import com.example.sumdays.daily.memo.MemoViewModelFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.sumdays.daily.memo.MemoDragAndDropCallback
import com.example.sumdays.audio.AudioRecorderHelper
import com.example.sumdays.image.ImageOcrHelper
import android.util.Log
import androidx.core.content.ContextCompat

// 일기 작성 및 수정 화면을 담당하는 액티비티
class DailyWriteActivity : AppCompatActivity() {

    // 오늘 날짜를 저장하는 변수
    private lateinit var date: String
    // 메모 목록을 표시하는 어댑터
    private lateinit var memoAdapter: MemoAdapter

    // UI 뷰 변수들
    private lateinit var dateTextView: TextView
    private lateinit var memoListView: RecyclerView
    private lateinit var memoInputEditText: EditText
    private lateinit var sendIcon: ImageView

    private lateinit var micIcon: ImageView
    private lateinit var photoIcon: ImageView

    //  AudioRecorderHelper 인스턴스 생성 (lazy 초기화)
    private lateinit var audioRecorderHelper: AudioRecorderHelper
    private lateinit var imageOcrHelper: ImageOcrHelper
    private lateinit var readDiaryButton: Button

    // ViewModel 초기화 (앱의 싱글톤 저장소를 사용)
    private val memoViewModel: MemoViewModel by viewModels {
        MemoViewModelFactory(
            (application as MyApplication).repository
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_daily_write)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.write)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        audioRecorderHelper = AudioRecorderHelper(
            activity = this,
            onRecordingStarted = {
                runOnUiThread {
                    Toast.makeText(this, "녹음 시작...", Toast.LENGTH_SHORT).show()
                    micIcon.setColorFilter(ContextCompat.getColor(this, R.color.green_light))
                }
            },
            onRecordingStopped = {
                runOnUiThread {
                    Toast.makeText(this, "녹음 완료. 텍스트 변환 중...", Toast.LENGTH_SHORT).show()
                    micIcon.clearColorFilter()
                }
            },
            onRecordingComplete = { filePath, transcribedText ->
                Log.d("DailyWriteActivity", "녹음 완료, 파일 경로: $filePath")
                Log.d("DailyWriteActivity", "변환된 텍스트: $transcribedText")
                runOnUiThread {
                    if (transcribedText != null) {
                        memoInputEditText.append("$transcribedText \n")
                        Toast.makeText(this, "텍스트 변환 완료.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "텍스트 변환 실패. 오디오 파일만 저장됨.", Toast.LENGTH_SHORT).show()
                        memoInputEditText.append("[오디오 파일: $filePath] \n")
                    }
                }
            },
            onRecordingFailed = { errorMessage ->
                runOnUiThread {
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            },
            onPermissionDenied = {
                runOnUiThread {
                    Toast.makeText(this, "음성 녹음을 사용하려면 마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            },
            onShowPermissionRationale = {
                runOnUiThread {
                    Toast.makeText(this, "메모를 음성으로 녹음하려면 마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )
        imageOcrHelper =createImageOcrHelper()

        // 모든 뷰 초기화
        initViews()

        // 인텐트 데이터 처리 및 데이터 관찰 시작
        handleIntent(intent)

        // UI 요소에 클릭 리스너 설정
        setupClickListeners()

        // 하단 네비게이션 바 설정
        setupNavigationBar()
    }
    /**
     * ImageOcrHelper 인스턴스를 생성하고 콜백을 정의하는 함수
     */
    private fun createImageOcrHelper(): ImageOcrHelper {
        return ImageOcrHelper(
            activity = this,
            onOcrSuccess = { extractedText ->
                runOnUiThread {
                    // Activity의 함수를 직접 호출하여 메모 추가
                    memoInputEditText.append(extractedText)
                    Toast.makeText(this, "텍스트 추출 성공!", Toast.LENGTH_SHORT).show()
                }
            },
            onOcrFailed = { errorMessage ->
                runOnUiThread {
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            },
            onImageSelected = { uri ->
                runOnUiThread {
                    Toast.makeText(this, "이미지 분석 중...", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    // 액티비티가 재사용될 때 새로운 인텐트 처리
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    // UI 뷰 변수들을 레이아웃 ID와 연결
    private fun initViews() {
        dateTextView = findViewById(R.id.date_text_view)
        memoListView = findViewById(R.id.memo_list_view)
        memoInputEditText = findViewById(R.id.memo_input_edittext)
        sendIcon = findViewById(R.id.send_icon)
        micIcon = findViewById(R.id.mic_icon)
        photoIcon = findViewById(R.id.photo_icon)
        readDiaryButton = findViewById(R.id.read_diary_button)
        memoListView.layoutManager = LinearLayoutManager(this)
        memoAdapter = MemoAdapter()
        memoListView.adapter = memoAdapter

        // 메모 아이템 클릭 시 수정 다이얼로그 표시
        memoAdapter.setOnItemClickListener(object : MemoAdapter.OnItemClickListener {
            override fun onItemClick(memo: Memo) {
                showEditMemoDialog(memo)
            }
        })

        // 드래그 앤 드롭 및 스와이프 콜백 정의
        val dragAndDropCallback = MemoDragAndDropCallback(
            adapter = memoAdapter,
            onMove = { fromPosition, toPosition ->
                // 이 부분은 뷰의 시각적인 이동을 처리하므로, 여기서는 아무것도 하지 않음
            },
            onDelete = { position ->
                // 스와이프 후 데이터 삭제 로직
                val memoToDelete = memoAdapter.currentList[position]
                memoViewModel.delete(memoToDelete)
            },
            onDragStart = { }, // <-- 드래그 시작 시 쓰레기통 표시
            onDragEnd = {
                // 드래그가 끝났을 때 최종 순서를 데이터베이스에 반영
                val updatedList = memoAdapter.currentList.toMutableList()
                for (i in updatedList.indices) {
                    updatedList[i] = updatedList[i].copy(order = i)
                }
                memoViewModel.updateAll(updatedList)
            }
        )

        // ItemTouchHelper를 생성하고 RecyclerView에 연결
        val itemTouchHelper = ItemTouchHelper(dragAndDropCallback)
        itemTouchHelper.attachToRecyclerView(memoListView)
    }

    // 인텐트에서 날짜 데이터를 가져와 화면에 표시하고, 해당 날짜의 메모를 관찰
    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleIntent(intent: Intent?) {
        date = intent?.getStringExtra("date") ?: LocalDate.now().toString()
        dateTextView.text = date

        // ViewModel의 데이터를 관찰하고 RecyclerView 업데이트
        memoViewModel.getMemosForDate(date).observe(this) { memos ->
            memos?.let {
                memoAdapter.submitList(it)
            }
        }
    }

    // 메모 수정 다이얼로그를 보여주는 함수
    fun showEditMemoDialog(memo: Memo) {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_memo, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_text_memo_content)
        editText.setText(memo.content)

        builder.setView(dialogView)
            .setPositiveButton("수정") { dialog, id ->
                val newContent = editText.text.toString()
                if (newContent.isNotEmpty()) {
                    val updatedMemo = memo.copy(content = newContent)
                    memoViewModel.update(updatedMemo)
                }
            }
            .setNegativeButton("취소") { dialog, id ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    // 주요 UI 요소에 클릭 리스너를 설정하는 함수
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupClickListeners() {
        readDiaryButton.setOnClickListener {
            // DailyReadActivity로 이동
            val intent = Intent(this, DailyReadActivity::class.java)
            intent.putExtra("date", date)
            startActivity(intent)
        }

        sendIcon.setOnClickListener {
            // 입력된 메모 내용 가져오기
            val memoContent = memoInputEditText.text.toString()

            // 메모 내용이 비어있지 않은 경우에만 저장
            if (memoContent.isNotEmpty()) {
                // 현재 시간을 "HH:mm" 형식으로 포맷
                val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

                // Room에 저장할 Memo 객체 생성
                val newMemo = Memo(
                    content = memoContent,
                    timestamp = currentTime,
                    date = date,
                    order = memoAdapter.itemCount
                )

                // ViewModel을 통해 메모를 데이터베이스에 삽입
                memoViewModel.insert(newMemo)

                // 입력 필드 초기화 및 키보드 숨기기
                memoInputEditText.text.clear()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
            } else {
                // 메모 내용이 비어있으면 토스트 메시지 표시
                Toast.makeText(this, "메모 내용을 입력해 주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        micIcon.setOnClickListener {
            // 버튼 클릭 시 Helper의 함수 호출
            audioRecorderHelper.checkPermissionAndToggleRecording()
        }
        photoIcon.setOnClickListener {
            // ImageOcrHelper의 이미지 선택 함수 호출
            showImageAnalysisOptions()
        }
    }

    private fun showImageAnalysisOptions() {
        val options = arrayOf("사진에서 글자 추출하기 (Extract)", "사진 내용으로 글 생성하기 (Describe)")

        AlertDialog.Builder(this)
            .setTitle("사진으로 무엇을 할까요?")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        // "글자 추출" 선택 -> type: "extract"
                        imageOcrHelper.selectImage("extract")
                    }
                    1 -> {
                        // "글 생성" 선택 -> type: "describe"
                        imageOcrHelper.selectImage("describe")
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // 하단 네비게이션 바의 버튼들 클릭 이벤트를 처리
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupNavigationBar() {
        val btnCalendar: ImageButton = findViewById(R.id.btnCalendar)
        val btnDaily: ImageButton = findViewById(R.id.btnDaily)
        val btnInfo: ImageButton = findViewById(R.id.btnInfo)
        val btnSum: ImageButton = findViewById(R.id.btnSum)

        btnSum.setOnClickListener {
            // 현재 LiveData에 있는 메모 리스트를 가져옵니다.
            val currentMemos = memoAdapter.currentList // ListAdapter의 현재 리스트
            val intent = Intent(this, DailySumActivity::class.java)
            intent.putExtra("date", date)

            // 메모 리스트를 Intent에 담아 전달합니다.
            intent.putParcelableArrayListExtra("memo_list", ArrayList(currentMemos))
            startActivity(intent)
        }

        btnCalendar.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }

        btnDaily.setOnClickListener {
            val today = LocalDate.now().toString()
            val currentLoadedDate = this.date

            if (today != currentLoadedDate) {
                val intent = Intent(this, DailyWriteActivity::class.java)
                intent.putExtra("date", today)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
            } else {
                Toast.makeText(this, "이미 오늘 날짜입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        btnInfo.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Activity 종료 시 Helper의 리소스 정리
        audioRecorderHelper.release()
    }
}