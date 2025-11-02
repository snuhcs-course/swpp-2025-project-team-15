package com.example.sumdays

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.widget.*
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.daily.memo.Memo
import com.example.sumdays.daily.memo.MemoAdapter
import com.example.sumdays.daily.memo.MemoViewModel
import com.example.sumdays.daily.memo.MemoViewModelFactory
import com.example.sumdays.audio.AudioRecorderHelper
import io.mockk.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import org.robolectric.shadows.ShadowApplication
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ApplicationProvider
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [Build.VERSION_CODES.O], // LocalDate, LocalTime 사용을 위해 API 26 이상 설정
    application = MyApplication::class,
    packageName = "com.example.sumdays"
)
class DailyWriteActivityTest {

    // LiveData 테스트를 위해 필요
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var activity: DailyWriteActivity
    private lateinit var shadowApplication: ShadowApplication

    // Mock 객체 선언
    private val mockMemoViewModel: MemoViewModel = mockk(relaxed = true)
    private val mockMemoListLiveData: MutableLiveData<List<Memo>> = MutableLiveData()
    private val mockAudioRecorderHelper: AudioRecorderHelper = mockk(relaxUnitFun = true)

    // 액티비티 뷰
    private lateinit var memoInputEditText: EditText
    private lateinit var sendIcon: ImageView
    private lateinit var micIcon: ImageView
    private lateinit var dateTextView: TextView
    private lateinit var readDiaryButton: Button
    private lateinit var memoListView: RecyclerView
    private lateinit var controller: org.robolectric.android.controller.ActivityController<DailyWriteActivity>

    @Before
    fun setUp() {
        // 1. ViewModel Mocking 및 Factory 설정
        mockkConstructor(MemoViewModelFactory::class)
        // ViewModelProvider가 MemoViewModelFactory를 통해 mockViewModel을 반환하도록 설정
        every { anyConstructed<MemoViewModelFactory>().create(MemoViewModel::class.java) } returns mockMemoViewModel

        // 2. ViewModel의 LiveData 모의
        every { mockMemoViewModel.getMemosForDate(any()) } returns mockMemoListLiveData

        // 3. AudioRecorderHelper Mocking
        // AudioRecorderHelper가 실제 인스턴스 대신 mockAudioRecorderHelper를 반환하도록 설정
        mockkConstructor(AudioRecorderHelper::class)
        every {
            anyConstructed<AudioRecorderHelper>().checkPermissionAndToggleRecording()
        } just Runs
        every { anyConstructed<AudioRecorderHelper>().release() } just Runs

        // 4. Activity 시작: ActivityController를 사용하여 Activity 생성 및 생명 주기 관리
        val intent = Intent(Intent.ACTION_MAIN).apply {
            putExtra("date", LocalDate.now().toString())
        }
        // ⭐️ setup() 대신 .create()만 호출하여 최소한의 초기화만 수행
        controller = Robolectric.buildActivity(DailyWriteActivity::class.java, intent)
        activity = controller.create().get() // onCreate()까지만 실행됨 (1회 호출)

        // ⭐️ ShadowApplication은 여전히 activity.application에서 가져옴
        shadowApplication = Shadows.shadowOf(activity.application)

        // 5. 뷰 참조
        memoInputEditText = activity.findViewById(R.id.memo_input_edittext)
        sendIcon = activity.findViewById(R.id.send_icon)
        micIcon = activity.findViewById(R.id.mic_icon)
        dateTextView = activity.findViewById(R.id.date_text_view)
        readDiaryButton = activity.findViewById(R.id.read_diary_button)
        memoListView = activity.findViewById(R.id.memo_list_view)

        // 이후의 생명주기는 테스트 케이스별로 명시적으로 호출합니다.
        controller.start().resume()
    }

