package com.niranjan.medqueue.navigation

import com.niranjan.medqueue.data.local.RequestEntity

sealed class Screen {
    object Splash      : Screen()
    object Home        : Screen()
    object RequestList : Screen()
    object Settings    : Screen()
    data class RequestDetail(val request: RequestEntity) : Screen()
    data class EditRequest(val request: RequestEntity)   : Screen()
}

enum class FilterTag(val label: String) {
    ALL("All"), TODAY("Today"), PENDING("Pending"), DELIVERED("Delivered")
}

