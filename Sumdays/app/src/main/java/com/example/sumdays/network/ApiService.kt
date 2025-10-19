package com.example.sumdays.network

import com.example.sumdays.daily.memo.AnalysisResponse
import com.example.sumdays.daily.memo.MergeRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response

interface ApiService {
    @POST("merge/")
    suspend fun mergeMemos(@Body req: MergeRequest): retrofit2.Response<com.google.gson.JsonObject>
}

// 응답 DTO (nullable 권장)
data class MergeResponse(
    val merged_memo: String?,
    val count: Int?,
    val end_flag: Boolean?
)
