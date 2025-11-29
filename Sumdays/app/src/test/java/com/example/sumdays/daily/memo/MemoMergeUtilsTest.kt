package com.example.sumdays.daily.memo

import com.example.sumdays.data.style.StylePrompt
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class MemoMergeUtilsTest {
    @Test
    fun convertStylePromptToMap_convertsAllFieldsCorrectly() {
        // given
        val prompt = StylePrompt(
            character_concept = "일상적인 삶을 살아가는 평범한 직장인으로, 규칙적인 생활 패턴과 소소한 행복을 추구하는 성격을 가진 캐릭터.",
            emotional_tone = "편안하고 소소한 일상의 즐거움과 아쉬움을 표현",
            formality = "비격식적이고 친근한 표현",
            lexical_choice = "일상적인 어휘 사용, 전문 용어 없음, 친근한 표현",
            pacing = "부드럽고 자연스러운 흐름",
            punctuation_style = "기본적인 문장부호 사용, 과도한 구두점 사용 없음",
            sentence_endings = listOf("~다", "~았다", "~었다"),
            sentence_length = "중간 길이의 문장으로 구성",
            sentence_structure = "주어-서술어-목적어 구조로 간결하게 서술",
            special_syntax = "특별한 구문 사용 없음",
            speech_quirks = "간결하고 명료한 표현, 감정의 과장 없음",
            tone = "담담하고 소박한 일상 회고"
        )

        // when
        val map = MemoMergeUtils.convertStylePromptToMap(prompt)

        // then
        // 기본 문자열 필드들
        assertEquals("일상적이고 솔직한 톤", map["tone"])
        assertEquals("구어체", map["formality"])
        assertEquals("짧음", map["sentence_length"])
        assertEquals("단문 위주", map["sentence_structure"])
        assertEquals("편안하고 일상적인 단어", map["lexical_choice"])
        assertEquals("약간 무기력", map["emotional_tone"])
        assertEquals("거의 없음", map["irony_or_sarcasm"])
        assertEquals("반말", map["slang_or_dialect"])
        assertEquals("보통", map["pacing"])

        // 리스트 필드들
        @Suppress("UNCHECKED_CAST")
        val endings = map["sentence_endings"] as List<*>
        assertTrue(endings.contains("~다"))
        assertTrue(endings.contains("~네"))
        assertTrue(endings.contains("~야"))

        @Suppress("UNCHECKED_CAST")
        val phrases = map["common_phrases"] as List<*>
        assertTrue(phrases.contains("그냥"))
        assertTrue(phrases.contains("아무래도"))
    }

    @Test
    fun extractMergedText_returnsDiaryWhenResultDiaryPresent() {
        // given
        val resultObj = JsonObject().apply {
            add("diary", JsonPrimitive("오늘의 일기입니다."))
        }
        val root = JsonObject().apply {
            add("result", resultObj)
        }

        // when
        val text = MemoMergeUtils.extractMergedText(root)

        // then
        assertEquals("오늘의 일기입니다.", text)
    }

    @Test
    fun extractMergedText_returnsMergedContentWhenNoDiary() {
        // given
        val inner = JsonObject().apply {
            add("merged_content", JsonPrimitive("중간 병합 결과입니다."))
        }
        val root = JsonObject().apply {
            add("merged_content", inner)
        }

        // when
        val text = MemoMergeUtils.extractMergedText(root)

        // then
        assertEquals("중간 병합 결과입니다.", text)
    }

    @Test
    fun extractMergedText_prefersDiaryOverMergedContentWhenBothExist() {
        // given
        val resultObj = JsonObject().apply {
            add("diary", JsonPrimitive("최종 일기 우선"))
        }
        val mergedInner = JsonObject().apply {
            add("merged_content", JsonPrimitive("이건 무시되어야 함"))
        }

        val root = JsonObject().apply {
            add("result", resultObj)
            add("merged_content", mergedInner)
        }

        // when
        val text = MemoMergeUtils.extractMergedText(root)

        // then
        assertEquals("최종 일기 우선", text)
    }

    @Test
    fun extractMergedText_returnsEmptyStringWhenNoValidFields() {
        // given
        val root = JsonObject().apply {
            add("something_else", JsonPrimitive("값"))
        }

        // when
        val text = MemoMergeUtils.extractMergedText(root)

        // then
        assertEquals("", text)
    }

    @Test
    fun extractMergedText_returnsEmptyStringWhenJsonEmpty() {
        // given
        val root = JsonObject()

        // when
        val text = MemoMergeUtils.extractMergedText(root)

        // then
        assertEquals("", text)
    }

}
