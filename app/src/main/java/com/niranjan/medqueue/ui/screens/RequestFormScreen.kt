package com.niranjan.medqueue.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
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
    snackbarHostState: SnackbarHostState,
    bottomBar: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun report(text: String) {
        scope.launch { snackbarHostState.showSnackbar(text) }
    }

    var phone     by rememberSaveable { mutableStateOf(initial?.phoneNumber.orEmpty()) }
    var name      by rememberSaveable { mutableStateOf(initial?.customerName.orEmpty()) }
    var medicine  by rememberSaveable { mutableStateOf(initial?.medicineName.orEmpty()) }
    var emergency by rememberSaveable { mutableStateOf(initial?.isEmergency ?: false) }
    var photoPath by rememberSaveable { mutableStateOf(initial?.prescriptionPath) }

    var phoneError by rememberSaveable { mutableStateOf<String?>(null) }
    var saving     by rememberSaveable { mutableStateOf(false) }

    val originalPhoto = initial?.prescriptionPath

    // New requests start with the customer on the phone — jump straight to
    // the phone field and pop the keyboard so they don't have to tap in.
    // Editing an existing request skips this: the worker opened it to look
    // something up, not necessarily to retype the number.
    val phoneFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    if (initial == null) {
        LaunchedEffect(Unit) {
            phoneFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

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
            report(photoFailed)
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
            report(photoFailed)
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        keyboardType = KeyboardType.Phone,
                        focusRequester = phoneFocusRequester
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
                                    report(cameraUnavailable)
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

/**
 * Renders the attached photo, or nothing at all.
 *
 * There is deliberately no empty-state box: the Camera and Gallery buttons
 * directly below already say what this section is for, so a dashed placeholder
 * only added height to a form the worker is trying to get through quickly.
 */
@Composable
private fun PrescriptionSlot(path: String?, onClear: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    // Keyed on the path so the stat() happens once per photo, not once per frame.
    val hasPhoto = remember(path) { PrescriptionStore.exists(path) }
    var showViewer by remember { mutableStateOf(false) }

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
                contentDescription = stringResource(R.string.action_view_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showViewer = true }
            )
            // 48dp target around a 30dp chip: the visual stays small so it does
            // not cover the photo, but the thumb gets something it can hit.
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_remove_photo),
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        if (showViewer) {
            ImageViewerDialog(
                file = File(path),
                contentDescription = stringResource(R.string.section_prescription),
                closeLabel = stringResource(R.string.action_close),
                onDismiss = { showViewer = false }
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
 * Hand-drawn rather than [Icons.Filled] — a real camera icon only exists in
 * material-icons-extended, and pulling in that whole artifact (there is no
 * shrinker on this build; see build.gradle.kts) for one glyph was not worth
 * the size. Drawn at the app's standard 16dp icon scale instead of the
 * mockup's 9dp box, which read as an empty placeholder rather than a camera.
 */
@Composable
private fun CameraGlyph() {
    Canvas(modifier = Modifier.size(16.dp)) {
        val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val bodyTop = size.height * 0.32f

        // Viewfinder bump
        drawRoundRect(
            color = Ink,
            topLeft = Offset(size.width * 0.32f, 0f),
            size = Size(size.width * 0.36f, bodyTop * 0.85f),
            cornerRadius = CornerRadius(1.dp.toPx()),
            style = stroke
        )
        // Body
        drawRoundRect(
            color = Ink,
            topLeft = Offset(0f, bodyTop),
            size = Size(size.width, size.height - bodyTop),
            cornerRadius = CornerRadius(size.width * 0.14f),
            style = stroke
        )
        // Lens
        drawCircle(
            color = Ink,
            radius = (size.height - bodyTop) * 0.26f,
            center = Offset(size.width / 2f, bodyTop + (size.height - bodyTop) / 2f),
            style = stroke
        )
    }
}

/** Photo-frame glyph — sun and mountains, the universal "image" mark. See [CameraGlyph]. */
@Composable
private fun GalleryGlyph() {
    Canvas(modifier = Modifier.size(16.dp)) {
        val strokeWidth = 1.4.dp.toPx()
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val inset = strokeWidth / 2f

        drawRoundRect(
            color = Ink,
            topLeft = Offset(inset, inset),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = CornerRadius(size.width * 0.16f),
            style = stroke
        )
        drawCircle(
            color = Ink,
            radius = size.width * 0.09f,
            center = Offset(size.width * 0.32f, size.height * 0.32f),
            style = stroke
        )
        clipRect(inset, inset, size.width - inset, size.height - inset) {
            val mountains = Path().apply {
                moveTo(inset, size.height * 0.8f)
                lineTo(size.width * 0.38f, size.height * 0.48f)
                lineTo(size.width * 0.56f, size.height * 0.66f)
                lineTo(size.width * 0.74f, size.height * 0.4f)
                lineTo(size.width - inset, size.height * 0.8f)
            }
            drawPath(mountains, color = Ink, style = stroke)
        }
    }
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
