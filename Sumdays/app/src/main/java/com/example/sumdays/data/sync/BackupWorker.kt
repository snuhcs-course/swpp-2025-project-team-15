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
import com.google.gson.GsonBuilder
import retrofit2.Response
import androidx.work.workDataOf
import com.example.sumdays.auth.SessionManager
import com.example.sumdays.data.dao.MemoDao
import com.example.sumdays.daily.memo.Memo
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.WeekFields
import kotlin.random.Random

suspend fun generateSampleMemos(memoDao: MemoDao) {

    val contents = listOf(
        "아침에 추웠다.",
        "버스가 늦어서 뛰어갔다.",
        "오늘 집중 잘 됐다.",
        "점심은 맛있었다.",
        "카페에서 공부함.",
        "운동하고 개운함.",
        "친구랑 수다 떨었음.",
        "비 와서 산책 못 함.",
        "라면 먹으며 유튜브 봤다.",
        "괜히 우울한 느낌의 하루.",
        "아이디어가 떠올랐다.",
        "귀찮아서 미뤄둠.",
        "영화 보고 쉼.",
        "생산적인 하루.",
        "잠 부족함.",
        "카페인 안 먹힘.",
        "가족과 통화.",
        "몸이 가벼워짐.",
        "상쾌한 산책.",
        "일 처리하고 후련함."
    )

    // 2025-11-17 ~ 2025-11-30
    val allDays = (17..30)

    for (day in allDays) {

        val date = "2025-11-%02d".format(day)  // yyyy-MM-dd

        // --- 1. 랜덤 시간 5개 생성 ---
        val times = (1..5).map {
            val hour = (9..22).random()     // 09~22시
            val minute = (0..59).random()
            hour to minute
        }.sortedWith(
            compareBy({ it.first }, { it.second })  // 시간순 정렬
        )

        // --- 2. 정렬된 순서대로 memo 생성 ---
        times.forEachIndexed { index, (hour, minute) ->
            val memo = Memo(
                content = contents.random(),
                timestamp = "%02d:%02d".format(hour, minute),
                date = date,
                order = index + 1,
                type = "text"
            )
            memoDao.insert(memo)
        }
    }
}


class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        try {
            Log.d("BackupWork","dowork (front to back")

            // 먼저 로그인 되어있는지 검사
            val token = SessionManager.getToken()
            if(token == null) {
                val serverFailData = workDataOf(
                    "type" to "token_error",
                    "message" to "token이 유효하지 않음"
                )
                return@withContext Result.failure(serverFailData)
            }
            val tokenHeader = "Bearer ${token}"

            // 1. dao 초기화
            val db = AppDatabase.getDatabase(applicationContext)
            val memoDao = db.memoDao()
            val userStyleDao = db.userStyleDao()
            val dailyEntryDao = db.dailyEntryDao()
            val weekSummaryDao = db.weekSummaryDao()

            // test code
            // generateSampleMemos(memoDao)
            //

            // testCode
            /*
            memoDao.clearAll()
            userStyleDao.clearAll()
            dailyEntryDao.clearAll()
            weekSummaryDao.clearAll()
            testEntityInsert(true, true, true, false)
            */

            // 2. edited, deleted 객체 가져오기 (memo, userStyle, dailyEntry, weekSummary)
            // 2-1. Memo
            val deletedMemoIds =  memoDao.getDeletedMemos().map { it.id }
            val editedMemos = memoDao.getEditedMemos()
            // 2-2. Style
            val deletedStyleIds = userStyleDao.getDeletedStyles().map { it.styleId }
            val editedStyles = userStyleDao.getEditedStyles()
            // 2-3. Entry
            val deletedEntryDates = dailyEntryDao.getDeletedEntries().map { it.date }
            val editedEntries = dailyEntryDao.getEditedEntries()
            // 2-4. Summary
            val deletedSummaryStartDates = weekSummaryDao.getDeletedSummaries().map { it.startDate }
            val editedSummaryEntities = weekSummaryDao.getEditedSummaries()
            val editedSummaries = editedSummaryEntities.map {it.weekSummary}

            // 3. 서버에 요청하기
            val syncRequest : SyncRequest = buildSyncRequest(deletedMemoIds, deletedStyleIds, deletedEntryDates, deletedSummaryStartDates,
                editedMemos, editedStyles, editedEntries, editedSummaries)
            val syncResponseBody = ApiClient.api.syncData(tokenHeader,syncRequest).body()


