package com.droidsiege.ui.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidsiege.engine.ScoreboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ModuleViewModel
    @Inject
    constructor(
        scoreboardRepository: ScoreboardRepository,
    ) : ViewModel() {
        val solvedById: StateFlow<Map<String, Boolean>> =
            scoreboardRepository.observeAll()
                .map { entries -> entries.associate { it.idKey to it.solved } }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = emptyMap(),
                )
    }
