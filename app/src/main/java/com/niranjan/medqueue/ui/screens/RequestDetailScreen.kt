package com.niranjan.medqueue.ui.screens

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MailOutline
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
import com.niranjan.medqueue.data.settings.SettingsPrefs
import com.niranjan.medqueue.prescription.PrescriptionStore
import com.niranjan.medqueue.ui.components.*
import com.niranjan.medqueue.ui.formatFull
import com.niranjan.medqueue.ui.theme.*
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
    onEdit: () -> Unit,
    onDelivered: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isPending = request.status == RequestStatus.PENDING
    val medicines = remember(request.medicineName) {
        request.medicineName.lines().filter { it.isNotBlank() }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            send(context, settingsPrefs, ContactAction.SMS, request.phoneNumber) { }
        } else {
            Toast.makeText(context, "SMS permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val dispatch: (ContactAction) -> Unit = { action ->
        send(context, settingsPrefs, action, request.phoneNumber) {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    Scaffold(
        bottomBar = bottomBar,
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            DetailHeader(
                title = stringResource(R.string.detail_title),
                onBack = onBack,
                backLabel = stringResource(R.string.action_back),
                actions = {
                    HeaderAction(stringResource(R.string.action_edit), Teal, onEdit)
                    HeaderAction(stringResource(R.string.action_delete), Red) { showDeleteConfirm = true }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        PhoneTile(request.phoneNumber, size = 46.dp, radius = 13.dp)

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = formatForDisplay(request.phoneNumber),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Ink
                                )
                                if (request.isEmergency) {
                                    UrgentPill(stringResource(R.string.badge_emergency))
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
                            status = request.status,
                            pendingLabel = stringResource(R.string.status_pending),
                            deliveredLabel = stringResource(R.string.status_delivered)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.requested_at, formatFull(request.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

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
                            contentDescription = stringResource(R.string.section_prescription),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .clip(shape)
                                .border(1.5.dp, Line, shape)
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
}

// ── Contact dispatch ──────────────────────────────────────────────────────────

/**
 * Runs a contact action and reports the outcome. [onNeedsSmsPermission] is
 * invoked instead of a toast when the SMS path needs a grant first.
 */
private fun send(
    context: Context,
    settingsPrefs: SettingsPrefs,
    action: ContactAction,
    phone: String,
    onNeedsSmsPermission: () -> Unit
) {
    val message = buildMessage(settingsPrefs.read(), action)
    when (val result = launchContactAction(context, action, phone, message)) {
        ContactActionResult.Success -> Unit
        is ContactActionResult.AutoSent ->
            Toast.makeText(context, smsSentMessage(result.parts), Toast.LENGTH_SHORT).show()
        ContactActionResult.InvalidPhone ->
            Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show()
        ContactActionResult.WhatsAppNotInstalled ->
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        ContactActionResult.NoHandler ->
            Toast.makeText(context, "No app found to handle this action", Toast.LENGTH_SHORT).show()
        ContactActionResult.SendFailed ->
            Toast.makeText(context, "Could not send — try again", Toast.LENGTH_SHORT).show()
        ContactActionResult.SmsPermissionNeeded -> onNeedsSmsPermission()
    }
}

/**
 * A background SMS is billed per part, and the bilingual template runs to
 * seven of them, so report the count rather than a silent "sent".
 */
internal fun smsSentMessage(parts: Int): String =
    if (parts > 1) "SMS sent ✓ ($parts messages)" else "SMS sent ✓"

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
