package com.niranjan.medqueue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.niranjan.medqueue.data.local.AppDatabase
import com.niranjan.medqueue.data.local.RequestEntity
import com.niranjan.medqueue.data.local.RequestStatus
import com.niranjan.medqueue.data.local.readyIndices
import com.niranjan.medqueue.data.local.toReadyItems
import com.niranjan.medqueue.prescription.PrescriptionStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RequestViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).requestDao()

    /** Live list of all requests, newest first. Observed by Compose UI. */
    val requests: StateFlow<List<RequestEntity>> = dao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addRequest(
        customerName: String,
        phoneNumber: String,
        medicineName: String,
        isEmergency: Boolean = false,
        prescriptionPath: String? = null
    ) {
        val phone = phoneNumber.filter(Char::isDigit)
        if (phone.isEmpty()) return   // nothing to contact — don't store a dead row

        viewModelScope.launch {
            dao.insert(
                RequestEntity(
                    customerName     = customerName.trim(),
                    phoneNumber      = phone,
                    medicineName     = medicineName.trim(),
                    isEmergency      = isEmergency,
                    prescriptionPath = prescriptionPath
                )
            )
        }
    }

    fun setEmergency(id: Int, isEmergency: Boolean) {
        viewModelScope.launch { dao.updateEmergency(id, isEmergency) }
    }

    fun markDelivered(id: Int) {
        viewModelScope.launch { dao.updateStatus(id, RequestStatus.DELIVERED) }
    }

    /**
     * Reverses [markDelivered] — backs the queue's undo action.
     *
     * `notifiedAt` is left alone: undoing the delivery does not un-send the
     * message, so the request drops back to "Notified", not to square one.
     */
    fun markPending(id: Int) {
        viewModelScope.launch { dao.updateStatus(id, RequestStatus.PENDING) }
    }

    /** Records that the customer was messaged about this request, just now. */
    fun markNotified(id: Int, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch { dao.updateNotifiedAt(id, timestamp) }
    }

    /** Ticks a single medicine line as arrived, or un-ticks it. */
    fun setItemReady(id: Int, index: Int, ready: Boolean) {
        viewModelScope.launch {
            val current = dao.getById(id)?.readyIndices() ?: return@launch
            val updated = if (ready) current + index else current - index
            dao.updateReadyItems(id, updated.toReadyItems())
        }
    }

    /**
     * Edit customer name, phone, medicine, emergency flag and prescription.
     * Status and createdAt are preserved.
     */
    fun updateRequest(
        id: Int,
        customerName: String,
        phoneNumber: String,
        medicineName: String,
        isEmergency: Boolean,
        prescriptionPath: String?
    ) {
        val phone = phoneNumber.filter(Char::isDigit)
        if (phone.isEmpty()) return

        viewModelScope.launch {
            val previous = dao.getById(id)
            val previousPath = previous?.prescriptionPath
            val trimmedMedicine = medicineName.trim()

            dao.updateFields(
                id           = id,
                customerName = customerName.trim(),
                phoneNumber  = phone,
                medicineName = trimmedMedicine
            )
            dao.updateEmergency(id, isEmergency)
            dao.updatePrescription(id, prescriptionPath)

            // readyItems are positional. Once the medicine list changes, index 2
            // no longer means what it meant when it was ticked, so drop them
            // rather than silently mark the wrong drug as arrived.
            if (previous != null && previous.medicineName != trimmedMedicine) {
                dao.updateReadyItems(id, "")
            }

            // A replaced or cleared photo would otherwise sit on disk forever.
            if (previousPath != null && previousPath != prescriptionPath) {
                PrescriptionStore.delete(previousPath)
            }
        }
    }

    fun deleteRequest(id: Int) {
        viewModelScope.launch {
            val doomed = dao.getById(id)
            dao.delete(id)
            PrescriptionStore.delete(doomed?.prescriptionPath)
        }
    }
}
