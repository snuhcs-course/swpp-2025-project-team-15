package com.example.sumdays.data.dao

import androidx.room.*
import com.example.sumdays.data.DailyEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyEntryDao {

    // ✅ 1️⃣ 특정 날짜의 엔트리(일기) 가져오기
    @Query("SELECT * FROM daily_entry WHERE date = :date")
    fun getEntry(date: String): Flow<DailyEntry?>

    @Query("""
    UPDATE daily_entry
    SET 
        diary = COALESCE(:diary, diary),
        keywords = COALESCE(:keywords, keywords),
        aiComment = COALESCE(:aiComment, aiComment),
        emotionScore = COALESCE(:emotionScore, emotionScore),
        emotionIcon = COALESCE(:emotionIcon, emotionIcon),
        themeIcon = COALESCE(:themeIcon, themeIcon)
    WHERE date = :date
    """)
    suspend fun updateEntry(
        date: String,
        diary: String? = null,
        keywords: String? = null,
        aiComment: String? = null,
        emotionScore: Double? = null,
        emotionIcon: String? = null,
        themeIcon: String? = null
    )

    // ✅ 4️⃣ 해당 날짜의 일기 삭제
    @Query("DELETE FROM daily_entry WHERE date = :date")
    suspend fun deleteEntry(date: String)
}
