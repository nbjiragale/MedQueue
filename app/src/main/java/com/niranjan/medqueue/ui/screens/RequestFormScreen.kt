package com.niranjan.medqueue.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.niranjan.medqueue.R
import com.niranjan.medqueue.data.local.RequestEntity
import com.niranjan.medqueue.prescription.PrescriptionStore
import com.niranjan.medqueue.ui.components.*
import com.niranjan.medqueue.ui.theme.*
import java.io.File

// ══════════════════════════════════════════════════════════════════════════════
// SHARED REQUEST FORM — backs both "New request" and "Edit request"
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun RequestFormScreen(
    title: String,
    subtitle: String,
    submitLabel: String,
    initial: RequestEntity?,
    onSubmit: (name: String, phone: String, medicine: String, emergency: Boolean, prescriptionPath: String?) -> Unit,
    onCancel: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    val context = LocalContext.current

    var phone     by rememberSaveable { mutableStateOf(initial?.phoneNumber.orEmpty()) }
    var name      by rememberSaveable { mutableStateOf(initial?.customerName.orEmpty()) }
    var medicine  by rememberSaveable { mutableStateOf(initial?.medicineName.orEmpty()) }
    var emergency by rememberSaveable { mutableStateOf(initial?.isEmergency ?: false) }
    var photoPath by rememberSaveable { mutableStateOf(initial?.prescriptionPath) }

    var phoneError by rememberSaveable { mutableStateOf<String?>(null) }
    var saving     by rememberSaveable { mutableStateOf(false) }

    val originalPhoto = initial?.prescriptionPath

    /** Drops a photo this screen created that is not the one we came in with. */
    fun discardIfOrphan(path: String?) {
        if (path != null && path != originalPhoto) PrescriptionStore.delete(path)
    }

    // ── Capture launchers ───────────────────────────────────────────────────
    var stagedCapture by remember { mutableStateOf<File?>(null) }
    val photoFailed = stringResource(R.string.photo_failed)

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val staged = stagedCapture
        stagedCapture = null
        if (!success || staged == null) {
            staged?.delete()
            return@rememberLauncherForActivityResult
        }
        val stored = PrescriptionStore.promoteCapture(context, staged)
        if (stored == null) {
            Toast.makeText(context, photoFailed, Toast.LENGTH_SHORT).show()
        } else {
            discardIfOrphan(photoPath)
            photoPath = stored
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val stored = PrescriptionStore.importFromUri(context, uri)
        if (stored == null) {
            Toast.makeText(context, photoFailed, Toast.LENGTH_SHORT).show()
        } else {
            discardIfOrphan(photoPath)
            photoPath = stored
        }
    }

    val cameraUnavailable = stringResource(R.string.camera_unavailable)
    val phoneRequired = stringResource(R.string.phone_required)
    val phoneInvalid  = stringResource(R.string.phone_invalid)

    Scaffold(
        bottomBar = bottomBar,
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            ScreenHeader(title = title, subtitle = subtitle, topPadding = 22.dp)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Customer ────────────────────────────────────────────────
                DsCard(spacing = 16.dp) {
                    Eyebrow(stringResource(R.string.section_customer))
                    DsField(
                        label = stringResource(R.string.label_phone),
                        value = phone,
                        onValueChange = { raw ->
                            phone = raw.filter(Char::isDigit).take(10)
                            phoneError = null
                        },
                        required = true,
                        prefix = stringResource(R.string.phone_prefix),
                        error = phoneError,
                        keyboardType = KeyboardType.Phone
                    )
                    DsField(
                        label = stringResource(R.string.label_name),
                        value = name,
                        onValueChange = { name = it },
                        placeholder = stringResource(R.string.customer_name_hint),
                        optionalHint = stringResource(R.string.label_optional)
                    )
                }

                // ── Medicine ────────────────────────────────────────────────
                DsCard(spacing = 10.dp) {
                    Eyebrow(stringResource(R.string.section_medicine))
                    DsField(
                        label = "",
                        value = medicine,
                        onValueChange = { medicine = it },
                        placeholder = stringResource(R.string.medicines_hint),
                        singleLine = false,
                        minLines = 3
                    )
                }

                // ── Prescription ────────────────────────────────────────────
                DsCard(spacing = 12.dp) {
                    Eyebrow(
                        stringResource(R.string.section_prescription),
                        trailing = stringResource(R.string.label_optional)
                    )
                    PrescriptionSlot(
                        path = photoPath,
                        onClear = {
                            discardIfOrphan(photoPath)
                            photoPath = null
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AttachButton(
                            label = stringResource(R.string.action_camera),
                            glyph = { CameraGlyph() },
                            onClick = {
                                runCatching {
                                    val (file, uri) = PrescriptionStore.newCameraTarget(context)
                                    stagedCapture = file
                                    cameraLauncher.launch(uri)
                                }.onFailure {
                                    stagedCapture = null
                                    Toast.makeText(context, cameraUnavailable, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        AttachButton(
                            label = stringResource(R.string.action_gallery),
                            glyph = { GalleryGlyph() },
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }
                }

                // ── Emergency ───────────────────────────────────────────────
                EmergencyCheck(
                    checked = emergency,
                    onToggle = { emergency = !emergency }
                )
            }

            // ── Footer ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryButton(
                    text = submitLabel,
                    loading = saving,
                    loadingText = stringResource(R.string.saving),
                    onClick = {
                        phoneError = when {
                            phone.isBlank()   -> phoneRequired
                            phone.length < 10 -> phoneInvalid
                            else              -> null
                        }
                        if (phoneError == null) {
                            saving = true
                            onSubmit(name, phone, medicine, emergency, photoPath)
                        }
                    }
                )
                QuietButton(
                    text = stringResource(R.string.cancel),
                    onClick = {
                        discardIfOrphan(photoPath)
                        onCancel()
                    }
                )
            }
        }
    }
}

// ── Prescription slot ─────────────────────────────────────────────────────────

@Composable
private fun PrescriptionSlot(path: String?, onClear: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    // Keyed on the path so the stat() happens once per photo, not once per frame.
    val hasPhoto = remember(path) { PrescriptionStore.exists(path) }

    if (path != null && hasPhoto) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(shape)
                .border(1.5.dp, Line, shape)
        ) {
            AsyncImage(
                model = File(path),
                contentDescription = stringResource(R.string.section_prescription),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clickable(onClick = onClear)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_remove_photo),
                    tint = Color.White,
                    modifier = Modifier.padding(6.dp).size(14.dp)
                )
            }
        }
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(shape)
                .background(Paper)
                .border(1.5.dp, DashedLine, shape)
        ) {
            Text(
                stringResource(R.string.prescription_empty),
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
        }
    }
}

@Composable
private fun RowScope.AttachButton(
    label: String,
    glyph: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Paper)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        glyph()
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Ink
        )
    }
}

/**
 * The mockup draws these two affordances as bare geometry rather than icons,
 * so they are reproduced as such — which also avoids pulling in the whole
 * material-icons-extended artifact for two glyphs.
 */
@Composable
private fun CameraGlyph() {
    Box(
        modifier = Modifier
            .size(11.dp)
            .border(1.5.dp, Ink, RoundedCornerShape(3.dp))
    )
}

@Composable
private fun GalleryGlyph() {
    Box(
        modifier = Modifier
            .width(12.dp)
            .height(9.dp)
            .border(1.5.dp, Ink, RoundedCornerShape(2.dp))
    )
}

// ── Emergency check ───────────────────────────────────────────────────────────

@Composable
private fun EmergencyCheck(checked: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) Red else RedTint)
                .border(1.5.dp, Red, RoundedCornerShape(6.dp))
        ) {
            if (checked) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        Text(
            stringResource(R.string.mark_emergency),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = Ink
        )
    }
}
