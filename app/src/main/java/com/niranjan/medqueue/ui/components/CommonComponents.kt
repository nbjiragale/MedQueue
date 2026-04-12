package com.niranjan.medqueue.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.medqueue.data.local.RequestStatus
import com.niranjan.medqueue.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════
// ── SHARED UI COMPONENTS
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Reusable gradient header that replaces TopAppBar on all screens.
 * Renders a full-width box with the dark forest-green vertical gradient,
 * an optional back-arrow in a translucent circle, title + subtitle,
 * and optional trailing action icons.
 */
@Composable
fun GradientHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(HeaderGradientStart, HeaderGradientMid, HeaderGradientEnd)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Optional back arrow in translucent circle
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
            }

            // Title + subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Trailing action icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}

/** Circle avatar with initials derived from a name, or a fallback letter. */
@Composable
fun InitialsAvatar(
    name: String,
    size: Int = 44,
    bgColor: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val initials = name.trim()
        .split(Regex("\\s+"))
        .take(2)
        .joinToString("") { it.firstOrNull()?.uppercaseChar()?.toString() ?: "" }
        .ifBlank { "U" }

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

/**
 * Section label with a coloured circle icon (matching the reference mockup).
 * [icon] is a Material ImageVector displayed inside a tinted circle.
 * [iconTint] is the circle background colour; the icon itself is white.
 */
@Composable
fun SectionLabel(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/** Legacy emoji-based section label kept for backward-compat. */
@Composable
fun SectionLabel(emoji: String, title: String, subtitle: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Text(emoji, fontSize = 16.sp)
        }
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/** Pill-shaped status badge with a coloured dot + text. */
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
                    .size(8.dp)
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

/** Red pill-shaped "Emergency" badge. */
@Composable
fun EmergencyBadge() {
    Surface(shape = RoundedCornerShape(50), color = StatusEmergencyBg) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(StatusEmergencyContent)
            )
            Text(
                text = "Emergency",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                ),
                color = StatusEmergencyContent
            )
        }
    }
}

/** Tappable emergency toggle chip for forms. */
@Composable
fun EmergencyToggleChip(isEmergency: Boolean, onToggle: () -> Unit) {
    val redTint = Color(0xFFD32F2F)
    Surface(
        onClick = onToggle,
        shape   = RoundedCornerShape(50),
        color   = if (isEmergency) StatusEmergencyBg else Color.Transparent,
        border  = if (isEmergency) null
                  else BorderStroke(1.dp, redTint.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isEmergency) StatusEmergencyContent else redTint
            )
            Text(
                text = if (isEmergency) "Emergency" else "Mark Emergency",
                fontWeight = if (isEmergency) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                color = if (isEmergency) StatusEmergencyContent else redTint
            )
            if (isEmergency) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove emergency",
                    modifier = Modifier.size(14.dp),
                    tint = StatusEmergencyContent.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/** White elevated card wrapper for form sections. */
@Composable
fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

/** Small medicine name chip shown inside list cards. */
@Composable
fun MedPill(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text     = text,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
