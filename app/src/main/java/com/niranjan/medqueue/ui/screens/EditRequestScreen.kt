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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.medqueue.R
import com.niranjan.medqueue.data.local.RequestEntity
import com.niranjan.medqueue.ui.components.EmergencyToggleChip
import com.niranjan.medqueue.ui.components.FormCard
import com.niranjan.medqueue.ui.components.GradientHeader
import com.niranjan.medqueue.ui.components.SectionLabel
import com.niranjan.medqueue.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════
// ── EDIT REQUEST SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRequestScreen(
    request     : RequestEntity,
    isEmergency : Boolean = false,
    onSave      : (String, String, String, Boolean) -> Unit,
    onBack      : () -> Unit,
    bottomBar   : @Composable () -> Unit = {}
) {
    var name          by remember { mutableStateOf(request.customerName) }
    var phone         by remember { mutableStateOf(request.phoneNumber) }
    var medicine      by remember { mutableStateOf(request.medicineName) }
    var emergency     by remember { mutableStateOf(isEmergency) }
    var phoneError    by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            GradientHeader(
                title    = stringResource(R.string.edit_request),
                subtitle = request.customerName.ifBlank { "Request #${request.id}" },
                onBack   = onBack
            )
        },
        bottomBar      = bottomBar,
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
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── Info banner ───────────────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(14.dp),
                    color    = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier              = Modifier.padding(14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("✏️", fontSize = 18.sp)
                        Text(
                            "Update customer or medicine details below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // ── Customer card ─────────────────────────────────────────────
                FormCard {
                    SectionLabel(
                        icon     = Icons.Filled.Person,
                        title    = stringResource(R.string.customer_details),
                        subtitle = "Who is this request for?",
                        iconTint = SectionIconGreen
                    )
                    Spacer(Modifier.height(14.dp))

                    // CUSTOMER NAME label
                    Text(
                        text  = stringResource(R.string.customer_name),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        placeholder = { Text(stringResource(R.string.customer_name_hint)) },
                        leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                        modifier    = Modifier.fillMaxWidth(), singleLine = true,
                        shape       = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))

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
                        leadingIcon = { Icon(Icons.Filled.Phone, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                        prefix      = { Text("+91  ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier        = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError         = phoneError.isNotEmpty(),
                        supportingText  = {
                            Text(if (phoneError.isNotEmpty()) phoneError else "10-digit mobile number",
                                color = if (phoneError.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // ── Medicine card ─────────────────────────────────────────────
                FormCard {
                    SectionLabel(
                        icon     = Icons.Filled.Create,
                        title    = stringResource(R.string.medicines),
                        subtitle = stringResource(R.string.medicines_subtitle),
                        iconTint = SectionIconOrange
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value         = medicine,
                        onValueChange = { medicine = it },
                        placeholder   = { Text(stringResource(R.string.medicines_hint)) },
                        modifier      = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        minLines      = 3, maxLines = 6, singleLine = false,
                        supportingText = {
                            Text("One medicine per line",
                                color = MaterialTheme.colorScheme.outline)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // ── Emergency toggle ────────────────────────────────────────
                EmergencyToggleChip(
                    isEmergency = emergency,
                    onToggle    = { emergency = !emergency }
                )
            }

            // ── Fixed bottom actions (always visible, no scroll needed) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PageBackground)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // ── Gradient save button ──────────────────────────────────
                Button(
                    onClick = {
                        phoneError = ""
                        when {
                            phone.isBlank()    -> phoneError = "Phone number is required"
                            phone.length < 10  -> phoneError = "Enter a valid 10-digit number"
                            else               -> onSave(name, phone, medicine, emergency)
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
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.save_changes), fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

