package com.example.sumdays.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sumdays.data.DailyEntry
import com.example.sumdays.data.style.UserStyle

@Dao
interface UserStyleDao {

    // 1. 새 스타일 저장 (AI 추출 성공 시)
    @Insert
    suspend fun insertStyleRaw(style: UserStyle)
    suspend fun insertStyle(style: UserStyle){
        insertStyleRaw(style.copy(isEdited = true, isDeleted = false))
    }

    // 2. 모든 스타일 목록 조회 (SettingsActivity에서 사용)
    // LiveData<List<UserStyle>>을 반환하여 단일 사용자에게 속한 모든 스타일을 표시
    @Query("SELECT * FROM user_style WHERE isDeleted = 0 ORDER BY styleId DESC")
    fun getAllStyles(): LiveData<List<UserStyle>>

    // 3. 스타일 삭제
    @Query("UPDATE user_style SET isDeleted = 1, isEdited = 1 WHERE styleId = :styleId")
    suspend fun markAsDeleted(styleId: Long)
    suspend fun deleteStyle(style: UserStyle) {
        markAsDeleted(style.styleId)
    }

    // 4. 활성 스타일 조회 (UserStats의 activeStyleId를 통해 조회)
    @Query("SELECT * FROM user_style WHERE styleId = :styleId AND isDeleted = 0")
    suspend fun getStyleById(styleId: Long): UserStyle?

    // 5. 모든 스타일 삭제 (계정 탈퇴 시 등 - userId 없이 전체 삭제)
    @Query("DELETE FROM user_style")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<UserStyle>)


    // 서버 sync 용도
    @Query("SELECT * FROM user_style WHERE isDeleted = 1")
    suspend fun getDeletedStyles(): List<UserStyle>

    // ✅ 7️⃣ 백업용 — 수정된(삭제되지 않은) 스타일만 조회
    @Query("SELECT * FROM user_style WHERE isEdited = 1 AND isDeleted = 0")
    suspend fun getEditedStyles(): List<UserStyle>

    // ✅ 8️⃣ 백업 후 — 삭제된 항목 실제 제거
    @Query("DELETE FROM user_style WHERE styleId IN (:ids)")
    suspend fun resetDeletedFlags(ids: List<Long>)

    // ✅ 9️⃣ 백업 후 — 수정 플래그 초기화
    @Query("UPDATE user_style SET isEdited = 0, isDeleted = 0 WHERE styleId IN (:ids)")
    suspend fun resetEditedFlags(ids: List<Long>)
}