package com.example.sumdays.statistics

// 주간 요약 데이터를 담을 클래스 (초록 블록 상징)
data class WeekSummary(
    val summaryText: String, // 주간 요약 텍스트
    val weekStartDate: String, // 주 시작 날짜 (YYYY-MM-DD)
    val weekEndDate: String, // 주 끝 날짜 (YYYY-MM-DD)
    val memoCount: Int // 해당 주에 작성된 일기 개수
)