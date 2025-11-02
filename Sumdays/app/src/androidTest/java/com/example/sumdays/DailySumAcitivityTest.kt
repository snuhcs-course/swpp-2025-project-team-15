package com.example.sumdays

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sumdays.daily.memo.Memo
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailySumActivityTest {

    private val testDate = "2025-10-31"

    @Before
    fun setUp() {
        // Espresso-Intents 초기화 (Activity 전환 검증용)
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    /** 공통: 액티비티 실행 헬퍼 */
    private fun launchWith(date: String = testDate, memos: ArrayList<Memo> = arrayListOf()) {
        val intent = Intent(getApplicationContext(), DailySumActivity::class.java)
            .putExtra("date", date)
            .putParcelableArrayListExtra("memo_list", memos)
        ActivityScenario.launch<DailySumActivity>(intent)
    }

//    @Test
//    fun clickingBackIcon_navigatesToDailyWriteWithSameDate() {
//        launchWith()
//
//        onView(withId(R.id.back_icon)).perform(click())
//
//        intended(allOf(
//            hasComponent(DailyWriteActivity::class.java.name),
//            hasExtra("date", testDate)
//        ))
//    }

    @Test
    fun clickingCalendarButton_navigatesToCalendarActivity() {
        launchWith()

        onView(withId(R.id.btnCalendar)).perform(click())

        intended(hasComponent(CalendarActivity::class.java.name))
    }

    @Test
    fun clickingDailyFab_navigatesToDailyWriteActivity() {
        // 오늘 날짜 문자열은 Activity 내부에서 LocalDate.now()로 넣으므로
        // 여기서는 대상 액티비티 전환만 검증한다.
        launchWith()

        onView(withId(R.id.btnDaily)).perform(click())

        intended(hasComponent(DailyWriteActivity::class.java.name))
    }

    @Test
    fun dateText_isBoundFromIntent() {
        launchWith(date = "2023-01-02")
        onView(withId(R.id.date_text_view)).check(matches(withText("2023-01-02")))
    }
}
