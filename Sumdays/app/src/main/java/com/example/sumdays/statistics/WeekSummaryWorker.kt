package com.example.sumdays.statistics

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sumdays.MyApplication
import org.threeten.bp.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import com.example.sumdays.daily.diary.AnalysisRepository
import com.example.sumdays.data.DailyEntry
import com.example.sumdays.network.ApiClient
import com.example.sumdays.network.DiaryItem
import com.example.sumdays.network.WeekAnalysisRequest

class WeekSummaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("WeekSummaryWorker", "🔄 주간 요약 생성 작업 시작 (스케줄러에 의해 실행됨)")

            // 1. 날짜 범위 계산: '지난주 월요일' ~ '지난주 일요일'
            // 이 작업은 '이번주 월요일' 00:00 이후에 실행된다고 가정합니다.
            val today = LocalDate.now()

            // 어제(일요일)가 포함된 주의 월요일을 찾습니다.
            // 예: 오늘이 11월 24일(월)이라면 -> 어제는 23일(일) -> 지난주 월요일은 17일
            val lastSunday = today.minusDays(1)
            val lastMonday = today.minusDays(7)

            val startDateStr = lastMonday.toString()
            val endDateStr = lastSunday.toString()

            Log.d("WeekSummaryWorker", "📅 분석 대상 기간: $lastMonday ~ $lastSunday")

            // 2. Repository 접근
            // (Hilt와 같은 DI를 안 쓰므로 Application을 캐스팅해서 가져옵니다)
            val app = applicationContext as MyApplication
            val repository = app.dailyEntryRepository
            val weekSummaryRepo = app.weekSummaryRepository

            // 3. DB에서 작성된 모든 날짜 목록 가져오기
            val allDates = repository.getAllWrittenDates() // List<String> "YYYY-MM-DD"
            val diaryList = mutableListOf<DailyEntry>()
            val idx = 0

            for (dateStr in allDates) {

                try {
                    val date = LocalDate.parse(dateStr)
                    if (!date.isBefore(lastMonday) && !date.isAfter(lastSunday)) {
                        val content = repository.getEntrySnapshot(dateStr)
                        if (content != null) {
                            diaryList.add(content)
                        }
                    }
                } catch (e: Exception) { continue }
            }

            // 4. 기간 내 작성된 일기 개수 카운트
            val count = diaryList.size

            Log.d("WeekSummaryWorker", "📊 지난주($lastMonday ~ $lastSunday) 작성된 일기 개수: $count 개")

            // 5. 조건 체크 (3개 이상)
            if (count >= 3) {
                Log.d("WeekSummaryWorker", "✅ 조건 충족! (3개 이상). AI 요약 요청 로직이 필요합니다.")

                // 5. ⭐ [직접 처리] 요청 데이터 생성 (DailyEntry -> DiaryRequestItem)
                val requestItems = diaryList.map { entry ->
                    DiaryItem(
                        date = entry.date,
                        diary = entry.diary,
                        emoji = entry.emotionIcon, // DB에 정보가 없다면 null
                        emotionScore = entry.emotionScore
                    )
                }
                val request = WeekAnalysisRequest(diaries = requestItems)

                // 6. ⭐ [직접 처리] API 호출 (ApiClient 직접 사용)
                try {
                    val response = ApiClient.api.summarizeWeek(request)

                    if (response.isSuccessful && response.body()?.success == true) {
                        val result = response.body()!!.result!!

                        // 7. ⭐ [직접 처리] 응답 변환 (DTO -> Entity)
                        val summary = result.toWeekSummary(startDateStr, endDateStr, count)

                        // 8. DB 저장
                        weekSummaryRepo.upsertWeekSummary(summary)
                        Log.d("WeekSummaryWorker", "🎉 주간 요약 저장 완료: ${summary.summary.title}")

                    } else {
                        Log.e("WeekSummaryWorker", "❌ AI 분석 실패: ${response.code()} ${response.errorBody()?.string()}")
                        return Result.retry() // 서버 오류면 나중에 재시도
                    }
                } catch (e: Exception) {
                    Log.e("WeekSummaryWorker", "❌ 네트워크 통신 중 오류: ${e.message}")
                    return Result.retry()
                }

            } else {
                Log.d("WeekSummaryWorker", "⚠️ 일기 부족 ($count/3). 요약을 생성하지 않습니다.")
            }

            Result.success()

        } catch (e: Exception) {
            Log.e("WeekSummaryWorker", "❌ 작업 중 오류 발생", e)
            Result.retry() // 오류 발생 시 나중에 다시 시도
        }
    }
}