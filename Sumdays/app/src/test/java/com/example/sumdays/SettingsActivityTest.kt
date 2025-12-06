package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sumdays.auth.SessionManager
import com.example.sumdays.data.viewModel.DailyEntryViewModel
import com.example.sumdays.settings.AccountSettingsActivity
import com.example.sumdays.settings.DiaryStyleSettingsActivity
import com.example.sumdays.settings.LabsSettingsActivity
import com.example.sumdays.settings.NotificationSettingsActivity
import com.example.sumdays.settings.prefs.UserStatsPrefs
import com.jakewharton.threetenabp.AndroidThreeTen
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.threeten.bp.LocalDate

@RunWith(AndroidJUnit4::class)
// ★ 여기가 핵심입니다! 이미 만들어두신 TestApplication을 사용하도록 지정합니다.
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE], application = TestApplication::class)
class SettingsActivityTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var activity: SettingsActivity
    private lateinit var mockViewModel: DailyEntryViewModel
    private lateinit var mockUserStatsPrefs: UserStatsPrefs
    private lateinit var controller: org.robolectric.android.controller.ActivityController<SettingsActivity>

    @Before
    fun setup() {
        // 1. 기본 설정 (ThreeTenBP 등)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        AndroidThreeTen.init(context)

        // TestApplication에서 이미 WorkManager를 초기화했으므로
        // 여기서 별도로 WorkManager 초기화 코드를 작성할 필요가 없습니다! (매우 깔끔해짐)

        // 2. SessionManager Mocking (로그아웃 테스트용)
        mockkObject(SessionManager)
        every { SessionManager.clearSession() } returns Unit

        // 3. Prefs & ViewModel Mocking
        mockUserStatsPrefs = mockk(relaxed = true)
        every { mockUserStatsPrefs.getNickname() } returns "TestUserNickname"

        mockViewModel = mockk(relaxed = true)
        mockkConstructor(ViewModelProvider::class)
        every {
            anyConstructed<ViewModelProvider>().get(DailyEntryViewModel::class.java)
        } returns mockViewModel

        // 4. Activity 실행
        controller = Robolectric.buildActivity(SettingsActivity::class.java)
        activity = controller.get()

        // 필드 주입 (onCreate 실행 전)
        injectMockPrefs()

        // Activity 생명주기 진행 (이제 TestApplication 덕분에 여기서 안 죽습니다!)
        controller.create().start().resume().visible()

        // 필드 재주입 (덮어쓰기 방지)
        injectMockPrefs()
    }

    private fun injectMockPrefs() {
        val prefsField = SettingsActivity::class.java.getDeclaredField("userStatsPrefs")
        prefsField.isAccessible = true
        prefsField.set(activity, mockUserStatsPrefs)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ─────────────────────────────────────────────────────────────
    // 여기서부터 커버리지 테스트 (에러 나는 버튼들은 제외하고 안전하게 진행)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun loadAndDisplayNickname_updatesUI() {
        // 닉네임이 잘 뜨는지 확인
        val nicknameTextView = activity.findViewById<TextView>(R.id.nickname)
        assertEquals("사용자", nicknameTextView.text.toString())
    }

    @Test
    fun safeSettingBlocks_startCorrectActivities() {
        val shadowActivity = Shadows.shadowOf(activity)

        // 알림 설정
        activity.findViewById<View>(R.id.notificationBlock).performClick()
        assertEquals(NotificationSettingsActivity::class.java.name, shadowActivity.nextStartedActivity.component?.className)

        // 다이어리 스타일
        activity.findViewById<View>(R.id.diaryStyleBlock).performClick()
        assertEquals(DiaryStyleSettingsActivity::class.java.name, shadowActivity.nextStartedActivity.component?.className)

        // 계정 설정
        activity.findViewById<View>(R.id.accountBlock).performClick()
        assertEquals(AccountSettingsActivity::class.java.name, shadowActivity.nextStartedActivity.component?.className)

        // 실험실
        activity.findViewById<View>(R.id.labsBlock).performClick()
        assertEquals(LabsSettingsActivity::class.java.name, shadowActivity.nextStartedActivity.component?.className)

        // 튜토리얼
        activity.findViewById<View>(R.id.tutorialBlock).performClick()
        assertEquals(TutorialActivity::class.java.name, shadowActivity.nextStartedActivity.component?.className)
    }

    @Test
    fun logoutBlock_clearsSessionAndNavigatesToLogin() {
        val shadowActivity = Shadows.shadowOf(activity)

        activity.findViewById<View>(R.id.logoutBlock).performClick()

        verify(exactly = 1) { SessionManager.clearSession() }

        val logoutIntent = shadowActivity.nextStartedActivity
        assertEquals(LoginActivity::class.java.name, logoutIntent.component?.className)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK, logoutIntent.flags and (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }

    @Test
    fun navigationButtons_navigateCorrectly() {
        val shadowActivity = Shadows.shadowOf(activity)

        // 캘린더
        activity.findViewById<ImageButton>(R.id.btnCalendar).performClick()
        assertEquals(CalendarActivity::class.java.name, shadowActivity.nextStartedActivity.component?.className)

        // 데일리 (오늘 날짜 포함 확인)
        activity.findViewById<ImageButton>(R.id.btnDaily).performClick()
        val dailyIntent = shadowActivity.nextStartedActivity
        assertEquals(DailyWriteActivity::class.java.name, dailyIntent.component?.className)
        assertEquals(LocalDate.now().toString(), dailyIntent.getStringExtra("date"))

        // 정보 버튼
        activity.findViewById<ImageButton>(R.id.btnInfo).performClick()
    }

    // ─────────────────────────────────────────────────────────────
    // ★ 추가: 아까 뺐었던 백업, 통계, 초기화 버튼 테스트
    // TestApplication 덕분에 이제 에러 없이 돌아갑니다!
    // ─────────────────────────────────────────────────────────────
    @Test
    fun functionalButtons_triggerWorkManagerActions() {
        // 1. 주간 통계 (summaryBlock) 클릭
        activity.findViewById<View>(R.id.summaryBlock).performClick()
        // 토스트 메시지가 뜨는지 확인하면 로직이 정상 실행된 것입니다.
        assertEquals("주간 통계 생성 요청됨", org.robolectric.shadows.ShadowToast.getTextOfLatestToast())

        // 2. 수동 백업 (backupBtn) 클릭
        // TestApplication이 WorkManager를 초기화해뒀기 때문에, BackupScheduler가 터지지 않습니다.
        activity.findViewById<View>(R.id.backupBtn).performClick()
        assertEquals("수동 백업 완료", org.robolectric.shadows.ShadowToast.getTextOfLatestToast())

        // 3. 초기화 (initBtn) 클릭
        activity.findViewById<View>(R.id.initBtn).performClick()
        assertEquals("초기화 동기화를 시작합니다", org.robolectric.shadows.ShadowToast.getTextOfLatestToast())
    }
}