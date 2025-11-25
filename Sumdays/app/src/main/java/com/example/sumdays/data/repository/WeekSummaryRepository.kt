package com.example.sumdays.data.repository

import com.example.sumdays.data.dao.WeekSummaryDao
import com.example.sumdays.data.WeekSummaryEntity
import com.example.sumdays.statistics.WeekSummary

class WeekSummaryRepository (
    private val dao: WeekSummaryDao
) {
    /**
     * 주간 통계 데이터를 데이터베이스에 저장하거나 업데이트합니다.
     */
    suspend fun upsertWeekSummary(summary: WeekSummary) {
        dao.upsert(summary)
    }

    /**
     * 특정 시작 날짜에 해당하는 주간 통계 데이터를 가져옵니다.
     */
    suspend fun getWeekSummary(startDate: String): WeekSummary? {
        return dao.getWeekSummary(startDate)
    }

    /**
     * 주간 통계 데이터를 삭제(소프트 삭제) 처리합니다.
     */
    suspend fun deleteWeekSummary(startDate: String) {
        dao.deleteWeekSummary(startDate)
    }

    /**
     * 통계 화면 초기 설정을 위해 저장된 모든 유효한 주간의 시작 날짜 목록을 오름차순으로 가져옵니다.
     */
    suspend fun getAllWrittenDatesAsc(): List<String> {
        return dao.getAllDatesAsc()
    }

    // (선택 사항) 복구 및 동기화 로직에 필요한 DAO 함수는 Repository에 추가하지 않았습니다.
}