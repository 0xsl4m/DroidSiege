package com.droidsiege.ui.modules

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.droidsiege.R
import com.droidsiege.ui.shell.SiegeModule

@Composable
internal fun ModuleHeader(module: SiegeModule) {
    when (module) {
        SiegeModule.AUTH -> LoginPlaceholder()
        SiegeModule.WALLET -> WalletPlaceholder()
        SiegeModule.VAULT -> VaultPlaceholder()
        SiegeModule.CHAT -> ChatPlaceholder()
        SiegeModule.OFFERS -> OffersPlaceholder()
        SiegeModule.PROFILE -> ProfilePlaceholder()
        SiegeModule.SETTINGS -> Unit
        SiegeModule.HUB -> Unit
    }
}

@Composable
private fun StubButton(labelRes: Int = R.string.module_stub_action) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            Toast.makeText(context, R.string.module_stub_message, Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(labelRes))
    }
}

@Composable
private fun LoginPlaceholder() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = "",
                onValueChange = { },
                label = { Text(stringResource(R.string.login_email)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = "",
                onValueChange = { },
                label = { Text(stringResource(R.string.login_password)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            StubButton(labelRes = R.string.login_sign_in)
            Text(
                text = stringResource(R.string.login_forgot),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun WalletPlaceholder() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.wallet_balance_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.wallet_balance_value),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            StubButton(labelRes = R.string.wallet_send)
            HorizontalDivider()
            MockRow(stringResource(R.string.wallet_tx1_name), stringResource(R.string.wallet_tx1_amount))
            MockRow(stringResource(R.string.wallet_tx2_name), stringResource(R.string.wallet_tx2_amount))
            MockRow(stringResource(R.string.wallet_tx3_name), stringResource(R.string.wallet_tx3_amount))
        }
    }
}

@Composable
private fun VaultPlaceholder() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.vault_title),
                style = MaterialTheme.typography.titleMedium,
            )
            MockRow(stringResource(R.string.vault_item1_name), stringResource(R.string.vault_item1_meta))
            MockRow(stringResource(R.string.vault_item2_name), stringResource(R.string.vault_item2_meta))
            MockRow(stringResource(R.string.vault_item3_name), stringResource(R.string.vault_item3_meta))
            StubButton(labelRes = R.string.vault_add)
        }
    }
}

@Composable
private fun ChatPlaceholder() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_title),
                style = MaterialTheme.typography.titleMedium,
            )
            MockRow(stringResource(R.string.chat_conv1_name), stringResource(R.string.chat_conv1_preview))
            MockRow(stringResource(R.string.chat_conv2_name), stringResource(R.string.chat_conv2_preview))
            MockRow(stringResource(R.string.chat_conv3_name), stringResource(R.string.chat_conv3_preview))
        }
    }
}

@Composable
private fun OffersPlaceholder() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.offers_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.offers_promo1_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.offers_promo1_body),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            StubButton(labelRes = R.string.offers_open)
        }
    }
}

@Composable
private fun ProfilePlaceholder() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Text(text = "S", style = MaterialTheme.typography.titleLarge)
                    }
                }
                Column {
                    Text(
                        text = stringResource(R.string.profile_name),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.profile_member_since),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider()
            MockRow(stringResource(R.string.profile_stat_challenges), stringResource(R.string.profile_stat_value))
        }
    }
}

@Composable
private fun MockRow(
    title: String,
    subtitle: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
