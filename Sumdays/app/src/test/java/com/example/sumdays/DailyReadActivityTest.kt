package com.example.sumdays

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sumdays.data.DailyEntry
import com.example.sumdays.data.viewModel.DailyEntryViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.view.View

@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE],
    application = TestApplication::class
)
class DailyReadActivityTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun createActivityWithDate(
        dateString: String?
    ): Pair<DailyReadActivity, DailyEntryViewModel> {
        val context: Context = ApplicationProvider.getApplicationContext()
        val intent = Intent(context, DailyReadActivity::class.java).apply {
            if (dateString != null) {
                putExtra("date", dateString)
            }
        }

        val controller = Robolectric.buildActivity(DailyReadActivity::class.java, intent)
        val activity = controller.get()

        val mockViewModel = mockk<DailyEntryViewModel>(relaxed = true)
        val fakeLiveData = MutableLiveData<DailyEntry?>().apply { value = null }
        every { mockViewModel.getEntry(any()) } returns fakeLiveData

        val delegateField =
            DailyReadActivity::class.java.getDeclaredField("viewModel\$delegate").apply {
                isAccessible = true
            }
        delegateField.set(activity, lazy { mockViewModel })

        controller.create().start().resume().visible()

        return activity to mockViewModel
    }

    private fun invokeUpdateUI(activity: DailyReadActivity) {
        val method = DailyReadActivity::class.java.getDeclaredMethod(
            "updateUI",
            DailyEntry::class.java
        )
        method.isAccessible = true
        method.invoke(activity, null)
    }


    @Test
    fun whenDateIsToday_dateTextShows오늘_andNextDayButtonHidden() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, _) = createActivityWithDate(todayString)
        invokeUpdateUI(activity)

        val dateText = activity.findViewById<TextView>(R.id.date_text)
        val nextButton = activity.findViewById<ImageView>(R.id.next_day_button)

        assertEquals("오늘", dateText.text.toString())
        assertEquals(ImageView.INVISIBLE, nextButton.visibility)
        assertFalse(nextButton.isEnabled)
    }

    @Test
    fun whenDateIsPast_dateTextShowsFormattedDate_andNextDayButtonVisible() {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val yesterdayString = formatter.format(cal.time)

        val (activity, _) = createActivityWithDate(yesterdayString)
        invokeUpdateUI(activity)

        val dateText = activity.findViewById<TextView>(R.id.date_text)
        val nextButton = activity.findViewById<ImageView>(R.id.next_day_button)

        assertEquals(yesterdayString, dateText.text.toString())
        assertEquals(ImageView.VISIBLE, nextButton.visibility)
        assertTrue(nextButton.isEnabled)
    }


    @Test
    fun saveDiaryContent_updatesViewModelWithGivenText() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, mockViewModel) = createActivityWithDate(todayString)

        val updatedContent = "테스트용 수정된 일기 내용"

        val method = DailyReadActivity::class.java.getDeclaredMethod(
            "saveDiaryContent",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(activity, updatedContent)

        verify(exactly = 1) {
            mockViewModel.updateEntry(date = todayString, diary = updatedContent)
        }
    }


    @Test
    fun prevDayButtonClick_movesToPreviousDay_andCallsViewModelWithPrevDate() {
        val baseCal = Calendar.getInstance()
        val todayString = formatter.format(baseCal.time)

        val (activity, mockViewModel) = createActivityWithDate(todayString)
        invokeUpdateUI(activity)

        val expectedPrevCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val expectedPrevString = formatter.format(expectedPrevCal.time)

        val prevButton = activity.findViewById<ImageView>(R.id.prev_day_button)
        prevButton.performClick()

        verify {
            mockViewModel.getEntry(expectedPrevString)
        }
    }

    @Test
    fun nextDayButtonClick_fromPast_movesToNextDay_andCallsViewModelWithNextDate() {
        val baseCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val yesterdayString = formatter.format(baseCal.time)

        val (activity, mockViewModel) = createActivityWithDate(yesterdayString)
        invokeUpdateUI(activity)

        val todayCal = Calendar.getInstance()
        val todayString = formatter.format(todayCal.time)

        val nextButton = activity.findViewById<ImageView>(R.id.next_day_button)
        nextButton.performClick()

        verify {
            mockViewModel.getEntry(todayString)
        }
    }


    @Test
    fun toggleEditMode_switchesBetweenViewAndEditStates() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, _) = createActivityWithDate(todayString)
        invokeUpdateUI(activity)

        val diaryTextView =
            activity.findViewById<TextView>(R.id.diary_content_text_view)
        val diaryEditText =
            activity.findViewById<EditText>(R.id.diary_content_edit_text)
        val editButton =
            activity.findViewById<ImageView>(R.id.edit_inplace_button)
        val saveButton =
            activity.findViewById<ImageView>(R.id.save_button)

        assertEquals(TextView.VISIBLE, diaryTextView.visibility)
        assertEquals(EditText.GONE, diaryEditText.visibility)
        assertEquals(ImageView.VISIBLE, editButton.visibility)
        assertEquals(ImageView.GONE, saveButton.visibility)

        editButton.performClick()

        assertEquals(TextView.GONE, diaryTextView.visibility)
        assertEquals(EditText.VISIBLE, diaryEditText.visibility)
        assertEquals(ImageView.GONE, editButton.visibility)
        assertEquals(ImageView.VISIBLE, saveButton.visibility)

        val method = DailyReadActivity::class.java.getDeclaredMethod(
            "toggleEditMode",
            Boolean::class.javaPrimitiveType
        )
        method.isAccessible = true
        method.invoke(activity, false)

        assertEquals(TextView.VISIBLE, diaryTextView.visibility)
        assertEquals(EditText.GONE, diaryEditText.visibility)
        assertEquals(ImageView.VISIBLE, editButton.visibility)
        assertEquals(ImageView.GONE, saveButton.visibility)
    }


    @Test
    fun clickingDateText_opensDatePickerDialog_withoutCrash() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, _) = createActivityWithDate(todayString)
        invokeUpdateUI(activity)

        val dateText = activity.findViewById<TextView>(R.id.date_text)
        dateText.performClick()
    }


    @Test
    fun whenIntentHasNoDate_initializeDateUsesToday() {
        val (activity, _) = createActivityWithDate(null)

        val field = DailyReadActivity::class.java.getDeclaredField("currentDate")
        field.isAccessible = true
        val currentDateInActivity = field.get(activity) as Calendar

        val today = Calendar.getInstance()
        fun Calendar.yyyymmdd(): Triple<Int, Int, Int> =
            Triple(
                get(Calendar.YEAR),
                get(Calendar.MONTH),
                get(Calendar.DAY_OF_MONTH)
            )

        assertEquals(today.yyyymmdd(), currentDateInActivity.yyyymmdd())
    }


    @Test
    fun clickingCalendarButton_startsCalendarActivity() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, _) = createActivityWithDate(todayString)
        invokeUpdateUI(activity)

        val btnCalendar = activity.findViewById<ImageButton>(R.id.btnCalendar)
        val shadowActivity = Shadows.shadowOf(activity)

        btnCalendar.performClick()

        val startedIntent = shadowActivity.nextStartedActivity
        assertEquals(CalendarActivity::class.java.name, startedIntent.component?.className)
    }

    @Test
    fun clickingDailyButton_startsDailyWriteActivityWithTodayDateExtra() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, _) = createActivityWithDate(todayString)
        invokeUpdateUI(activity)

        val btnDaily =
            activity.findViewById<FloatingActionButton>(R.id.btnDaily)
        val shadowActivity = Shadows.shadowOf(activity)

        btnDaily.performClick()

        val startedIntent = shadowActivity.nextStartedActivity
        assertEquals(DailyWriteActivity::class.java.name, startedIntent.component?.className)

        val extraDate = startedIntent.getStringExtra("date")
        assertEquals(formatter.format(Calendar.getInstance().time), extraDate)
    }

    @Test
    fun clickingInfoButton_startsSettingsActivity() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, _) = createActivityWithDate(todayString)
        invokeUpdateUI(activity)

        val btnInfo = activity.findViewById<ImageButton>(R.id.btnInfo)
        val shadowActivity = Shadows.shadowOf(activity)

        btnInfo.performClick()

        val startedIntent = shadowActivity.nextStartedActivity
        assertEquals(SettingsActivity::class.java.name, startedIntent.component?.className)
    }


    @Test
    fun showReanalysisDialog_runsWithoutCrash() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, _) = createActivityWithDate(todayString)

        val updatedContent = "재분석 다이얼로그 테스트용 내용"

        val method = DailyReadActivity::class.java.getDeclaredMethod(
            "showReanalysisDialog",
            String::class.java
        )
        method.isAccessible = true

        method.invoke(activity, updatedContent)
    }


    @Test
    fun addPhoto_updatesListAndViewModel() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, mockViewModel) = createActivityWithDate(todayString)

        val dummyFile = File(activity.filesDir, "dummy.jpg")
        val dummyUri = Uri.fromFile(dummyFile)

        val addPhotoMethod = DailyReadActivity::class.java.getDeclaredMethod(
            "addPhoto",
            Uri::class.java
        ).apply { isAccessible = true }

        addPhotoMethod.invoke(activity, dummyUri)

        val field = DailyReadActivity::class.java.getDeclaredField("currentPhotoUris").apply {
            isAccessible = true
        }
        @Suppress("UNCHECKED_CAST")
        val list = field.get(activity) as MutableList<Uri>

        assertEquals(1, list.size)
        assertEquals(dummyUri, list[0])

        verify {
            mockViewModel.updateEntry(date = todayString, photoUrls = any())
        }
    }


    @Test
    fun changeDate_fromTodayWithPositiveAmount_doesNotLoadFutureEntry() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, mockViewModel) = createActivityWithDate(todayString)
        invokeUpdateUI(activity)

        val method = DailyReadActivity::class.java.getDeclaredMethod(
            "changeDate",
            Int::class.javaPrimitiveType
        ).apply { isAccessible = true }

        method.invoke(activity, 1)

        verify(exactly = 1) {
            mockViewModel.getEntry(todayString)
        }
    }

    @Test
    fun deletePhoto_withFileUri_deletesFileAndUpdatesViewModel() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, mockViewModel) = createActivityWithDate(todayString)

        val tempFile = File.createTempFile("test_photo", ".jpg", activity.filesDir)
        assertTrue(tempFile.exists())
        val uri = Uri.fromFile(tempFile)

        val listField =
            DailyReadActivity::class.java.getDeclaredField("currentPhotoUris").apply {
                isAccessible = true
            }
        @Suppress("UNCHECKED_CAST")
        val list = listField.get(activity) as MutableList<Uri>
        list.add(uri)

        val deleteMethod = DailyReadActivity::class.java.getDeclaredMethod(
            "deletePhoto",
            Int::class.javaPrimitiveType
        ).apply { isAccessible = true }

        deleteMethod.invoke(activity, 0)

        assertFalse(tempFile.exists())
        assertTrue(list.isEmpty())

        verify {
            mockViewModel.updateEntry(date = todayString, photoUrls = any())
        }
    }

    @Test
    fun deletePhoto_withNonFileUri_updatesViewModelWithoutCrash() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, mockViewModel) = createActivityWithDate(todayString)

        val uri = Uri.parse("content://example/photo/1")

        val listField =
            DailyReadActivity::class.java.getDeclaredField("currentPhotoUris").apply {
                isAccessible = true
            }
        @Suppress("UNCHECKED_CAST")
        val list = listField.get(activity) as MutableList<Uri>
        list.add(uri)

        val deleteMethod = DailyReadActivity::class.java.getDeclaredMethod(
            "deletePhoto",
            Int::class.javaPrimitiveType
        ).apply { isAccessible = true }

        deleteMethod.invoke(activity, 0)

        assertTrue(list.isEmpty())

        verify {
            mockViewModel.updateEntry(date = todayString, photoUrls = any())
        }
    }

    @Test
    fun showDeleteConfirmDialog_runsWithoutCrash() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, _) = createActivityWithDate(todayString)

        val method = DailyReadActivity::class.java.getDeclaredMethod(
            "showDeleteConfirmDialog",
            Int::class.javaPrimitiveType
        ).apply { isAccessible = true }

        method.invoke(activity, 0)
    }


    @Test
    fun showPhotoDialog_displaysFullscreenDialog() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, _) = createActivityWithDate(todayString)

        val method = DailyReadActivity::class.java.getDeclaredMethod(
            "showPhotoDialog",
            String::class.java
        ).apply { isAccessible = true }

        method.invoke(activity, "https://example.com/test.jpg")

        val latestDialog = ShadowDialog.getLatestDialog()
        assertNotNull(latestDialog)
        assertTrue(latestDialog!!.isShowing)
    }

    @Test
    fun showAndHideKeyboard_doNotCrash() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, _) = createActivityWithDate(todayString)
        invokeUpdateUI(activity)

        val editText =
            activity.findViewById<EditText>(R.id.diary_content_edit_text)

        val showMethod = DailyReadActivity::class.java.getDeclaredMethod(
            "showKeyboard",
            View::class.java
        ).apply { isAccessible = true }

        val hideMethod = DailyReadActivity::class.java.getDeclaredMethod(
            "hideKeyboard",
            View::class.java
        ).apply { isAccessible = true }

        showMethod.invoke(activity, editText)
        hideMethod.invoke(activity, editText)
    }
}
