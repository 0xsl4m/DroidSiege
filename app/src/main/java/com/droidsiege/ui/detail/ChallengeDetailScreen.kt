package com.droidsiege.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.droidsiege.R
import com.droidsiege.core.Challenge
import com.droidsiege.core.LearnContent
import com.droidsiege.ui.components.CodeSnippetDiff
import com.droidsiege.ui.components.FlagInput
import com.droidsiege.ui.components.HintPanel
import com.droidsiege.ui.components.LevelChip
import com.droidsiege.ui.components.MastgRefRow
import com.droidsiege.ui.components.OwaspRefChips
import com.droidsiege.ui.components.SectionHeader
import com.droidsiege.ui.components.SecureToggle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeDetailScreen(
    onBack: () -> Unit,
    viewModel: ChallengeDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val challenge = viewModel.challenge
    val secureMode by viewModel.secureMode.collectAsStateWithLifecycle()
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val hintsUsed by viewModel.hintsUsed.collectAsStateWithLifecycle()
    val awardNow by viewModel.awardNow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            val message =
                when (event) {
                    is DetailEvent.Solved -> context.getString(R.string.detail_flag_accepted, event.points)
                    DetailEvent.WrongFlag -> context.getString(R.string.detail_flag_wrong)
                    DetailEvent.AlreadySolved -> context.getString(R.string.detail_already_solved)
                }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(challenge.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.tab_challenge)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.tab_learn)) },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.tab_hints)) },
                )
            }
            when (selectedTab) {
                0 ->
                    ChallengeTab(
                        challenge = challenge,
                        secureMode = secureMode,
                        solved = entry?.solved == true,
                        onSubmitFlag = viewModel::submitFlag,
                        onToggleSecure = viewModel::toggleSecureMode,
                    )
                1 -> LearnTab(learn = challenge.learn)
                else ->
                    HintsTab(
                        challenge = challenge,
                        hintsUsed = hintsUsed,
                        awardNow = awardNow,
                        onReveal = viewModel::revealNextHint,
                    )
            }
        }
    }
}

@Composable
private fun ChallengeTab(
    challenge: Challenge,
    secureMode: Boolean,
    solved: Boolean,
    onSubmitFlag: (String) -> Unit,
    onToggleSecure: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LevelChip(difficulty = challenge.id.level)
            Text(
                text = stringResource(R.string.challenge_points, challenge.id.level.points),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = challenge.brief,
            style = MaterialTheme.typography.bodyLarge,
        )
        SectionHeader(text = stringResource(R.string.section_owasp))
        OwaspRefChips(refs = challenge.owaspRefs)
        SectionHeader(text = stringResource(R.string.section_challenge_screen))
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                challenge.Screen(secureMode = secureMode)
            }
        }
        SecureToggle(secureMode = secureMode, onChange = { onToggleSecure() })
        SectionHeader(text = stringResource(R.string.section_flag))
        FlagInput(solved = solved, onSubmit = onSubmitFlag)
    }
}

@Composable
private fun LearnTab(learn: LearnContent) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        Text(
            text = learn.theory,
            style = MaterialTheme.typography.bodyMedium,
        )
        SectionHeader(text = stringResource(R.string.learn_references))
        MastgRefRow(refs = learn.mastgRefs)
        SectionHeader(text = stringResource(R.string.learn_diff))
        CodeSnippetDiff(
            vulnerableSnippet = learn.vulnerableSnippet,
            fixSnippet = learn.fixSnippet,
        )
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = learn.takeaway,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun HintsTab(
    challenge: Challenge,
    hintsUsed: Int,
    awardNow: Int,
    onReveal: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        HintPanel(
            challenge = challenge,
            hintsUsed = hintsUsed,
            awardNow = awardNow,
            onReveal = onReveal,
        )
    }
}
