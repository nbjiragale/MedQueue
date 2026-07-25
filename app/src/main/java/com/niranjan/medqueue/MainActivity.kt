package com.niranjan.medqueue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.remember
import com.niranjan.medqueue.data.settings.SettingsPrefs
import com.niranjan.medqueue.reminders.Reminders
import com.niranjan.medqueue.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RequestViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
