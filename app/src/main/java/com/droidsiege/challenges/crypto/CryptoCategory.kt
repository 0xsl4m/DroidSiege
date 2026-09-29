package com.droidsiege.challenges.crypto

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VpnKey
import com.droidsiege.core.CategoryContributor
import com.droidsiege.core.CategoryMeta
import com.droidsiege.core.Challenge

object CryptoCategory : CategoryContributor {
    override val category: CategoryMeta = CategoryMeta(
        id = "crypto",
        title = "Cryptography",
        owasp = "M10",
        icon = Icons.Outlined.VpnKey,
    )

    override val challenges: List<Challenge> = listOf(
        HardcodedL1Challenge(),
        HardcodedL2Challenge(),
        HardcodedL3Challenge(),
        HardcodedL4Challenge(),
        EcbIvL1Challenge(),
        EcbIvL2Challenge(),
        EcbIvL3Challenge(),
        EcbIvL4Challenge(),
        HomegrownL1Challenge(),
        HomegrownL2Challenge(),
        HomegrownL3Challenge(),
        HomegrownL4Challenge(),
        KdfL1Challenge(),
        KdfL2Challenge(),
        KdfL3Challenge(),
        KdfL4Challenge(),
        RandomL1Challenge(),
        RandomL2Challenge(),
        RandomL3Challenge(),
        RandomL4Challenge(),
    )
}
