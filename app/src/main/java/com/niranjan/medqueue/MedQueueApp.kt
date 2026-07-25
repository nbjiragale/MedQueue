package com.niranjan.medqueue

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.niranjan.medqueue.data.settings.SettingsPrefs
import com.niranjan.medqueue.navigation.Screen
import com.niranjan.medqueue.navigation.ScreenSaver
import com.niranjan.medqueue.ui.components.MedQueueBottomBar
import com.niranjan.medqueue.ui.screens.*

@Composable
fun MedQueueApp(vm: RequestViewModel, settingsPrefs: SettingsPrefs) {
    var screen: Screen by rememberSaveable(stateSaver = ScreenSaver) {
        mutableStateOf(Screen.Splash)
    }
    val requests by vm.requests.collectAsState()
    val emergencyIds by vm.emergencyIds.collectAsState()

    // System back walks the hierarchy instead of dropping straight out of the
    // app. The list is the root, so back there keeps the default behaviour.
    BackHandler(enabled = screen !is Screen.RequestList && screen !is Screen.Splash) {
        screen = when (val s = screen) {
            is Screen.EditRequest -> Screen.RequestDetail(s.requestId)
            else                  -> Screen.RequestList
        }
    }

    val bottomBar: @Composable () -> Unit = {
        MedQueueBottomBar(currentScreen = screen, onNavigate = { screen = it })
    }

    when (val s = screen) {
        Screen.Splash -> SplashScreen(
            onFinished = { screen = Screen.RequestList }
        )
        Screen.Home -> HomeScreen(
            onSave = { name, phone, medicine, isEmergency ->
                vm.addRequest(name, phone, medicine, isEmergency)
                screen = Screen.RequestList
            },
            onBack    = { screen = Screen.RequestList },
            bottomBar = bottomBar
        )
        Screen.RequestList -> RequestListScreen(
            requests     = requests,
            emergencyIds = emergencyIds,
            onAddClick   = { screen = Screen.Home },
            onItemClick  = { screen = Screen.RequestDetail(it.id) },
            bottomBar    = bottomBar
        )
        is Screen.RequestDetail -> {
            // Resolved live, so an edit or a delivery is reflected immediately.
            val request = requests.firstOrNull { it.id == s.requestId }
            if (request == null) {
                // Row is gone (deleted, or restored against a stale id).
                LaunchedEffect(s.requestId) { screen = Screen.RequestList }
            } else {
                RequestDetailScreen(
                    request       = request,
                    isEmergency   = request.isEmergency,
                    settingsPrefs = settingsPrefs,
                    onEdit        = { screen = Screen.EditRequest(request.id) },
                    onDelivered   = { vm.markDelivered(request.id); screen = Screen.RequestList },
                    onDelete      = { vm.deleteRequest(request.id); screen = Screen.RequestList },
                    onBack        = { screen = Screen.RequestList },
                    bottomBar     = bottomBar
                )
            }
        }
        is Screen.EditRequest -> {
            val request = requests.firstOrNull { it.id == s.requestId }
            if (request == null) {
                LaunchedEffect(s.requestId) { screen = Screen.RequestList }
            } else {
                EditRequestScreen(
                    request     = request,
                    isEmergency = request.isEmergency,
                    onSave      = { name, phone, medicine, emergency ->
                        vm.updateRequest(request.id, name, phone, medicine)
                        vm.setEmergency(request.id, emergency)
                        screen = Screen.RequestDetail(request.id)
                    },
                    onBack      = { screen = Screen.RequestDetail(request.id) },
                    bottomBar   = bottomBar
                )
            }
        }
        Screen.Settings -> SettingsScreen(
            settingsPrefs = settingsPrefs,
            onBack        = { screen = Screen.RequestList },
            bottomBar     = bottomBar
        )
    }
}
