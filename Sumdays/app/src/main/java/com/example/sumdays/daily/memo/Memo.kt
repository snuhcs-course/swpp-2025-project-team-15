package com.example.sumdays.daily.memo

import androidx.room.Entity
import androidx.room.PrimaryKey

// Room 데이터베이스 테이블을 정의하는 Entity 클래스
@Entity(tableName = "memo_table")
data class Memo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val content: String,
    val timestamp: String,
    val date: String
)