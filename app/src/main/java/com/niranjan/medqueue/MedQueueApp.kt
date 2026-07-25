package com.niranjan.medqueue

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.niranjan.medqueue.autosend.AutoSendPrefs
import com.niranjan.medqueue.contact.ContactAction
import com.niranjan.medqueue.contact.ContactActionResult
import com.niranjan.medqueue.contact.buildMessage
import com.niranjan.medqueue.contact.launchContactAction
import com.niranjan.medqueue.data.local.RequestEntity
import com.niranjan.medqueue.data.settings.SettingsPrefs
import com.niranjan.medqueue.navigation.Screen
import com.niranjan.medqueue.navigation.ScreenSaver
import com.niranjan.medqueue.ui.components.MedQueueBottomBar
import com.niranjan.medqueue.ui.screens.*

@Composable
fun MedQueueApp(vm: RequestViewModel, settingsPrefs: SettingsPrefs) {
    val context = LocalContext.current

    var screen: Screen by rememberSaveable(stateSaver = ScreenSaver) {
        mutableStateOf(Screen.Splash)
    }
    val requests by vm.requests.collectAsState()

    // System back walks the hierarchy instead of dropping straight out of the
    // app. The list is the root, so back there keeps the default behaviour.
    BackHandler(
        enabled = screen !is Screen.RequestList &&
                screen !is Screen.Splash &&
                screen !is Screen.Onboarding
    ) {
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
            onFinished = {
                screen = if (settingsPrefs.hasOnboarded()) Screen.RequestList else Screen.Onboarding
            }
        )

        Screen.Onboarding -> OnboardingScreen(
            settingsPrefs = settingsPrefs,
            onFinished = { screen = Screen.RequestList }
        )

        Screen.Home -> HomeScreen(
            onSave = { name, phone, medicine, emergency, prescription ->
                vm.addRequest(name, phone, medicine, emergency, prescription)
                screen = Screen.RequestList
            },
            onBack = { screen = Screen.RequestList },
            bottomBar = bottomBar
        )

        Screen.RequestList -> RequestListScreen(
            requests = requests,
            onAddClick = { screen = Screen.Home },
            onItemClick = { screen = Screen.RequestDetail(it.id) },
            bottomBar = bottomBar
        )

        is Screen.RequestDetail -> {
            // Resolved live, so an edit or a delivery is reflected immediately.
            val request = requests.firstOrNull { it.id == s.requestId }
            if (request == null) {
                // Row is gone (deleted, or restored against a stale id).
                LaunchedEffect(s.requestId) { screen = Screen.RequestList }
            } else {
                RequestDetailScreen(
                    request = request,
                    settingsPrefs = settingsPrefs,
                    onEdit = { screen = Screen.EditRequest(request.id) },
                    onDelivered = {
                        vm.markDelivered(request.id)
                        notifyOnDelivered(context, settingsPrefs, request)
                        screen = Screen.RequestList
                    },
                    onDelete = {
                        vm.deleteRequest(request.id)
                        screen = Screen.RequestList
                    },
                    onBack = { screen = Screen.RequestList },
                    bottomBar = bottomBar
                )
            }
        }

        is Screen.EditRequest -> {
            val request = requests.firstOrNull { it.id == s.requestId }
            if (request == null) {
                LaunchedEffect(s.requestId) { screen = Screen.RequestList }
            } else {
                EditRequestScreen(
                    request = request,
                    onSave = { name, phone, medicine, emergency, prescription ->
                        vm.updateRequest(request.id, name, phone, medicine, emergency, prescription)
                        screen = Screen.RequestDetail(request.id)
                    },
                    onBack = { screen = Screen.RequestDetail(request.id) },
                    bottomBar = bottomBar
                )
            }
        }

        Screen.Settings -> SettingsScreen(
            settingsPrefs = settingsPrefs,
            onBack = { screen = Screen.RequestList },
            bottomBar = bottomBar
        )
    }
}

/**
 * Auto-notify on "mark delivered", which is what the redesign's Settings copy
 * promises ("Sends automatically when marked delivered").
 *
 * This opens WhatsApp with the message prefilled; the accessibility service
 * taps Send only if the worker has separately enabled it. Failures are
 * deliberately quiet apart from a toast — the delivery status has already been
 * saved, and a messaging hiccup should not read as though that failed too.
 */
private fun notifyOnDelivered(
    context: Context,
    settingsPrefs: SettingsPrefs,
    request: RequestEntity
) {
    if (!AutoSendPrefs.isAutoSendEnabled(context)) return

    val message = buildMessage(settingsPrefs.read(), ContactAction.WHATSAPP)
    val result = launchContactAction(context, ContactAction.WHATSAPP, request.phoneNumber, message)

    if (result == ContactActionResult.WhatsAppNotInstalled) {
        Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
    }
}
