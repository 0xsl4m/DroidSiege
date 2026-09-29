package com.droidsiege.ui.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidsiege.core.CategoryMeta
import com.droidsiege.core.ChallengeRegistry
import com.droidsiege.engine.ScoreboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CategoryProgress(
    val meta: CategoryMeta,
    val solved: Int,
    val total: Int,
)

data class HubUiState(
    val totalScore: Int,
    val solvedCount: Int,
    val categories: List<CategoryProgress>,
)

@HiltViewModel
class HubViewModel
    @Inject
    constructor(
        scoreboardRepository: ScoreboardRepository,
    ) : ViewModel() {
        val uiState: StateFlow<HubUiState> =
            scoreboardRepository.observeAll()
                .map { entries ->
                    HubUiState(
                        totalScore = entries.sumOf { it.pointsAwarded },
                        solvedCount = entries.count { it.solved },
                        categories =
                            ChallengeRegistry.categories.map { meta ->
                                CategoryProgress(
                                    meta = meta,
                                    solved = entries.count { it.category == meta.id && it.solved },
                                    total = ChallengeRegistry.challengesByCategory(meta.id).size,
                                )
                            },
                    )
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = HubUiState(totalScore = 0, solvedCount = 0, categories = emptyList()),
                )
    }
