package com.droidsiege.ui.modules.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidsiege.engine.BackendUrlStore
import com.droidsiege.engine.ScoreboardRepository
import com.droidsiege.engine.SecureModeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsEvent {
    data object ProgressReset : SettingsEvent

    data object BackendUrlSaved : SettingsEvent

    data object BackendUrlInvalid : SettingsEvent
}

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val secureModeStore: SecureModeStore,
        private val backendUrlStore: BackendUrlStore,
        private val scoreboardRepository: ScoreboardRepository,
    ) : ViewModel() {
        val secureMode: StateFlow<Boolean> =
            secureModeStore.secureMode
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = false,
                )

        val backendUrl: StateFlow<String> =
            backendUrlStore.backendUrl
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = "",
                )

        private val events = Channel<SettingsEvent>(Channel.BUFFERED)
        val eventFlow = events.receiveAsFlow()

        fun setSecureMode(enabled: Boolean) {
            viewModelScope.launch {
                secureModeStore.setSecureMode(enabled)
            }
        }

        fun saveBackendUrl(url: String) {
            viewModelScope.launch {
                val trimmed = url.trim()
                if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                    backendUrlStore.setBackendUrl(trimmed)
                    events.send(SettingsEvent.BackendUrlSaved)
                } else {
                    events.send(SettingsEvent.BackendUrlInvalid)
                }
            }
        }

        fun resetProgress() {
            viewModelScope.launch {
                scoreboardRepository.resetProgress()
                events.send(SettingsEvent.ProgressReset)
            }
        }
    }
