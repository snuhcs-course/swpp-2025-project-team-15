package com.example.sumdays.daily.memo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Room의 DAO(Data Access Object) 인터페이스
@Dao
interface MemoDao {
    // 새로운 메모를 데이터베이스에 삽입
    @Insert
    suspend fun insert(memo: Memo)

    // 특정 날짜의 메모만 조회
    // Flow를 반환하여 데이터 변경 시 실시간으로 알림을 받음
    @Query("SELECT * FROM memo_table WHERE date = :date ORDER BY timestamp ASC")
    fun getMemosForDate(date: String): Flow<List<Memo>>

    // 모든 메모를 삭제
    @Query("DELETE FROM memo_table")
    suspend fun deleteAllMemos()
}