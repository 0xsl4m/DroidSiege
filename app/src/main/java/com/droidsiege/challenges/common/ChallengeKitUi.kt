package com.droidsiege.challenges.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.droidsiege.R

/**
 * Screen for artifact-based challenges: a short note plus buttons that perform the
 * challenge's real side effects (writing files, prefs, logs, ...). The player finds the
 * flag in the produced artifact and submits it through the normal flag input.
 */
@Composable
fun ActionChallengeScreen(
    secureMode: Boolean,
    actions: List<KitAction>,
    note: String? = null,
) {
    val context = LocalContext.current
    var status by rememberSaveable { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        note?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        actions.forEach { action ->
            Button(
                onClick = { status = action.run(context, secureMode) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(action.label)
            }
        }
        if (status.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

/**
 * Console-style screen for crypto challenges: shows one or more immutable lines
 * (ciphertexts, salts, parameters) plus optional interactive probe buttons.
 */
@Composable
fun ConsoleChallengeScreen(
    secureMode: Boolean,
    lines: List<Pair<String, String>> = emptyList(),
    probes: List<KitAction> = emptyList(),
    note: String? = null,
) {
    val context = LocalContext.current
    var status by rememberSaveable { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        note?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        lines.forEach { (label, value) ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
        probes.forEach { probe ->
            Button(
                onClick = { status = probe.run(context, secureMode) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(probe.label)
            }
        }
        if (status.isNotEmpty()) {
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Gate screen: the player must enter the predicted value to unlock the flag. */
@Composable
fun GateChallengeScreen(
    secureMode: Boolean,
    note: String? = null,
    inputLabel: String,
    unlockLabel: String,
    lockedMessage: String,
    expectedInput: (secureMode: Boolean) -> String?,
    unlockedContent: String,
) {
    var input by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var unlocked by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        note?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(inputLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                val expected = expectedInput(secureMode)
                if (expected != null && input.trim() == expected) {
                    unlocked = true
                    message = unlockedContent
                } else {
                    message = lockedMessage
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(unlockLabel)
        }
        if (message.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color =
                    if (unlocked) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        OutlinedButton(
            onClick = { message = context.getString(R.string.challenge_gate_reset) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.challenge_gate_reset))
        }
    }
}
