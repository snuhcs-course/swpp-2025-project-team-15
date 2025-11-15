package com.example.sumdays.settings.prefs

import android.content.Context
import android.content.SharedPreferences
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class UserStatsPrefsTest {

    // 의존성 모의 처리
    @MockK
    private lateinit var context: Context
    @MockK
    private lateinit var sharedPreferences: SharedPreferences
    @MockK(relaxUnitFun = true) // editor.apply()가 Unit을 반환하므로 relax
    private lateinit var editor: SharedPreferences.Editor

    // 테스트 대상 클래스 (Class Under Test)
    private lateinit var userStatsPrefs: UserStatsPrefs

    // SharedPreferences 키 (테스트 검증을 위해 원본 클래스에서 가져옴)
    companion object {
        private const val APP_PREFS_NAME = "user_stats_prefs"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_STREAK = "current_streak"
        private const val KEY_LEAF_COUNT = "leaf_count"
        private const val KEY_GRAPE_COUNT = "grape_count"
        private const val KEY_ACTIVE_STYLE_ID = "active_style_id"
        private const val DEFAULT_STYLE_ID = -1L
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        // --- 모의 객체 체이닝 설정 ---

        // 1. context.getSharedPreferences(...)가 mock SharedPreferences를 반환하도록 설정
        every {
            context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
        } returns sharedPreferences

        // 2. sharedPreferences.edit()가 mock Editor를 반환하도록 설정
        every { sharedPreferences.edit() } returns editor

        // 3. editor.put...(...) 메서드들이 체이닝(.apply() 호출)을 위해
        //    자기 자신(editor)을 반환하도록 설정
        every { editor.putString(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor

        // 4. 테스트 대상 클래스 초기화
        userStatsPrefs = UserStatsPrefs(context)
    }

    // --- 닉네임 테스트 ---

    @Test
    fun `saveNickname - saves correct value to prefs`() {
        val testNickname = "TestUser"

        // 실행
        userStatsPrefs.saveNickname(testNickname)

        // 검증 (Editor가 올바른 키/값으로 호출되었는지 확인)
        verify { editor.putString(KEY_NICKNAME, testNickname) }
        verify { editor.apply() } // apply()가 호출되었는지 확인
    }

    @Test
    fun `getNickname - returns saved value`() {
        val savedNickname = "MyUser"
        // 모의 설정 (Prefs가 "MyUser"를 반환하도록)
        every { sharedPreferences.getString(KEY_NICKNAME, "사용자") } returns savedNickname

        // 실행
        val nickname = userStatsPrefs.getNickname()

        // 검증
        assertEquals(savedNickname, nickname)
    }

    @Test
    fun `getNickname - returns default value when empty`() {
        val defaultNickname = "사용자"
        // 모의 설정 (Prefs가 기본값("사용자")을 반환하도록)
        every { sharedPreferences.getString(KEY_NICKNAME, defaultNickname) } returns defaultNickname

        // 실행
        val nickname = userStatsPrefs.getNickname()

        // 검증
        assertEquals(defaultNickname, nickname)
    }

    @Test
    fun `getNickname - returns default value when prefs return null`() {
        // 원본 코드의 '?: "사용자"' 폴백 로직 테스트
        every { sharedPreferences.getString(KEY_NICKNAME, "사용자") } returns null

        // 실행
        val nickname = userStatsPrefs.getNickname()

        // 검증
        assertEquals("사용자", nickname)
    }

    // --- Streak 테스트 ---

    @Test
    fun `saveStreak - saves correct value`() {
        // 실행
        userStatsPrefs.saveStreak(10)

        // 검증
        verify { editor.putInt(KEY_STREAK, 10) }
        verify { editor.apply() }
    }

    @Test
    fun `getStreak - returns saved value`() {
        // 모의 설정
        every { sharedPreferences.getInt(KEY_STREAK, 0) } returns 5

        // 실행
        val streak = userStatsPrefs.getStreak()

        // 검증
        assertEquals(5, streak)
    }

    // --- Leaf Count (증가 로직) 테스트 ---

    @Test
    fun `incrementLeafCount - reads current value and writes incremented value`() {
        // 모의 설정 (현재 값 5를 반환)
        every { sharedPreferences.getInt(KEY_LEAF_COUNT, 0) } returns 5

        // 실행
        userStatsPrefs.incrementLeafCount()

        // 검증 (5 + 1 = 6이 저장되었는지 확인)
        verify { editor.putInt(KEY_LEAF_COUNT, 6) }
        verify { editor.apply() }
    }

    // --- Grape Count (증가 로직) 테스트 ---

    @Test
    fun `incrementGrapeCount - reads current value and writes incremented value`() {
        // 모의 설정 (현재 값 2를 반환)
        every { sharedPreferences.getInt(KEY_GRAPE_COUNT, 0) } returns 2

        // 실행
        userStatsPrefs.incrementGrapeCount()

        // 검증 (2 + 1 = 3이 저장되었는지 확인)
        verify { editor.putInt(KEY_GRAPE_COUNT, 3) }
        verify { editor.apply() }
    }

    // --- ★ AI 스타일 ID 테스트 ★ ---

    @Test
    fun `saveActiveStyleId - saves correct ID`() {
        val styleId = 12345L

        // 실행
        userStatsPrefs.saveActiveStyleId(styleId)

        // 검증
        verify { editor.putLong(KEY_ACTIVE_STYLE_ID, styleId) }
        verify { editor.apply() }
    }

    @Test
    fun `getActiveStyleId - returns saved ID when it exists`() {
        val styleId = 12345L
        // 모의 설정
        every { sharedPreferences.getLong(KEY_ACTIVE_STYLE_ID, DEFAULT_STYLE_ID) } returns styleId

        // 실행
        val activeId = userStatsPrefs.getActiveStyleId()

        // 검증
        assertEquals(styleId, activeId)
    }

    @Test
    fun `getActiveStyleId - returns null when no ID is set (default)`() {
        // 모의 설정 (기본값 -1L이 반환되는 상황)
        every { sharedPreferences.getLong(KEY_ACTIVE_STYLE_ID, DEFAULT_STYLE_ID) } returns DEFAULT_STYLE_ID

        // 실행
        val activeId = userStatsPrefs.getActiveStyleId()

        // 검증 (ID가 -1L일 때 null을 반환하는 핵심 로직 테스트)
        assertNull(activeId)
    }

    @Test
    fun `clearActiveStyleId - removes the key from prefs`() {
        // 실행
        userStatsPrefs.clearActiveStyleId()

        // 검증 (remove가 호출되었는지 확인)
        verify { editor.remove(KEY_ACTIVE_STYLE_ID) }
        verify { editor.apply() }
    }
}