package com.niranjan.medqueue

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.niranjan.medqueue.data.local.RequestEntity
import com.niranjan.medqueue.data.local.RequestStatus
import com.niranjan.medqueue.contact.ContactAction
import com.niranjan.medqueue.contact.ContactActionResult
import com.niranjan.medqueue.contact.buildMessage
import com.niranjan.medqueue.contact.launchContactAction
import com.niranjan.medqueue.data.settings.AppSettings
import com.niranjan.medqueue.data.settings.SettingsPrefs
import com.niranjan.medqueue.ui.theme.MyApplicationTheme

// ── Activity ─────────────────────────────────────────────────────────────────

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

// ── Navigation ────────────────────────────────────────────────────────────────

sealed class Screen {
    object Home         : Screen()
    object RequestList  : Screen()
    object Settings     : Screen()
    data class RequestDetail(val request: RequestEntity) : Screen()
}

// ── App root ──────────────────────────────────────────────────────────────────

@Composable
fun MedQueueApp(vm: RequestViewModel, settingsPrefs: SettingsPrefs) {
    var screen: Screen by remember { mutableStateOf(Screen.RequestList) }
    val requests by vm.requests.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val s = screen) {
                Screen.Home -> HomeScreen(
                    onSave = { name, phone, medicine ->
                        vm.addRequest(name, phone, medicine)
                        screen = Screen.RequestList
                    },
                    onBack = { screen = Screen.RequestList }
                )
                Screen.RequestList -> RequestListScreen(
                    requests = requests,
                    onAddClick      = { screen = Screen.Home },
                    onItemClick     = { screen = Screen.RequestDetail(it) },
                    onSettingsClick = { screen = Screen.Settings }
                )
                is Screen.RequestDetail -> RequestDetailScreen(
                    request      = s.request,
                    settingsPrefs = settingsPrefs,
                    onDelivered  = { vm.markDelivered(s.request.id); screen = Screen.RequestList },
                    onDelete     = { vm.deleteRequest(s.request.id); screen = Screen.RequestList },
                    onBack       = { screen = Screen.RequestList }
                )
                Screen.Settings -> SettingsScreen(
                    settingsPrefs = settingsPrefs,
                    onBack        = { screen = Screen.RequestList }
                )
            }
        }
    }
}

// ── Home Screen ───────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(onSave: (String, String, String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var name     by remember { mutableStateOf("") }
    var phone    by remember { mutableStateOf("") }
    var medicine by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("New Request", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(value = name,     onValueChange = { name = it },     label = { Text("Customer Name") },  modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = phone,    onValueChange = { phone = it },    label = { Text("Phone Number") },   modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = medicine, onValueChange = { medicine = it }, label = { Text("Medicine Name") },  modifier = Modifier.fillMaxWidth(), singleLine = true)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                when {
                    name.isBlank()     -> Toast.makeText(context, "Name is required",     Toast.LENGTH_SHORT).show()
                    phone.isBlank()    -> Toast.makeText(context, "Phone is required",    Toast.LENGTH_SHORT).show()
                    medicine.isBlank() -> Toast.makeText(context, "Medicine is required", Toast.LENGTH_SHORT).show()
                    else               -> onSave(name, phone, medicine)
                }
            }) { Text("Save") }
            OutlinedButton(onClick = onBack) { Text("Cancel") }
        }
    }
}

// ── Request List Screen ───────────────────────────────────────────────────────

@Composable
fun RequestListScreen(
    requests: List<RequestEntity>,
    onAddClick: () -> Unit,
    onItemClick: (RequestEntity) -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MedQueue", style = MaterialTheme.typography.titleLarge)
            Row {
                TextButton(onClick = onSettingsClick) { Text("Settings") }
                Button(onClick = onAddClick) { Text("+ Add") }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No requests yet. Tap + Add.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(requests, key = { it.id }) { request ->
                    RequestListItem(request = request, onClick = { onItemClick(request) })
                }
            }
        }
    }
}

@Composable
fun RequestListItem(request: RequestEntity, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(request.medicineName, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${request.customerName} · ${request.phoneNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = request.status.name,
                style = MaterialTheme.typography.labelSmall,
                color = if (request.status == RequestStatus.PENDING)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ── Request Detail Screen ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    request: RequestEntity,
    settingsPrefs: SettingsPrefs,
    onDelivered: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showContactSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Back") }
        Text("Request Detail", style = MaterialTheme.typography.titleLarge)
        HorizontalDivider()
        Text("Name: ${request.customerName}",     style = MaterialTheme.typography.bodyLarge)
        Text("Phone: ${request.phoneNumber}",     style = MaterialTheme.typography.bodyLarge)
        Text("Medicine: ${request.medicineName}", style = MaterialTheme.typography.bodyLarge)
        Text(
            text  = "Status: ${request.status.name}",
            style = MaterialTheme.typography.bodyLarge,
            color = if (request.status == RequestStatus.PENDING)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary
        )
        HorizontalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showContactSheet = true }) { Text("Send Msg") }
            if (request.status == RequestStatus.PENDING) {
                Button(onClick = onDelivered) { Text("Delivered") }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) { Text("Delete Request") }
    }

    if (showContactSheet) {
        ContactActionSheet(
            onDismiss = { showContactSheet = false },
            onAction  = { action ->
                showContactSheet = false
                val message = buildMessage(request.customerName, request.medicineName)
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

// ── Contact Action Sheet (Stories 5–8) ───────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactActionSheet(
    onDismiss: () -> Unit,
    onAction: (ContactAction) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            TextButton(
                onClick = { onAction(ContactAction.WHATSAPP) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("WhatsApp") }
            TextButton(
                onClick = { onAction(ContactAction.SMS) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("SMS") }
            TextButton(
                onClick = { onAction(ContactAction.CALL) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Call") }
        }
    }
}

// ── Settings Screen ───────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(settingsPrefs: SettingsPrefs, onBack: () -> Unit) {
    val context = LocalContext.current
    val current  = remember { settingsPrefs.read() }
    var shopName    by remember { mutableStateOf(current.shopName) }
    var yogeshName  by remember { mutableStateOf(current.yogeshName) }
    var yogeshPhone by remember { mutableStateOf(current.yogeshPhone) }
    var ownerPhone  by remember { mutableStateOf(current.ownerPhone) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Back") }
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(value = shopName,    onValueChange = { shopName = it },    label = { Text("Shop Name") },    modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = yogeshName,  onValueChange = { yogeshName = it },  label = { Text("Worker Name") },  modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = yogeshPhone, onValueChange = { yogeshPhone = it }, label = { Text("Worker Phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = ownerPhone,  onValueChange = { ownerPhone = it },  label = { Text("Owner Phone") },  modifier = Modifier.fillMaxWidth(), singleLine = true)

        Button(
            onClick = {
                settingsPrefs.save(AppSettings(shopName, yogeshName, yogeshPhone, ownerPhone))
                Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                onBack()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save Settings") }
    }
}