package com.example.sumdays.statistics

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

object WeekSummaryScheduler {

    private const val WORK_NAME = "WeekSummaryGenerationWork"

    fun scheduleWeeklyTask(context: Context) {
        Log.d("WeekSummaryScheduler", "📅 주간 요약 스케줄러 등록 시작...")

        // 1. 제약 조건: 인터넷 연결 필수 (나중에 AI 요청해야 하니까)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 2. 최초 실행 지연 시간(Initial Delay) 계산
        // '지금'으로부터 '다음주 월요일 00:00'까지 몇 초 남았는지 계산
        val now = LocalDateTime.now()

        // 다음 월요일 00:00 찾기
        var nextRunTime = now.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            .with(LocalTime.MIDNIGHT)

        // 만약 계산된 시간이 과거라면(거의 없겠지만), 1주 뒤로 미룸
        if (now.isAfter(nextRunTime)) {
            nextRunTime = nextRunTime.plusWeeks(1)
        }

        val initialDelaySeconds = Duration.between(now, nextRunTime).seconds
        Log.d("WeekSummaryScheduler", "⏳ 다음 실행(월요일 자정)까지 남은 시간: ${initialDelaySeconds}초")

        // 3. 주기적 작업 생성 (7일 간격)
        val workRequest = PeriodicWorkRequestBuilder<WeekSummaryWorker>(7, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)
            .addTag("weekly_summary")
            .build()

        // 4. WorkManager에 등록
        // ExistingPeriodicWorkPolicy.KEEP: 이미 등록된 작업이 있으면 덮어쓰지 않고 유지함 (중복 방지)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        Log.d("WeekSummaryScheduler", "✅ 스케줄러 등록 요청 완료")
    }
}