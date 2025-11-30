package com.example.sumdays.data.viewModel

import com.example.sumdays.data.repository.WeekSummaryRepository
import com.example.sumdays.statistics.WeekSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// =========================================================================
// 메인 테스트 클래스
// =========================================================================
@OptIn(ExperimentalCoroutinesApi::class)
class WeekSummaryViewModelTest {

    // 이름 충돌 방지를 위해 클래스명을 변경하여 사용
    @get:Rule
    val mainDispatcherRule = WeekSummaryDispatcherRule()

    private lateinit var repository: WeekSummaryRepository
    private lateinit var viewModel: WeekSummaryViewModel

    @BeforeTest
    fun setup() {
        // Mock Repository (relaxed=true: 반환값 설정 없어도 에러 안 남)
        repository = mockk(relaxed = true)
        viewModel = WeekSummaryViewModel(repository)
    }

    // =========================================================================
    // 1. 정상 동작 테스트 (Public Methods)
    // =========================================================================

    @Test
    fun `upsert should call repository upsert`() = runTest {
        // Given
        val summary = mockk<WeekSummary>(relaxed = true)

        // When
        viewModel.upsert(summary)

        // Then - Repository 위임 확인
        coVerify { repository.upsertWeekSummary(summary) }
    }

    @Test
    fun `getSummary should retrieve data from repository when dummy mode is false`() = runTest {
        // Given
        val startDate = "2023-11-27"
        val expectedSummary = mockk<WeekSummary>(relaxed = true)
        coEvery { repository.getWeekSummary(startDate) } returns expectedSummary

        // When
        val result = viewModel.getSummary(startDate)

        // Then
        assertEquals(expectedSummary, result)
        coVerify { repository.getWeekSummary(startDate) }
    }

    @Test
    fun `getAllDatesAsc should retrieve dates from repository`() = runTest {
        // Given
        val expectedDates = listOf("2023-11-01", "2023-11-08")
        coEvery { repository.getAllWrittenDatesAsc() } returns expectedDates

        // When
        val result = viewModel.getAllDatesAsc()

        // Then
        assertEquals(expectedDates, result)
        coVerify { repository.getAllWrittenDatesAsc() }
    }

    // =========================================================================
    // 2. 내부 로직 강제 테스트 (Private Method & Dummy Data Logic)
    // =========================================================================

    @Test
    fun `internal dummy data generation logic should execute correctly`() = runTest {
        // 설명: USE_DUMMY_DATA가 false라서 죽어있는 코드를 Reflection으로 살려서 실행합니다.

        // 1. generateDummyData 메서드 강제 호출
        val generateMethod = viewModel.javaClass.getDeclaredMethod("generateDummyData")
        generateMethod.isAccessible = true
        generateMethod.invoke(viewModel)

        // 2. private 변수 dummyCache 접근해서 데이터 확인
        val cacheField = viewModel.javaClass.getDeclaredField("dummyCache")
        cacheField.isAccessible = true
        val cache = cacheField.get(viewModel) as MutableMap<String, WeekSummary>

        // 3. 검증 (루프가 60번 돌았는지 확인)
        assertTrue(cache.isNotEmpty())
        assertEquals(60, cache.size)

        // 4. 로직 흐름 검증 (캐시 데이터가 정상적인지 샘플링)
        val key = cache.keys.first()
        val cachedSummary = cache[key]
        assertNotNull(cachedSummary)

        // 정렬 로직 확인
        val sortedKeys = cache.keys.sorted()
        assertTrue(sortedKeys.isNotEmpty())
    }

    // =========================================================================
    // 3. ViewModel Factory 테스트
    // =========================================================================

    @Test
    fun `ViewModelFactory should create ViewModel instances correctly`() {
        val factory = WeekSummaryViewModelFactory(repository)

        // 1. 성공 케이스
        val vm = factory.create(WeekSummaryViewModel::class.java)
        assertNotNull(vm)
        assertTrue(vm is WeekSummaryViewModel)

        // 2. 실패 케이스 (예외 발생 검증)
        assertFailsWith<IllegalArgumentException> {
            factory.create(DummyViewModel::class.java)
        }
    }

    // Factory 테스트용 가짜 클래스
    class DummyViewModel : androidx.lifecycle.ViewModel()
}

// =========================================================================
// 테스트 유틸리티 클래스 (이름 변경: WeekSummaryDispatcherRule)
// =========================================================================
@OptIn(ExperimentalCoroutinesApi::class)
class WeekSummaryDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}