package com.niranjan.medqueue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.remember
import com.niranjan.medqueue.data.settings.SettingsPrefs
import com.niranjan.medqueue.ui.theme.MyApplicationTheme

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
