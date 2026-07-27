package com.niranjan.medqueue.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.niranjan.medqueue.R
import com.niranjan.medqueue.autosend.AutoSendPrefs
import com.niranjan.medqueue.contact.buildMessage
import com.niranjan.medqueue.data.settings.AppSettings
import com.niranjan.medqueue.data.settings.SettingsPrefs
import com.niranjan.medqueue.reminders.Notifications
import com.niranjan.medqueue.reminders.Reminders
import com.niranjan.medqueue.ui.components.*
import com.niranjan.medqueue.ui.theme.*
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════════════
// SETTINGS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsScreen(
    settingsPrefs: SettingsPrefs,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    bottomBar: @Composable () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val saved = remember { settingsPrefs.read() }

    var shopName      by rememberSaveable { mutableStateOf(saved.shopName) }
    var shopAddress   by rememberSaveable { mutableStateOf(saved.shopAddress) }
    var contact1Name  by rememberSaveable { mutableStateOf(saved.contact1Name) }
    var contact1Phone by rememberSaveable { mutableStateOf(saved.contact1Phone) }
    var contact2Name  by rememberSaveable { mutableStateOf(saved.contact2Name) }
    var contact2Phone by rememberSaveable { mutableStateOf(saved.contact2Phone) }

    val preview = remember(shopName, shopAddress, contact1Name, contact1Phone, contact2Name, contact2Phone) {
        buildMessage(
            AppSettings(shopName, shopAddress, contact1Name, contact1Phone, contact2Name, contact2Phone)
        )
    }

    val settingsSaved = stringResource(R.string.settings_saved)

    Scaffold(
        bottomBar = bottomBar,
        containerColor = Paper,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            ScreenHeader(
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.settings_subtitle)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Shop ────────────────────────────────────────────────────
                DsCard(spacing = 14.dp) {
                    Eyebrow(stringResource(R.string.section_shop))
                    DsField(
                        label = stringResource(R.string.label_shop_name),
                        value = shopName,
                        onValueChange = { shopName = it },
                        placeholder = stringResource(R.string.label_shop_name_hint)
                    )
                    DsField(
                        label = stringResource(R.string.label_address),
                        value = shopAddress,
                        onValueChange = { shopAddress = it },
                        placeholder = stringResource(R.string.label_address_hint)
                    )
                }

                // ── Primary contact ─────────────────────────────────────────
                DsCard(spacing = 14.dp) {
                    Eyebrow(stringResource(R.string.section_primary_contact))
                    DsField(
                        label = stringResource(R.string.label_name),
                        value = contact1Name,
                        onValueChange = { contact1Name = it }
                    )
                    DsField(
                        label = stringResource(R.string.label_phone),
                        value = contact1Phone,
                        onValueChange = { raw -> contact1Phone = raw.filter(Char::isDigit).take(10) },
                        prefix = stringResource(R.string.phone_prefix),
                        keyboardType = KeyboardType.Phone
                    )
                }

                // ── Secondary contact ───────────────────────────────────────
                // Not in the mockup, which assumed the short one-line template.
                // The bilingual template renders this contact, so the field has
                // to stay reachable or that line goes permanently blank.
                DsCard(spacing = 14.dp) {
                    Eyebrow(stringResource(R.string.section_secondary_contact))
                    DsField(
                        label = stringResource(R.string.label_name),
                        value = contact2Name,
                        onValueChange = { contact2Name = it },
                        optionalHint = stringResource(R.string.label_optional)
                    )
                    DsField(
                        label = stringResource(R.string.label_phone),
                        value = contact2Phone,
                        onValueChange = { raw -> contact2Phone = raw.filter(Char::isDigit).take(10) },
                        prefix = stringResource(R.string.phone_prefix),
                        keyboardType = KeyboardType.Phone
                    )
                }

                // ── Message preview ─────────────────────────────────────────
                DsCard(spacing = 10.dp) {
                    Eyebrow(stringResource(R.string.section_message_preview))
                    Surface(shape = RoundedCornerShape(14.dp), color = Paper) {
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                AutoSendWhatsAppCard()
                AutoSendSmsCard()
                RemindersCard(settingsPrefs)
            }

            // ── Save ────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 12.dp)
            ) {
                PrimaryButton(
                    text = stringResource(R.string.save_settings),
                    onClick = {
                        settingsPrefs.save(
                            AppSettings(
                                shopName.trim(), shopAddress.trim(),
                                contact1Name.trim(), contact1Phone,
                                contact2Name.trim(), contact2Phone
                            )
                        )
                        scope.launch { snackbarHostState.showSnackbar(settingsSaved) }
                        onBack()
                    }
                )
            }
        }
    }
}

