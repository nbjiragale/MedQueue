package com.niranjan.medqueue

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.medqueue.data.local.RequestEntity
import com.niranjan.medqueue.data.local.RequestStatus
import com.niranjan.medqueue.contact.ContactAction
import com.niranjan.medqueue.contact.ContactActionResult
import com.niranjan.medqueue.contact.buildMessage
import com.niranjan.medqueue.contact.launchContactAction
import com.niranjan.medqueue.data.settings.AppSettings
import com.niranjan.medqueue.data.settings.SettingsPrefs
import com.niranjan.medqueue.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ── Activity ──────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    private val viewModel: RequestViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val settingsPrefs = remember { SettingsPrefs(this) }
                MedQueueApp(vm = viewModel, settingsPrefs = settingsPrefs)
            }
        }
    }
}

// ── Navigation ─────────────────────────────────────────────────────────────────

sealed class Screen {
    object Home        : Screen()
    object RequestList : Screen()
    object Settings    : Screen()
    data class RequestDetail(val request: RequestEntity) : Screen()
    data class EditRequest(val request: RequestEntity)   : Screen()
}

// ── App root ───────────────────────────────────────────────────────────────────

@Composable
fun MedQueueApp(vm: RequestViewModel, settingsPrefs: SettingsPrefs) {
    var screen: Screen by remember { mutableStateOf(Screen.RequestList) }
    val requests by vm.requests.collectAsState()

    val bottomBar: @Composable () -> Unit = {
        MedQueueBottomBar(currentScreen = screen, onNavigate = { screen = it })
    }

    when (val s = screen) {
        Screen.Home -> HomeScreen(
            onSave = { name, phone, medicine ->
                vm.addRequest(name, phone, medicine)
                screen = Screen.RequestList
            },
            onBack    = { screen = Screen.RequestList },
            bottomBar = bottomBar
        )
        Screen.RequestList -> RequestListScreen(
            requests   = requests,
            onAddClick = { screen = Screen.Home },
            onItemClick = { screen = Screen.RequestDetail(it) },
            bottomBar  = bottomBar
        )
        is Screen.RequestDetail -> RequestDetailScreen(
            request       = s.request,
            settingsPrefs = settingsPrefs,
            onEdit        = { screen = Screen.EditRequest(s.request) },
            onDelivered   = { vm.markDelivered(s.request.id); screen = Screen.RequestList },
            onDelete      = { vm.deleteRequest(s.request.id); screen = Screen.RequestList },
            onBack        = { screen = Screen.RequestList }
        )
        is Screen.EditRequest -> EditRequestScreen(
            request = s.request,
            onSave  = { name, phone, medicine ->
                vm.updateRequest(s.request.id, name, phone, medicine)
                screen = Screen.RequestList
            },
            onBack  = { screen = Screen.RequestDetail(s.request) }
        )
        Screen.Settings -> SettingsScreen(
            settingsPrefs = settingsPrefs,
            onBack        = { screen = Screen.RequestList },
            bottomBar     = bottomBar
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ── FILTER TAG ENUM
// ══════════════════════════════════════════════════════════════════════════════

enum class FilterTag(val label: String) {
    ALL("All"), TODAY("Today"), PENDING("Pending"), DELIVERED("Delivered")
}

// ══════════════════════════════════════════════════════════════════════════════
// ── BOTTOM NAVIGATION BAR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun MedQueueBottomBar(currentScreen: Screen, onNavigate: (Screen) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.border(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
        )
    ) {
        data class NavItem(val screen: Screen, val label: String, val icon: @Composable () -> Unit)
        val items = listOf(
            NavItem(Screen.Home,        "New",      { Icon(Icons.Filled.Add,                    "New") }),
            NavItem(Screen.RequestList, "Queue",    { Icon(Icons.AutoMirrored.Filled.List,      "Queue") }),
            NavItem(Screen.Settings,    "Settings", { Icon(Icons.Filled.Settings,               "Settings") })
        )
        items.forEach { item ->
            val selected = when (item.screen) {
                Screen.Home        -> currentScreen is Screen.Home
                Screen.RequestList -> currentScreen is Screen.RequestList
                Screen.Settings    -> currentScreen is Screen.Settings
                else               -> false
            }
            NavigationBarItem(
                selected = selected,
                onClick  = { onNavigate(item.screen) },
                icon     = item.icon,
                label    = { Text(item.label, fontSize = 11.sp) },
                colors   = NavigationBarItemDefaults.colors(
                    selectedIconColor   = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor      = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ── SHARED HELPERS
// ══════════════════════════════════════════════════════════════════════════════

/** Circle avatar with initials derived from a name, or a fallback icon. */
@Composable
private fun InitialsAvatar(name: String, size: Int = 44) {
    val initials = name.trim()
        .split(Regex("\\s+"))
        .take(2)
        .joinToString("") { it.firstOrNull()?.uppercaseChar()?.toString() ?: "" }
        .ifBlank { "#" }

    val bgColor = MaterialTheme.colorScheme.primaryContainer
    val textColor = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bgColor)
    ) {
        Text(
            text = initials,
            fontSize = (size * 0.38f).sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}

/** Subtle section label used above groups of form fields. */
@Composable
private fun SectionLabel(emoji: String, title: String, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Text(emoji, fontSize = 16.sp)
        }
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/** Pill-shaped status badge. */
@Composable
fun StatusBadge(status: RequestStatus) {
    val isPending = status == RequestStatus.PENDING
    val bgColor   = if (isPending) StatusPendingBg   else StatusDeliveredBg
    val fgColor   = if (isPending) StatusPendingContent else StatusDeliveredContent
    val label     = if (isPending) "Pending"         else "Delivered"

    Surface(shape = RoundedCornerShape(50), color = bgColor) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fgColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                ),
                color = fgColor
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ── REQUEST LIST SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestListScreen(
    requests: List<RequestEntity>,
    onAddClick: () -> Unit,
    onItemClick: (RequestEntity) -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    var searchQuery  by remember { mutableStateOf("") }
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
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
                        ) {
                            Text("💊", fontSize = 16.sp)
                        }
                        Column {
                            Text("MedQueue", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(
                                "Medicine requests",
                                style = MaterialTheme.typography.labelSmall,
                                color = TopBarContent.copy(alpha = 0.65f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = TopBarContainer,
                    titleContentColor = TopBarContent
                )
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = onAddClick,
                icon           = { Icon(Icons.Filled.Add, contentDescription = null) },
                text           = { Text("New Request", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
                shape          = RoundedCornerShape(16.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // ── Stats bar ──────────────────────────────────────────────────
            if (requests.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatChip(label = "Total",     value = requests.size.toString(),  modifier = Modifier.weight(1f))
                    StatChip(label = "Pending",   value = pendingCount.toString(),   color = StatusPendingContent,   bg = StatusPendingBg.copy(alpha = 0.7f),   modifier = Modifier.weight(1f))
                    StatChip(label = "Delivered", value = deliveredCount.toString(), color = StatusDeliveredContent, bg = StatusDeliveredBg.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
                    StatChip(label = "Today",     value = todayCount.toString(),     modifier = Modifier.weight(1f))
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }

            // ── Search bar ─────────────────────────────────────────────────
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder   = { Text("Search by name or phone…", color = MaterialTheme.colorScheme.outline) },
                leadingIcon   = {
                    Icon(Icons.Filled.Search, "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp))
                },
                trailingIcon  = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape      = RoundedCornerShape(14.dp),
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )

            // ── Filter chips ───────────────────────────────────────────────
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
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor     = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // ── List / empty state ─────────────────────────────────────────
            if (filteredRequests.isEmpty()) {
                EmptyState(
                    hasRequests = requests.isNotEmpty(),
                    modifier    = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredRequests, key = { it.id }) { request ->
                        RequestListItem(request = request, onClick = { onItemClick(request) })
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ── Mini stat chip used in the stats bar ─────────────────────────────────────

@Composable
private fun StatChip(
    label    : String,
    value    : String,
    modifier : Modifier = Modifier,
    color    : Color    = MaterialTheme.colorScheme.onSurface,
    bg       : Color    = MaterialTheme.colorScheme.surfaceVariant
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(10.dp),
        color    = bg
    ) {
        Column(
            modifier            = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            Text(label, fontSize = 10.sp, color = color.copy(alpha = 0.75f), letterSpacing = 0.2.sp)
        }
    }
}

// ── Empty state illustration ──────────────────────────────────────────────────

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
                text  = if (hasRequests) "No matches found" else "No requests yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text      = if (hasRequests) "Try a different search or filter" else "Tap New Request to get started",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Request list card ─────────────────────────────────────────────────────────

@Composable
fun RequestListItem(request: RequestEntity, onClick: () -> Unit) {
    val dateFormat  = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val displayName = request.customerName.ifBlank { "Unknown" }
    val isPending   = request.status == RequestStatus.PENDING
    val cardBg      = if (isPending) CardPendingBg else CardDeliveredBg
    val stripeColor = if (isPending) StripePending  else StripeDelivered

    val medicineLines = remember(request.medicineName) {
        request.medicineName.lines().filter { it.isNotBlank() }
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {

            // Left accent stripe
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        stripeColor,
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )

            // Content
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.Top
            ) {
                // Avatar
                InitialsAvatar(name = displayName, size = 42)

                // Text content
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {

                    // Row: Name + status
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text     = displayName,
                            style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        StatusBadge(status = request.status)
                    }

                    // Phone
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(
                            Icons.Filled.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint     = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text  = "+91 ${request.phoneNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Medicine pill-list (first 2 lines + overflow count)
                    val visibleMeds = medicineLines.take(2)
                    val overflow    = medicineLines.size - visibleMeds.size
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        modifier              = Modifier.padding(top = 2.dp)
                    ) {
                        visibleMeds.forEach { med ->
                            MedPill(text = med)
                        }
                        if (overflow > 0) {
                            Text(
                                "+$overflow more",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Date
                    Text(
                        text  = dateFormat.format(Date(request.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/** Small medicine name chip shown inside list cards. */
@Composable
private fun MedPill(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text     = text,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ── HOME SCREEN  (New Request)
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSave    : (String, String, String) -> Unit,
    onBack    : () -> Unit,
    bottomBar : @Composable () -> Unit = {}
) {
    var name          by remember { mutableStateOf("") }
    var phone         by remember { mutableStateOf("") }
    var medicine      by remember { mutableStateOf("") }
    var phoneError    by remember { mutableStateOf("") }
    var medicineError by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Request", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor          = TopBarContainer,
                    titleContentColor       = TopBarContent,
                    navigationIconContentColor = TopBarContent
                )
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Customer card ────────────────────────────────────────────
            FormCard {
                SectionLabel("👤", "Customer Details", "Who is this request for?")
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Customer Name (optional)") },
                    leadingIcon   = {
                        Icon(Icons.Filled.Person, null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp))
                    },
                    modifier   = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape      = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value         = phone,
                    onValueChange = { raw ->
                        phone = raw.filter { it.isDigit() }
                        if (phone.isNotEmpty()) phoneError = ""
                    },
                    label       = { Text("Phone Number *") },
                    leadingIcon = {
                        Icon(Icons.Filled.Phone, null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp))
                    },
                    prefix  = {
                        Text("+91  ",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier        = Modifier.fillMaxWidth(),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError         = phoneError.isNotEmpty(),
                    supportingText  = {
                        Text(
                            if (phoneError.isNotEmpty()) phoneError else "10-digit mobile number",
                            color = if (phoneError.isNotEmpty()) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.outline
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ── Medicine card ────────────────────────────────────────────
            FormCard {
                SectionLabel("💊", "Medicines", "List what the customer needs")
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value         = medicine,
                    onValueChange = {
                        medicine = it
                        if (medicine.isNotEmpty()) medicineError = ""
                    },
                    label         = { Text("Medicines *") },
                    placeholder   = { Text("e.g.\nParacetamol 500mg\nAmoxicillin 250mg") },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 130.dp),
                    minLines        = 4,
                    maxLines        = 8,
                    singleLine      = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    isError         = medicineError.isNotEmpty(),
                    supportingText  = {
                        Text(
                            if (medicineError.isNotEmpty()) medicineError else "One medicine per line",
                            color = if (medicineError.isNotEmpty()) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.outline
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ── Actions ──────────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    phoneError = ""; medicineError = ""
                    when {
                        phone.isBlank()   -> phoneError    = "Phone number is required"
                        phone.length < 10 -> phoneError    = "Enter a valid 10-digit number"
                        medicine.isBlank()-> medicineError = "At least one medicine is required"
                        else              -> onSave(name, phone, medicine)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save Request", fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}

/** White elevated card wrapper for form sections. */
@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors    = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ── REQUEST DETAIL SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    request       : RequestEntity,
    settingsPrefs : SettingsPrefs,
    onEdit        : () -> Unit,
    onDelivered   : () -> Unit,
    onDelete      : () -> Unit,
    onBack        : () -> Unit
) {
    val context         = LocalContext.current
    var showContactSheet by remember { mutableStateOf(false) }
    val dateFormat      = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val displayName     = request.customerName.ifBlank { "Unknown Customer" }
    val isPending       = request.status == RequestStatus.PENDING

    val medicineLines = remember(request.medicineName) {
        request.medicineName.lines().filter { it.isNotBlank() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Detail", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, "Edit", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, "Delete",
                            tint = TopBarContent.copy(alpha = 0.65f),
                            modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor          = TopBarContainer,
                    titleContentColor       = TopBarContent,
                    navigationIconContentColor = TopBarContent,
                    actionIconContentColor  = TopBarContent
                )
            )
        }
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
            val heroBg = if (isPending) CardPendingBg else CardDeliveredBg
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
                        StatusBadge(status = request.status)
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
                val toast   = when (result) {
                    ContactActionResult.Success              -> null
                    ContactActionResult.InvalidPhone         -> "Invalid phone number"
                    ContactActionResult.WhatsAppNotInstalled -> "WhatsApp not installed"
                    ContactActionResult.NoHandler            -> "No app found to handle this action"
                }
                toast?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
            }
        )
    }
}

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

// ══════════════════════════════════════════════════════════════════════════════
// ── EDIT REQUEST SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRequestScreen(
    request : RequestEntity,
    onSave  : (String, String, String) -> Unit,
    onBack  : () -> Unit
) {
    var name          by remember { mutableStateOf(request.customerName) }
    var phone         by remember { mutableStateOf(request.phoneNumber) }
    var medicine      by remember { mutableStateOf(request.medicineName) }
    var phoneError    by remember { mutableStateOf("") }
    var medicineError by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Edit Request", fontWeight = FontWeight.SemiBold)
                        Text(
                            request.customerName.ifBlank { "Request #${request.id}" },
                            style = MaterialTheme.typography.labelSmall,
                            color = TopBarContent.copy(alpha = 0.65f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor          = TopBarContainer,
                    titleContentColor       = TopBarContent,
                    navigationIconContentColor = TopBarContent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
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
                SectionLabel("👤", "Customer Details", "Who is this request for?")
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label       = { Text("Customer Name (optional)") },
                    leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                    modifier    = Modifier.fillMaxWidth(), singleLine = true,
                    shape       = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value         = phone,
                    onValueChange = { raw ->
                        phone = raw.filter { it.isDigit() }
                        if (phone.isNotEmpty()) phoneError = ""
                    },
                    label       = { Text("Phone Number *") },
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
                SectionLabel("💊", "Medicines", "List what the customer needs")
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value         = medicine,
                    onValueChange = { medicine = it; if (medicine.isNotEmpty()) medicineError = "" },
                    label         = { Text("Medicines *") },
                    placeholder   = { Text("e.g.\nParacetamol 500mg\nAmoxicillin 250mg") },
                    modifier      = Modifier.fillMaxWidth().heightIn(min = 130.dp),
                    minLines      = 4, maxLines = 8, singleLine = false,
                    isError       = medicineError.isNotEmpty(),
                    supportingText = {
                        Text(if (medicineError.isNotEmpty()) medicineError else "One medicine per line",
                            color = if (medicineError.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ── Actions ───────────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    phoneError = ""; medicineError = ""
                    when {
                        phone.isBlank()    -> phoneError    = "Phone number is required"
                        phone.length < 10  -> phoneError    = "Enter a valid 10-digit number"
                        medicine.isBlank() -> medicineError = "At least one medicine is required"
                        else               -> onSave(name, phone, medicine)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save Changes", fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}

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
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor          = TopBarContainer,
                    titleContentColor       = TopBarContent,
                    navigationIconContentColor = TopBarContent
                )
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Info banner ───────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier              = Modifier.padding(14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("⚙️", fontSize = 18.sp)
                    Text(
                        "These details appear in every message sent to your customers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── Shop card ─────────────────────────────────────────────────
            FormCard {
                SectionLabel("🏪", "Shop Information", "Your pharmacy / store name")
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = shopName, onValueChange = { shopName = it },
                    label       = { Text("Shop Name") },
                    leadingIcon = { Icon(Icons.Filled.Home, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                    modifier    = Modifier.fillMaxWidth(), singleLine = true,
                    shape       = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = shopAddress, onValueChange = { shopAddress = it },
                    label       = { Text("Shop Address") },
                    placeholder = { Text("e.g. Ainapur") },
                    leadingIcon = { Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                    modifier    = Modifier.fillMaxWidth(), singleLine = true,
                    shape       = RoundedCornerShape(12.dp)
                )
            }

            // ── Contact 1 card ────────────────────────────────────────────
            FormCard {
                SectionLabel("📞", "Primary Contact", "Main point of contact")
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = contact1Name, onValueChange = { contact1Name = it },
                    label = { Text("Name") },
                    leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value         = contact1Phone,
                    onValueChange = { raw -> contact1Phone = raw.filter { it.isDigit() } },
                    label       = { Text("Phone") },
                    leadingIcon = { Icon(Icons.Filled.Phone, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                    prefix      = { Text("+91  ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier    = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ── Contact 2 card ────────────────────────────────────────────
            FormCard {
                SectionLabel("📞", "Secondary Contact", "Backup point of contact")
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = contact2Name, onValueChange = { contact2Name = it },
                    label = { Text("Name") },
                    leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value         = contact2Phone,
                    onValueChange = { raw -> contact2Phone = raw.filter { it.isDigit() } },
                    label       = { Text("Phone") },
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
                                "Message Preview",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "How customers will see your message",
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
                                "WhatsApp",
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

            // ── Save button ───────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    settingsPrefs.save(AppSettings(shopName, shopAddress, contact1Name, contact1Phone, contact2Name, contact2Phone))
                    Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save Settings", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}