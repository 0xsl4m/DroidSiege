package com.droidsiege.challenges.advanced

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import com.droidsiege.core.CategoryContributor
import com.droidsiege.core.CategoryMeta
import com.droidsiege.core.Challenge

object AdvancedCategory : CategoryContributor {
    override val category: CategoryMeta = CategoryMeta(
        id = "advanced",
        title = "Advanced Attacks",
        owasp = "M8+",
        icon = Icons.Outlined.BugReport,
    )

    override val challenges: List<Challenge> = listOf(
        StrandhoggL1Challenge(),
        TapjackingL1Challenge(),
        CustomTabsL1Challenge(),
        NativeL1Challenge(),
        NativeL2Challenge(),
        NativeL3Challenge(),
        NativeL4Challenge(),
    )
}
