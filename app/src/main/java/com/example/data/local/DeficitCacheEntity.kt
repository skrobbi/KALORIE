package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deficit_cache")
data class DeficitCacheEntity(
    @PrimaryKey val id: Int = 1,
    val analysisJson: String,
    val period: String,
    val timestamp: Long = System.currentTimeMillis()
)
