package com.example.sumdays.daily.diary


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.sumdays.daily.memo.Memo
import com.example.sumdays.data.AppDatabase
import com.example.sumdays.data.DailyEntry
import kotlinx.coroutines.launch

class DailyEntryViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).dailyEntryDao()

    fun getEntry(date: String) =
        dao.getEntry(date).asLiveData()

    // ✅ 2️⃣ 부분 업데이트 (선택 인자 방식)
    fun updateEntry(
        date: String,
        diary: String? = null,
        keywords: String? = null,
        aiComment: String? = null,
        emotionScore: Double? = null,
        emotionIcon: String? = null,
        themeIcon: String? = null
    ) = viewModelScope.launch {
        dao.updateEntry(
            date = date,
            diary = diary,
            keywords = keywords,
            aiComment = aiComment,
            emotionScore = emotionScore,
            emotionIcon = emotionIcon,
            themeIcon = themeIcon
        )
    }
    fun deleteEntry(date: String) = viewModelScope.launch {
        dao.deleteEntry(date)
    }
}
