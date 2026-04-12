package com.niranjan.medqueue.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.medqueue.navigation.Screen

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

