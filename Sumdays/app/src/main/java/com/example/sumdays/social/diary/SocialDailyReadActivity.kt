package com.example.sumdays

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sumdays.databinding.ActivityDailyReadBinding // 💡 기존 XML 그대로 쉐어링!
import com.example.sumdays.image.GalleryItem
import com.example.sumdays.image.PhotoGalleryAdapter
import com.example.sumdays.theme.FoxRepository
import com.example.sumdays.theme.ThemePrefs
import com.example.sumdays.theme.ThemeRepository
import com.example.sumdays.utils.setupEdgeToEdge
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SocialDiaryReadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyReadBinding
    private lateinit var targetDateString: String // 📅 인텐트로 넘겨받은 "YYYY-MM-DD" 타깃 날짜
    private lateinit var photoGalleryAdapter: PhotoGalleryAdapter
    private val currentPhotoList = mutableListOf<String>()

    private val displayFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyReadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 달력에서 던져준 날짜 낚아채기
        targetDateString = intent.getStringExtra("date") ?: ""

        if (targetDateString.isEmpty()) {
            Toast.makeText(this, "올바르지 않은 날짜입니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupPhotoGallery()
        setupClickListeners()
        applyThemeModeSettings()

        // 🌟 [핵심] 로컬 DB 감시 싹 제거하고, 서버 연동 대체용 더미 원샷 트리거!
        fetchFriendDiaryFromServer(targetDateString)

        val rootView = findViewById<View>(R.id.main)
        setupEdgeToEdge(rootView)
    }

    /**
     * 🌐 [서버 호출 대체] 날짜를 찔러서 친구의 일기 내용과 사진 리스트를 땡겨와 렌더링하는 심장부 함수
     */
    private fun fetchFriendDiaryFromServer(dateKey: String) {
        // 상단 날짜 텍스트 렌더링
        binding.dateText.text = dateKey

        // 미래로 이동하는 불필요한 화살표 버튼 통제 (기존 뷰 레이아웃 의존성 해결)
        binding.nextDayButton.visibility = View.INVISIBLE
        binding.nextDayButton.isEnabled = false
        binding.prevDayButton.visibility = View.INVISIBLE
        binding.prevDayButton.isEnabled = false

        // 📝 날짜별 친구 일기 본문 & 사진 더미 데이터 주입
        var diaryText = ""
        currentPhotoList.clear()

        when (dateKey) {
            "2026-06-01" -> {
                diaryText = "오늘 민규랑 같이 컴공 부전공 과제하는데 코딩 폼 미쳤다... 토스 가고 싶다더니 진짜 괴물 개발자 다 된 듯? ㄷㄷ"
                // 사진이 있는 날이라면 예시로 샘플 Uri 문자열 투입 (나중에 서버에서 이미지 URL 배열로 쏴줄 구역)
                currentPhotoList.add("https://picsum.photos/200")
            }
            "2026-06-20" -> {
                diaryText = "식품영양학과 전공 시험 끝! 끝나고 파워빌딩 스타일로 스쿼트 조졌는데 1RM 갱신함 기분 최고 👊"
            }
            "2026-06-25" -> {
                diaryText = "Generative AI 활용에 관한 논문 스크립트 작성 완료. 생성형 AI 유저들의 만족도 조사가 아주 유의미하게 나왔다."
                currentPhotoList.add("https://picsum.photos/300")
                currentPhotoList.add("https://picsum.photos/400")
            }
            else -> {
                diaryText = "친구가 작성한 일기 내용을 불러올 수 없습니다."
            }
        }

        // 렌더링 알맹이 꽂아넣기
        binding.diaryContentEditText.setText(diaryText)
        binding.diaryContentTextView.text = diaryText

        // 사진 UI 새로고침
        updatePhotoGalleryUI()
    }

    private fun updatePhotoGalleryUI() {
        // 🦊 친구 일기장에서는 사진 추가 버튼(+)이 나오면 안 되므로 GalleryItem.Add를 과감히 제거!
        val items = currentPhotoList.map { GalleryItem.Photo(it) }
        photoGalleryAdapter.submitList(items)

        binding.photoGalleryRecyclerView.visibility =
            if (currentPhotoList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupPhotoGallery() {
        photoGalleryAdapter = PhotoGalleryAdapter(
            onPhotoClick = { /* 필요 시 전체화면 보기 구현 */ },
            onDeleteClick = { /* 친구 일기이므로 삭제 권한 없음 */ },
            onAddClick = { /* 친구 일기이므로 추가 권한 없음 */ }
        )

        binding.photoGalleryRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SocialDiaryReadActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = photoGalleryAdapter
        }
    }

    private fun setupClickListeners() {
        // 뒤로가기 버튼 클릭 시 화면 닫기
        binding.read_back_button.setOnClickListener { finish() }

        // 🛡️ 친구 전용 뷰어이므로 수정/작성창 이동 관련 모든 상호작용 뷰 차단 및 증발 처리
        binding.editInplaceButton.visibility = View.GONE
        binding.editMemosButton.visibility = View.GONE
        binding.saveButton.visibility = View.GONE
        binding.dateText.isClickable = false

        // 형이 만든 야매 치트키 뷰 투명화 및 터치 스킵
        val bottomLayout = findViewById<View>(R.id.bottom_buttons_layout)
        bottomLayout?.visibility = View.INVISIBLE
    }

    private fun applyThemeModeSettings() {
        val themeRepo = ThemeRepository
        themeRepo.updateOwned()

        val themeKey = ThemePrefs.getTheme(this)
        val currentTheme = themeRepo.ownedThemes[themeKey] ?: themeRepo.allThemeMap[themeKey]

        if (currentTheme != null) {
            binding.root.setBackgroundResource(currentTheme.backgroundColor)
        }
    }
}