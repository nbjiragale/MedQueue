package com.niranjan.medqueue

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import com.niranjan.medqueue.contact.ContactAction
import com.niranjan.medqueue.contact.ContactActionResult
import com.niranjan.medqueue.contact.buildMessage
import com.niranjan.medqueue.contact.launchContactAction
import com.niranjan.medqueue.data.local.RequestEntity
import com.niranjan.medqueue.data.local.medicineLines
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

    // One host shared by the queue and detail screens, so a snackbar raised on
    // one survives the navigation that follows the action that raised it.
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val deliveredMessage = stringResource(R.string.delivered_undo_message)
    val undoLabel = stringResource(R.string.action_undo)
    val contactFailed = stringResource(R.string.queue_notify_failed)

    /**
     * Marks a request delivered, and nothing else.
     *
     * This used to fire the availability message too, which had it backwards:
     * "delivered" means the customer already walked in and took the medicine,
     * so messaging them "your medicines are ready, please collect" at that
     * moment reached someone who had just collected. Telling the customer is
     * the separate Notify action, which is where the auto-send setting applies.
     */
    fun deliver(request: RequestEntity) {
        vm.markDelivered(request.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = deliveredMessage,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) vm.markPending(request.id)
        }
    }

    // System back walks the hierarchy instead of dropping straight out of the
    // app. The list is the root, so back there keeps the default behaviour.
    BackHandler(
        enabled = screen !is Screen.RequestList &&
                screen !is Screen.Splash &&
                screen !is Screen.Onboarding
    ) {
        screen = when (val s = screen) {
            is Screen.EditRequest   -> Screen.RequestDetail(s.requestId)
            Screen.MessageTemplate  -> Screen.Settings
            else                    -> Screen.RequestList
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
            snackbarHostState = snackbarHostState,
            bottomBar = bottomBar
        )

        Screen.RequestList -> RequestListScreen(
            requests = requests,
            snackbarHostState = snackbarHostState,
            onAddClick = { screen = Screen.Home },
            onItemClick = { screen = Screen.RequestDetail(it.id) },
            onNotify = { request ->
                val message = buildMessage(
                    settings = settingsPrefs.read(),
                    action = ContactAction.WHATSAPP,
                    templates = settingsPrefs.templates(),
                    customerName = request.customerName,
                    medicines = request.medicineLines()
                )
                val result = launchContactAction(
                    context, ContactAction.WHATSAPP, request.phoneNumber, message
                )
                when (result) {
                    ContactActionResult.Success,
                    is ContactActionResult.AutoSent -> vm.markNotified(request.id)
                    else -> scope.launch { snackbarHostState.showSnackbar(contactFailed) }
                }
            },
            onDeliver = { request -> deliver(request) },
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
                    snackbarHostState = snackbarHostState,
                    onEdit = { screen = Screen.EditRequest(request.id) },
                    onDelivered = {
                        deliver(request)
                        screen = Screen.RequestList
                    },
                    onNotified = { vm.markNotified(request.id) },
                    onToggleItem = { index, ready -> vm.setItemReady(request.id, index, ready) },
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
                    snackbarHostState = snackbarHostState,
                    bottomBar = bottomBar
                )
            }
        }

        Screen.Settings -> SettingsScreen(
            settingsPrefs = settingsPrefs,
            onBack = { screen = Screen.RequestList },
            onEditTemplate = { screen = Screen.MessageTemplate },
            snackbarHostState = snackbarHostState,
            bottomBar = bottomBar
        )

        Screen.MessageTemplate -> MessageTemplateScreen(
            settingsPrefs = settingsPrefs,
            onBack = { screen = Screen.Settings },
            snackbarHostState = snackbarHostState,
            bottomBar = bottomBar
        )
    }
}
