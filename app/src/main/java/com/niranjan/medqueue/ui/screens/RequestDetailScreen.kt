@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.niranjan.medqueue.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.niranjan.medqueue.R
import com.niranjan.medqueue.contact.*
import com.niranjan.medqueue.data.local.RequestEntity
import com.niranjan.medqueue.data.local.RequestStatus
import com.niranjan.medqueue.data.local.stage
import com.niranjan.medqueue.data.settings.SettingsPrefs
import com.niranjan.medqueue.prescription.PrescriptionStore
import com.niranjan.medqueue.ui.components.*
import com.niranjan.medqueue.ui.formatFull
import com.niranjan.medqueue.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

// ══════════════════════════════════════════════════════════════════════════════
// REQUEST DETAIL
//
// The redesign replaces the old "Contact" bottom sheet with three always-visible
// buttons, so reaching a customer is one tap rather than two.
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun RequestDetailScreen(
    request: RequestEntity,
    settingsPrefs: SettingsPrefs,
    snackbarHostState: SnackbarHostState,
    onEdit: () -> Unit,
    onDelivered: () -> Unit,
    onNotified: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPhotoViewer by remember { mutableStateOf(false) }

    val isPending = request.status == RequestStatus.PENDING
    val medicines = remember(request.medicineName) {
        request.medicineName.lines().filter { it.isNotBlank() }
    }

    // Resolved here because stringResource is only callable from a composable,
    // and the dispatch below runs from callbacks.
    val labels = ContactLabels(
        invalidPhone     = stringResource(R.string.error_invalid_phone),
        whatsAppMissing  = stringResource(R.string.error_whatsapp_missing),
        noHandler        = stringResource(R.string.error_no_handler),
        sendFailed       = stringResource(R.string.error_send_failed),
        permissionDenied = stringResource(R.string.error_sms_permission_denied),
        smsSent          = stringResource(R.string.sms_sent),
        retry            = stringResource(R.string.action_retry),
        smsInstead       = stringResource(R.string.action_send_sms_instead)
    )
    val smsSentParts = { parts: Int -> context.getString(R.string.sms_sent_parts, parts) }

    val feedback: (String, String?, () -> Unit) -> Unit = { text, actionLabel, action ->
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = text,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) action()
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Retry the send now that we can. The permission callback is a
            // no-op this time round so a repeated denial cannot loop.
            runContact(
                context, settingsPrefs, ContactAction.SMS, request.phoneNumber,
                labels, smsSentParts, onNotified, onNeedsSmsPermission = {}, onFeedback = feedback
            )
        } else {
            feedback(labels.permissionDenied, null) {}
        }
    }

    val dispatch: (ContactAction) -> Unit = { action ->
        runContact(
            context, settingsPrefs, action, request.phoneNumber,
            labels, smsSentParts, onNotified,
            onNeedsSmsPermission = { smsPermissionLauncher.launch(Manifest.permission.SEND_SMS) },
            onFeedback = feedback
        )
    }

    Scaffold(
        bottomBar = bottomBar,
        containerColor = Paper,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            DetailHeader(
                title = stringResource(R.string.detail_title),
                onBack = onBack,
                backLabel = stringResource(R.string.action_back),
                actions = {
                    HeaderAction(stringResource(R.string.action_edit), Teal, onEdit)
                    // Delete lives behind an overflow rather than as a second
                    // small text target 14dp from Edit.
                    OverflowDelete(
                        menuLabel = stringResource(R.string.action_more),
                        deleteLabel = stringResource(R.string.action_delete),
                        onDelete = { showDeleteConfirm = true }
                    )
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Hero ────────────────────────────────────────────────────
                DsCard(spacing = 0.dp) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        PhoneTile(request.phoneNumber, size = 46.dp, radius = 13.dp)

                        Column(modifier = Modifier.weight(1f)) {
                            // Number and badge share a line when they fit and
                            // the badge drops to its own line when they don't,
                            // mirroring the mockup's flex-wrap. A plain Row
                            // would hand the number all the width and crush
                            // the badge to a single-character column.
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = formatForDisplay(request.phoneNumber),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Ink,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                                if (request.isEmergency) {
                                    Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                                        UrgentPill(stringResource(R.string.badge_emergency))
                                    }
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = request.customerName.ifBlank {
                                    stringResource(R.string.no_name_provided)
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                                color = Muted
                            )
                        }

                        StatusPill(
                            stage = request.stage,
                            pendingLabel = stringResource(R.string.status_pending),
                            notifiedLabel = stringResource(R.string.status_notified),
                            deliveredLabel = stringResource(R.string.status_delivered)
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.requested_at, formatFull(request.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                    // The whole point of tracking this: whether the customer has
                    // already been told, and when.
                    request.notifiedAt?.let { at ->
                        Text(
                            text = stringResource(R.string.notified_at, formatFull(at)),
                            style = MaterialTheme.typography.bodySmall,
                            color = Teal
                        )
                    }
                }

                // ── Medicines ───────────────────────────────────────────────
                if (medicines.isNotEmpty()) {
                    DsCard(spacing = 10.dp) {
                        Eyebrow(stringResource(R.string.section_medicines))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            medicines.forEachIndexed { index, med ->
                                Text(
                                    text = "${index + 1}. $med",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Ink
                                )
                            }
                        }
                    }
                }

                // ── Prescription ────────────────────────────────────────────
                DsCard(spacing = 12.dp) {
                    Eyebrow(stringResource(R.string.section_prescription))
                    val shape = RoundedCornerShape(14.dp)
                    val hasPhoto = remember(request.prescriptionPath) {
                        PrescriptionStore.exists(request.prescriptionPath)
                    }
                    if (hasPhoto) {
                        AsyncImage(
                            model = File(request.prescriptionPath!!),
                            contentDescription = stringResource(R.string.action_view_photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .clip(shape)
                                .border(1.5.dp, Line, shape)
                                .clickable { showPhotoViewer = true }
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(shape)
                                .background(Paper)
                                .border(1.5.dp, Line, shape)
                        ) {
                            Text(
                                stringResource(R.string.prescription_none),
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted
                            )
                        }
                    }
                }
            }

            // ── Footer actions ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ContactButton(
                        label = stringResource(R.string.action_call),
                        icon = { Icon(Icons.Filled.Call, null, modifier = Modifier.size(16.dp)) },
                        background = Paper,
                        foreground = Ink,
                        onClick = { dispatch(ContactAction.CALL) }
                    )
                    ContactButton(
                        label = stringResource(R.string.action_whatsapp),
                        icon = {
                            Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(16.dp))
                        },
                        background = WhatsAppTint,
                        foreground = WhatsAppInk,
                        onClick = { dispatch(ContactAction.WHATSAPP) }
                    )
                    ContactButton(
                        label = stringResource(R.string.action_sms),
                        icon = { Icon(Icons.Filled.MailOutline, null, modifier = Modifier.size(16.dp)) },
                        background = Paper,
                        foreground = Ink,
                        onClick = { dispatch(ContactAction.SMS) }
                    )
                }

                if (isPending) {
                    PrimaryButton(
                        text = stringResource(R.string.mark_delivered),
                        onClick = onDelivered
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        DeleteDialog(
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            }
        )
    }

    if (showPhotoViewer && request.prescriptionPath != null) {
        ImageViewerDialog(
            file = File(request.prescriptionPath),
            contentDescription = stringResource(R.string.section_prescription),
            closeLabel = stringResource(R.string.action_close),
            onDismiss = { showPhotoViewer = false }
        )
    }
}

