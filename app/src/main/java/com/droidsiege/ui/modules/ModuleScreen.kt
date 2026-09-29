package com.droidsiege.ui.modules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.droidsiege.R
import com.droidsiege.core.ChallengeRegistry
import com.droidsiege.ui.components.ChallengeCard
import com.droidsiege.ui.components.EmptyState
import com.droidsiege.ui.components.SectionHeader
import com.droidsiege.ui.shell.SiegeModule

@Composable
fun ModuleScreen(
    module: SiegeModule,
    paddingValues: PaddingValues,
    onChallengeClick: (String) -> Unit,
    viewModel: ModuleViewModel = hiltViewModel(),
) {
    val solvedById by viewModel.solvedById.collectAsStateWithLifecycle()
    val challenges = ChallengeRegistry.challengesInCategories(module.categories)

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        ModuleHeader(module = module)
        SectionHeader(text = stringResource(R.string.module_section_title))
        if (challenges.isEmpty()) {
            EmptyState(text = stringResource(R.string.module_empty))
        } else {
            challenges.forEach { challenge ->
                ChallengeCard(
                    challenge = challenge,
                    solved = solvedById[challenge.id.key] == true,
                    onClick = { onChallengeClick(challenge.id.key) },
                )
            }
        }
    }
}
