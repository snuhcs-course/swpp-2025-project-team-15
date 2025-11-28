package com.example.sumdays.data.sync

import androidx.work.*
import java.util.concurrent.TimeUnit

object BackupScheduler {
    // 3시간마다 자동 백업
    fun scheduleAutoBackup() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Wi-Fi 연결 시만 수행
            .build()

        val request = PeriodicWorkRequestBuilder<BackupWorker>(
            3, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance()
            .enqueueUniquePeriodicWork(
                "auto_backup",
                ExistingPeriodicWorkPolicy.KEEP, // 기존 스케줄 유지
                request
            )
    }

    // 수동 백업 (즉시 한 번 실행)
    fun triggerManualBackup() {
        val request = OneTimeWorkRequestBuilder<BackupWorker>().build()
        WorkManager.getInstance().enqueue(request)
    }
}
