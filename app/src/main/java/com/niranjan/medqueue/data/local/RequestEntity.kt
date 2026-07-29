package com.niranjan.medqueue.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

// ── Status ──────────────────────────────────────────────────────────────────

enum class RequestStatus {
    PENDING,
    DELIVERED
}

// ── Stage ───────────────────────────────────────────────────────────────────

/**
 * What the queue actually needs to show, which is one step finer than
 * [RequestStatus].
 *
 * "Stock arrived, I messaged the customer" and "the customer came and took it"
 * are hours or days apart in a shop, and the gap between them is the state the
 * worker most needs to see — otherwise the same customer gets messaged twice,
 * or never. [NOTIFIED] is that gap.
 *
 * Derived rather than stored as a third [RequestStatus] value so an existing
 * row can never hold an impossible status/timestamp combination.
 */
enum class RequestStage { PENDING, NOTIFIED, DELIVERED }

val RequestEntity.stage: RequestStage
    get() = when {
        status == RequestStatus.DELIVERED -> RequestStage.DELIVERED
        notifiedAt != null                -> RequestStage.NOTIFIED
        else                              -> RequestStage.PENDING
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
    val isEmergency: Boolean = false,
    /**
     * Absolute path to the prescription photo in internal storage, or null.
     * Stored as a path rather than a content URI because the source URI (camera
     * capture, photo picker) is only valid for the lifetime of the grant.
     */
    val prescriptionPath: String? = null,
    /**
     * When the customer was last messaged about this request, or null if they
     * never have been. Set by WhatsApp/SMS sends, and deliberately *not*
     * cleared when a delivery is undone — the message was still sent.
     */
    val notifiedAt: Long? = null,
    /**
     * Indices into [medicineName]'s lines that have arrived, comma-separated
     * (`"0,2"`). Empty means nothing has come in yet.
     *
     * Fulfilment used to be all-or-nothing, so "two of the three are in" had
     * nowhere to live. Stored as indices rather than names because the same
     * drug can legitimately appear twice on one order; cleared whenever the
     * medicine list is edited, since the indices no longer refer to anything.
     */
    val readyItems: String = ""
)

/**
 * [medicineName] split into its individual entries.
 *
 * The field is one newline-delimited blob, and the same split was being redone
 * inline wherever it was needed. Indices here are the indices [readyItems]
 * refers to, so the two must stay derived from the same rule.
 */
fun RequestEntity.medicineLines(): List<String> =
    medicineName.lines().filter { it.isNotBlank() }

/** The medicine lines that have been marked as arrived. */
fun RequestEntity.readyIndices(): Set<Int> =
    if (readyItems.isBlank()) emptySet()
    else readyItems.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()

/** Inverse of [readyIndices] — the storage form. */
fun Set<Int>.toReadyItems(): String = sorted().joinToString(",")

