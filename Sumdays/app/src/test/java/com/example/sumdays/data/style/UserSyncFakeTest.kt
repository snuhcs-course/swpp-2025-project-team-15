package com.example.sumdays

import com.example.sumdays.data.sync.*
import com.example.sumdays.daily.memo.Memo
import com.example.sumdays.data.DailyEntry
import com.example.sumdays.data.style.UserStyle
import com.example.sumdays.statistics.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 완전 Fake 환경에서 SyncDto / buildSyncRequest / 데이터 구조 전체를 커버하는 테스트
 * Room, Retrofit, Worker, SessionManager 어떤 것도 필요 없음
 */
class UserSyncFakeTest {

    @Test
    fun `deletedPayload 생성 테스트`() {
        val deleted = DeletedPayload(
            memo = listOf(1,2),
            userStyle = listOf(10L),
            dailyEntry = listOf("2025-01-01"),
            weekSummary = listOf("2025-01-07")
        )

        assertEquals(2, deleted.memo!!.size)
        assertEquals(10L, deleted.userStyle!!.first())
        assertEquals("2025-01-01", deleted.dailyEntry!!.first())
        assertEquals("2025-01-07", deleted.weekSummary!!.first())
    }

    @Test
    fun `editedPayload 생성 테스트`() {
        val memo = Memo(
            id = 1,
            content = "hi",
            timestamp = "10:00",
            date = "2025-01-01",
            order = 0
        )

        val entry = DailyEntry(
            date = "2025-01-02",
            diary = "text",
            keywords = "a;b",
            aiComment = "cmt",
            emotionScore = 0.3,
            emotionIcon = "😀",
            themeIcon = "🌙"
        )

        val style = UserStyle(
            styleId = 1L,
            styleName = "테스트",
            styleVector = listOf(0.1f),
            styleExamples = listOf("ex"),
            stylePrompt = com.example.sumdays.data.style.StylePrompt(
                character_concept = "a", emotional_tone = "b", formality = "c",
                lexical_choice = "d", pacing = "e", punctuation_style = "f",
                sentence_endings = listOf("x"), sentence_length = "l",
                sentence_structure = "s", special_syntax = "ss",
                speech_quirks = "qq", tone = "tt"
            ),
            sampleDiary = "d"
        )

        val summary = WeekSummary(
            startDate = "2025-01-03",
            endDate = "2025-01-09",
            diaryCount = 3,
            emotionAnalysis = EmotionAnalysis(
                distribution = mapOf("positive" to 1),
                dominantEmoji = "😀",
                emotionScore = 0.8
            ),
            highlights = listOf(Highlight("2025-01-05","good")),
            insights = Insights("nice","trend"),
            summary = SummaryDetails(listOf("t"),"ov","ti")
        )

        val edited = EditedPayload(
            memo = listOf(
                MemoPayload(
                    room_id = memo.id,
                    content = memo.content,
                    timestamp = memo.timestamp,
                    date = memo.date,
                    memo_order = memo.order,
                    type = memo.type
                )
            ),
            userStyle = listOf(
                UserStylePayload(
                    styleId = style.styleId,
                    styleName = style.styleName,
                    styleVector = style.styleVector,
                    styleExamples = style.styleExamples,
                    stylePrompt = style.stylePrompt,
                    sampleDiary = style.sampleDiary
                )
            ),
            dailyEntry = listOf(
                DailyEntryPayload(
                    date = entry.date,
                    diary = entry.diary,
                    keywords = entry.keywords,
                    aiComment = entry.aiComment,
                    emotionScore = entry.emotionScore,
                    emotionIcon = entry.emotionIcon,
                    themeIcon = entry.themeIcon
                )
            ),
            weekSummary = listOf(
                WeekSummaryPayload(
                    startDate = summary.startDate,
                    endDate = summary.endDate,
                    diaryCount = summary.diaryCount,
                    emotionAnalysis = summary.emotionAnalysis,
                    highlights = summary.highlights,
                    insights = summary.insights,
                    summary = summary.summary
                )
            )
        )

        assertEquals("hi", edited.memo!!.first().content)
        assertEquals("테스트", edited.userStyle!!.first().styleName)
        assertEquals("2025-01-02", edited.dailyEntry!!.first().date)
        assertEquals("2025-01-03", edited.weekSummary!!.first().startDate)
    }

    @Test
    fun `buildSyncRequest 정상 생성`() = runBlocking {
        val memo = Memo(
            id = 1, content = "c", timestamp = "t",
            date = "2025-01-01", order = 0
        )

        val entry = DailyEntry(
            date = "2025-01-02",
            diary = "d",
            keywords = "k",
            aiComment = "a",
            emotionScore = 0.1,
            emotionIcon = "😀",
            themeIcon = "🌙"
        )

        val style = UserStyle(
            styleId = 1L,
            styleName = "스타일",
            styleVector = listOf(1f),
            styleExamples = listOf("a"),
            stylePrompt = com.example.sumdays.data.style.StylePrompt(
                character_concept = "a", emotional_tone = "b", formality = "c",
                lexical_choice = "d", pacing = "e", punctuation_style = "f",
                sentence_endings = listOf("x"), sentence_length = "l",
                sentence_structure = "s", special_syntax = "ss",
                speech_quirks = "qq", tone = "tt"
            ),
            sampleDiary = "s"
        )

        val summary = WeekSummary(
            startDate = "2025-01-03",
            endDate = "2025-01-09",
            diaryCount = 2,
            emotionAnalysis = EmotionAnalysis(
                distribution = mapOf("positive" to 1),
                dominantEmoji = "😀",
                emotionScore = 0.5
            ),
            highlights = listOf(Highlight("2025-01-04","good")),
            insights = Insights("a","b"),
            summary = SummaryDetails(listOf("z"),"ov","ti")
        )

        val req = buildSyncRequest(
            deletedMemoIds = listOf(1),
            deletedStyleIds = listOf(2),
            deletedEntryDates = listOf("2025-01-10"),
            deletedSummaryStartDates = listOf("2025-01-20"),

            editedMemos = listOf(memo),
            editedStyles = listOf(style),
            editedEntries = listOf(entry),
            editedSummaries = listOf(summary)
        )

        assertNotNull(req.deleted)
        assertNotNull(req.edited)
        assertEquals(1, req.deleted!!.memo!!.first())
        assertEquals("스타일", req.edited!!.userStyle!!.first().styleName)
    }

    @Test
    fun `SyncResponse 값 확인`() {
        val res = SyncResponse("success","ok")
        assertEquals("success", res.status)
        assertEquals("ok", res.message)
    }
}