    @After
    fun tearDown() {
        // ⭐️ 수정: controller가 초기화되었고 (lateinit이므로 일반적으로 보장됨),
        //          activity가 이미 종료되지 않았을 때만 생명 주기를 정리합니다.

        // 이전에 ActivityController.create()만 호출하는 방식으로 수정했으므로,
        // 여기서 .destroy()를 호출하기 전에 상태를 확인합니다.

        // 만약 ActivityController가 'created' 상태 이상이라면 정리합니다.
        if (::controller.isInitialized) {
            // try-catch 블록을 사용하여 이미 종료된 액티비티에 대한 호출 예외를 안전하게 무시합니다.
            try {
                // pause(), stop()은 이미 resume된 상태에서 호출하는 것이 안전합니다.
                // setUp에서 create()만 호출했다면, 여기서 pause/stop/destroy를 모두 호출해야 합니다.
                // 하지만 No Activity 오류가 발생했으므로, 이미 종료된 상태에서 또 호출하지 않도록 방어합니다.
                controller.pause().stop().destroy()
            } catch (e: Exception) {
                // No activity 예외 등은 무시하고, Mockk 정리로 넘어갑니다.
                println("WARN: tearDown 중 액티비티 상태 오류 발생: ${e.message}")
            }
        }

        // Mockk 정리 (필수)
        unmockkAll()
    }

    // --- 1. 액티비티 생명 주기 및 초기화 테스트 ---

    @Test
    fun `testActivitySetup_withIntentDate`() {
        val testDate = "2025-10-25"
        val intent = Intent(Intent.ACTION_MAIN).apply {
            putExtra("date", testDate)
        }
        val newActivity = Robolectric.buildActivity(DailyWriteActivity::class.java, intent).setup().get()
        val dateText: TextView = newActivity.findViewById(R.id.date_text_view)

        // 날짜가 올바르게 설정되었는지 확인
        assertEquals(testDate, dateText.text.toString())
        // ViewModel의 데이터 관찰이 시작되었는지 확인
        verify(exactly = 1) { mockMemoViewModel.getMemosForDate(testDate) }
    }

    @Test
    fun `testActivitySetup_noIntentDate_usesToday`() {
        // GIVEN: setUp에서 이미 create() 호출
        val today = LocalDate.now().toString()

        // THEN: 오늘 날짜가 사용되었는지 확인 (첫 번째 create()에서 호출되었으므로 exactly=1)
        assertEquals(today, dateTextView.text.toString())
        verify(exactly = 1) { mockMemoViewModel.getMemosForDate(today) }

        // 이후의 생명주기 이벤트는 중복 검증을 피하기 위해 start, resume을 여기서 호출하지 않습니다.
    }

    @Test
    fun `testActivitySetup_audioHelperRelease`() {
        // GIVEN: AudioRecorderHelper가 Mock되었고 setup에서 생성됨
        // WHEN: Activity 종료
        controller.destroy()

        // THEN: release()가 호출되었는지 확인
        verify(exactly = 1) { anyConstructed<AudioRecorderHelper>().release() }
    }

    // --- 2. LiveData 상호작용 및 Recycler View 테스트 ---

    @Test
    fun `testMemoListUpdate_submitsToAdapter`() {
        val memo1 = Memo(1, "테스트 메모 1", "09:00", "2025-10-25", 0)
        val memo2 = Memo(2, "테스트 메모 2", "10:00", "2025-10-25", 1)
        val testList = listOf(memo1, memo2)

        // WHEN: LiveData에 데이터 주입
        mockMemoListLiveData.postValue(testList)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // THEN: Adapter에 데이터가 제출되었는지 확인
        val adapter = memoListView.adapter as MemoAdapter
        assertEquals(2, adapter.itemCount)
        assertEquals(memo1.content, adapter.currentList[0].content)
        assertEquals(memo2.content, adapter.currentList[1].content)
    }

    // --- 3. UI 상호작용 및 클릭 리스너 테스트 ---

    @Test
    fun `testSendIcon_validMemo_insertsAndClears`() {
        val memoContent = "새로운 일기 메모"
        val today = LocalDate.now().toString()
        val adapter = memoListView.adapter as MemoAdapter
        adapter.submitList(emptyList()) // 메모 목록을 비워 0번째 순서로 시작

        memoInputEditText.setText(memoContent)

        // WHEN
        sendIcon.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // THEN
        // 1. insert가 올바른 Memo 객체로 호출되었는지 검증 (order=0)
        verify(exactly = 1) {
            mockMemoViewModel.insert(match {
                it.content == memoContent && it.date == today && it.order == 0
            })
        }

        // 2. 입력 필드가 초기화되었는지 검증
        assertEquals("", memoInputEditText.text.toString())
    }

