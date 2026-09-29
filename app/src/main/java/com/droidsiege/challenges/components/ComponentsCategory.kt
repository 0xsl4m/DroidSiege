package com.droidsiege.challenges.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoorFront
import com.droidsiege.core.CategoryContributor
import com.droidsiege.core.CategoryMeta
import com.droidsiege.core.Challenge

object ComponentsCategory : CategoryContributor {
    override val category: CategoryMeta = CategoryMeta(
        id = "components",
        title = "Components & IPC",
        owasp = "M8",
        icon = Icons.Outlined.DoorFront,
    )

    override val challenges: List<Challenge> = listOf(
        ExportedL1Challenge(),
        ExportedL2Challenge(),
        ExportedL3Challenge(),
        ExportedL4Challenge(),
        ProviderL1Challenge(),
        ProviderL2Challenge(),
        ProviderL3Challenge(),
        ProviderL4Challenge(),
        IntentRedirL1Challenge(),
        IntentRedirL2Challenge(),
        IntentRedirL3Challenge(),
        IntentRedirL4Challenge(),
        DeepLinkL1Challenge(),
        DeepLinkL2Challenge(),
        DeepLinkL3Challenge(),
        DeepLinkL4Challenge(),
    )
}
