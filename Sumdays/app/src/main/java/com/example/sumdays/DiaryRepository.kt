package com.example.sumdays

object DiaryRepository {
    private val diaryMap = mutableMapOf<String, String>()

    fun getDiary(date: String): String? = diaryMap[date]

    fun saveDiary(date: String, text: String) {
        diaryMap[date] = text
    }
}
