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

    /** Falls back to PENDING rather than throwing if the stored value is unrecognised. */
    @TypeConverter
    fun toStatus(value: String): RequestStatus =
        RequestStatus.entries.firstOrNull { it.name == value } ?: RequestStatus.PENDING
}

// ── Entity ───────────────────────────────────────────────────────────────────

@Entity(tableName = "requests")
data class RequestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerName: String,
    val phoneNumber: String,
    val medicineName: String,
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val isEmergency: Boolean = false
)

