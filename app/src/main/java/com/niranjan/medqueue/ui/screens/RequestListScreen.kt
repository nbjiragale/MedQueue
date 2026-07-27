@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.niranjan.medqueue.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.niranjan.medqueue.R
import com.niranjan.medqueue.contact.formatForDisplay
import com.niranjan.medqueue.data.local.RequestEntity
import com.niranjan.medqueue.data.local.RequestStage
import com.niranjan.medqueue.data.local.RequestStatus
import com.niranjan.medqueue.data.local.stage
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
    snackbarHostState: SnackbarHostState,
    onAddClick: () -> Unit,
    onItemClick: (RequestEntity) -> Unit,
    onNotify: (RequestEntity) -> Unit,
    onDeliver: (RequestEntity) -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf("") }

    // Opens on Pending, not All. "All" led with yesterday's finished work and
    // pushed the live queue below the fold.
    var filter by rememberSaveable { mutableStateOf(FilterTag.PENDING) }

    val toNotifyCount = remember(requests) { requests.count { it.stage == RequestStage.PENDING } }
    val awaitingCount = remember(requests) { requests.count { it.stage == RequestStage.NOTIFIED } }

    // Recomputed against the request list rather than remembered once, so the
    // count stays right if the app is left open across midnight.
    val todayCount = remember(requests) {
        val dayStart = startOfDay(System.currentTimeMillis())
        requests.count { it.createdAt >= dayStart }
    }

    val visible = remember(requests, query, filter) {
        val q = query.trim().lowercase()
        requests
            .filter { req ->
                val matchesQuery = q.isBlank() ||
                        req.customerName.lowercase().contains(q) ||
                        req.phoneNumber.contains(q) ||
                        req.medicineName.lowercase().contains(q)
                val matchesFilter = when (filter) {
                    FilterTag.ALL       -> true
                    FilterTag.PENDING   -> req.status == RequestStatus.PENDING
                    FilterTag.URGENT    -> req.isEmergency && req.status == RequestStatus.PENDING
                    FilterTag.DELIVERED -> req.status == RequestStatus.DELIVERED
                }
                matchesQuery && matchesFilter
            }
            // Open emergencies float to the top. A 3dp red stripe is invisible
            // once the row has scrolled off, which is where a 9am emergency ended
            // up under a strict newest-first sort. sortedWith is stable, so
            // createdAt DESC still orders everything within each group.
            .sortedWith(
                compareByDescending<RequestEntity> {
                    it.isEmergency && it.status == RequestStatus.PENDING
                }
            )
    }

    Scaffold(
        bottomBar = bottomBar,
        containerColor = Paper,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    subtitle = stringResource(
                        R.string.queue_subtitle, toNotifyCount, awaitingCount, todayCount
                    )
                )

                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 10.dp),
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
                        RequestRow(
                            request = request,
                            onClick = { onItemClick(request) },
                            onNotify = { onNotify(request) },
                            onDeliver = { onDeliver(request) }
                        )
                    }
                }
            }
        }
    }
}

private fun FilterTag.labelRes(): Int = when (this) {
    FilterTag.ALL       -> R.string.filter_all
    FilterTag.PENDING   -> R.string.filter_pending
    FilterTag.URGENT    -> R.string.filter_urgent
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
                        contentDescription = stringResource(R.string.action_clear_search),
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
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 16.dp)
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

// ── Row ───────────────────────────────────────────────────────────────────────

@Composable
private fun RequestRow(
    request: RequestEntity,
    onClick: () -> Unit,
    onNotify: () -> Unit,
    onDeliver: () -> Unit
) {
    val medicines = remember(request.medicineName) {
        request.medicineName.lines().filter { it.isNotBlank() }
    }
    val noName = request.customerName.isBlank()
    val noNameLabel = stringResource(R.string.no_name_provided)
    val noMedicineLabel = stringResource(R.string.no_medicines_listed)

    // Lead with the medicine. The phone number used to be the headline, but
    // nobody scans a queue by phone number — when stock lands the worker is
    // looking for a drug name, and when a customer walks in they give a name.
    val headline = remember(medicines, request.customerName, noMedicineLabel) {
        when {
            medicines.isNotEmpty()       -> medicines.joinToString(", ")
            request.customerName.isNotBlank() -> request.customerName
            else                         -> noMedicineLabel
        }
    }
    val headlineIsPlaceholder = medicines.isEmpty() && noName

    val secondary = remember(request.customerName, request.phoneNumber, medicines, noNameLabel) {
        val who = if (noName) noNameLabel else request.customerName
        val number = formatForDisplay(request.phoneNumber)
        // The name is already the headline when there are no medicines — don't
        // print it twice.
        if (medicines.isEmpty()) number else "$who · $number"
    }

    val stage = request.stage
    val showActions = request.status == RequestStatus.PENDING

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

            Column(
                modifier = Modifier.padding(
                    start = if (request.isEmergency) 12.dp else 14.dp,
                    end = 14.dp,
                    top = 14.dp,
                    bottom = if (showActions) 10.dp else 14.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PhoneTile(request.phoneNumber)

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (headlineIsPlaceholder) Muted else Ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        SecondaryLine(secondary, italic = noName && medicines.isNotEmpty())

                        // Badges wrap onto their own line rather than competing
                        // with the headline for width.
                        if (medicines.size > 1 || request.isEmergency) {
                            Spacer(Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (medicines.size > 1) {
                                    CountPill(
                                        pluralStringResource(
                                            R.plurals.medicine_count, medicines.size, medicines.size
                                        )
                                    )
                                }
                                if (request.isEmergency) {
                                    UrgentPill(stringResource(R.string.badge_urgent))
                                }
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        StatusPill(
                            stage = stage,
                            pendingLabel = stringResource(R.string.status_pending),
                            notifiedLabel = stringResource(R.string.status_notified),
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

                // ── Inline actions ──────────────────────────────────────────
                // Notifying a customer and closing a request used to cost four
                // taps across two screens. They are the two things this app is
                // for, so they live on the row.
                if (showActions) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RowAction(
                            label = stringResource(
                                if (stage == RequestStage.NOTIFIED) R.string.action_notify_again
                                else R.string.action_notify
                            ),
                            icon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            background = WhatsAppTint,
                            foreground = WhatsAppInk,
                            onClick = onNotify
                        )
                        RowAction(
                            label = stringResource(R.string.status_delivered),
                            icon = {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            background = Paper,
                            foreground = Ink,
                            onClick = onDeliver
                        )
                    }
                }
            }
        }
    }
}
