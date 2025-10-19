package com.example.sumdays.daily.diary
import android.util.Log

object DiaryRepository {
    private val diaryMap = mutableMapOf<String, String>()

    fun getDiary(date: String): String? = diaryMap[date]

    fun saveDiary(date: String, text: String) {
        Log.d("date2","$date")
        Log.d("text","$text")
        diaryMap[date] = text
    }
}