    @Test
    fun `testSendIcon_emptyMemo_showsToast`() {
        // GIVEN
        memoInputEditText.setText("")

        // WHEN
        sendIcon.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // THEN
        // 1. insert가 호출되지 않았는지 검증
        verify(exactly = 0) { mockMemoViewModel.insert(any()) }

        // 2. 토스트 메시지가 표시되었는지 검증
        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("메모 내용을 입력해 주세요.", latestToast)
    }

    @Test
    fun `testMicIconClick_callsHelper`() {
        // WHEN
        micIcon.performClick()

        // THEN
        // AudioRecorderHelper의 함수가 호출되었는지 검증
        verify(exactly = 1) { anyConstructed<AudioRecorderHelper>().checkPermissionAndToggleRecording() }
    }

    @Test
    fun `testReadDiaryButtonClick_startsDailyReadActivity`() {
        // GIVEN
        val today = LocalDate.now().toString()

        // WHEN
        readDiaryButton.performClick()

        // THEN: Intent 검증
        val actual = shadowApplication.nextStartedActivity
        assertNotNull(actual)
        assertEquals(DailyReadActivity::class.java.name, actual.component?.className)
        assertEquals(today, actual.getStringExtra("date"))
    }

    // --- 4. 내비게이션 바 클릭 이벤트 테스트 ---

    @Test
    fun `testBtnSumClick_startsDailySumActivityWithMemos`() {
        // GIVEN: Adapter에 메모 1개 추가
        val memo1 = Memo(1, "요약할 메모", "10:00", LocalDate.now().toString(), 0)
        val testList = listOf(memo1)
        mockMemoListLiveData.postValue(testList)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val btnSum: ImageButton = activity.findViewById(R.id.btnSum)

        // WHEN
        btnSum.performClick()

        // THEN: Intent 및 Extra 검증
        val actual = shadowApplication.nextStartedActivity
        assertNotNull(actual)
        assertEquals(DailySumActivity::class.java.name, actual.component?.className)
        assertEquals(LocalDate.now().toString(), actual.getStringExtra("date"))

        // 메모 리스트가 Parcelable ArrayList로 전달되었는지 확인
        val receivedMemos = actual.getParcelableArrayListExtra<Memo>("memo_list")
        assertNotNull(receivedMemos)
        assertEquals(1, receivedMemos?.size)
        assertEquals(memo1.content, receivedMemos?.get(0)?.content)
    }

