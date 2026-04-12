package com.niranjan.medqueue.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.medqueue.R
import com.niranjan.medqueue.data.local.RequestEntity
import com.niranjan.medqueue.data.local.RequestStatus
import com.niranjan.medqueue.navigation.FilterTag
import com.niranjan.medqueue.ui.components.InitialsAvatar
import com.niranjan.medqueue.ui.components.MedPill
import com.niranjan.medqueue.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════════════
// ── REQUEST LIST SCREEN  (redesigned to match gradient-header mockup)
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestListScreen(
    requests: List<RequestEntity>,
    emergencyIds: Set<Int> = emptySet(),
    onAddClick: () -> Unit,
    onItemClick: (RequestEntity) -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(FilterTag.ALL) }

    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val pendingCount   = remember(requests) { requests.count { it.status == RequestStatus.PENDING } }
    val deliveredCount = remember(requests) { requests.count { it.status == RequestStatus.DELIVERED } }
    val todayCount     = remember(requests) { requests.count { it.createdAt >= todayStart } }

    val filteredRequests = remember(requests, searchQuery, activeFilter) {
        requests.filter { req ->
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isBlank() ||
                    req.customerName.lowercase().contains(q) ||
                    req.phoneNumber.contains(q)
            val matchesTag = when (activeFilter) {
                FilterTag.ALL       -> true
                FilterTag.TODAY     -> req.createdAt >= todayStart
                FilterTag.PENDING   -> req.status == RequestStatus.PENDING
                FilterTag.DELIVERED -> req.status == RequestStatus.DELIVERED
            }
            matchesSearch && matchesTag
        }
    }

    Scaffold(
        bottomBar = bottomBar,
        floatingActionButton = {
            FloatingActionButton(
                onClick        = onAddClick,
                shape          = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_request))
            }
        },
        containerColor = PageBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // ══════════════════════════════════════════════════════════════
            // ── Gradient header block (title + stats)
            // ══════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(HeaderGradientStart, HeaderGradientMid, HeaderGradientEnd)
                        )
                    )
                    .padding(top = 16.dp, bottom = 24.dp)
            ) {
                // ── Top row: "M" avatar + title + bell icon ──────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Green "M" avatar
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Text(
                                "M",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.app_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    // Bell icon
                    IconButton(onClick = { /* notifications */ }) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = stringResource(R.string.notification),
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Stats pills row ──────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatPill(
                        value = requests.size.toString(),
                        label = stringResource(R.string.stat_total),
                        bgColor = StatTotalBg,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        value = pendingCount.toString(),
                        label = stringResource(R.string.stat_pending),
                        bgColor = StatPendingBg,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        value = deliveredCount.toString(),
                        label = stringResource(R.string.stat_delivered),
                        bgColor = StatDeliveredBg,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        value = todayCount.toString(),
                        label = stringResource(R.string.stat_today),
                        bgColor = StatTodayBg,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Search bar ───────────────────────────────────────────────
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder   = {
                    Text(
                        stringResource(R.string.search_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, stringResource(R.string.back), modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor   = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Filter chips ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTag.entries.forEach { tag ->
                    FilterChip(
                        selected = activeFilter == tag,
                        onClick  = { activeFilter = tag },
                        label    = {
                            Text(
                                tag.label,
                                fontWeight = if (activeFilter == tag) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize   = 13.sp
                            )
                        },
                        shape  = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                            selectedLabelColor     = MaterialTheme.colorScheme.surface,
                            containerColor         = MaterialTheme.colorScheme.surface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            enabled = true,
                            selected = activeFilter == tag
                        )
                    )
                }
            }

            // ── Divider ──────────────────────────────────────────────────
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── List / empty state ───────────────────────────────────────
            if (filteredRequests.isEmpty()) {
                EmptyState(
                    hasRequests = requests.isNotEmpty(),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredRequests, key = { it.id }) { request ->
                        RequestListItem(
                            request     = request,
                            isEmergency = request.id in emergencyIds,
                            onClick     = { onItemClick(request) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ── Stat pill (inside gradient header) ────────────────────────────────────────

@Composable
private fun StatPill(
    value: String,
    label: String,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(hasRequests: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(if (hasRequests) "🔍" else "💊", fontSize = 30.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text  = stringResource(if (hasRequests) R.string.no_matches else R.string.no_requests_yet),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text      = stringResource(if (hasRequests) R.string.try_different_search else R.string.tap_plus_hint),
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Smart date formatter ──────────────────────────────────────────────────────

private fun formatSmartDate(timestamp: Long): String {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val yesterdayCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        timestamp >= todayCal.timeInMillis     -> {
            val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
        timestamp >= yesterdayCal.timeInMillis  -> "Yesterday, ${timeFormat.format(Date(timestamp))}"
        else -> {
            val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}

// ── Request list card (matches the reference image) ───────────────────────────

@Composable
fun RequestListItem(request: RequestEntity, isEmergency: Boolean = false, onClick: () -> Unit) {
    val displayName = request.customerName.ifBlank { "Unknown" }
    val isPending   = request.status == RequestStatus.PENDING

    // Status badge colours
    val statusLabel    = if (isPending) "Pending" else "Delivered"
    val statusDotColor = if (isPending) StatusPendingContent else StatusDeliveredContent
    val statusTextColor = if (isPending) StatusPendingContent else StatusDeliveredContent
    val statusBgColor   = if (isPending) StatusPendingBg else StatusDeliveredBg

    // Emergency → red bottom border accent
    val cardBorder = if (isEmergency) {
        androidx.compose.foundation.BorderStroke(1.dp, StatusEmergencyBg.copy(alpha = 0.5f))
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border    = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ── Top row: avatar + name/phone + status ────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Circle avatar
                InitialsAvatar(name = displayName, size = 44)

                Spacer(modifier = Modifier.width(12.dp))

                // Name + phone
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "+91 ${request.phoneNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Status badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusBgColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusDotColor)
                        )
                        Text(
                            text = statusLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusTextColor
                        )
                    }
                }
            }

            // ── Medicine pills row (if any) ──────────────────────────
            val medicineLines = remember(request.medicineName) {
                request.medicineName.lines().filter { it.isNotBlank() }
            }
            if (medicineLines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val visibleMeds = medicineLines.take(2)
                    visibleMeds.forEach { med ->
                        MedPill(text = med.take(20))
                    }
                    val remaining = medicineLines.size - 2
                    if (remaining > 0) {
                        Text(
                            text = "+$remaining more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Date ─────────────────────────────────────────────────
            Text(
                text = formatSmartDate(request.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