// ── Auto-send: WhatsApp ───────────────────────────────────────────────────────

@Composable
private fun AutoSendWhatsAppCard() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(AutoSendPrefs.isAutoSendEnabled(context)) }
    var serviceActive by remember { mutableStateOf(AutoSendPrefs.isServiceEnabled(context)) }

    // The accessibility service is toggled outside our process, so the only
    // reliable moment to re-read it is when we return to the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                serviceActive = AutoSendPrefs.isServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DsCard(spacing = 12.dp) {
        ToggleRow(
            title = stringResource(R.string.auto_send_whatsapp),
            subtitle = stringResource(R.string.auto_send_whatsapp_sub),
            checked = enabled,
            onCheckedChange = { on ->
                enabled = on
                AutoSendPrefs.setAutoSendEnabled(context, on)
                if (on && !AutoSendPrefs.isServiceEnabled(context)) {
                    AutoSendPrefs.openAccessibilitySettings(context)
                }
            }
        )
        if (enabled) {
            StatusNote(
                text = stringResource(
                    if (serviceActive) R.string.auto_send_service_active
                    else R.string.auto_send_service_inactive
                ),
                ok = serviceActive,
                onClick = { AutoSendPrefs.openAccessibilitySettings(context) }
            )
        }
    }
}

// ── Auto-send: SMS ────────────────────────────────────────────────────────────

@Composable
private fun AutoSendSmsCard() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(AutoSendPrefs.isSmsAutoSendEnabled(context)) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        enabled = granted
        AutoSendPrefs.setSmsAutoSendEnabled(context, granted)
    }

    DsCard(spacing = 12.dp) {
        ToggleRow(
            title = stringResource(R.string.auto_send_sms),
            subtitle = stringResource(R.string.auto_send_sms_sub),
            checked = enabled,
            onCheckedChange = { on ->
                if (on && !hasPermission) {
                    launcher.launch(Manifest.permission.SEND_SMS)
                } else {
                    enabled = on
                    AutoSendPrefs.setSmsAutoSendEnabled(context, on)
                }
            }
        )
        if (enabled) {
            StatusNote(
                text = stringResource(
                    if (hasPermission) R.string.auto_send_sms_permission_granted
                    else R.string.auto_send_sms_permission_denied
                ),
                ok = hasPermission,
                onClick = { launcher.launch(Manifest.permission.SEND_SMS) }
            )
        }
    }
}

// ── Reminders ─────────────────────────────────────────────────────────────────

@Composable
private fun RemindersCard(settingsPrefs: SettingsPrefs) {
    val context = LocalContext.current
    var pendingAlerts by remember { mutableStateOf(settingsPrefs.pendingAlertsEnabled()) }
    var dailySummary  by remember { mutableStateOf(settingsPrefs.dailySummaryEnabled()) }
    var canPost       by remember { mutableStateOf(Notifications.canPost(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        canPost = granted
        Reminders.sync(context)
    }

    fun askIfNeeded() {
        if (!canPost && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DsCard(spacing = 14.dp) {
        Eyebrow(stringResource(R.string.section_reminders))

        ToggleRow(
            title = stringResource(R.string.reminder_pending_alerts),
            subtitle = stringResource(R.string.reminder_pending_alerts_sub),
            checked = pendingAlerts,
            onCheckedChange = { on ->
                pendingAlerts = on
                settingsPrefs.setPendingAlertsEnabled(on)
                if (on) askIfNeeded()
                Reminders.sync(context)
            }
        )

        ToggleRow(
            title = stringResource(R.string.reminder_daily_summary),
            subtitle = stringResource(R.string.reminder_daily_summary_sub),
            checked = dailySummary,
            onCheckedChange = { on ->
                dailySummary = on
                settingsPrefs.setDailySummaryEnabled(on)
                if (on) askIfNeeded()
                Reminders.sync(context)
            }
        )

        if ((pendingAlerts || dailySummary) && !canPost) {
            StatusNote(
                text = stringResource(R.string.notifications_blocked),
                ok = false,
                onClick = { askIfNeeded() }
            )
        }
    }
}