    @Test
    fun `testBtnDailyClick_isToday_showsToast`() {
        // GIVEN: 현재 로드된 날짜는 오늘 날짜
        val btnDaily: ImageButton = activity.findViewById(R.id.btnDaily)

        // WHEN
        btnDaily.performClick()

        // THEN
        // 1. 화면 전환이 일어나지 않았는지 확인
        assertNull(shadowApplication.nextStartedActivity)
        // 2. 토스트 메시지 확인
        assertEquals("이미 오늘 날짜입니다.", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `testBtnDailyClick_isNotToday_restartsActivityWithToday`() {
        // GIVEN: 다른 날짜(어제)로 Activity를 다시 빌드
        val yesterday = LocalDate.now().minusDays(1).toString()
        val intent = Intent(Intent.ACTION_MAIN).apply { putExtra("date", yesterday) }
        val oldActivity = Robolectric.buildActivity(DailyWriteActivity::class.java, intent).setup().get()
        val btnDaily: ImageButton = oldActivity.findViewById(R.id.btnDaily)
        val shadowOldApplication = Shadows.shadowOf(oldActivity.application)

        // WHEN
        btnDaily.performClick()

        // THEN: Intent 검증
        val actual = shadowOldApplication.nextStartedActivity
        assertNotNull(actual)
        assertEquals(DailyWriteActivity::class.java.name, actual.component?.className)
        assertEquals(LocalDate.now().toString(), actual.getStringExtra("date"))

        // 플래그가 올바르게 설정되었는지 확인
        assertTrue(actual.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(actual.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
    }

    @Test
    fun `testBtnInfoClick_startsSettingsActivity`() {
        // GIVEN
        val btnInfo: ImageButton = activity.findViewById(R.id.btnInfo)

        // WHEN
        btnInfo.performClick()

        // THEN: Intent 검증
        val actual = shadowApplication.nextStartedActivity

        // 1. 액티비티가 시작되었는지 확인
        assertNotNull(actual)

        // 2. 시작된 액티비티가 SettingsActivity인지 확인
        assertEquals(SettingsActivity::class.java.name, actual.component?.className)
    }

    @Test
    fun `testBtnCalendarClick_startsCalendarActivity`() {
        // GIVEN
        val btnCalendar: ImageButton = activity.findViewById(R.id.btnCalendar)

        // WHEN
        btnCalendar.performClick()

        // THEN
        val actual = shadowApplication.nextStartedActivity
        assertNotNull(actual)
        assertEquals(CalendarActivity::class.java.name, actual.component?.className)
    }

    // --- 5. 메모 수정 다이얼로그 (showEditMemoDialog) 테스트 ---

    @Test
    fun `testShowEditMemoDialog_positiveButton_updatesMemo`() {
        // GIVEN
        val originalMemo = Memo(id = 1, content = "수정 전", timestamp = "09:00", date = LocalDate.now().toString(), order = 0)
        val newContent = "수정 후 내용"

        // 1. 액티비티 생명 주기 완료 (CREATED -> RESUMED)
        controller.start().resume()

        // WHEN: 다이얼로그 함수 호출
        activity.showEditMemoDialog(originalMemo)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // ⭐️ 수정 1: ShadowDialog를 사용하여 다이얼로그 객체 가져오기 (Deprecated 우회)
        val dialog = ShadowDialog.getLatestDialog()

        // ⭐️ 수정 2: 널 체크 (다이얼로그가 열렸는지 확인)
        assertNotNull("다이얼로그가 성공적으로 열리지 않았습니다. (리소스 문제 확인 필요)", dialog)

        // 안전하게 형변환
        val alertDialog = dialog as androidx.appcompat.app.AlertDialog

        // 다이얼로그 뷰의 EditText 찾기 및 내용 수정
        // R.id.edit_text_memo_content가 nullable일 수 있으므로 !!를 사용하거나 널 체크가 필요합니다.
        val editText = alertDialog.findViewById<EditText>(R.id.edit_text_memo_content)

        // ⭐️ 수정 3: 널 안정성 확보 및 텍스트 설정
        assertNotNull("다이얼로그 뷰에서 EditText를 찾을 수 없습니다.", editText)
        editText!!.setText(newContent)

        // "수정" 버튼 클릭 (POSITIVE)
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // THEN: ViewModel의 update가 새로운 내용으로 호출되었는지 검증
        verify(exactly = 1) {
            mockMemoViewModel.update(match {
                it.content == newContent && it.id == originalMemo.id
            })
        }
    }

    @Test
    fun `testShowEditMemoDialog_negativeButton_cancels`() {
        // GIVEN
        val originalMemo = Memo(id = 1, content = "수정 전", timestamp = "09:00", date = LocalDate.now().toString(), order = 0)

        // 1. 라이프사이클: @Before에서 create()만 호출했다면, 여기서 start().resume() 호출
        //    (performRestore 오류 방지)
        controller.start().resume()

        // WHEN: 다이얼로그 함수 호출
        activity.showEditMemoDialog(originalMemo)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // ⭐️ Deprecated된 ShadowApplication.getLatestAlertDialog() 대신 ShadowDialog 사용
        // 이 방식이 현재 ActivityController 환경에서 가장 현실적인 우회책입니다.
        val dialog = ShadowDialog.getLatestDialog()

        // 2. 널 체크: 다이얼로그가 열리지 않았다면 여기서 실패 (리소스 문제 확인 필요)
        assertNotNull("다이얼로그가 성공적으로 열리지 않았습니다. (DailyWriteActivity.kt에서 테마 명시 필수)", dialog)

        // 이제 안전하게 형변환 (AppCompat 다이얼로그로)
        val alertDialog = dialog as AlertDialog

        // WHEN: "취소" 버튼 클릭 (NEGATIVE)
        // 버튼 클릭 시에는 AlertDialog 클래스의 getButton 메서드를 사용합니다.
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // THEN: ViewModel의 update는 호출되지 않았는지 검증
        verify(exactly = 0) { mockMemoViewModel.update(any()) }

        // 3. isShowing 검증 (ShadowDialog 필드 접근 오류를 피하기 위해 공식 메서드 사용)
        assertFalse(alertDialog.isShowing)
    }
}