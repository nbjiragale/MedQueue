@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.niranjan.medqueue.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.niranjan.medqueue.R
import com.niranjan.medqueue.contact.formatForDisplay
import com.niranjan.medqueue.data.local.RequestEntity
import com.niranjan.medqueue.data.local.RequestStatus
import com.niranjan.medqueue.navigation.FilterTag
import com.niranjan.medqueue.ui.components.*
import com.niranjan.medqueue.ui.formatRelative
import com.niranjan.medqueue.ui.startOfDay
import com.niranjan.medqueue.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════
// QUEUE — the landing screen
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun RequestListScreen(
    requests: List<RequestEntity>,
    onAddClick: () -> Unit,
    onItemClick: (RequestEntity) -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(FilterTag.ALL) }

    val pendingCount = remember(requests) { requests.count { it.status == RequestStatus.PENDING } }

    // Recomputed against the request list rather than remembered once, so the
    // count stays right if the app is left open across midnight.
    val todayCount = remember(requests) {
        val dayStart = startOfDay(System.currentTimeMillis())
        requests.count { it.createdAt >= dayStart }
    }

    val visible = remember(requests, query, filter) {
        val q = query.trim().lowercase()
        requests.filter { req ->
            val matchesQuery = q.isBlank() ||
                    req.customerName.lowercase().contains(q) ||
                    req.phoneNumber.contains(q) ||
                    req.medicineName.lowercase().contains(q)
            val matchesFilter = when (filter) {
                FilterTag.ALL       -> true
                FilterTag.PENDING   -> req.status == RequestStatus.PENDING
                FilterTag.DELIVERED -> req.status == RequestStatus.DELIVERED
            }
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        bottomBar = bottomBar,
        containerColor = Paper,
        // Headers and the bottom bar consume the system-bar insets themselves.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                shape = RoundedCornerShape(17.dp),
                containerColor = Teal,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 10.dp),
                modifier = Modifier.size(54.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_request))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── White header block: title, counts, search, filters ──────────
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                ScreenHeader(
                    title = stringResource(R.string.queue_title),
                    subtitle = stringResource(R.string.queue_subtitle, pendingCount, todayCount)
                )

                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterTag.entries.forEach { tag ->
                        FilterPill(
                            label = stringResource(tag.labelRes()),
                            selected = filter == tag,
                            onClick = { filter = tag }
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // ── List ────────────────────────────────────────────────────────
            if (visible.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    if (requests.isEmpty()) {
                        EmptyNote(
                            text = stringResource(R.string.empty_no_requests),
                            hint = stringResource(R.string.empty_no_requests_hint),
                            modifier = Modifier.padding(top = 48.dp)
                        )
                    } else {
                        EmptyNote(
                            text = stringResource(R.string.empty_no_matches),
                            modifier = Modifier.padding(top = 48.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visible, key = { it.id }) { request ->
                        RequestRow(request = request, onClick = { onItemClick(request) })
                    }
                }
            }
        }
    }
}

private fun FilterTag.labelRes(): Int = when (this) {
    FilterTag.ALL       -> R.string.filter_all
    FilterTag.PENDING   -> R.string.filter_pending
    FilterTag.DELIVERED -> R.string.filter_delivered
}

// ── Search ────────────────────────────────────────────────────────────────────

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                stringResource(R.string.search_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Muted
            )
        },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_back),
                        tint = Muted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
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
}

// ── Filter pill ───────────────────────────────────────────────────────────────

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Teal else Paper,
        onClick = onClick
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = if (selected) Color.White else Muted,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

// ── Row ───────────────────────────────────────────────────────────────────────

@Composable
private fun RequestRow(request: RequestEntity, onClick: () -> Unit) {
    val medicines = remember(request.medicineName) {
        request.medicineName.lines().filter { it.isNotBlank() }
    }
    val noName = request.customerName.isBlank()
    val noNameLabel = stringResource(R.string.no_name_provided)

    val secondary = remember(request.customerName, medicines, noNameLabel) {
        val who = if (noName) noNameLabel else request.customerName
        if (medicines.isEmpty()) who else "$who · ${medicines.joinToString(", ")}"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        onClick = onClick
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Emergency accent runs the full height of the card's left edge.
            if (request.isEmergency) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(Red)
                )
            }

            Row(
                modifier = Modifier.padding(
                    start = if (request.isEmergency) 12.dp else 14.dp,
                    end = 14.dp,
                    top = 14.dp,
                    bottom = 14.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PhoneTile(request.phoneNumber)

                Column(modifier = Modifier.weight(1f)) {
                    // Same wrap rule as the detail hero: never squeeze the
                    // badge, never break the number.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = formatForDisplay(request.phoneNumber),
                            style = MaterialTheme.typography.titleSmall,
                            color = Ink,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        if (request.isEmergency) {
                            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                                UrgentPill(stringResource(R.string.badge_urgent))
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    SecondaryLine(secondary, italic = noName)
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    StatusPill(
                        status = request.status,
                        pendingLabel = stringResource(R.string.status_pending),
                        deliveredLabel = stringResource(R.string.status_delivered)
                    )
                    Text(
                        text = formatRelative(request.createdAt),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                        color = Muted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
