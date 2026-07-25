package com.niranjan.medqueue.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.medqueue.contact.ContactAction
import com.niranjan.medqueue.contact.ContactActionResult
import com.niranjan.medqueue.contact.buildMessage
import com.niranjan.medqueue.contact.launchContactAction
import com.niranjan.medqueue.autosend.AutoSendPrefs
import com.niranjan.medqueue.data.local.RequestEntity
import com.niranjan.medqueue.data.local.RequestStatus
import com.niranjan.medqueue.data.settings.SettingsPrefs
import com.niranjan.medqueue.ui.components.EmergencyBadge
import com.niranjan.medqueue.ui.components.GradientHeader
import com.niranjan.medqueue.ui.components.InitialsAvatar
import com.niranjan.medqueue.ui.components.SectionLabel
import com.niranjan.medqueue.ui.components.StatusBadge
import com.niranjan.medqueue.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════════════
// ── REQUEST DETAIL SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    request       : RequestEntity,
    isEmergency   : Boolean = false,
    settingsPrefs : SettingsPrefs,
    onEdit        : () -> Unit,
    onDelivered   : () -> Unit,
    onDelete      : () -> Unit,
    onBack        : () -> Unit,
    bottomBar     : @Composable () -> Unit = {}
) {
    val context         = LocalContext.current
    var showContactSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dateFormat      = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val displayName     = request.customerName.ifBlank { "Unknown Customer" }
    val isPending       = request.status == RequestStatus.PENDING

    val medicineLines = remember(request.medicineName) {
        request.medicineName.lines().filter { it.isNotBlank() }
    }

    // SMS permission launcher — when granted, retry the SMS send
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission just granted — retry the auto-send
            val message = buildMessage(settingsPrefs.read(), ContactAction.SMS)
            val result  = launchContactAction(context, ContactAction.SMS, request.phoneNumber, message)
            val toast   = when (result) {
                is ContactActionResult.AutoSent -> smsSentMessage(result.parts)
                else                            -> "SMS failed — try again"
            }
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "SMS permission denied", Toast.LENGTH_SHORT).show()
            // Disable the auto-send toggle since user denied
            AutoSendPrefs.setSmsAutoSendEnabled(context, false)
        }
    }

    Scaffold(
        topBar = {
            GradientHeader(
                title  = "Request Detail",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, "Edit",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, "Delete",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp))
                    }
                }
            )
        },
        bottomBar      = bottomBar,
        containerColor = PageBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Hero card ────────────────────────────────────────────────
            val heroBg = if (isEmergency) CardEmergencyBg else if (isPending) CardPendingBg else CardDeliveredBg
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = heroBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        InitialsAvatar(name = displayName, size = 52)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text  = displayName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Phone, null,
                                    modifier = Modifier.size(14.dp),
                                    tint     = MaterialTheme.colorScheme.primary)
                                Text(
                                    text  = "+91 ${request.phoneNumber}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatusBadge(status = request.status)
                            if (isEmergency) EmergencyBadge()
                        }
                    }

                    HorizontalDivider(
                        modifier  = Modifier.padding(vertical = 14.dp),
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.DateRange, null,
                            modifier = Modifier.size(14.dp),
                            tint     = MaterialTheme.colorScheme.outline)
                        Text(
                            text  = dateFormat.format(Date(request.createdAt)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // ── Medicines card ────────────────────────────────────────────
            ElevatedCard(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(18.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                colors    = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SectionLabel("💊", "Medicines", "${medicineLines.size} item${if (medicineLines.size != 1) "s" else ""}")
                    Spacer(Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        medicineLines.forEachIndexed { index, med ->
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Text(
                                        text      = "${index + 1}",
                                        fontSize  = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color     = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text  = med,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // ── Action buttons ────────────────────────────────────────────
            Spacer(Modifier.height(2.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick  = { showContactSheet = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    border   = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Contact", fontWeight = FontWeight.SemiBold)
                }
                if (isPending) {
                    Button(
                        onClick  = onDelivered,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = StatusDeliveredBg,
                            contentColor   = StatusDeliveredContent
                        )
                    ) {
                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mark Delivered", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // ── Contact sheet ─────────────────────────────────────────────────────
    if (showContactSheet) {
        ContactActionSheet(
            onDismiss = { showContactSheet = false },
            onAction  = { action ->
                showContactSheet = false
                val message = buildMessage(settingsPrefs.read(), action)
                val result  = launchContactAction(context, action, request.phoneNumber, message)
                when (result) {
                    ContactActionResult.Success              -> { /* opened app */ }
                    is ContactActionResult.AutoSent          ->
                        Toast.makeText(context, smsSentMessage(result.parts), Toast.LENGTH_SHORT).show()
                    ContactActionResult.InvalidPhone         ->
                        Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show()
                    ContactActionResult.WhatsAppNotInstalled ->
                        Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                    ContactActionResult.NoHandler            ->
                        Toast.makeText(context, "No app found to handle this action", Toast.LENGTH_SHORT).show()
                    ContactActionResult.SendFailed           ->
                        Toast.makeText(context, "Could not send — try again", Toast.LENGTH_SHORT).show()
                    ContactActionResult.SmsPermissionNeeded  ->
                        smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                }
            }
        )
    }

    // ── Delete confirmation dialog ────────────────────────────────────────
    if (showDeleteConfirm) {
        AlertDialog(onDismissRequest = { showDeleteConfirm = false }) {
            Card(
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Icon circle ──
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(StatusEmergencyBg.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint     = StatusEmergencyBg,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Title ──
                    Text(
                        text  = "Delete Request",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(8.dp))

                    // ── Message ──
                    Text(
                        text  = "Do you really want to delete this request?\nThis action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── Buttons ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel button — outlined, uses app primary green
                        OutlinedButton(
                            onClick  = { showDeleteConfirm = false },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape    = RoundedCornerShape(14.dp),
                            border   = ButtonDefaults.outlinedButtonBorder(enabled = true)
                        ) {
                            Text(
                                "Cancel",
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Delete button — solid red matching emergency badge
                        Button(
                            onClick  = {
                                showDeleteConfirm = false
                                onDelete()
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = StatusEmergencyBg,
                                contentColor   = StatusEmergencyContent
                            )
                        ) {
                            Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * A background SMS is billed per part, and the bilingual template runs to
 * seven of them, so report the count rather than a silent "sent".
 */
private fun smsSentMessage(parts: Int): String =
    if (parts > 1) "SMS sent ✓ ($parts messages)" else "SMS sent ✓"

// ══════════════════════════════════════════════════════════════════════════════
// ── CONTACT ACTION SHEET
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactActionSheet(
    onDismiss : () -> Unit,
    onAction  : (ContactAction) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 28.dp)) {
            Text(
                text     = "Contact customer via",
                style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            data class ActionItem(val label: String, val sub: String, val icon: @Composable () -> Unit, val action: ContactAction)
            val items = listOf(
                ActionItem("WhatsApp", "Send a WhatsApp message",
                    { Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color(0xFF25D366)) }, ContactAction.WHATSAPP),
                ActionItem("SMS", "Send a text message",
                    { Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.secondary) }, ContactAction.SMS),
                ActionItem("Call", "Place a phone call",
                    { Icon(Icons.Filled.Phone, null, tint = MaterialTheme.colorScheme.tertiary) }, ContactAction.CALL)
            )
            items.forEach { item ->
                ListItem(
                    headlineContent    = { Text(item.label, fontWeight = FontWeight.Medium) },
                    supportingContent  = { Text(item.sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent     = item.icon,
                    trailingContent    = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp)) },
                    modifier           = Modifier.clickable { onAction(item.action) }
                )
            }
        }
    }
}

