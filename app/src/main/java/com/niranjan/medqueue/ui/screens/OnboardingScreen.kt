package com.niranjan.medqueue.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.niranjan.medqueue.R
import com.niranjan.medqueue.autosend.AutoSendPrefs
import com.niranjan.medqueue.data.settings.AppSettings
import com.niranjan.medqueue.data.settings.SettingsPrefs
import com.niranjan.medqueue.reminders.Reminders
import com.niranjan.medqueue.ui.components.*
import com.niranjan.medqueue.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════
// ONBOARDING — first-run shop setup
//
// Two steps, matching the mockup's progress dots: shop identity first (it feeds
// every customer message), then the reminder preferences.
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun OnboardingScreen(
    settingsPrefs: SettingsPrefs,
    onFinished: () -> Unit
) {
    val context = LocalContext.current

    var step by rememberSaveable { mutableIntStateOf(0) }

    var shopName    by rememberSaveable { mutableStateOf("") }
    var shopAddress by rememberSaveable { mutableStateOf("") }
    var ownerName   by rememberSaveable { mutableStateOf("") }
    var ownerPhone  by rememberSaveable { mutableStateOf("") }

    var shopNameError  by rememberSaveable { mutableStateOf<String?>(null) }
    var ownerNameError by rememberSaveable { mutableStateOf<String?>(null) }
    var phoneError     by rememberSaveable { mutableStateOf<String?>(null) }

    var autoSendWhatsApp by rememberSaveable { mutableStateOf(true) }
    var pendingAlerts    by rememberSaveable { mutableStateOf(true) }
    var dailySummary     by rememberSaveable { mutableStateOf(false) }

    // Reminders are the whole point of step 2, so ask for the permission there.
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Either answer is fine — the workers no-op when posting is blocked. */ }

    val requiredMissing = stringResource(R.string.phone_required)
    val phoneInvalid    = stringResource(R.string.phone_invalid)

    Scaffold(
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            ScreenHeader(
                title = stringResource(
                    if (step == 0) R.string.onboarding_title else R.string.onboarding_prefs_title
                ),
                subtitle = stringResource(
                    if (step == 0) R.string.onboarding_subtitle else R.string.onboarding_prefs_subtitle
                ),
                topPadding = 28.dp
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (step == 0) {
                    DsCard(spacing = 16.dp) {
                        Eyebrow(stringResource(R.string.onboarding_shop_details))
                        DsField(
                            label = stringResource(R.string.label_shop_name),
                            value = shopName,
                            onValueChange = { shopName = it; shopNameError = null },
                            placeholder = stringResource(R.string.label_shop_name_hint),
                            required = true,
                            error = shopNameError
                        )
                        DsField(
                            label = stringResource(R.string.label_address),
                            value = shopAddress,
                            onValueChange = { shopAddress = it },
                            placeholder = stringResource(R.string.label_address_hint)
                        )
                    }

                    DsCard(spacing = 16.dp) {
                        Eyebrow(stringResource(R.string.section_primary_contact))
                        DsField(
                            label = stringResource(R.string.onboarding_your_name),
                            value = ownerName,
                            onValueChange = { ownerName = it; ownerNameError = null },
                            placeholder = stringResource(R.string.onboarding_your_name_hint),
                            required = true,
                            error = ownerNameError
                        )
                        DsField(
                            label = stringResource(R.string.label_phone),
                            value = ownerPhone,
                            onValueChange = { raw ->
                                ownerPhone = raw.filter(Char::isDigit).take(10)
                                phoneError = null
                            },
                            placeholder = stringResource(R.string.label_phone_hint),
                            required = true,
                            prefix = stringResource(R.string.phone_prefix),
                            error = phoneError,
                            keyboardType = KeyboardType.Phone
                        )
                    }

                    DsCard {
                        ToggleRow(
                            title = stringResource(R.string.auto_send_whatsapp),
                            subtitle = stringResource(R.string.auto_send_whatsapp_sub_onboarding),
                            checked = autoSendWhatsApp,
                            onCheckedChange = { autoSendWhatsApp = it }
                        )
                    }
                } else {
                    DsCard(spacing = 16.dp) {
                        Eyebrow(stringResource(R.string.section_reminders))
                        ToggleRow(
                            title = stringResource(R.string.reminder_pending_alerts),
                            subtitle = stringResource(R.string.reminder_pending_alerts_sub),
                            checked = pendingAlerts,
                            onCheckedChange = { on ->
                                pendingAlerts = on
                                if (on) requestNotifications(notificationLauncher::launch)
                            }
                        )
                        ToggleRow(
                            title = stringResource(R.string.reminder_daily_summary),
                            subtitle = stringResource(R.string.reminder_daily_summary_sub),
                            checked = dailySummary,
                            onCheckedChange = { on ->
                                dailySummary = on
                                if (on) requestNotifications(notificationLauncher::launch)
                            }
                        )
                    }
                }
            }

            // ── Footer: action + progress dots ──────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryButton(
                    text = stringResource(
                        if (step == 0) R.string.onboarding_continue else R.string.onboarding_get_started
                    ),
                    onClick = {
                        if (step == 0) {
                            shopNameError  = if (shopName.isBlank()) requiredMissing else null
                            ownerNameError = if (ownerName.isBlank()) requiredMissing else null
                            phoneError = when {
                                ownerPhone.isBlank()   -> requiredMissing
                                ownerPhone.length < 10 -> phoneInvalid
                                else                   -> null
                            }
                            val ok = shopNameError == null &&
                                    ownerNameError == null &&
                                    phoneError == null
                            if (ok) {
                                step = 1
                                requestNotifications(notificationLauncher::launch)
                            }
                        } else {
                            settingsPrefs.save(
                                AppSettings(
                                    shopName      = shopName.trim(),
                                    shopAddress   = shopAddress.trim(),
                                    contact1Name  = ownerName.trim(),
                                    contact1Phone = ownerPhone
                                )
                            )
                            AutoSendPrefs.setAutoSendEnabled(context, autoSendWhatsApp)
                            settingsPrefs.setPendingAlertsEnabled(pendingAlerts)
                            settingsPrefs.setDailySummaryEnabled(dailySummary)
                            settingsPrefs.setOnboarded(true)
                            Reminders.sync(context)
                            onFinished()
                        }
                    }
                )

                if (step == 1) {
                    QuietButton(
                        text = stringResource(R.string.onboarding_back),
                        onClick = { step = 0 }
                    )
                }

                StepDots(current = step, total = 2, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

/** Elongated pill for the active step, small dots for the rest. */
@Composable
private fun StepDots(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val active = index == current
            Box(
                modifier = Modifier
                    .height(5.dp)
                    .width(if (active) 16.dp else 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (active) Teal else TrackOff)
            )
        }
    }
}

/** Only API 33+ has a notification permission to ask for. */
private inline fun requestNotifications(launch: (String) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
