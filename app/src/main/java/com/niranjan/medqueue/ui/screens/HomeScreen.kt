package com.niranjan.medqueue.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.medqueue.R
import com.niranjan.medqueue.ui.components.EmergencyToggleChip
import com.niranjan.medqueue.ui.components.FormCard
import com.niranjan.medqueue.ui.components.GradientHeader
import com.niranjan.medqueue.ui.components.SectionLabel
import com.niranjan.medqueue.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSave    : (String, String, String, Boolean) -> Unit,
    onBack    : () -> Unit,
    bottomBar : @Composable () -> Unit = {}
) {
    var name          by remember { mutableStateOf("") }
    var phone         by remember { mutableStateOf("") }
    var medicine      by remember { mutableStateOf("") }
    var isEmergency   by remember { mutableStateOf(false) }
    var phoneError    by remember { mutableStateOf("") }

    val phoneFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        phoneFocusRequester.requestFocus()
        keyboardController?.show()
    }

    Scaffold(
        topBar = {
            GradientHeader(
                title    = stringResource(R.string.new_request),
                subtitle = stringResource(R.string.new_request_subtitle),
                onBack   = onBack
            )
        },
        bottomBar     = bottomBar,
        containerColor = PageBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Scrollable form content ─────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // ── Customer card ────────────────────────────────────────────
                FormCard {
                    SectionLabel(
                        icon     = Icons.Filled.Person,
                        title    = stringResource(R.string.customer_details),
                        subtitle = stringResource(R.string.customer_details_subtitle),
                        iconTint = SectionIconGreen
                    )
                    Spacer(Modifier.height(12.dp))

                    // CUSTOMER NAME label
                    Text(
                        text       = stringResource(R.string.customer_name),
                        style      = MaterialTheme.typography.labelSmall.copy(
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value         = name,
                        onValueChange = { name = it },
                        placeholder   = { Text(stringResource(R.string.customer_name_hint)) },
                        leadingIcon   = {
                            Icon(Icons.Filled.Person, null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp))
                        },
                        modifier   = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape      = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(10.dp))

                    // PHONE NUMBER * label
                    Row {
                        Text(
                            text  = stringResource(R.string.phone_number),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight    = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text  = " *",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value         = phone,
                        onValueChange = { raw ->
                            phone = raw.filter { it.isDigit() }
                            if (phone.isNotEmpty()) phoneError = ""
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Phone, null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp))
                        },
                        prefix  = {
                            Text(stringResource(R.string.phone_prefix) + "  ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier        = Modifier.fillMaxWidth().focusRequester(phoneFocusRequester),
                        singleLine      = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError         = phoneError.isNotEmpty(),
                        supportingText  = if (phoneError.isNotEmpty()) {
                            { Text(phoneError, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // ── Medicine card ────────────────────────────────────────────
                FormCard {
                    SectionLabel(
                        icon     = Icons.Filled.Create,
                        title    = stringResource(R.string.medicines),
                        subtitle = stringResource(R.string.medicines_subtitle),
                        iconTint = SectionIconOrange
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value         = medicine,
                        onValueChange = { medicine = it },
                        placeholder   = { Text(stringResource(R.string.medicines_hint)) },
                        modifier      = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        minLines        = 3,
                        maxLines        = 6,
                        singleLine      = false,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // ── Fixed bottom area (emergency + actions, always visible) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PageBackground)
                    .padding(horizontal = 14.dp)
                    .padding(top = 6.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ── Emergency toggle ──────────────────────────────────────
                EmergencyToggleChip(
                    isEmergency = isEmergency,
                    onToggle    = { isEmergency = !isEmergency }
                )

                // ── Gradient save button ──────────────────────────────────
                val phoneRequiredMsg = stringResource(R.string.phone_required)
                val phoneInvalidMsg  = stringResource(R.string.phone_invalid)
                Button(
                    onClick = {
                        phoneError = ""
                        when {
                            phone.isBlank()   -> phoneError = phoneRequiredMsg
                            phone.length < 10 -> phoneError = phoneInvalidMsg
                            else              -> onSave(name, phone, medicine, isEmergency)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(26.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(HeaderGradientMid, HeaderGradientEnd)
                                ),
                                shape = RoundedCornerShape(26.dp)
                            ),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.save_request), fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }

                TextButton(
                    onClick  = onBack,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

