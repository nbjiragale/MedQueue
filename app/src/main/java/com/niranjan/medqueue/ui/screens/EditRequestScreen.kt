package com.niranjan.medqueue.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.niranjan.medqueue.R
import com.niranjan.medqueue.data.local.RequestEntity

/**
 * "Edit request" — the shared form seeded from an existing row.
 */
@Composable
fun EditRequestScreen(
    request: RequestEntity,
    onSave: (name: String, phone: String, medicine: String, emergency: Boolean, prescriptionPath: String?) -> Unit,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    RequestFormScreen(
        title = stringResource(R.string.edit_request),
        subtitle = stringResource(R.string.edit_request_subtitle),
        submitLabel = stringResource(R.string.save_changes),
        initial = request,
        onSubmit = onSave,
        onCancel = onBack,
        bottomBar = bottomBar
    )
}
