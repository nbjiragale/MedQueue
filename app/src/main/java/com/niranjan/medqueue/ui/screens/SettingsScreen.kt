package com.niranjan.medqueue.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.medqueue.R
import com.niranjan.medqueue.contact.buildMessage
import com.niranjan.medqueue.data.settings.AppSettings
import com.niranjan.medqueue.data.settings.SettingsPrefs
import com.niranjan.medqueue.ui.components.FormCard
import com.niranjan.medqueue.ui.components.GradientHeader
import com.niranjan.medqueue.ui.components.SectionLabel
import com.niranjan.medqueue.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════
// ── SETTINGS SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsPrefs : SettingsPrefs,
    onBack        : () -> Unit,
    bottomBar     : @Composable () -> Unit = {}
) {
    val context          = LocalContext.current
    val current          = remember { settingsPrefs.read() }
    var shopName         by remember { mutableStateOf(current.shopName) }
    var shopAddress      by remember { mutableStateOf(current.shopAddress) }
    var contact1Name     by remember { mutableStateOf(current.contact1Name) }
    var contact1Phone    by remember { mutableStateOf(current.contact1Phone) }
    var contact2Name     by remember { mutableStateOf(current.contact2Name) }
    var contact2Phone    by remember { mutableStateOf(current.contact2Phone) }

    val previewMessage = remember(shopName, shopAddress, contact1Name, contact1Phone, contact2Name, contact2Phone) {
        buildMessage(AppSettings(shopName, shopAddress, contact1Name, contact1Phone, contact2Name, contact2Phone))
    }

    Scaffold(
        topBar = {
            Column {
                GradientHeader(
                    title    = stringResource(R.string.settings_title),
                    subtitle = stringResource(R.string.settings_subtitle),
                    onBack   = onBack
                )
                // ── Info banner inside gradient area ────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(HeaderGradientEnd, HeaderGradientEnd.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(14.dp),
                        color    = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier              = Modifier.padding(14.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                stringResource(R.string.settings_info_banner),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Shop card ─────────────────────────────────────────────────
            FormCard {
                SectionLabel(
                    icon     = Icons.Filled.Home,
                    title    = stringResource(R.string.shop_information),
                    subtitle = stringResource(R.string.shop_info_subtitle),
                    iconTint = SectionIconGreen
                )
                Spacer(Modifier.height(14.dp))

                // SHOP NAME label
                Text(
                    text  = stringResource(R.string.shop_name),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = shopName, onValueChange = { shopName = it },
                    leadingIcon = { Icon(Icons.Filled.Home, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                    modifier    = Modifier.fillMaxWidth(), singleLine = true,
                    shape       = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))

                // SHOP ADDRESS label
                Text(
                    text  = stringResource(R.string.shop_address),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = shopAddress, onValueChange = { shopAddress = it },
                    placeholder = { Text("e.g. Ainapur") },
                    leadingIcon = { Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                    modifier    = Modifier.fillMaxWidth(), singleLine = true,
                    shape       = RoundedCornerShape(12.dp)
                )
            }

            // ── Contact 1 card ────────────────────────────────────────────
            FormCard {
                SectionLabel(
                    icon     = Icons.Filled.Phone,
                    title    = stringResource(R.string.primary_contact),
                    subtitle = stringResource(R.string.primary_contact_subtitle),
                    iconTint = SectionIconGreen
                )
                Spacer(Modifier.height(14.dp))

                // NAME label
                Text(
                    text  = stringResource(R.string.name_label),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = contact1Name, onValueChange = { contact1Name = it },
                    leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))

                // PHONE label
                Text(
                    text  = stringResource(R.string.phone_label),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value         = contact1Phone,
                    onValueChange = { raw -> contact1Phone = raw.filter { it.isDigit() } },
                    leadingIcon = { Icon(Icons.Filled.Phone, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                    prefix      = { Text("+91  ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier    = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ── Contact 2 card ────────────────────────────────────────────
            FormCard {
                SectionLabel(
                    icon     = Icons.Filled.Phone,
                    title    = stringResource(R.string.secondary_contact),
                    subtitle = stringResource(R.string.secondary_contact_subtitle),
                    iconTint = SectionIconPurple
                )
                Spacer(Modifier.height(14.dp))

                // NAME label
                Text(
                    text  = stringResource(R.string.name_label),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = contact2Name, onValueChange = { contact2Name = it },
                    leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))

                // PHONE label
                Text(
                    text  = stringResource(R.string.phone_label),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value         = contact2Phone,
                    onValueChange = { raw -> contact2Phone = raw.filter { it.isDigit() } },
                    leadingIcon = { Icon(Icons.Filled.Phone, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                    prefix      = { Text("+91  ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier    = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ── Message preview card ──────────────────────────────────────
            ElevatedCard(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(18.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors    = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header row with icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF25D366).copy(alpha = 0.15f))
                        ) {
                            Text("💬", fontSize = 16.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.message_preview),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                stringResource(R.string.message_preview_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        // WhatsApp badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF25D366).copy(alpha = 0.12f)
                        ) {
                            Text(
                                stringResource(R.string.whatsapp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF25D366)
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))

                    // Chat bubble background
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp),
                            shape = RoundedCornerShape(
                                topStart = 4.dp,
                                topEnd = 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp
                            ),
                            color = Color(0xFFDCF8C6),
                            shadowElevation = 0.5.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text     = previewMessage,
                                    style    = MaterialTheme.typography.bodySmall.copy(
                                        lineHeight = 18.sp,
                                        color = Color(0xFF1B1B1B)
                                    )
                                )
                                Spacer(Modifier.height(6.dp))
                                // Timestamp row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Preview",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            color = Color(0xFF6B8F71)
                                        )
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("✓✓", fontSize = 10.sp, color = Color(0xFF53BDEB))
                                }
                            }
                        }
                    }
                }
            }

            // ── Save button (gradient) ──────────────────────────────────
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    settingsPrefs.save(AppSettings(shopName, shopAddress, contact1Name, contact1Phone, contact2Name, contact2Phone))
                    Toast.makeText(context, context.getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
                    onBack()
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
                        Text(stringResource(R.string.save_settings), fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

