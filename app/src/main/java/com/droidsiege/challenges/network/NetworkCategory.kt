package com.droidsiege.challenges.network

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiOff
import com.droidsiege.core.CategoryContributor
import com.droidsiege.core.CategoryMeta
import com.droidsiege.core.Challenge

object NetworkCategory : CategoryContributor {
    override val category: CategoryMeta = CategoryMeta(
        id = "network",
        title = "Insecure Communication",
        owasp = "M5",
        icon = Icons.Outlined.WifiOff,
    )

    override val challenges: List<Challenge> = listOf(
        CleartextL1Challenge(),
        CleartextL2Challenge(),
        CleartextL3Challenge(),
        CleartextL4Challenge(),
        PinningL1Challenge(),
        PinningL2Challenge(),
        PinningL3Challenge(),
        PinningL4Challenge(),
        TrustAllL1Challenge(),
        TrustAllL2Challenge(),
        TrustAllL3Challenge(),
        TrustAllL4Challenge(),
        UrlLeakL1Challenge(),
        UrlLeakL2Challenge(),
        UrlLeakL3Challenge(),
        UrlLeakL4Challenge(),
    )
}
