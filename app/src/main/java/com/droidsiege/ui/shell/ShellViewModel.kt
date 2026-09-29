package com.droidsiege.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidsiege.engine.ScoreboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ShellViewModel
    @Inject
    constructor(
        scoreboardRepository: ScoreboardRepository,
    ) : ViewModel() {
        val totalScore: StateFlow<Int> =
            scoreboardRepository.totalScore
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = 0,
                )
    }
