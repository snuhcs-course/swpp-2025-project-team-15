package com.example.sumdays.statistics

// 월간 요약 데이터를 담을 클래스 (포도 상징)
// 'ai/summarize-month' API의 result 객체 전체를 파싱
data class MonthSummary(
    val insights: Insights,
    val summary: SummaryDetails,
    val weeks: List<WeekSummaryForMonth> // 월별 API 내부에 포함된 주간 데이터
)

// WeekSummaryForMonth는 MonthSummary 내부에 포함된 간소화된 주간 데이터
data class WeekSummaryForMonth(
    val averageEmotion: Float,
    val dominantEmoji: String,
    val keywords: List<String>,
    val overview: String,
    val title: String,
    val weekRange: String
)