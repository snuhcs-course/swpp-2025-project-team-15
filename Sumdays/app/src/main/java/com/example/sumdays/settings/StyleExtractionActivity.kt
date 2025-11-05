package com.example.sumdays.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.sumdays.auth.SessionManager
import com.example.sumdays.data.style.UserStyle
import com.example.sumdays.data.style.UserStyleViewModel
import com.example.sumdays.databinding.ActivityStyleExtractionBinding
import com.example.sumdays.network.*
import com.example.sumdays.settings.prefs.UserStatsPrefs
import com.example.sumdays.utils.FileUtil // Uri에서 File 경로를 가져오는 유틸리티 클래스 가정
import com.google.gson.Gson // StylePrompt 저장을 위해 Gson 사용
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import kotlin.coroutines.CoroutineContext

class StyleExtractionActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var binding: ActivityStyleExtractionBinding
    private lateinit var styleViewModel: UserStyleViewModel
    private val selectedImageUris = mutableListOf<Uri>() // 선택된 이미지 URI 목록


    // Activity 종료 시 코루틴 Job 취소
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    // 이미지 다중 선택을 위한 Launcher
    private val selectImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImageUris.clear()
            selectedImageUris.addAll(uris)
            updateImageCountUi()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStyleExtractionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ViewModel 초기화 (UserStyleViewModel 사용 가정)
        styleViewModel = ViewModelProvider(this).get(UserStyleViewModel::class.java)

        setupHeader()
        setupListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun setupHeader() {
        binding.header.headerTitle.text = "스타일 추출"
        binding.header.headerBackIcon.setOnClickListener { finish() }
    }

    private fun setupListeners() {
        binding.selectImagesButton.setOnClickListener {
            // 이미지/사진 파일 선택
            selectImagesLauncher.launch("image/*")
        }

        binding.runExtractionButton.setOnClickListener {
            handleExtractStyle()
        }
    }

    private fun updateImageCountUi() {
        binding.selectedImageCount.text = "선택된 이미지: ${selectedImageUris.size}개"
    }

    // --- 1. 스타일 추출 실행 (유효성 검사 포함) ---

    private fun handleExtractStyle() {
        val textInput = binding.diaryTextInput.text.toString().trim()

        // 텍스트를 줄바꿈 기준으로 분리하고, 빈 줄은 제거하여 유효한 텍스트 일기 목록을 만듭니다.
        val textDiaries = textInput.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val totalCount = textDiaries.size + selectedImageUris.size

        // 최소 5개 유효성 검사
        if (totalCount < 5) {
            Toast.makeText(this, "텍스트와 이미지를 합쳐 최소 5개 이상 제공해야 합니다. (현재: $totalCount 개)", Toast.LENGTH_LONG).show()
            return
        }

        // UI 비활성화 (로딩 상태 표시)
        binding.runExtractionButton.isEnabled = false
        binding.runExtractionButton.text = "스타일 분석 중..."

        // Multipart 요청 구성 시작 (IO 스레드에서 파일 처리)
        launch(Dispatchers.IO) {
            try {
                // 2. Multipart Part 구성
                val diaryParts = createTextParts(textDiaries)
                val imageParts = createImageParts(selectedImageUris)

                // 3. 서버 호출
                callApi(diaryParts, imageParts)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@StyleExtractionActivity, "파일 처리 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
                    resetUi()
                }
            }
        }
    }

    // --- 2. Multipart 구성 도우미 함수 ---

    private fun createTextParts(diaries: List<String>): List<MultipartBody.Part> {
        return diaries.map { diaryText ->
            // 서버의 배열 요구사항에 맞게 "diaries[]" 키 사용
            MultipartBody.Part.createFormData("diaries[]", diaryText)
        }
    }

    private fun createImageParts(uris: List<Uri>): List<MultipartBody.Part> {
        return uris.mapNotNull { uri ->
            // Uri에서 실제 File 경로를 얻는 유틸리티 함수 필요
            val file = FileUtil.getFileFromUri(this, uri)
            if (file != null && file.exists()) {
                val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                // 서버의 키인 "images"와 파일명, RequestBody 전달
                MultipartBody.Part.createFormData("images", file.name, requestBody)
            } else {
                null
            }
        }
    }

    // --- 3. Retrofit API 호출 ---

    private fun callApi(diaryParts: List<MultipartBody.Part>, imageParts: List<MultipartBody.Part>) {
        ApiClient.api.extractStyle(diaryParts, imageParts)
            .enqueue(object : Callback<StyleExtractionResponse> {

                override fun onResponse(call: Call<StyleExtractionResponse>, response: Response<StyleExtractionResponse>) {
                    launch { // CoroutineScope를 사용하여 Main/IO 스레드 관리
                        resetUi() // UI 활성화

                        if (response.isSuccessful) {
                            val styleResponse = response.body()
                            if (styleResponse != null && styleResponse.success) {
                                // 4. 추출 성공 및 Room DB 저장
                                saveStyleData(styleResponse)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@StyleExtractionActivity, "스타일 추출 완료! 설정 목록에서 확인하세요.", Toast.LENGTH_LONG).show()
                                    finish() // 이전 화면으로 돌아가 목록 업데이트 유도
                                }
                            } else {
                                // 서버 응답 실패 처리
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@StyleExtractionActivity, styleResponse?.message ?: "스타일 추출 실패", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            // HTTP 에러 처리
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@StyleExtractionActivity, "서버 응답 오류 (코드: ${response.code()})", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<StyleExtractionResponse>, t: Throwable) {
                    launch(Dispatchers.Main) {
                        resetUi()
                        Toast.makeText(this@StyleExtractionActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    // --- 4. Room DB 저장 로직 (IO 스레드) ---

    private suspend fun saveStyleData(response: StyleExtractionResponse) {
        val newStyleName = generateUniqueStyleName() // 고유한 이름 생성 함수 필요

        val newStyle = UserStyle(
            styleName = newStyleName,
            styleVector = response.style_vector,
            styleExamples = response.style_examples,
            stylePrompt = response.style_prompt
        )

        // 1. 새로운 스타일 저장
        styleViewModel.insertStyle(newStyle)
    }

    // --- 5. UI 및 유틸리티 ---

    private fun resetUi() {
        binding.runExtractionButton.isEnabled = true
        binding.runExtractionButton.text = "스타일 추출 실행"
    }

    // StyleName 생성 로직 (현재는 미구현)
    private suspend fun generateUniqueStyleName(): String {
        val styleCount = styleViewModel.getAllStyles().value?.size ?: 0
        return "나의 스타일 - ${styleCount + 1}번째"
    }
}