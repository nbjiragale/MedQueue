package com.niranjan.medqueue.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.medqueue.data.local.RequestStage
import com.niranjan.medqueue.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════
// Design-system primitives — "MedQueue Redesign.dc.html"
//
// The mockup is built from a small set of repeated shapes: a white header
// strip, white cards on a paper background, teal eyebrow labels, filled input
// chips, and pill badges. Each of those is one composable here.
// ══════════════════════════════════════════════════════════════════════════════

private val CardShape  = RoundedCornerShape(18.dp)
private val FieldShape = RoundedCornerShape(12.dp)
private val ButtonShape = RoundedCornerShape(14.dp)

// ── Headers ───────────────────────────────────────────────────────────────────

/**
 * White header strip with a big title and a muted subtitle.
 *
 * [trailing] holds controls that belong to the screen rather than to a row —
 * the queue's search toggle, for instance.
 */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    topPadding: Dp = 20.dp,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(start = 20.dp, end = if (trailing != null) 8.dp else 20.dp, top = topPadding, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Ink)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/** Back-arrow header with a centred title and trailing text actions. */
@Composable
fun DetailHeader(
    title: String,
    onBack: () -> Unit,
    backLabel: String,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = backLabel,
                tint = Ink,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            ),
            color = Ink,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            content = actions
        )
    }
}

/**
 * Trailing text action in a [DetailHeader].
 *
 * The label is small by design, but the target it sits in is not: 48dp minimum,
 * because these are thumb targets on a counter phone.
 */
@Composable
fun HeaderAction(label: String, tint: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .padding(horizontal = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = tint
        )
    }
}

// ── Cards ─────────────────────────────────────────────────────────────────────

/** The mockup's white card: radius 18, padding 18, barely-there shadow. */
@Composable
fun DsCard(
    modifier: Modifier = Modifier,
    padding: Dp = 18.dp,
    spacing: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content
        )
    }
}

/** Teal uppercase section label that opens every card. */
@Composable
fun Eyebrow(text: String, trailing: String? = null) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(text.uppercase(), style = SectionEyebrow, color = Teal)
        if (trailing != null) {
            Text(
                "  $trailing",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
        }
    }
}

// ── Inputs ────────────────────────────────────────────────────────────────────

/**
 * Label + filled input chip + optional error line, matching the mockup's form
 * rows. The chip is borderless until it errors, when it picks up a red tint
 * and a red outline.
 */
@Composable
fun DsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    required: Boolean = false,
    optionalHint: String? = null,
    prefix: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val hasError = !error.isNullOrBlank()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // A blank label means the field stands alone under a card eyebrow,
        // as the mockup's medicine box does — don't reserve a row for nothing.
        if (label.isNotBlank()) {
            Row {
                Text(label, style = MaterialTheme.typography.labelMedium, color = Muted)
                if (required) {
                    Text(" *", style = MaterialTheme.typography.labelMedium, color = Red)
                }
                if (optionalHint != null) {
                    Text(
                        " $optionalHint",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                        color = Muted
                    )
                }
            }
        }

        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (hasError) Modifier.border(1.5.dp, Red, FieldShape) else Modifier
                ),
            placeholder = placeholder?.let {
                { Text(it, style = MaterialTheme.typography.bodyLarge, color = Placeholder) }
            },
            prefix = prefix?.let {
                {
                    Text(
                        "$it  ",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Ink
                    )
                }
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = singleLine,
            minLines = minLines,
            isError = hasError,
            shape = FieldShape,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.colors(
                focusedContainerColor    = Paper,
                unfocusedContainerColor  = Paper,
                disabledContainerColor   = Paper,
                errorContainerColor      = RedTint,
                focusedIndicatorColor    = Color.Transparent,
                unfocusedIndicatorColor  = Color.Transparent,
                disabledIndicatorColor   = Color.Transparent,
                errorIndicatorColor      = Color.Transparent,
                focusedTextColor         = Ink,
                unfocusedTextColor       = Ink,
                cursorColor              = Teal
            )
        )

        if (hasError) {
            Text(
                error!!,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Red
            )
        }
    }
}

// ── Badges ────────────────────────────────────────────────────────────────────

/**
 * Amber "Pending" → teal "Notified" → grey "Delivered".
 *
 * Teal marks the live middle state rather than the finished one on purpose:
 * a notified request is the one still owed something (the customer walking in),
 * while a delivered request needs nothing and recedes.
 */
@Composable
fun StatusPill(
    stage: RequestStage,
    pendingLabel: String,
    notifiedLabel: String,
    deliveredLabel: String
) {
    val (text, background, foreground) = when (stage) {
        RequestStage.PENDING   -> Triple(pendingLabel, AmberTint, Amber)
        RequestStage.NOTIFIED  -> Triple(notifiedLabel, TealTint, Teal)
        RequestStage.DELIVERED -> Triple(deliveredLabel, SlateTint, Muted)
    }
    Pill(text = text, background = background, foreground = foreground)
}

