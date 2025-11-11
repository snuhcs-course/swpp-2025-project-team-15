package com.example.sumdays.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sumdays.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)

            // flag memo 가져오기
            val deletedMemos = db.memoDao().getDeletedMemos()
            val editedMemos = db.memoDao().getEditedMemos()
            val deletedIds = deletedMemos.map { it.id }
            val editedIds = editedMemos.map { it.id }

            // 서버에 요청하기
            // 2️⃣ 로그로 서버 전송 대체
            Log.d("BackupWorker", "🗑️ [Deleted Memos]")
            if (deletedMemos.isEmpty()) {
                Log.d("BackupWorker", "  (none)")
            } else {
                deletedMemos.forEach { memo ->
                    Log.d(
                        "BackupWorker",
                        "  id=${memo.id}, timestamp=${memo.timestamp}, content=\"${memo.content}\""
                    )
                }
            }

            Log.d("BackupWorker", "📝 [Edited Memos]")
            if (editedMemos.isEmpty()) {
                Log.d("BackupWorker", "  (none)")
            } else {
                editedMemos.forEach { memo ->
                    Log.d(
                        "BackupWorker",
                        "  id=${memo.id}, timestamp=${memo.timestamp}, content=\"${memo.content}\""
                    )
                }
            }

            // 서버에 성공했으면
            db.memoDao().resetDeletedFlags(deletedIds)
            db.memoDao().resetEdittedFlags(editedIds)





            Result.success()
        } catch (e: Exception) {
            Log.e("BackupWorker", "백업 실패: ${e.message}")
            Result.retry()
        }
    }
}
