package com.example.sumdays.network

import com.google.gson.annotations.SerializedName

data class WeekAnalysisRequest(
    @SerializedName("diaries")
    val diaries: List<DiaryItem>
)

data class DiaryItem(
    @SerializedName("date")
    val date: String,          // "2025-10-13"

    @SerializedName("diary")
    val diary: String?,         // 일기 본문

    @SerializedName("emoji")
    val emoji: String?,        // "📚" (nullable 가능성 대비)

    @SerializedName("emotion_score")
    val emotionScore: Double?  // 0.2 (nullable 가능성 대비)
)