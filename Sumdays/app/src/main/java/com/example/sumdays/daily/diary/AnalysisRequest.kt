package com.example.sumdays.daily.diary
import com.example.sumdays.daily.memo.MemoPayload
import com.google.gson.annotations.SerializedName

data class AnalysisRequest(
    @SerializedName("diary") val diary: String
)
