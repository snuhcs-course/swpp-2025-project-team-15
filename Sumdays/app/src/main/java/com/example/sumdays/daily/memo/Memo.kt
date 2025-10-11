package com.example.sumdays.daily.memo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Room 데이터베이스 테이블을 정의하는 Entity 클래스
@Entity(tableName = "memo_table")
data class Memo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "timestamp") val timestamp: String,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "memo_order") val order: Int
)