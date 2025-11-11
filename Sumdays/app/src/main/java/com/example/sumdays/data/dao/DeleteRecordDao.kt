package com.example.sumdays.data.dao

import androidx.room.*
import com.example.sumdays.data.DeleteRecord
import com.example.sumdays.data.WeekSummaryEntity
import com.example.sumdays.statistics.WeekSummary

@Dao
interface DeleteRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DeleteRecord)

    @Query("SELECT * FROM delete_flag WHERE type = :type")
    suspend fun getRecordsByType(type: String): List<DeleteRecord>

    @Query("DELETE FROM delete_flag WHERE id IN (:ids)")
    suspend fun deleteRecords(ids: List<Int>)

    @Query("DELETE FROM delete_flag")
    suspend fun clearAll()
}