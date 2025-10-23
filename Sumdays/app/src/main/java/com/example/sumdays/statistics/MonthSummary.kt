package com.example.sumdays.statistics

// 월간 요약 데이터를 담을 클래스 (포도 상징)
data class MonthSummary(
    val summaryText: String, // 월간 요약 텍스트
    val createdAt: String // 요약 생성일
)