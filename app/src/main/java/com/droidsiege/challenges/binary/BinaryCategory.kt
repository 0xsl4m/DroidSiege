package com.droidsiege.challenges.binary

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import com.droidsiege.core.CategoryContributor
import com.droidsiege.core.CategoryMeta
import com.droidsiege.core.Challenge

object BinaryCategory : CategoryContributor {
    override val category: CategoryMeta = CategoryMeta(
        id = "binary",
        title = "Binary Protections",
        owasp = "M7",
        icon = Icons.Outlined.Shield,
    )

    override val challenges: List<Challenge> = listOf(
        RootDetectL1Challenge(), RootDetectL2Challenge(), RootDetectL3Challenge(),
        FridaL1Challenge(), FridaL2Challenge(), FridaL3Challenge(),
        NoobfuscL1Challenge(), NoobfuscL2Challenge(), NoobfuscL3Challenge(), NoobfuscL4Challenge(),
    )
}
