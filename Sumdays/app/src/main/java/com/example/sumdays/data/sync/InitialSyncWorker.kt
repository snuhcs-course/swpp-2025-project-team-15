package com.example.sumdays.data.sync

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.sumdays.data.AppDatabase
import com.example.sumdays.network.ApiClient
import kotlinx.coroutines.runBlocking

/**
 * 로그인 직후 단 한 번 실행되는 초기 동기화 작업.
 * 서버 → 로컬 DB 전체 업데이트.
 */
class InitialSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    private val db = AppDatabase.getDatabase(appContext)
    private val dailyDao = db.dailyEntryDao()
    private val memoDao = db.memoDao()
    private val styleDao = db.userStyleDao()
    private val api = ApiClient.apiService

    override fun doWork(): Result = runBlocking {
        try {
            // 1) 서버에서 전체 데이터 fetch
            val syncResponse = api.fetchAllData() // /api/db/full-sync 요청 (예시)

            if (!syncResponse.isSuccessful || syncResponse.body() == null) {
                return@runBlocking Result.failure()
            }

            val data = syncResponse.body()!!

            // 2) 트랜잭션으로 Room DB 전체 업데이트
            db.runInTransaction {
                // 기존 데이터 삭제 (필요하다면)
                dailyDao.clearAll()
                memoDao.clearAll()
                photoDao.clearAll()
                styleDao.clearAll()

                // 새 데이터 삽입
                dailyDao.insertAll(data.dailyEntries)
                memoDao.insertAll(data.memos)
                photoDao.insertAll(data.photos)
                styleDao.insert(data.userStyle)
            }

            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
