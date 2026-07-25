package com.niranjan.medqueue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.niranjan.medqueue.data.local.AppDatabase
import com.niranjan.medqueue.data.local.RequestEntity
import com.niranjan.medqueue.data.local.RequestStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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

    /**
     * IDs flagged as emergency, derived from the rows themselves.
     *
     * The flag used to live in a MutableStateFlow here, which meant it was lost
     * on process death — and on a plain rotation. It is now a column on
     * [RequestEntity]; this projection exists so existing callers keep working.
     * New UI can read `request.isEmergency` directly.
     */
    val emergencyIds: StateFlow<Set<Int>> = requests
        .map { list -> list.filter { it.isEmergency }.map { it.id }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )

    fun addRequest(
        customerName: String,
        phoneNumber: String,
        medicineName: String,
        isEmergency: Boolean = false
    ) {
        val phone = phoneNumber.filter(Char::isDigit)
        if (phone.isEmpty()) return   // nothing to contact — don't store a dead row

        viewModelScope.launch {
            dao.insert(
                RequestEntity(
                    customerName = customerName.trim(),
                    phoneNumber  = phone,
                    medicineName = medicineName.trim(),
                    isEmergency  = isEmergency
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

    /** Edit customer name, phone, medicine — status and createdAt are preserved. */
    fun updateRequest(id: Int, customerName: String, phoneNumber: String, medicineName: String) {
        val phone = phoneNumber.filter(Char::isDigit)
        if (phone.isEmpty()) return

        viewModelScope.launch {
            dao.updateFields(
                id           = id,
                customerName = customerName.trim(),
                phoneNumber  = phone,
                medicineName = medicineName.trim()
            )
        }
    }

    fun deleteRequest(id: Int) {
        viewModelScope.launch { dao.delete(id) }
    }
}
