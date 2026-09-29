package com.droidsiege.challenges.demo

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Science
import com.droidsiege.core.CategoryContributor
import com.droidsiege.core.CategoryMeta
import com.droidsiege.core.Challenge

object DemoCategory : CategoryContributor {
    override val category: CategoryMeta =
        CategoryMeta(
            id = "demo",
            title = "Demo",
            owasp = "M9",
            icon = Icons.Outlined.Science,
        )

    override val challenges: List<Challenge> = listOf(HelloFlagChallenge())
}
