package com.example.sumdays.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import com.google.gson.Gson

// --- 너의 엔티티 & DAO 관련 ---
import com.example.sumdays.data.AppDatabase
import com.example.sumdays.daily.memo.Memo
import com.example.sumdays.data.DailyEntry
import com.example.sumdays.data.WeekSummaryEntity
import com.example.sumdays.data.style.StylePrompt
import com.example.sumdays.data.style.UserStyle
import com.example.sumdays.statistics.*

// --- 네트워크 ---
import com.example.sumdays.network.ApiClient

class InitialSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private fun convertToWeekSummary(p: WeekSummaryPayload, gson: Gson): WeekSummary {
        val emotionAnalysis = gson.fromJson(
            gson.toJson(p.emotionAnalysis),
            EmotionAnalysis::class.java
        )

        val highlights = gson.fromJson(
            gson.toJson(p.highlights),
            Array<Highlight>::class.java
        ).toList()

        val insights = gson.fromJson(
            gson.toJson(p.insights),
            Insights::class.java

        )

        val summary = gson.fromJson(
            gson.toJson(p.summary),
            SummaryDetails::class.java
        )

        return WeekSummary(
            startDate = p.startDate,
            endDate = p.endDate,
            diaryCount = p.diaryCount ?: 0,
            emotionAnalysis = emotionAnalysis,
            highlights = highlights,
            insights = insights,
            summary = summary
        )
    }


    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("initWork","dowork (back to frontr")
        try {
            // 1) 서버에서 전체 데이터 가져오기
            val response = ApiClient.api.fetchServerData()
            if (!response.isSuccessful || response.body() == null) {
                return@withContext Result.failure()
            }
            val serverData = response.body()!!

            // 2) Room DB
            val db = AppDatabase.getDatabase(applicationContext)
            val memoDao = db.memoDao()
            val dailyDao = db.dailyEntryDao()
            val styleDao = db.userStyleDao()
            val weekDao = db.weekSummaryDao()

            // 3) 트랜잭션으로 Room DB 업데이트
            db.withTransaction {

                // --- 3-1. 로컬 DB 초기화 ---
                memoDao.clearAll()
                dailyDao.clearAll()
                styleDao.clearAll()
                weekDao.clearAll()

                // --- 3-2. 서버 데이터 삽입 ---
                // Memo
                val memoList = serverData.memo.map { payload ->
                    Memo(
                        id = payload.room_id,
                        content = payload.content ?: "",
                        timestamp = payload.timestamp ?: "",
                        date = payload.date,
                        order = payload.memo_order,
                        type = payload.type ?: "text",
                        isEdited = false,
                        isDeleted = false
                    )
                }
                memoDao.insertAll(memoList)

                // DailyEntry
                val dailyEntryList = serverData.dailyEntry.map { p ->
                    DailyEntry(
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
                }
                dailyDao.insertAll(dailyEntryList)

                // UserStyle
                val gson = Gson()
                val styleList = serverData.userStyle.map { p ->
                    val stylePrompt = gson.fromJson(
                        gson.toJson(p.stylePrompt),
                        StylePrompt::class.java
                    )
                    UserStyle(
                        styleId = p.styleId,
                        styleName = p.styleName,
                        styleVector = p.styleVector,
                        styleExamples = p.styleExamples,
                        stylePrompt = stylePrompt,
                        isEdited = false,
                        isDeleted = false
                    )
                }
                styleDao.insertAll(styleList)

                // WeekSummary
                val weekSummaryList = serverData.weekSummary.map { payload ->
                    WeekSummaryEntity(
                        startDate = payload.startDate,
                        weekSummary = convertToWeekSummary(payload, gson),
                        isEdited = false,
                        isDeleted = false
                    )
                }
                weekDao.insertAll(weekSummaryList)


            }

            return@withContext Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure()
        }
    }
}
