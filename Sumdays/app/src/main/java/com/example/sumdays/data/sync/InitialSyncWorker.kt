package com.example.sumdays.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sumdays.data.AppDatabase
import com.example.sumdays.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InitialSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1) 서버에서 전체 데이터 가져오기
            val response = ApiClient.api.fetchServerData()   // /api/db/fetchServerData
            if (!response.isSuccessful || response.body() == null) {
                return@withContext Result.failure()
            }

            val serverData = response.body()!!  // SyncFetchResponse

            // 2) Room DB DAO 가져오기
            val db = AppDatabase.getDatabase(applicationContext)
            val memoDao = db.memoDao()
            val dailyDao = db.dailyEntryDao()
            val styleDao = db.userStyleDao()
            val weekDao = db.weekSummaryDao()

            // 3) 트랜잭션으로 Room DB 업데이트
            db.runInTransaction {

                // --- 3-1. 로컬 DB 초기화 ---
                memoDao.clearAll()
                dailyDao.clearAll()
                styleDao.clearAll()
                weekDao.clearAll()

                // --- 3-2. 서버 데이터 삽입 ---
                // Memo
                serverData.memo.forEach { payload ->
                    memoDao.insert(
                        com.example.sumdays.daily.memo.Memo(
                            id = payload.room_id,
                            content = payload.content ?: "",
                            timestamp = payload.timestamp ?: "",
                            date = payload.date,
                            order = payload.memo_order,
                            type = payload.type ?: "text",
                            isEdited = false,
                            isDeleted = false
                        )
                    )
                }

                // DailyEntry
                serverData.dailyEntry.forEach { p ->
                    dailyDao.insert(
                        com.example.sumdays.data.DailyEntry(
                            date = p.date,
                            diary = p.diary,
                            keywords = p.keywords,
                            aiComment = p.aiComment,
                            emotionScore = p.emotionScore,
                            emotionIcon = p.emotionIcon,
                            themeIcon = p.themeIcon,
                            isEdited = false,
                            isDeleted = false
                        )
                    )
                }

                // UserStyle
                serverData.userStyle.forEach { p ->
                    styleDao.insertStyle(
                        com.example.sumdays.data.style.UserStyle(
                            styleId = p.styleId,
                            styleName = p.styleName,
                            styleVector = p.styleVector,
                            styleExamples = p.styleExamples,
                            stylePrompt = p.stylePrompt
                        )
                    )
                }

                // WeekSummary
                serverData.weekSummary.forEach { p ->
                    weekDao.insert(
                        com.example.sumdays.statistics.WeekSummary(
                            startDate = p.startDate,
                            endDate = p.endDate,
                            diaryCount = p.diaryCount ?: 0,
                            emotionAnalysis = p.emotionAnalysis,
                            highlights = p.highlights,
                            insights = p.insights,
                            summary = p.summary,
                            isEdited = false,
                            isDeleted = false
                        )
                    )
                }
            }

            return@withContext Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure()
        }
    }
}
