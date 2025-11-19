package com.example.sumdays.network

import com.example.sumdays.daily.diary.AnalysisRequest
import com.example.sumdays.daily.memo.MergeRequest
import com.example.sumdays.data.sync.SyncRequest
import com.example.sumdays.data.sync.SyncResponse
import com.example.sumdays.data.sync.SyncFetchResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Streaming

interface ApiService {
    @POST("/api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
    // ★★★ 회원가입 API 함수 추가 ★★★
    @POST("/api/auth/signup")
    fun signup(@Body request: SignupRequest): Call<SignupResponse>

    // 1. 닉네임 변경
    @PUT("api/auth/nickname")
    fun updateNickname(@Header("Authorization") token: String, @Body request: UpdateNicknameRequest): Call<UpdateNicknameResponse>

    // 2. 비밀번호 변경
    @PUT("api/auth/password")
    fun changePassword(@Header("Authorization") token: String, @Body request: ChangePasswordRequest): Call<ChangePasswordResponse>

    @POST("/api/db/sync")
    suspend fun syncData(
        @Header("Authorization") token: String,
        @Body body: SyncRequest
    ): retrofit2.Response<SyncResponse>

    @GET("/api/db/sync")
    suspend fun fetchServerData(
        @Header("Authorization") token: String
    ): retrofit2.Response<SyncFetchResponse>


    @POST("/api/ai/merge")
    suspend fun mergeMemos(@Body req: MergeRequest): retrofit2.Response<com.google.gson.JsonObject>
    @Streaming
    @POST("/api/ai/merge")
    fun mergeMemosStream(@Body request: MergeRequest): Call<ResponseBody>
    @Multipart
    @POST("/api/ai/stt/memo") // 서버의 STT 엔드포인트 경로
    fun transcribeAudio(
        // 'audio'는 서버에서 파일을 받을 때 사용할 필드 이름
        @Part audio: MultipartBody.Part
    ): Call<STTResponse>
    @Multipart
    @POST("/api/ai/ocr/memo")
    fun extractTextFromImage(
        @Part image: MultipartBody.Part,
        // ★★★ 'type' 필드 추가 (RequestBody 형태) ★★★
        @Part("type") type: RequestBody
    ): Call<OcrResponse>
    @POST("/api/ai/analyze")
    suspend fun diaryAnalyze(@Body req: AnalysisRequest): retrofit2.Response<com.google.gson.JsonObject>

    @Multipart
    @POST("/api/ai/extract-style")
    fun extractStyle(
        // 1. diaries: JSON 배열을 담은 하나의 RequestBody 파트 (String으로 변환하여 사용)
        @Part("diaries") diaries: RequestBody,
        // 2. images: MultipartBody.Part 리스트 (파일명과 함께 전송)
        @Part images: List<MultipartBody.Part>
    ): Call<StyleExtractionResponse>
}

// 응답 DTO (nullable 권장)
data class MergeResponse(
    val merged_memo: String?,
    val count: Int?,
    val end_flag: Boolean?
)
