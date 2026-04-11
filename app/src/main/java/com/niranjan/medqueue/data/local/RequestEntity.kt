package com.niranjan.medqueue.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

// ── Status ──────────────────────────────────────────────────────────────────

enum class RequestStatus {
    PENDING,
    DELIVERED
}

// ── TypeConverter (used by AppDatabase) ─────────────────────────────────────

class RequestConverters {
    @TypeConverter
    fun fromStatus(status: RequestStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): RequestStatus = RequestStatus.valueOf(value)
}

// ── Entity ───────────────────────────────────────────────────────────────────

@Entity(tableName = "requests")
data class RequestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerName: String,
    val phoneNumber: String,
    val medicineName: String,
    val status: RequestStatus = RequestStatus.PENDING
)

