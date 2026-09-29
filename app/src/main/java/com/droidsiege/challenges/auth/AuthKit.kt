package com.droidsiege.challenges.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** A single labelled field for the auth consoles. */
@Composable
fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** A primary action button for the auth consoles. */
@Composable
fun AuthButton(
    label: String,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }
}

/** The console box the auth screens report into. */
@Composable
fun AuthConsole(output: String) {
    if (output.isNotEmpty()) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = output,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
