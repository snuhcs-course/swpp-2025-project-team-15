package com.example.sumdays.data.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sumdays.data.repository.WeekSummaryRepository
import com.example.sumdays.statistics.EmotionAnalysis
import com.example.sumdays.statistics.Highlight
import com.example.sumdays.statistics.Insights
import com.example.sumdays.statistics.SummaryDetails
import com.example.sumdays.statistics.WeekSummary
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import kotlin.random.Random

class WeekSummaryViewModel (
    private val repository: WeekSummaryRepository
) : ViewModel() {


    private val USE_DUMMY_DATA = true

    // 더미 데이터를 메모리에 캐싱하기 위한 맵 (날짜 -> 요약)
    private val dummyCache = mutableMapOf<String, WeekSummary>()

    init {
        if (USE_DUMMY_DATA) {
            generateDummyData()
        }
    }


    // ------------------------------------------------------------------------
//  더미 데이터 생성 로직 (테스트용)
// ------------------------------------------------------------------------
    private fun generateDummyData() {
        val dummyCount = 60 // 생성할 데이터 개수

        // 과거 -> 최신 순으로 생성 (getAllDatesAsc가 오름차순이므로)
        // 예: 60주 전부터 오늘까지
        val baseDate = LocalDate.of(2024,10,7)

        for (i in 0 until dummyCount) {
            val startDate = baseDate.plusWeeks(i.toLong()).toString()
            val endDate = baseDate.plusWeeks(i.toLong()).plusDays(6).toString()

            // 랜덤 감정 및 데이터 생성
            val emotions = listOf("positive", "neutral", "negative")
            val dominantEmoji = when (i % 3) {
                0 -> "😊"
                1 -> "😐"
                else -> "😠"
            }
            val trend = if (i % 2 == 0) "increasing" else "decreasing"

            val summary = WeekSummary(
                startDate = startDate,
                endDate = endDate,
                diaryCount = Random.nextInt(1, 8), // 1~7 랜덤
                emotionAnalysis = EmotionAnalysis(
                    distribution = mapOf(
                        "positive" to Random.nextInt(10, 50),
                        "neutral" to Random.nextInt(5, 30),
                        "negative" to Random.nextInt(0, 20)
                    ),
                    dominantEmoji = dominantEmoji,
                    emotionScore = 0.0, // 0.0 ~ 1.0
                    trend = trend
                ),
                highlights = listOf(
                    Highlight(date = startDate, summary = "테스트 하이라이트 $i - 1"),
                    Highlight(date = endDate, summary = "테스트 하이라이트2 $i - 1")
                ),
                insights = Insights(
                    advice = "테스트 조언 $i: 꾸준함이 중요합니다.",
                    emotionCycle = "안정 -> 변화 -> 안정"
                ),
                summary = SummaryDetails(
                    emergingTopics = listOf("테스트", "개발", "통계"),
                    overview = "이것은 $startDate 주차의 테스트용 개요 데이터입니다.",
                    title = "테스트 주간 보고서 #$i"
                )
            )
            dummyCache[startDate] = summary
        }
    }

    private fun generateDummyData2() {
        val startDate = LocalDate.of(2025, 11, 17).toString()
        val endDate = LocalDate.of(2025, 11, 30).toString()

        val summary = WeekSummary(
            startDate = startDate,
            endDate = endDate,
            diaryCount = Random.nextInt(1, 8), // 1~7 랜덤
            emotionAnalysis = EmotionAnalysis(
                distribution = mapOf(
                    "positive" to Random.nextInt(10, 50),
                    "neutral" to Random.nextInt(5, 30),
                    "negative" to Random.nextInt(0, 20)
                ),
                dominantEmoji = "😊",
                emotionScore = 0.0,
                trend = "increasing"
            ),
            highlights = listOf(
                Highlight(date = startDate, summary = "테스트 하이라이트 1"),
                Highlight(date = LocalDate.of(2025,11,23).toString(), summary = "테스트 하이라이트 2"),
                Highlight(date = endDate, summary = "테스트 하이라이트 3")
            ),
            insights = Insights(
                advice = "유의미한 패턴을 기반으로 한 테스트 조언입니다.",
                emotionCycle = "안정 → 변화 → 안정"
            ),
            summary = SummaryDetails(
                emergingTopics = listOf("테스트", "개발", "통계"),
                overview = "이것은 2025-11-17 주차의 테스트용 개요 데이터입니다.",
                title = "테스트 주간 보고서"
            )
        )

        dummyCache[startDate] = summary
    }


    // 주간 통계 데이터를 저장/업데이트합니다.

    fun upsert(summary: WeekSummary) {
        viewModelScope.launch {
            repository.upsertWeekSummary(summary)
        }
    }


    // 특정 주간의 통계 데이터를 가져옵니다. (단일 호출이므로 LiveData로 감싸지 않습니다.)

    suspend fun getSummary(startDate: String): WeekSummary? {
        if (USE_DUMMY_DATA) {
            return dummyCache[startDate]
        }
        return repository.getWeekSummary(startDate)
    }


    // 통계 화면 초기 세팅을 위해 저장된 모든 주간 날짜 목록을 가져옵니다.

    suspend fun getAllDatesAsc(): List<String> {
        if (USE_DUMMY_DATA) {
            return dummyCache.keys.sorted()
        }
        return repository.getAllWrittenDatesAsc()
    }
}


// ViewModel을 인스턴스화하기 위한 팩토리 클래스 (DI를 사용하지 않을 경우)
class WeekSummaryViewModelFactory(
    private val repository: WeekSummaryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeekSummaryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeekSummaryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}