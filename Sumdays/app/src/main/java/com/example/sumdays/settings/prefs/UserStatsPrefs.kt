package com.example.sumdays.settings.prefs

import android.content.Context
import android.content.SharedPreferences

class UserStatsPrefs(context: Context) {

    // 1. 상수 정의
    companion object {
        private const val APP_PREFS_NAME = "user_stats_prefs" // SharedPreferences 파일 이름
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_STREAK = "current_streak"
        private const val KEY_LEAF_COUNT = "leaf_count"
        private const val KEY_GRAPE_COUNT = "grape_count"
    }

    // SharedPreferences 인스턴스 초기화
    private val prefs: SharedPreferences =
        context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)

    // --- 닉네임 관리 ---

    fun saveNickname(nickname: String) {
        prefs.edit().putString(KEY_NICKNAME, nickname).apply()
    }

    fun getNickname(): String {
        // 기본값은 "사용자" 또는 앱에서 정의한 익명 값으로 설정
        return prefs.getString(KEY_NICKNAME, "사용자") ?: "사용자"
    }

    // --- Strike 관리 ---

    fun saveStreak(streak: Int) {
        prefs.edit().putInt(KEY_STREAK, streak).apply()
    }

    fun getStreak(): Int {
        return prefs.getInt(KEY_STREAK, 0) // 기본값 0
    }

    // --- Leaf Count 관리 ---

    fun incrementLeafCount() {
        val currentCount = getLeafCount()
        prefs.edit().putInt(KEY_LEAF_COUNT, currentCount + 1).apply()
    }

    fun getLeafCount(): Int {
        return prefs.getInt(KEY_LEAF_COUNT, 0) // 기본값 0
    }

    // --- Grape Count 관리 ---

    fun incrementGrapeCount() {
        val currentCount = getGrapeCount()
        prefs.edit().putInt(KEY_GRAPE_COUNT, currentCount + 1).apply()
    }

    fun getGrapeCount(): Int {
        return prefs.getInt(KEY_GRAPE_COUNT, 0) // 기본값 0
    }

    // 필요하다면, 모든 통계 정보를 한 번에 저장/로드하는 메서드를 추가할 수 있습니다.
}