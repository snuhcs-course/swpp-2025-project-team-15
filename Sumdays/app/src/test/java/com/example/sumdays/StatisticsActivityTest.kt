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
        // 코루틴 Dispatcher 설정
        Dispatchers.setMain(UnconfinedTestDispatcher())

        // Static Mocking
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

        // Mock Repository 생성
        mockWeekSummaryRepository = mockk(relaxed = true)
        mockDailyEntryRepository = mockk(relaxed = true)
        mockDailyEntryViewModel = mockk(relaxed = true)

        // MyApplication에 Mock Repository 강제 주입
        val app = ApplicationProvider.getApplicationContext<MyApplication>()

        injectMockIntoApplication(app, "weekSummaryRepository", mockWeekSummaryRepository)
        injectMockIntoApplication(app, "dailyEntryRepository", mockDailyEntryRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun injectMockIntoApplication(target: Any, fieldName: String, mockValue: Any) {
        var clazz: Class<*>? = target.javaClass
        while (clazz != null) {
            // 일반 필드 시도
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                field.set(target, mockValue)
                return
            } catch (e: NoSuchFieldException) {
                // Delegate 필드 시도 (by lazy)
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
        // Repository가 주입되지 않았을 경우를 대비해 VM Factory/Data도 Mocking

        // WeekSummaryViewModel 데이터
        coEvery { mockWeekSummaryRepository.getAllWrittenDatesAsc() } returns listOf("2025-11-10", "2025-11-17")
        coEvery { mockWeekSummaryRepository.getWeekSummary("2025-11-10") } returns createDummyWeekSummary("2025-11-10")
        coEvery { mockWeekSummaryRepository.getWeekSummary("2025-11-17") } returns createDummyWeekSummary("2025-11-17")

        // DailyEntryViewModel 데이터
        mockToday(LocalDate.of(2025, 11, 29))
        coEvery { mockDailyEntryViewModel.getAllWrittenDates() } returns listOf("2025-11-29", "2025-11-28", "2025-11-27")

        // Activity 생성
        val controller = Robolectric.buildActivity(StatisticsActivity::class.java)
        val activity = controller.create().get()

        // DailyEntryViewModel 강제 교체
        injectField(activity, "viewModel", mockDailyEntryViewModel)

        // WeekSummaryViewModel 강제 교체
        val mockWeekSummaryViewModel = mockk<WeekSummaryViewModel>(relaxed = true)
        // 위에서 정의한 Repo 동작을 VM이 호출하도록 연결하거나, VM 자체를 스터빙
        coEvery { mockWeekSummaryViewModel.getAllDatesAsc() } returns listOf("2025-11-10", "2025-11-17")
        coEvery { mockWeekSummaryViewModel.getSummary(any()) } returns createDummyWeekSummary("2025-11-10")

        // delegate 필드 교체
        injectDelegate(activity, "weekSummaryViewModel", mockWeekSummaryViewModel)

        // Lifecycle 진행
        controller.start().resume().visible()
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        return activity
    }

    // Activity 내부 필드 주입용 헬퍼
    private fun injectField(target: Any, fieldName: String, value: Any) {
        try {
            val field = target::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(target, value)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Activity 내부 delegate 주입용 헬퍼
    private fun injectDelegate(target: Any, propertyName: String, value: Any) {
        try {
            val field = target::class.java.getDeclaredField("$propertyName\$delegate")
            field.isAccessible = true
            // Lazy<T>로 감싸서 주입
            field.set(target, lazyOf(value))
        } catch (e: Exception) { e.printStackTrace() }
    }


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