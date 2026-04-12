package com.niranjan.medqueue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.niranjan.medqueue.data.local.AppDatabase
import com.niranjan.medqueue.data.local.RequestEntity
import com.niranjan.medqueue.data.local.RequestStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

    /** In-memory set of request IDs flagged as emergency (no DB column). */
    private val _emergencyIds = MutableStateFlow<Set<Int>>(emptySet())
    val emergencyIds: StateFlow<Set<Int>> = _emergencyIds

    fun addRequest(customerName: String, phoneNumber: String, medicineName: String, isEmergency: Boolean = false) {
        viewModelScope.launch {
            val rowId = dao.insert(
                RequestEntity(
                    customerName = customerName.trim(),
                    phoneNumber  = phoneNumber.trim(),
                    medicineName = medicineName.trim()
                )
            )
            if (isEmergency) {
                _emergencyIds.update { it + rowId.toInt() }
            }
        }
    }

    fun toggleEmergency(id: Int) {
        _emergencyIds.update { ids ->
            if (id in ids) ids - id else ids + id
        }
    }

    fun setEmergency(id: Int, isEmergency: Boolean) {
        _emergencyIds.update { ids ->
            if (isEmergency) ids + id else ids - id
        }
    }

    fun markDelivered(id: Int) {
        viewModelScope.launch { dao.updateStatus(id, RequestStatus.DELIVERED) }
    }

    /** Edit customer name, phone, medicine — status is preserved. */
    fun updateRequest(id: Int, customerName: String, phoneNumber: String, medicineName: String) {
        viewModelScope.launch {
            dao.updateFields(
                id           = id,
                customerName = customerName.trim(),
                phoneNumber  = phoneNumber.trim(),
                medicineName = medicineName.trim()
            )
        }
    }

    fun deleteRequest(id: Int) {
        viewModelScope.launch { dao.delete(id) }
        _emergencyIds.update { it - id }
    }
}

