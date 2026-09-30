package com.droidsiege.challenges.privacy

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import com.droidsiege.core.CategoryContributor
import com.droidsiege.core.CategoryMeta
import com.droidsiege.core.Challenge

object PrivacyCategory : CategoryContributor {
    override val category: CategoryMeta = CategoryMeta(
        id = "privacy",
        title = "Inadequate Privacy",
        owasp = "M6",
        icon = Icons.Outlined.VisibilityOff,
    )

    override val challenges: List<Challenge> = listOf(
        PiiLogsL1Challenge(), PiiLogsL2Challenge(), PiiLogsL3Challenge(), PiiLogsL4Challenge(),
        PermsL1Challenge(), PermsL2Challenge(), PermsL3Challenge(), PermsL4Challenge(),
        RecentsL1Challenge(), RecentsL2Challenge(), RecentsL3Challenge(), RecentsL4Challenge(),
    )
}