/** Red uppercase urgency pill. */
@Composable
fun UrgentPill(label: String) {
    Pill(text = label.uppercase(), background = RedTint, foreground = Red)
}

/**
 * Neutral "N items" pill on a queue row.
 *
 * A five-item order and a one-item order rendered as the same width of
 * truncated grey text, so the size of the job was invisible until the request
 * was opened.
 */
@Composable
fun CountPill(label: String) {
    Pill(text = label, background = SlateTint, foreground = Muted)
}

@Composable
private fun Pill(text: String, background: Color, foreground: Color) {
    Surface(shape = RoundedCornerShape(9.dp), color = background) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            // A pill must never wrap. Squeezed into a narrow slot it would
            // otherwise break one letter per line and stretch its whole row.
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

/**
 * Rounded teal tile carrying the last two digits of the phone number.
 *
 * The mockup uses this instead of name initials because a request's name is
 * optional — the phone number is the only field guaranteed to be present.
 */
@Composable
fun PhoneTile(phone: String, size: Dp = 40.dp, radius: Dp = 12.dp) {
    val digits = phone.filter(Char::isDigit)
    val label = if (digits.length >= 2) digits.takeLast(2) else digits.ifBlank { "—" }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(TealTint)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
            color = Teal
        )
    }
}

// ── Buttons ───────────────────────────────────────────────────────────────────

/** Full-width teal call-to-action with the mockup's soft coloured shadow. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        // Minimum rather than fixed: at a 1.5x system font scale — common on a
        // shopkeeper's phone — a hard 50dp clipped the label.
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 50.dp),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (loading) TealDark else Teal,
            contentColor = Color.White,
            disabledContainerColor = Teal.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.8f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.35f)
            )
            Spacer(Modifier.width(9.dp))
        }
        Text(
            text = if (loading) (loadingText ?: text) else text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/** Quiet full-width text button used for "Cancel". */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColors(contentColor = Muted)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
    }
}

/** One third of the detail screen's Call / WhatsApp / SMS row. */
@Composable
fun RowScope.ContactButton(
    label: String,
    icon: @Composable () -> Unit,
    background: Color,
    foreground: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .weight(1f)
            .clip(ButtonShape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides foreground) { icon() }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = foreground
        )
    }
}

/**
 * Compact action inside a queue row — "Notify" and "Delivered".
 *
 * Held to the 48dp minimum even though it costs list density: these are the two
 * taps the whole app exists for, and they are made one-handed at a counter.
 */
@Composable
fun RowScope.RowAction(
    label: String,
    icon: @Composable () -> Unit,
    background: Color,
    foreground: Color,
    onClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 8.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides foreground) { icon() }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Toggles ───────────────────────────────────────────────────────────────────

/** Title + subtitle on the left, switch on the right. */
@Composable
fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                color = Ink
            )
            Spacer(Modifier.height(3.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Teal,
                checkedBorderColor = Teal,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = TrackOff,
                uncheckedBorderColor = TrackOff
            )
        )
    }
}

/**
 * One option in a pick-exactly-one list — a tinted, tappable row that carries
 * its own selected state. Used by the language picker.
 *
 * The whole row is the target rather than just the radio: this is tapped from
 * behind a counter, often one-handed, and a 20dp dot is not a fair target.
 */
@Composable
fun ChoiceRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        shape = FieldShape,
        color = if (selected) TealTint else Paper
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = if (selected) Teal else Ink,
                modifier = Modifier.weight(1f)
            )
            RadioButton(
                selected = selected,
                // Already handled by the row, and a second target here would
                // read as two controls to a screen reader.
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Teal,
                    unselectedColor = Muted
                )
            )
        }
    }
}

/** Inline status strip under a toggle — teal when healthy, red when it needs a tap. */
@Composable
fun StatusNote(
    text: String,
    ok: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null && !ok) Modifier.clickable(onClick = onClick) else Modifier),
        shape = FieldShape,
        color = if (ok) TealTint else RedTint
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (ok) Teal else Red,
            modifier = Modifier.padding(12.dp)
        )
    }
}

// ── Misc ──────────────────────────────────────────────────────────────────────

/** Centred empty state with a dashed circle, as used in the queue. */
@Composable
fun EmptyNote(text: String, hint: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(1.5.dp, DashedLine, CircleShape)
        ) {
            Text("—", style = MaterialTheme.typography.bodyLarge, color = Muted)
        }
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Muted
        )
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}

/** Single-line body text that ellipsises — used for the list's secondary line. */
@Composable
fun SecondaryLine(text: String, italic: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.let {
            if (italic) it.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) else it
        },
        color = Muted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
