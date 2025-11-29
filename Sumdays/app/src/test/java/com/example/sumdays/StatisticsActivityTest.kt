package com.example.sumdays

import android.app.Application
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.example.sumdays.data.repository.DailyEntryRepository
import com.example.sumdays.data.repository.WeekSummaryRepository
import com.example.sumdays.data.viewModel.DailyEntryViewModel
import com.example.sumdays.data.viewModel.WeekSummaryViewModel
import com.example.sumdays.statistics.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import org.robolectric.RobolectricTestRunner
import org.threeten.bp.Clock
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.lang.reflect.Field

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestApplication::class)
class StatisticsActivityTest {

    private lateinit var mockWeekSummaryRepository: WeekSummaryRepository
    private lateinit var mockDailyEntryRepository: DailyEntryRepository
    private lateinit var mockDailyEntryViewModel: DailyEntryViewModel

    @Before
    fun setup() {
        // 1. 코루틴 Dispatcher 설정
        Dispatchers.setMain(UnconfinedTestDispatcher())

        // 2. Static Mocking
        mockkStatic(Glide::class)
        mockkStatic(LocalDate::class)
        mockkStatic(Log::class)

        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        // Glide 크래시 방지
        val mockRequestManager = mockk<RequestManager>(relaxed = true)
        val mockRequestBuilder = mockk<RequestBuilder<GifDrawable>>(relaxed = true)
        every { Glide.with(any<android.app.Activity>()) } returns mockRequestManager
        every { mockRequestManager.asGif() } returns mockRequestBuilder
        every { mockRequestBuilder.load(any<Int>()) } returns mockRequestBuilder

        // 3. Mock Repository 생성
        mockWeekSummaryRepository = mockk(relaxed = true)
        mockDailyEntryRepository = mockk(relaxed = true)
        mockDailyEntryViewModel = mockk(relaxed = true)

        // 4. [핵심] MyApplication에 Mock Repository 강제 주입
        val app = ApplicationProvider.getApplicationContext<MyApplication>()

        // by lazy 필드($delegate)까지 고려하여 주입
        injectMockIntoApplication(app, "weekSummaryRepository", mockWeekSummaryRepository)
        injectMockIntoApplication(app, "dailyEntryRepository", mockDailyEntryRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    /**
     * [강력한 주입 헬퍼]
     * 1. 일반 필드 검색
     * 2. Kotlin 'by lazy' 필드(fieldName$delegate) 검색 및 Lazy wrapper 처리
     * 3. 상위 클래스 탐색
     */
    private fun injectMockIntoApplication(target: Any, fieldName: String, mockValue: Any) {
        var clazz: Class<*>? = target.javaClass
        while (clazz != null) {
            // 1. 일반 필드 시도
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                field.set(target, mockValue)
                return
            } catch (e: NoSuchFieldException) {
                // 2. Delegate 필드 시도 (by lazy)
                try {
                    val delegateField = clazz.getDeclaredField("$fieldName\$delegate")
                    delegateField.isAccessible = true
                    // Mock 객체를 Lazy<T>로 감싸서 주입
                    delegateField.set(target, lazyOf(mockValue))
                    return
                } catch (e2: NoSuchFieldException) {
                    // 상위 클래스로 이동
                    clazz = clazz.superclass
                }
            } catch (e: Exception) {
                throw RuntimeException("Failed to inject $fieldName", e)
            }
        }
        // 필드를 못 찾았지만, TestApplication 구조상 필드가 없을 수도 있음 (예: get() 메서드만 있는 경우)
        // 이 경우 테스트를 강제 종료하지 않고 경고만 남기거나 무시 (VM Factory Mocking으로 방어)
        println("WARNING: Could not find field '$fieldName' in ${target.javaClass}. DB access might occur if ViewModels use it.")
    }

    private fun mockToday(date: LocalDate) {
        every { LocalDate.now() } returns date
        every { LocalDate.now(any<Clock>()) } returns date
        every { LocalDate.now(any<ZoneId>()) } returns date
        every { LocalDate.of(any<Int>(), any<Int>(), any<Int>()) } answers { callOriginal() }
        every { LocalDate.ofEpochDay(any<Long>()) } answers { callOriginal() }
        every { LocalDate.parse(any<String>()) } answers { callOriginal() }
    }

    private fun createDummyWeekSummary(startDate: String): WeekSummary {
        return WeekSummary(
            startDate = startDate,
            endDate = "2025-11-XX",
            diaryCount = 5,
            emotionAnalysis = EmotionAnalysis(mapOf(), "😊", 0.8),
            highlights = listOf(),
            insights = Insights("Good", "Cycle"),
            summary = SummaryDetails(listOf(), "Overview", "Title")
        )
    }

    private fun createActivity(): StatisticsActivity {
        // [중요] Repository가 주입되지 않았을 경우를 대비해 VM Factory/Data도 Mocking

        // 1. WeekSummaryViewModel 데이터
        coEvery { mockWeekSummaryRepository.getAllWrittenDatesAsc() } returns listOf("2025-11-10", "2025-11-17")
        coEvery { mockWeekSummaryRepository.getWeekSummary("2025-11-10") } returns createDummyWeekSummary("2025-11-10")
        coEvery { mockWeekSummaryRepository.getWeekSummary("2025-11-17") } returns createDummyWeekSummary("2025-11-17")

        // 2. DailyEntryViewModel 데이터
        mockToday(LocalDate.of(2025, 11, 29))
        coEvery { mockDailyEntryViewModel.getAllWrittenDates() } returns listOf("2025-11-29", "2025-11-28", "2025-11-27")

        // 3. Activity 생성
        val controller = Robolectric.buildActivity(StatisticsActivity::class.java)
        val activity = controller.create().get()

        // 4. DailyEntryViewModel 강제 교체 (Activity가 생성한 진짜 VM 덮어쓰기)
        injectField(activity, "viewModel", mockDailyEntryViewModel)

        // 5. WeekSummaryViewModel 강제 교체 (by viewModels delegate)
        val mockWeekSummaryViewModel = mockk<WeekSummaryViewModel>(relaxed = true)
        // 위에서 정의한 Repo 동작을 VM이 호출하도록 연결하거나, VM 자체를 스터빙
        coEvery { mockWeekSummaryViewModel.getAllDatesAsc() } returns listOf("2025-11-10", "2025-11-17")
        coEvery { mockWeekSummaryViewModel.getSummary(any()) } returns createDummyWeekSummary("2025-11-10")

        // delegate 필드 교체
        injectDelegate(activity, "weekSummaryViewModel", mockWeekSummaryViewModel)

        // 6. Lifecycle 진행
        controller.start().resume().visible()
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        return activity
    }

    // Activity 내부 필드 주입용 단순 헬퍼
    private fun injectField(target: Any, fieldName: String, value: Any) {
        try {
            val field = target::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(target, value)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Activity 내부 delegate 주입용 헬퍼 (by viewModels)
    private fun injectDelegate(target: Any, propertyName: String, value: Any) {
        try {
            // Kotlin delegate 필드명 규칙: "propertyName$delegate"
            val field = target::class.java.getDeclaredField("$propertyName\$delegate")
            field.isAccessible = true
            // Lazy<T>로 감싸서 주입
            field.set(target, lazyOf(value))
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ─────────────────────────────────────────────────────────────
    // 테스트 케이스
    // ─────────────────────────────────────────────────────────────

//    @Test
//    fun onCreate_loadsData_andUpdatesHeaderStats() {
//        val activity = createActivity()
//
//        val recyclerView = activity.findViewById<RecyclerView>(R.id.recyclerView)
//        assertNotNull("RecyclerView가 null입니다.", recyclerView)
//
//        // Adapter 확인 (데이터 2개 + 여분 10개 = 12)
//        assertNotNull("Adapter가 연결되지 않았습니다.", recyclerView.adapter)
//        assertEquals(12, recyclerView.adapter?.itemCount)
//
//        val tvLeafCount = activity.findViewById<TextView>(R.id.tv_leaf_count)
//        val tvStrikeCount = activity.findViewById<TextView>(R.id.tv_strike_count)
//
//        assertEquals("🍃: 2", tvLeafCount.text.toString())
//        assertEquals("🔥: 3", tvStrikeCount.text.toString())
//    }

    @Test
    fun calculateStreak_logicCheck() {
        val activity = createActivity()

        mockToday(LocalDate.of(2025, 11, 29))
        val dates1 = listOf("2025-11-29", "2025-11-28", "2025-11-27")
        assertEquals(3, activity.calculateCurrentStreak(dates1))

        mockToday(LocalDate.of(2025, 12, 1))
        val dates2 = listOf("2025-11-29")
        assertEquals(0, activity.calculateCurrentStreak(dates2))
    }

    @Test
    fun backButton_finishesActivity() {
        val activity = createActivity()
        val btnBack = activity.findViewById<ImageButton>(R.id.btn_back)
        btnBack.performClick()
        assertTrue(activity.isFinishing)
    }

    @Test
    fun clickingLeafItem_navigatesToDetailActivity() {
        val activity = createActivity()
        val recyclerView = activity.findViewById<RecyclerView>(R.id.recyclerView)

        //
        // 레이아웃 강제 수행
        recyclerView.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY)
        )
        recyclerView.layout(0, 0, 1000, 2000)

        val adapter = recyclerView.adapter!!
        val targetPosition = adapter.itemCount - 1

        val holder = adapter.createViewHolder(recyclerView, 0)
        adapter.bindViewHolder(holder, targetPosition)

        val btnWeeklyStats = holder.itemView.findViewById<ImageButton>(R.id.btnWeeklyStats)
        assertTrue("통계 버튼이 비활성화 상태입니다.", btnWeeklyStats.isEnabled)

        btnWeeklyStats.performClick()

        val expectedIntent = Intent(activity, WeekStatsDetailActivity::class.java)
        val actualIntent = Shadows.shadowOf(activity).nextStartedActivity

        assertNotNull("이동할 Intent가 발생하지 않았습니다.", actualIntent)
        assertEquals(expectedIntent.component, actualIntent.component)

        val extra = actualIntent.getParcelableExtra<WeekSummary>("week_summary")
        assertNotNull(extra)
        assertEquals("2025-11-10", extra?.startDate)
    }

    @Test
    fun moveButtons_triggerScroll() {
        val activity = createActivity()
        val btnTop = activity.findViewById<ImageButton>(R.id.btn_move_to_latest_leaf)
        val btnBottom = activity.findViewById<ImageButton>(R.id.btn_move_to_bottom_leaf)

        btnTop.performClick()
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        btnBottom.performClick()
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    @Test
    fun showLoading_togglesVisibility() {
        val activity = createActivity()
        val loadingOverlay = activity.findViewById<View>(R.id.loading_overlay)
        assertEquals(View.GONE, loadingOverlay.visibility)
    }
}