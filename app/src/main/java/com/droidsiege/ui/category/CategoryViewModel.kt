package com.droidsiege.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidsiege.core.CategoryMeta
import com.droidsiege.core.Challenge
import com.droidsiege.core.ChallengeRegistry
import com.droidsiege.engine.ScoreboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ChallengeRow(
    val challenge: Challenge,
    val solved: Boolean,
)

data class CategoryUiState(
    val category: CategoryMeta?,
    val rows: List<ChallengeRow>,
)

@HiltViewModel
class CategoryViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        scoreboardRepository: ScoreboardRepository,
    ) : ViewModel() {
        private val categoryId: String = checkNotNull(savedStateHandle["categoryId"])

        val uiState: StateFlow<CategoryUiState> =
            scoreboardRepository.observeAll()
                .map { entries ->
                    CategoryUiState(
                        category = ChallengeRegistry.categoryById(categoryId),
                        rows =
                            ChallengeRegistry.challengesByCategory(categoryId).map { challenge ->
                                ChallengeRow(
                                    challenge = challenge,
                                    solved = entries.any { it.idKey == challenge.id.key && it.solved },
                                )
                            },
                    )
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue =
                        CategoryUiState(
                            category = ChallengeRegistry.categoryById(categoryId),
                            rows = emptyList(),
                        ),
                )
    }