// ── Contact dispatch ──────────────────────────────────────────────────────────

/** Strings the contact dispatch needs, resolved once in composition. */
private data class ContactLabels(
    val invalidPhone: String,
    val whatsAppMissing: String,
    val noHandler: String,
    val sendFailed: String,
    val permissionDenied: String,
    val smsSent: String,
    val retry: String,
    val smsInstead: String
)

/**
 * Runs a contact action and reports the outcome through [onFeedback], which is
 * wired to a snackbar rather than a toast — a toast cannot carry the "Retry"
 * or "Send SMS instead" that these failures need to be recoverable.
 *
 * Top-level rather than a local lambda so the recovery actions can re-enter it.
 */
private fun runContact(
    context: Context,
    settingsPrefs: SettingsPrefs,
    action: ContactAction,
    phone: String,
    labels: ContactLabels,
    smsSentParts: (Int) -> String,
    onNotified: () -> Unit,
    onNeedsSmsPermission: () -> Unit,
    onFeedback: (String, String?, () -> Unit) -> Unit
) {
    val message = buildMessage(settingsPrefs.read(), action)
    when (val result = launchContactAction(context, action, phone, message)) {
        // WhatsApp and SMS both count as telling the customer. A call only
        // opens the dialler, so it proves nothing about whether they heard.
        ContactActionResult.Success ->
            if (action != ContactAction.CALL) onNotified()

        is ContactActionResult.AutoSent -> {
            onNotified()
            val text = if (result.parts > 1) smsSentParts(result.parts) else labels.smsSent
            onFeedback(text, null) {}
        }

        ContactActionResult.InvalidPhone ->
            onFeedback(labels.invalidPhone, null) {}

        ContactActionResult.WhatsAppNotInstalled ->
            onFeedback(labels.whatsAppMissing, labels.smsInstead) {
                runContact(
                    context, settingsPrefs, ContactAction.SMS, phone,
                    labels, smsSentParts, onNotified, onNeedsSmsPermission, onFeedback
                )
            }

        ContactActionResult.NoHandler ->
            onFeedback(labels.noHandler, null) {}

        ContactActionResult.SendFailed ->
            onFeedback(labels.sendFailed, labels.retry) {
                runContact(
                    context, settingsPrefs, action, phone,
                    labels, smsSentParts, onNotified, onNeedsSmsPermission, onFeedback
                )
            }

        ContactActionResult.SmsPermissionNeeded ->
            onNeedsSmsPermission()
    }
}

// ── Overflow ──────────────────────────────────────────────────────────────────

@Composable
private fun OverflowDelete(menuLabel: String, deleteLabel: String, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = menuLabel,
                tint = Ink,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        deleteLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = Red
                    )
                },
                onClick = {
                    open = false
                    onDelete()
                }
            )
        }
    }
}

// ── Delete confirmation ───────────────────────────────────────────────────────

@Composable
private fun DeleteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(RedTint)
            ) {
                Text("!", style = MaterialTheme.typography.titleLarge, color = Red)
            }
        },
        title = {
            Text(
                stringResource(R.string.delete_confirm_title),
                style = MaterialTheme.typography.titleSmall,
                color = Ink
            )
        },
        text = {
            Text(
                stringResource(R.string.delete_confirm_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.action_delete),
                    style = MaterialTheme.typography.labelLarge,
                    color = Red
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = Muted
                )
            }
        }
    )
}
