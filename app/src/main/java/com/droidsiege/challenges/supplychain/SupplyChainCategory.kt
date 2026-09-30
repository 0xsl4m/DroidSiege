package com.droidsiege.challenges.supplychain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LinkOff
import com.droidsiege.core.CategoryContributor
import com.droidsiege.core.CategoryMeta
import com.droidsiege.core.Challenge

object SupplyChainCategory : CategoryContributor {
    override val category: CategoryMeta = CategoryMeta(
        id = "supplychain",
        title = "Supply Chain",
        owasp = "M2",
        icon = Icons.Outlined.LinkOff,
    )

    override val challenges: List<Challenge> = listOf(
        VulndepL1Challenge(),
        VulndepL2Challenge(),
        VulndepL3Challenge(),
        VulndepL4Challenge(),
        DynLoadL1Challenge(),
        DynLoadL2Challenge(),
        DynLoadL3Challenge(),
        DynLoadL4Challenge(),
    )
}
