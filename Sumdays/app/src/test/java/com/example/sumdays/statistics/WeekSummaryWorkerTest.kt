package com.example.sumdays.statistics

import android.util.Log
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.sumdays.MyApplication
import com.example.sumdays.TestApplication
import com.example.sumdays.data.DailyEntry
import com.example.sumdays.data.repository.DailyEntryRepository
import com.example.sumdays.data.repository.WeekSummaryRepository
import com.example.sumdays.network.ApiClient
import com.example.sumdays.network.ApiService
import com.example.sumdays.network.NetworkEmotionAnalysis
import com.example.sumdays.network.NetworkHighlight
import com.example.sumdays.network.NetworkInsights
import com.example.sumdays.network.NetworkSummaryDetails
import com.example.sumdays.network.WeekAnalysisResponse
import com.example.sumdays.network.WeekAnalysisResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.threeten.bp.Clock
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.TemporalAccessor
import retrofit2.Response
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestApplication::class)
class WeekSummaryWorkerTest {

    // mockContext는 제거하고 mockApplication을 직접 사용합니다.
    private lateinit var mockApplication: MyApplication
    private lateinit var mockWorkerParams: WorkerParameters

    private lateinit var mockDailyEntryRepository: DailyEntryRepository
    private lateinit var mockWeekSummaryRepository: WeekSummaryRepository
    private lateinit var mockApiService: ApiService

    @Before
    fun setup() {
        // 1. Static & Object Mocking
        mockkObject(ApiClient)

        mockkStatic(LocalDate::class)
        mockkStatic(Log::class)

        // Log.e 호출 시 콘솔 출력 (디버깅용)
        every { Log.e(any(), any(), any()) } answers {
            val tag = firstArg<String>()
            val msg = secondArg<String>()
            val tr = thirdArg<Throwable>()
            println("[TestLog] ERROR: $tag: $msg")
            tr.printStackTrace()
            0
        }
        every { Log.e(any(), any()) } answers {
            println("[TestLog] ERROR: ${firstArg<String>()}: ${secondArg<String>()}")
            0
        }
        every { Log.d(any(), any()) } returns 0

        // 2. Mock 생성
        mockApiService = mockk(relaxed = true)
        mockApplication = mockk(relaxed = true) // MyApplication Mock
        mockDailyEntryRepository = mockk(relaxed = true)
        mockWeekSummaryRepository = mockk(relaxed = true)
        mockWorkerParams = mockk(relaxed = true)

        // 3. 의존성 연결
        every { ApiClient.api } returns mockApiService

        // [중요] Application Mock이 Context 역할을 수행하므로 applicationContext 호출 시 자기 자신 반환
        every { mockApplication.applicationContext } returns mockApplication
        every { mockApplication.dailyEntryRepository } returns mockDailyEntryRepository
        every { mockApplication.weekSummaryRepository } returns mockWeekSummaryRepository

        // Background Executor (동기 실행)
        every { mockWorkerParams.backgroundExecutor } returns Executor { it.run() }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createWorker(inputData: Data = Data.EMPTY): WeekSummaryWorker {
        every { mockWorkerParams.inputData } returns inputData
        // [수정 핵심] Worker 생성자에 mockContext가 아닌 mockApplication을 전달
        // 이렇게 하면 Worker 내부의 'applicationContext as MyApplication' 캐스팅이 성공합니다.
        return WeekSummaryWorker(mockApplication, mockWorkerParams)
    }

    private fun mockToday(date: LocalDate) {
        every { LocalDate.now() } returns date
        every { LocalDate.now(any<Clock>()) } returns date
        every { LocalDate.now(any<ZoneId>()) } returns date

        every { LocalDate.of(any<Int>(), any<Int>(), any<Int>()) } answers { callOriginal() }
        every { LocalDate.ofEpochDay(any<Long>()) } answers { callOriginal() }
        every { LocalDate.from(any<TemporalAccessor>()) } answers { callOriginal() }
        every { LocalDate.parse(any<String>()) } answers { callOriginal() }
        every { LocalDate.parse(any<String>(), any<DateTimeFormatter>()) } answers { callOriginal() }
    }

    private fun createDailyEntry(
        date: String,
        diary: String = "Test Diary",
        emotionScore: Double? = 0.5,
        emotionIcon: String? = "😐"
    ): DailyEntry {
        return DailyEntry(
            date = date,
            diary = diary,
            keywords = null,
            aiComment = null,
            emotionScore = emotionScore,
            emotionIcon = emotionIcon,
            themeIcon = null,
            isEdited = false,
            isDeleted = false,
            photoUrls = null
        )
    }

    // ─────────────────────────────────────────────────────────────
    // 1. 테스트 모드 (IS_TEST_MODE = true)
    // ─────────────────────────────────────────────────────────────



    // ─────────────────────────────────────────────────────────────
    // 2. 일기 개수 부족 (3개 미만)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun insufficient_entry_no_api_call() = runBlocking {
        // Given: 2025-10-20 (월) -> 지난주: 10/13 ~ 10/19
        mockToday(LocalDate.of(2025, 10, 20))

        val dates = listOf("2025-10-13", "2025-10-14")

        coEvery { mockDailyEntryRepository.getAllWrittenDates() } returns dates
        coEvery { mockDailyEntryRepository.getEntrySnapshot(any()) } returns createDailyEntry(
            date = "2025-10-13"
        )

        val worker = createWorker()

        // When
        val result = worker.doWork()

        // Then
        assertTrue("결과가 Retry입니다. 콘솔의 [TestLog]를 확인하세요.", result is ListenableWorker.Result.Success)
        coVerify(exactly = 0) { mockApiService.summarizeWeek(any()) }
        coVerify(exactly = 0) { mockWeekSummaryRepository.upsertWeekSummary(any()) }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. 정상 동작 (API 성공 -> DB 저장)
    // ─────────────────────────────────────────────────────────────



    // ─────────────────────────────────────────────────────────────
    // 4. API 실패 및 예외 처리
    // ─────────────────────────────────────────────────────────────

    @Test
    fun fail_api_call_return_retry() = runBlocking {
        // Given
        mockToday(LocalDate.of(2025, 10, 20))

        coEvery { mockDailyEntryRepository.getAllWrittenDates() } returns listOf("2025-10-13", "2025-10-14", "2025-10-15")
        coEvery { mockDailyEntryRepository.getEntrySnapshot(any()) } returns createDailyEntry("2025-10-13")

        // API 500 에러
        val errorResponse = Response.error<WeekAnalysisResponse>(
            500,
            okhttp3.ResponseBody.create(null, "Server Error")
        )
        coEvery { mockApiService.summarizeWeek(any()) } returns errorResponse

        val worker = createWorker()

        // When
        val result = worker.doWork()

        // Then
        assertTrue(result is ListenableWorker.Result.Retry)
        coVerify(exactly = 0) { mockWeekSummaryRepository.upsertWeekSummary(any()) }
    }

    @Test
    fun network_error_return_retry() = runBlocking {
        // Given
        mockToday(LocalDate.of(2025, 10, 20))

        coEvery { mockDailyEntryRepository.getAllWrittenDates() } returns listOf("2025-10-13", "2025-10-14", "2025-10-15")
        coEvery { mockDailyEntryRepository.getEntrySnapshot(any()) } returns createDailyEntry("2025-10-13")

        // API 호출 시 Exception throw
        coEvery { mockApiService.summarizeWeek(any()) } throws RuntimeException("Connection Timeout")

        val worker = createWorker()

        // When
        val result = worker.doWork()

        // Then
        assertTrue(result is ListenableWorker.Result.Retry)
    }
}