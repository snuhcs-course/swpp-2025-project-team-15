package com.example.sumdays.statistics

import android.content.Intent
import android.os.Build
import android.widget.ImageButton
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sumdays.R
import com.example.sumdays.TestApplication
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class WeekStatsDetailActivityTest {

    /**
     * 테스트 시나리오 1: Intent로 전달된 WeekSummary 데이터가 화면 뷰에 올바르게 바인딩되는지 검증
     */
    @Test
    fun activity_displays_correct_data_from_intent() {
        // Arrange: 테스트용 더미 WeekSummary 데이터 생성
        val emotionAnalysis = EmotionAnalysis(
            distribution = mapOf("positive" to 5, "neutral" to 2),
            dominantEmoji = "🌟",
            emotionScore = 0.8,
            trend = "increasing"
        )

        val highlights = listOf(
            Highlight("2025-10-13", "하이라이트 1"),
            Highlight("2025-10-14", "하이라이트 2")
        )

        val insights = Insights(
            advice = "꾸준함이 중요합니다.",
            emotionCycle = "안정적인 흐름"
        )

        val summaryDetails = SummaryDetails(
            emergingTopics = listOf("성장", "테스트"),
            overview = "이번 주는 매우 생산적이었습니다.",
            title = "테스트 주간 보고서"
        )

        val mockWeekSummary = WeekSummary(
            startDate = "2025-10-13",
            endDate = "2025-10-19",
            diaryCount = 5,
            emotionAnalysis = emotionAnalysis,
            highlights = highlights,
            insights = insights,
            summary = summaryDetails
        )

        // Intent 생성 및 데이터 담기
        val intent = Intent(ApplicationProvider.getApplicationContext(), WeekStatsDetailActivity::class.java).apply {
            putExtra("week_summary", mockWeekSummary)
        }

        // Act: Activity 실행
        ActivityScenario.launch<WeekStatsDetailActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->

                // Assert: 뷰를 직접 찾아 값을 검증

                // 날짜 범위 표시 확인
                val rangeText: TextView = activity.findViewById(R.id.week_range_text_view)
                assertThat(rangeText.text.toString(), `is`("2025-10-13 ~ 2025-10-19"))

                // 요약 내용(Overview) 확인
                val overviewText: TextView = activity.findViewById(R.id.summary_content_text_view)
                assertThat(overviewText.text.toString(), `is`("이번 주는 매우 생산적이었습니다."))

                // 조언 내용(Advice) 확인
                val adviceText: TextView = activity.findViewById(R.id.feedback_content_text_view)
                assertThat(adviceText.text.toString(), `is`("꾸준함이 중요합니다."))
            }
        }
    }

    /**
     * 테스트 시나리오 2: 뒤로가기 버튼 클릭 시 액티비티가 종료(finish)되는지 검증
     */
    @Test
    fun back_button_finishes_activity() {
        val emptyData = WeekSummary(
            "2025-01-01", "2025-01-07", 0,
            EmotionAnalysis(emptyMap(), "", 0.0, ""),
            emptyList(),
            Insights("", ""),
            SummaryDetails(emptyList(), "", "")
        )

        val intent = Intent(ApplicationProvider.getApplicationContext(), WeekStatsDetailActivity::class.java).apply {
            putExtra("week_summary", emptyData)
        }

        ActivityScenario.launch<WeekStatsDetailActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                // Act: 뒤로가기 버튼 클릭
                val backButton: ImageButton = activity.findViewById(R.id.back_button)
                backButton.performClick()

                // Assert: 액티비티가 종료 상태인지 확인
                assertThat(activity.isFinishing, `is`(true))
            }
        }
    }
}