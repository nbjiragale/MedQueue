package com.niranjan.medqueue

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.remember
import com.niranjan.medqueue.data.settings.AppLocale
import com.niranjan.medqueue.data.settings.SettingsPrefs
import com.niranjan.medqueue.reminders.Reminders
import com.niranjan.medqueue.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RequestViewModel by viewModels()

    /**
     * Swaps in the shop's chosen language before any resource is resolved.
     * Runs again after [recreate], which is how the Settings picker takes
     * effect without a restart.
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The app renders light-only (see MyApplicationTheme), so pin the system
        // bars to dark icons. The no-arg default follows the system theme, which
        // on a dark-mode phone drew light icons over our light header.
        enableEdgeToEdge(
            statusBarStyle     = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        // Brings scheduled reminder work in line with the saved toggles. Cheap
        // and idempotent, and it repairs the schedule after an app update or a
        // "clear data" wipes WorkManager's queue.
        Reminders.sync(applicationContext)

        setContent {
            MyApplicationTheme {
                val settingsPrefs = remember { SettingsPrefs(this) }
                MedQueueApp(vm = viewModel, settingsPrefs = settingsPrefs)
            }
        }
    }
}
