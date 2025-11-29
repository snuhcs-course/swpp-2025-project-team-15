package com.example.sumdays

import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.sumdays.daily.memo.MemoRepository
import com.example.sumdays.daily.memo.MemoViewModel
import org.mockito.Mockito.mock

/**
 * Robolectric 테스트 환경에서 사용되는 Application 클래스입니다.
 * * 1. MyApplication을 상속받아 Activity 내의 캐스팅 오류를 방지합니다.
 * 2. WorkManager를 테스트 모드(동기 실행)로 초기화하여 스케줄러 관련 오류를 방지합니다.
 * 3. super.onCreate()를 호출하지 않음으로써 실제 앱의 무거운 초기화(DB, 백그라운드 작업)를 막습니다.
 */
class TestApplication : MyApplication(), Configuration.Provider {

    // 테스트 코드에서 참조하여 사용할 수 있는 Mock 객체들
    val mockRepository: MemoRepository = mock()
    val mockViewModel: MemoViewModel = mock()

    override fun onCreate() {
        // 주의: super.onCreate()를 호출하지 않습니다.
        // 실제 MyApplication의 onCreate에는 BackupScheduler 등 실제 로직이 포함되어 있어
        // 테스트 환경에서 충돌을 일으킬 수 있습니다.

        // 1. 테스트용 WorkManager 설정 (SynchronousExecutor를 사용하여 즉시 실행)
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()

        // 2. 테스트용 WorkManager 강제 초기화
        WorkManagerTestInitHelper.initializeTestWorkManager(this, config)
    }

    // WorkManager가 설정을 요청할 때 호출되는 메서드
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
}