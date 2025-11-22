package com.example.sumdays.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sumdays.data.repository.WeekSummaryRepository
import com.example.sumdays.statistics.WeekSummary
import kotlinx.coroutines.launch

class WeekSummaryViewModel (
    private val repository: WeekSummaryRepository
) : ViewModel() {

    // 💡 모든 WeekSummary 데이터를 비동기로 로드하는 Flow나 LiveData를 여기에 추가할 수 있습니다.
    // 현재는 ID(날짜) 기반의 단일 호출만 구현합니다.

    /**
     * 주간 통계 데이터를 저장/업데이트합니다.
     */
    fun upsert(summary: WeekSummary) {
        viewModelScope.launch {
            repository.upsertWeekSummary(summary)
        }
    }

    /**
     * 특정 주간의 통계 데이터를 가져옵니다. (단일 호출이므로 LiveData로 감싸지 않습니다.)
     */
    suspend fun getSummary(startDate: String): WeekSummary? {
        return repository.getWeekSummary(startDate)
    }

    /**
     * 통계 화면 초기 세팅을 위해 저장된 모든 주간 날짜 목록을 가져옵니다.
     */
    suspend fun getAllDatesAsc(): List<String> {
        return repository.getAllWrittenDatesAsc()
    }
}


// ViewModel을 인스턴스화하기 위한 팩토리 클래스 (DI를 사용하지 않을 경우)
class WeekSummaryViewModelFactory(
    private val repository: WeekSummaryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeekSummaryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeekSummaryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}