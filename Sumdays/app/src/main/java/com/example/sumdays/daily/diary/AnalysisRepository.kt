package com.example.sumdays.daily.diary

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.sumdays.network.ApiClient
import com.google.gson.JsonObject

object AnalysisRepository {


    // 메모리 내 캐시 (날짜 String을 키로, AnalysisResponse를 값으로 저장)
    private val analysisCache = mutableMapOf<String, AnalysisResponse>()
    fun getAnalysis(date: String): AnalysisResponse? {
            return analysisCache[date]
    }

    //요청 후 캐시에 저장
    suspend fun requestAnalysis(date: String): AnalysisResponse? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AnalysisRepository", "서버에 '$date' 분석 결과 요청...")
                val diary = DiaryRepository.getDiary(date)
                if (diary == null) {
                    Log.e("AnalysisRepository", "Date cannot be null for analysis request")
                    return@withContext null // Null이면 더 진행하지 않음
                }
                // 2️. 요청 객체 생성
                val request = AnalysisRequest(diary = diary)
                // 3️. API 호출
                val response = ApiClient.api.diaryAnalyze(request)

                val json = response.body() ?: throw IllegalStateException("Empty body")
                val analysis = extractAnalysis(json)
                analysisCache[date] = analysis
                // 4️. 서버가 돌려준 병합 결과 반환
                return@withContext analysis
            } catch (e: Exception) {
                Log.e("AnalysisRepository", "'$date' 분석 결과 요청 중 예외 발생", e)
                null // 네트워크 오류 등 예외 발생
            } as AnalysisResponse?
        }
    }

    private fun extractAnalysis(json: JsonObject): AnalysisResponse {
        // 기본값 설정 (파싱 실패 대비)
        var aiComment: String? = null
        var analysisBlock: AnalysisBlock? = null
        var diary: String? = null
        var entryDate: String? = null
        var icon: String? = null
        var userId: Int? = null

        // "result" 필드가 있는지, JsonObject 타입인지 확인
        if (json.has("result") && json.get("result").isJsonObject) {
            val resultObj = json.getAsJsonObject("result")

            // 각 필드를 안전하게 추출 (getAsString 등은 null일 경우 예외 발생 가능하므로 get() 사용 후 타입 확인)
            aiComment = resultObj.get("ai_comment")?.takeIf { !it.isJsonNull }?.asString
            diary = resultObj.get("diary")?.takeIf { !it.isJsonNull }?.asString
            entryDate = resultObj.get("entry_date")?.takeIf { !it.isJsonNull }?.asString
            icon = resultObj.get("icon")?.takeIf { !it.isJsonNull }?.asString
            userId = resultObj.get("user_id")?.takeIf { !it.isJsonNull }?.asInt

            // "analysis" 블록 파싱
            if (resultObj.has("analysis") && resultObj.get("analysis").isJsonObject) {
                val analysisObj = resultObj.getAsJsonObject("analysis")
                val emotionScore = analysisObj.get("emotion_score")?.takeIf { !it.isJsonNull }?.asDouble
                val keywords = analysisObj.get("keywords")?.takeIf { it.isJsonArray }
                    ?.asJsonArray?.mapNotNull { it?.takeIf { !it.isJsonNull }?.asString }

                analysisBlock = AnalysisBlock(emotionScore, keywords)
            }
        } else {
            println("Warning: 'result' field not found or not an object in the JSON.")
        }

        // 최종 AnalysisResponse 객체 생성 및 반환
        return AnalysisResponse(
            aiComment = aiComment,
            analysis = analysisBlock,
            diary = diary,
            entryDate = entryDate,
            icon = icon,
            userId = userId
        )
    }

    /**
     * 특정 날짜의 캐시된 분석 결과를 삭제합니다. (선택적 기능)
     * 예를 들어, 사용자가 일기를 수정했을 때 이전 분석 결과를 무효화하기 위해 사용할 수 있습니다.
     * @param date 날짜 키
     */
    fun clearCacheForDate(date: String) {
        analysisCache.remove(date)
        Log.d("AnalysisRepository", "'$date' 캐시 삭제됨")
    }

    /**
     * 모든 캐시된 분석 결과를 삭제합니다. (선택적 기능)
     * 예를 들어, 로그아웃 시 호출할 수 있습니다.
     */
    fun clearAllCache() {
        analysisCache.clear()
        Log.d("AnalysisRepository", "모든 분석 캐시 삭제됨")
    }


}
