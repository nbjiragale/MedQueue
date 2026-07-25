package com.niranjan.medqueue.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.niranjan.medqueue.R
import com.niranjan.medqueue.navigation.Screen
import com.niranjan.medqueue.ui.theme.Muted
import com.niranjan.medqueue.ui.theme.Teal
import com.niranjan.medqueue.ui.theme.TealTint

/**
 * Custom bottom bar rather than M3 [androidx.compose.material3.NavigationBar]:
 * the mockup highlights the selected item with a rounded teal-tint tile behind
 * the icon, which the stock pill-shaped indicator can't express.
 *
 * Order follows the redesign — Queue first, since that is the screen a worker
 * lands on and returns to.
 */
@Composable
fun MedQueueBottomBar(currentScreen: Screen, onNavigate: (Screen) -> Unit) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 10.dp)
        ) {
            NavItem(
                icon = Icons.AutoMirrored.Filled.List,
                label = stringResource(R.string.nav_queue),
                selected = currentScreen is Screen.RequestList,
                onClick = { onNavigate(Screen.RequestList) }
            )
            NavItem(
                icon = Icons.Filled.Add,
                label = stringResource(R.string.nav_new),
                selected = currentScreen is Screen.Home,
                onClick = { onNavigate(Screen.Home) }
            )
            NavItem(
                icon = Icons.Filled.Settings,
                label = stringResource(R.string.nav_settings),
                selected = currentScreen is Screen.Settings,
                onClick = { onNavigate(Screen.Settings) }
            )
        }
    }
}

@Composable
private fun RowScope.NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (selected) Teal else Muted
    val interaction = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (selected) TealTint else Color.Transparent)
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = tint
        )
    }
}