            // 임시 테스트 시작
            // printEditedUserStyles(sr = syncRequest)

            // 임시 테스트 종료

            // 4-1. 성공 -> flag 해제
            if (syncResponseBody != null && syncResponseBody.status == "success"){
                val editedMemoIds = editedMemos.map { it.id }
                val editedStyleIds = editedStyles.map { it.styleId }
                val editedEntryDates = editedEntries.map { it.date }
                val editedSummaryStartDates = editedSummaries.map { it.startDate }

                memoDao.resetDeletedFlags(deletedMemoIds)
                memoDao.resetEditedFlags(editedMemoIds)
                userStyleDao.resetDeletedFlags(deletedStyleIds)
                userStyleDao.resetEditedFlags(editedStyleIds)
                dailyEntryDao.resetDeletedFlags(deletedEntryDates)
                dailyEntryDao.resetEditedFlags(editedEntryDates)
                weekSummaryDao.resetDeletedFlags(deletedSummaryStartDates)
                weekSummaryDao.resetEditedFlags(editedSummaryStartDates)

                // test code
                /* 0. testCode
                memoDao.clearAll()
                userStyleDao.clearAll()
                dailyEntryDao.clearAll()
                weekSummaryDao.clearAll()
                */
                ///

                return@withContext Result.success()
            }

            // 4-2. 실패
            else {
                val serverFailData = workDataOf(
                    "type" to "server_error",
                    "message" to (syncResponseBody?.message ?: "서버 응답 없음")
                )
                return@withContext Result.failure(serverFailData)
            }
        } catch (e: Exception) {
            val exceptionData = workDataOf(
                "type" to "exception",
                "message" to (e.message ?: "알 수 없는 오류")
            )
            return@withContext Result.failure(exceptionData)
        }
    }

    private fun logTest(syncRequest: SyncRequest) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(syncRequest)
        Log.d("SyncRequest", json)
    }

    private suspend fun testEntityInsert(memo : Boolean, userStyle : Boolean, dailyEntry: Boolean, weekSummary: Boolean) {
        // test code
        val db = AppDatabase.getDatabase(applicationContext)
        if (memo) {
            val exampleMemo = com.example.sumdays.daily.memo.Memo(
                date = "2025-11-19",
                order = 3,
                content = "테스트 메모 - 백업 검증용",
                timestamp = "21:33"
            )
            db.memoDao().insert(exampleMemo)
        }
        if (dailyEntry) {
            val exampleEntry = com.example.sumdays.data.DailyEntry(
                date = "2025-11-14",
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
                    emotionScore = 0.78,
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
                styleName = "무색무취",
                styleVector = listOf(0.12f, -0.03f, 0.88f, 0.45f),
                styleExamples = listOf(
                    "일어나서 물을 한 잔 마셨다",
                    "조용히 하루가 지나갔다",
                    "일기를 쓰고 자야겠다고 생각했다",
                    "창문을 여니 바깥 공기가 들어왔다"
                ),
                stylePrompt = StylePrompt(
                    character_concept = "일상적인 삶을 살아가는 평범한 사람. 소소한 일상을 관찰하고 기록하는 성향을 가진 인물.",
                    emotional_tone = "감정이 드러나지 않고 중립적인 톤으로, 일상적인 사건을 기록하는 데 집중한다.",
                    formality = "비격식적인 대화체로, 자연스러운 흐름을 유지하며 친근한 느낌을 준다.",
                    lexical_choice = "일상적인 단어와 표현을 사용하여 친근함을 느끼게 한다.",
                    pacing = "느긋하고 여유로운 흐름, 빠르지 않게 사건을 나열.",
                    punctuation_style = "기본적인 문장 부호 사용, 복잡한 구두점은 없다.",
                    sentence_endings = listOf("~었다.", "~했다.", "~었다고 생각했다."),
                    sentence_length = "중간 길이의 문장들이 많으며, 간결하게 표현되어 있다.",
                    sentence_structure = "주어-서술어 구조가 명확하며, 문장이 단순하고 직관적이다.",
                    special_syntax = "일상적인 표현을 그대로 사용하며, 특별한 구문은 없음.",
                    speech_quirks = "특별한 말투의 버릇은 없으며, 대화체적인 표현이 자연스럽다.",
                    tone = "담담하고 차분한 어조로 일상의 소소한 사건들을 서술."
                ),
            )
            db.userStyleDao().insertStyle(exampleUserStyle)
        }
    }
}
