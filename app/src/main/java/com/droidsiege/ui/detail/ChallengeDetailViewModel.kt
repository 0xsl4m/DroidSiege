package com.droidsiege.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidsiege.core.Challenge
import com.droidsiege.core.ChallengeRegistry
import com.droidsiege.engine.HintController
import com.droidsiege.engine.ScoreboardEntity
import com.droidsiege.engine.ScoreboardRepository
import com.droidsiege.engine.SecureModeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetailEvent {
    data class Solved(val points: Int) : DetailEvent

    data object WrongFlag : DetailEvent

    data object AlreadySolved : DetailEvent
}

@HiltViewModel
class ChallengeDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val scoreboardRepository: ScoreboardRepository,
        private val hintController: HintController,
        private val secureModeStore: SecureModeStore,
    ) : ViewModel() {
        private val idKey: String = checkNotNull(savedStateHandle["idKey"])

        val challenge: Challenge =
            requireNotNull(ChallengeRegistry.challengeByKey(idKey)) {
                "No challenge registered for key $idKey"
            }

        val secureMode: StateFlow<Boolean> =
            secureModeStore.secureMode
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = false,
                )

        val entry: StateFlow<ScoreboardEntity?> =
            scoreboardRepository.observe(idKey)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = null,
                )

        val hintsUsed: StateFlow<Int> =
            hintController.hintsUsed(idKey)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = 0,
                )

        val awardNow: StateFlow<Int> =
            hintsUsed
                .map { used -> hintController.awardFor(challenge, used) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = challenge.id.level.points,
                )

        private val events = Channel<DetailEvent>(Channel.BUFFERED)
        val eventFlow = events.receiveAsFlow()

        fun submitFlag(input: String) {
            viewModelScope.launch {
                if (!challenge.validateFlag(input)) {
                    events.send(DetailEvent.WrongFlag)
                    return@launch
                }
                val solved =
                    scoreboardRepository.markSolved(
                        challenge = challenge,
                        hintsUsed = hintsUsed.value,
                    )
                if (solved) {
                    events.send(DetailEvent.Solved(hintController.awardFor(challenge, hintsUsed.value)))
                } else {
                    events.send(DetailEvent.AlreadySolved)
                }
            }
        }

        fun revealNextHint() {
            viewModelScope.launch {
                hintController.revealNext(challenge)
            }
        }

        fun toggleSecureMode() {
            viewModelScope.launch {
                secureModeStore.setSecureMode(!secureMode.value)
            }
        }
    }
