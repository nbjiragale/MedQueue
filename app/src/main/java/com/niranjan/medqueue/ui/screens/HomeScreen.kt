package com.niranjan.medqueue.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.niranjan.medqueue.R

/**
 * "New request" — the shared form seeded with nothing.
 */
@Composable
fun HomeScreen(
    onSave: (name: String, phone: String, medicine: String, emergency: Boolean, prescriptionPath: String?) -> Unit,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    RequestFormScreen(
        title = stringResource(R.string.new_request),
        subtitle = stringResource(R.string.new_request_subtitle),
        submitLabel = stringResource(R.string.save_request),
        initial = null,
        onSubmit = onSave,
        onCancel = onBack,
        bottomBar = bottomBar
    )
}
