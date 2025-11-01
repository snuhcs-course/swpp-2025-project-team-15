package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import com.example.sumdays.daily.diary.AnalysisRepository
import com.example.sumdays.daily.diary.AnalysisResponse
import com.example.sumdays.daily.diary.DiaryRepository
import com.example.sumdays.daily.memo.Memo
import com.example.sumdays.daily.memo.MemoMergeAdapter
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class DailySumActivityTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDate = "2025-11-01"


    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockkConstructor(MemoMergeAdapter::class)
        coEvery { anyConstructed<MemoMergeAdapter>().skipMerge() } returns "merged-result-from-test"
        every { anyConstructed<MemoMergeAdapter>().undoLastMerge() } returns true

        mockkObject(DiaryRepository)
        every { DiaryRepository.saveDiary(any(), any()) } just Runs

        mockkObject(AnalysisRepository)
        // ← 함수 시그니처가 AnalysisResponse? 라면 null 반환으로 맞추세요.
        coEvery { AnalysisRepository.requestAnalysis(any()) } returns null
    }


    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    /** Helper: Activity 실행 */
    private fun launchWith(
        date: String = testDate,
        memos: ArrayList<Memo> = arrayListOf()
    ): DailySumActivity {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(ctx, DailySumActivity::class.java).apply {
            putExtra("date", date)
            putParcelableArrayListExtra("memo_list", memos)
        }
        return Robolectric.buildActivity(DailySumActivity::class.java, intent).setup().get()
    }

    @Test
    fun clickingSkip_savesDiary_and_navigatesToRead() {
        val activity = launchWith()

        activity.findViewById<ImageButton>(R.id.skip_icon).performClick()

        // ★ 코루틴 작업 마무리
        testDispatcher.scheduler.advanceUntilIdle()
        // ★ 메인 루퍼 큐도 비우기
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()

        // 1) 어댑터의 skipMerge가 실제로 불렸는지 먼저 확인
        coVerify(exactly = 1) { anyConstructed<MemoMergeAdapter>().skipMerge() }

        // 2) 저장 호출 검증
        verify(exactly = 1) { DiaryRepository.saveDiary("2025-11-01", "merged-result-from-test") }

        // 3) 화면 전환 검증
        val started = Shadows.shadowOf(activity).nextStartedActivity
        assert(started != null)
        assert(started.component?.className == DailyReadActivity::class.java.name)
    }

    @Test
    fun clickingUndo_invokesAdapterUndo() {
        val activity = launchWith()

        val undo = activity.findViewById<ImageButton>(R.id.undo_button)
        undo.performClick()

        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()

        verify(exactly = 1) { anyConstructed<MemoMergeAdapter>().undoLastMerge() }
    }

    @Test
    fun clickingBack_navigatesToWrite_withSameDateExtra() {
        val activity = launchWith()

        val back = activity.findViewById<ImageView>(R.id.back_icon)
        back.performClick()

        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()

        val startedIntent = Shadows.shadowOf(activity).nextStartedActivity
        assert(startedIntent != null)
        assert(startedIntent.component?.className == DailyWriteActivity::class.java.name)
        assert(startedIntent.getStringExtra("date") == testDate)
    }
}
