package com.droidsiege.ui.modules.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.droidsiege.BuildConfig
import com.droidsiege.R
import com.droidsiege.ui.components.SectionHeader
import com.droidsiege.ui.components.SecureToggle

@Composable
fun SettingsScreen(
    paddingValues: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val secureMode by viewModel.secureMode.collectAsStateWithLifecycle()
    val savedUrl by viewModel.backendUrl.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var urlField by rememberSaveable { mutableStateOf("") }
    var urlInitialized by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    if (savedUrl.isNotEmpty() && !urlInitialized) {
        urlField = savedUrl
        urlInitialized = true
    }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            val message =
                when (event) {
                    SettingsEvent.ProgressReset -> context.getString(R.string.settings_reset_done)
                    SettingsEvent.BackendUrlSaved -> context.getString(R.string.settings_backend_saved)
                    SettingsEvent.BackendUrlInvalid -> context.getString(R.string.settings_backend_invalid)
                }
            snackbarHostState.showSnackbar(message)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        SecureToggle(
            secureMode = secureMode,
            onChange = { viewModel.setSecureMode(it) },
        )

        SectionHeader(text = stringResource(R.string.settings_backend_section))
        OutlinedTextField(
            value = urlField,
            onValueChange = { urlField = it },
            label = { Text(stringResource(R.string.settings_backend_label)) },
            supportingText = { Text(stringResource(R.string.settings_backend_supporting)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = { viewModel.saveBackendUrl(urlField) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_backend_save))
        }

        SectionHeader(text = stringResource(R.string.settings_progress_section))
        Button(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_reset))
        }

        Text(
            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.settings_reset_title)) },
            text = { Text(stringResource(R.string.settings_reset_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetProgress()
                    },
                ) {
                    Text(stringResource(R.string.settings_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.settings_reset_cancel))
                }
            },
        )
    }

    SnackbarHost(hostState = snackbarHostState)
}
