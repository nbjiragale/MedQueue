package com.niranjan.medqueue

import androidx.compose.runtime.*
import com.niranjan.medqueue.data.settings.SettingsPrefs
import com.niranjan.medqueue.navigation.Screen
import com.niranjan.medqueue.ui.components.MedQueueBottomBar
import com.niranjan.medqueue.ui.screens.*

@Composable
fun MedQueueApp(vm: RequestViewModel, settingsPrefs: SettingsPrefs) {
    var screen: Screen by remember { mutableStateOf(Screen.Splash as Screen) }
    val requests by vm.requests.collectAsState()
    val emergencyIds by vm.emergencyIds.collectAsState()

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
            onItemClick  = { screen = Screen.RequestDetail(it) },
            bottomBar    = bottomBar
        )
        is Screen.RequestDetail -> RequestDetailScreen(
            request       = s.request,
            isEmergency   = s.request.id in emergencyIds,
            settingsPrefs = settingsPrefs,
            onEdit        = { screen = Screen.EditRequest(s.request) },
            onDelivered   = { vm.markDelivered(s.request.id); screen = Screen.RequestList },
            onDelete      = { vm.deleteRequest(s.request.id); screen = Screen.RequestList },
            onBack        = { screen = Screen.RequestList },
            bottomBar     = bottomBar
        )
        is Screen.EditRequest -> EditRequestScreen(
            request     = s.request,
            isEmergency = s.request.id in emergencyIds,
            onSave      = { name, phone, medicine, emergency ->
                vm.updateRequest(s.request.id, name, phone, medicine)
                vm.setEmergency(s.request.id, emergency)
                screen = Screen.RequestList
            },
            onBack      = { screen = Screen.RequestDetail(s.request) },
            bottomBar   = bottomBar
        )
        Screen.Settings -> SettingsScreen(
            settingsPrefs = settingsPrefs,
            onBack        = { screen = Screen.RequestList },
            bottomBar     = bottomBar
        )
    }
}

