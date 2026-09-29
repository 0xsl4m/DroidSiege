package com.droidsiege.challenges.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Password
import com.droidsiege.core.CategoryContributor
import com.droidsiege.core.CategoryMeta
import com.droidsiege.core.Challenge

object AuthCategory : CategoryContributor {
    override val category: CategoryMeta = CategoryMeta(
        id = "auth",
        title = "Credentials & Auth",
        owasp = "M1/M3",
        icon = Icons.Outlined.Password,
    )

    override val challenges: List<Challenge> = listOf(
        CredsL1Challenge(),
        CredsL2Challenge(),
        CredsL3Challenge(),
        CredsL4Challenge(),
        SessionL1Challenge(),
        SessionL2Challenge(),
        SessionL3Challenge(),
        SessionL4Challenge(),
        AuthCheckL1Challenge(),
        AuthCheckL2Challenge(),
        AuthCheckL3Challenge(),
        AuthCheckL4Challenge(),
        PinLockL1Challenge(),
        PinLockL2Challenge(),
        PinLockL3Challenge(),
        PinLockL4Challenge(),
    )
}
