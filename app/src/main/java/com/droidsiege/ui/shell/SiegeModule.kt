package com.droidsiege.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class SiegeModule(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val categories: List<String>,
) {
    HUB("hub", "Challenges", Icons.Outlined.EmojiEvents, emptyList()),
    AUTH("auth", "Login", Icons.Outlined.Lock, listOf("auth", "demo")),
    WALLET("wallet", "Wallet", Icons.Outlined.AccountBalanceWallet, listOf("network", "crypto", "storage")),
    VAULT("vault", "Vault", Icons.Outlined.Inventory2, listOf("storage", "crypto")),
    CHAT("chat", "Chat", Icons.Outlined.Chat, listOf("webview", "inject")),
    OFFERS("offers", "Offers", Icons.Outlined.Newspaper, listOf("webview", "components", "advanced")),
    PROFILE("profile", "Profile", Icons.Outlined.Person, listOf("massassign", "privacy")),
    SETTINGS("settings", "Settings", Icons.Outlined.Settings, listOf("components", "storage")),
    ;

    companion object {
        val topLevel: List<SiegeModule> = entries.toList()
    }
}
