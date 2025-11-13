package com.example.sumdays.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sumdays.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.sumdays.data.DailyEntry
import com.example.sumdays.data.style.StylePrompt
import com.example.sumdays.data.style.UserStyle
import com.example.sumdays.statistics.WeekSummary
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.example.sumdays.network.ApiClient
import retrofit2.Response


class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 0. testCode
            testEntityInsert(false,false,false,false)

            // 1. dao 초기화
            val db = AppDatabase.getDatabase(applicationContext)
            val memoDao = db.memoDao()
            val userStyleDao = db.userStyleDao()
            val dailyEntryDao = db.dailyEntryDao()
            val weekSummaryDao = db.weekSummaryDao()

            // 2. edited, deleted 객체 가져오기 (memo, userStyle, dailyEntry, weekSummary)
            val deletedMemoIds =  memoDao.getDeletedMemos().map { it.id }
            val editedMemos = memoDao.getEditedMemos()
            val editedMemoIds = editedMemos.map { it.id }

            val deletedStyleIds = userStyleDao.getDeletedStyles().map { it.styleId }
            val editedStyles = userStyleDao.getEditedStyles()
            val editedStyleIds = editedStyles.map { it.styleId }

            val deletedEntryDates = dailyEntryDao.getDeletedEntries().map { it.date }
            val editedEntries = dailyEntryDao.getEditedEntries()
            val editedEntryDates = editedEntries.map { it.date }

            val deletedSummaryStartDates = weekSummaryDao.getDeletedSummaries().map { it.startDate }
            val editedSummaryEntities = weekSummaryDao.getEditedSummaries()
            val editedSummaries = editedSummaryEntities.map {it.weekSummary}
            val editedSummaryStartDates = editedSummaries.map { it.startDate }

            // 3. 서버에 요청하기
            val syncRequest : SyncRequest = buildSyncRequest(deletedMemoIds, deletedStyleIds, deletedEntryDates, deletedSummaryStartDates,
                editedMemos, editedStyles, editedEntries, editedSummaries)
            val response: Response<SyncResponse>  = ApiClient.api.syncData(syncRequest)
            logTest(syncRequest)


            // 4. 서버에 응닫 받으면, flag 초기화
            memoDao.resetDeletedFlags(deletedMemoIds)
            memoDao.resetEditedFlags(editedMemoIds)
            userStyleDao.resetDeletedFlags(deletedStyleIds)
            userStyleDao.resetEditedFlags(editedStyleIds)
            dailyEntryDao.resetDeletedFlags(deletedEntryDates)
            dailyEntryDao.resetEditedFlags(editedEntryDates)
            weekSummaryDao.resetDeletedFlags(deletedSummaryStartDates)
            weekSummaryDao.resetEditedFlags(editedSummaryStartDates)

            Result.success()
        } catch (e: Exception) {
            Log.e("BackupWorker", "백업 실패: ${e.message}")
            Result.retry()
        }
    }

    private fun logTest(syncRequest: SyncRequest) {
        Log.d("BackupWorker", syncRequest.toString())
    }

    private suspend fun testEntityInsert(memo : Boolean, userStyle : Boolean, dailyEntry: Boolean, weekSummary: Boolean) {
        // test code
        val db = AppDatabase.getDatabase(applicationContext)
        if (memo) {
            val exampleMemo = com.example.sumdays.daily.memo.Memo(
                id = 0,
                date = "2022/01",
                order = 3,
                content = "테스트 메모 - 백업 검증용",
                timestamp = System.currentTimeMillis().toString(),
            )
            db.memoDao().insert(exampleMemo)
        }
        if (dailyEntry) {
            val exampleEntry = com.example.sumdays.data.DailyEntry(
                date = "2025-11-11",
                diary = "오늘은 Room 백업 기능을 테스트했다.",
                keywords = "테스트;백업;Room",
                aiComment = "테스트용으로 삽입된 일기입니다.",
                emotionScore = 0.87,
                emotionIcon = "😊",
                themeIcon = "🌙",
                isEdited = true,
                isDeleted = false
            )
            db.dailyEntryDao().updateEntry(
                date = exampleEntry.date,
                diary = exampleEntry.diary,
                keywords = exampleEntry.keywords,
                aiComment = exampleEntry.aiComment,
                emotionScore = exampleEntry.emotionScore,
                emotionIcon = exampleEntry.emotionIcon,
                themeIcon = exampleEntry.themeIcon
            )
        }
        if (weekSummary) {
            val exampleSummary = WeekSummary(
                startDate = "2025-11-03",
                endDate = "2025-11-09",
                diaryCount = 5,
                emotionAnalysis = com.example.sumdays.statistics.EmotionAnalysis(
                    distribution = mapOf("positive" to 3, "neutral" to 1, "negative" to 1),
                    dominantEmoji = "😄",
                    emotionScore = 0.78f,
                    trend = "increasing"
                ),
                highlights = listOf(
                    com.example.sumdays.statistics.Highlight(
                        date = "2025-11-05",
                        summary = "긍정적인 감정이 지속된 한 주였다."
                    ),
                    com.example.sumdays.statistics.Highlight(
                        date = "2025-11-07",
                        summary = "테스트 데이터를 기반으로 주간 요약을 생성했다."
                    )
                ),
                insights = com.example.sumdays.statistics.Insights(
                    advice = "감정의 흐름을 잘 유지해 보세요.",
                    emotionCycle = "감정 변동이 완화되는 경향"
                ),
                summary = com.example.sumdays.statistics.SummaryDetails(
                    emergingTopics = listOf("테스트", "백업", "RoomDB"),
                    overview = "테스트 주간의 주요 활동을 정리함.",
                    title = "테스트 주간 요약"
                )
            )
            db.weekSummaryDao().upsert(exampleSummary)
        }
        if (userStyle) {
            val exampleUserStyle = UserStyle(
                styleName = "시니컬 스타일",
                styleVector = listOf(0.12f, -0.03f, 0.88f, 0.45f),
                styleExamples = listOf(
                    "세상일 다 그런 거지.",
                    "기대하지 않으면 실망도 없지.",
                    "어차피 다 똑같잖아."
                ),
                stylePrompt = StylePrompt(
                    common_phrases = listOf("그런 거지", "어차피", "그래봤자"),
                    emotional_tone = "냉소적이고 거리감 있는",
                    formality = "비격식체",
                    irony_or_sarcasm = "자주 사용함",
                    lexical_choice = "일상적 단어, 약간의 비꼼 포함",
                    pacing = "느릿하고 여유로운 리듬",
                    sentence_endings = listOf("지", "잖아", "거지"),
                    sentence_length = "짧은 문장이 많음",
                    sentence_structure = "단문 위주, 간결함",
                    slang_or_dialect = "일부 구어체 사용",
                    tone = "시니컬함과 무심함이 섞인 어조"
                ),
            )
            db.userStyleDao().insertStyle(exampleUserStyle)
        }
    }
}
