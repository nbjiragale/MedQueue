package com.niranjan.medqueue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.niranjan.medqueue.data.local.AppDatabase
import com.niranjan.medqueue.data.local.RequestEntity
import com.niranjan.medqueue.data.local.RequestStatus
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

    fun addRequest(customerName: String, phoneNumber: String, medicineName: String) {
        viewModelScope.launch {
            dao.insert(
                RequestEntity(
                    customerName = customerName.trim(),
                    phoneNumber  = phoneNumber.trim(),
                    medicineName = medicineName.trim()
                )
            )
        }
    }

    fun markDelivered(id: Int) {
        viewModelScope.launch {
            dao.updateStatus(id, RequestStatus.DELIVERED)
        }
    }

    fun deleteRequest(id: Int) {
        viewModelScope.launch {
            dao.delete(id)
        }
    }
}

