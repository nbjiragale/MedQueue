package com.niranjan.medqueue.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.niranjan.medqueue.R
import com.niranjan.medqueue.contact.DEFAULT_SMS_TEMPLATE
import com.niranjan.medqueue.contact.DEFAULT_WHATSAPP_TEMPLATE
import com.niranjan.medqueue.contact.MessageTemplates
import com.niranjan.medqueue.contact.TemplateToken
import com.niranjan.medqueue.contact.estimateSmsParts
import com.niranjan.medqueue.contact.renderTemplate
import com.niranjan.medqueue.data.settings.SettingsPrefs
import com.niranjan.medqueue.ui.components.*
import com.niranjan.medqueue.ui.theme.*
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════════════
// MESSAGE TEMPLATE EDITOR
//
// Two templates, because WhatsApp renders markdown and SMS does not — and
// because SMS is billed per part, which the editor reports as you type. The
// stock bilingual text costs seven parts; most shops can halve that by cutting
// the second language they never needed.
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun MessageTemplateScreen(
    settingsPrefs: SettingsPrefs,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    bottomBar: @Composable () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    // Read once: re-reading on every recomposition would stamp on the edit in
    // progress the moment anything else in the screen changed.
    val stored = remember { settingsPrefs.templates() }
    val settings = remember { settingsPrefs.read() }

    var editingSms by rememberSaveable { mutableStateOf(false) }

    var whatsApp by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(stored.whatsApp))
    }
    var sms by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(stored.sms))
    }

    val current = if (editingSms) sms else whatsApp
    val setCurrent: (TextFieldValue) -> Unit = { if (editingSms) sms = it else whatsApp = it }

    /** Drops the token in at the caret, replacing any selection. */
    fun insertToken(token: String) {
        val start = current.selection.min
        val end = current.selection.max
        setCurrent(
            current.copy(
                text = current.text.replaceRange(start, end, token),
                selection = TextRange(start + token.length)
            )
        )
    }

    val sampleCustomer = stringResource(R.string.template_sample_customer)
    val sampleMedicine = stringResource(R.string.template_sample_medicine)

    val preview = remember(current.text, settings, sampleCustomer, sampleMedicine) {
        renderTemplate(
            template = current.text,
            settings = settings,
            customerName = sampleCustomer,
            medicines = listOf(sampleMedicine)
        )
    }
    val smsParts = remember(preview, editingSms) {
        if (editingSms) estimateSmsParts(preview) else 0
    }

    val savedMessage = stringResource(R.string.template_saved)
    val resetMessage = stringResource(R.string.template_reset)

    Scaffold(
        bottomBar = bottomBar,
        containerColor = Paper,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            DetailHeader(
                title = stringResource(R.string.template_title),
                onBack = onBack,
                backLabel = stringResource(R.string.action_back),
                actions = {
                    HeaderAction(stringResource(R.string.template_reset_action), Red) {
                        setCurrent(
                            TextFieldValue(
                                if (editingSms) DEFAULT_SMS_TEMPLATE else DEFAULT_WHATSAPP_TEMPLATE
                            )
                        )
                        scope.launch { snackbarHostState.showSnackbar(resetMessage) }
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Which template ──────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TabPill(
                        label = stringResource(R.string.action_whatsapp),
                        selected = !editingSms,
                        onClick = { editingSms = false }
                    )
                    TabPill(
                        label = stringResource(R.string.action_sms),
                        selected = editingSms,
                        onClick = { editingSms = true }
                    )
                }

                // ── Editor ──────────────────────────────────────────────────
                DsCard(spacing = 12.dp) {
                    Eyebrow(
                        stringResource(R.string.template_section_text),
                        trailing = if (editingSms) {
                            stringResource(R.string.template_sms_parts, smsParts)
                        } else null
                    )

                    TextField(
                        value = current,
                        onValueChange = setCurrent,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        minLines = 8,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor   = Paper,
                            unfocusedContainerColor = Paper,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor        = Ink,
                            unfocusedTextColor      = Ink,
                            cursorColor             = Teal
                        )
                    )

                    if (editingSms) {
                        // Seven parts is seven times the price. Say so where the
                        // shopkeeper can act on it, not in a release note.
                        StatusNote(
                            text = stringResource(R.string.template_sms_cost_hint, smsParts),
                            ok = smsParts <= 2
                        )
                    }
                }

                // ── Placeholders ────────────────────────────────────────────
                DsCard(spacing = 10.dp) {
                    Eyebrow(stringResource(R.string.template_section_tokens))
                    Text(
                        stringResource(R.string.template_tokens_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TemplateToken.entries.forEach { token ->
                            TokenChip(
                                label = token.token,
                                description = stringResource(token.labelRes()),
                                onClick = { insertToken(token.token) }
                            )
                        }
                    }
                }

                // ── Preview ─────────────────────────────────────────────────
                DsCard(spacing = 10.dp) {
                    Eyebrow(stringResource(R.string.section_message_preview))
                    Surface(shape = RoundedCornerShape(14.dp), color = Paper) {
                        Text(
                            text = preview.ifBlank { stringResource(R.string.template_empty_preview) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (preview.isBlank()) Muted else Ink,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // ── Save ────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 12.dp)
            ) {
                PrimaryButton(
                    text = stringResource(R.string.template_save),
                    onClick = {
                        settingsPrefs.saveTemplates(
                            MessageTemplates(whatsApp = whatsApp.text, sms = sms.text)
                        )
                        scope.launch { snackbarHostState.showSnackbar(savedMessage) }
                        onBack()
                    }
                )
            }
        }
    }
}

/** Human-readable name for each placeholder. */
private fun TemplateToken.labelRes(): Int = when (this) {
    TemplateToken.SHOP      -> R.string.token_shop
    TemplateToken.ADDRESS   -> R.string.token_address
    TemplateToken.CONTACT   -> R.string.token_contact
    TemplateToken.CONTACT2  -> R.string.token_contact2
    TemplateToken.CUSTOMER  -> R.string.token_customer
    TemplateToken.MEDICINES -> R.string.token_medicines
}

// ── Bits ──────────────────────────────────────────────────────────────────────

@Composable
private fun TabPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Teal else MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (selected) Color.White else Muted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TokenChip(label: String, description: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = TealTint,
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Teal,
                maxLines = 1
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                maxLines = 1
            )
        }
    }
}
