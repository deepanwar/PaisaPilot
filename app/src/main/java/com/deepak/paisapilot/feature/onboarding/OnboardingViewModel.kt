package com.deepak.paisapilot.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.paisapilot.core.data.prefs.SettingsRepository
import com.deepak.paisapilot.domain.profile.DisplayNameResult
import com.deepak.paisapilot.domain.profile.NameSubmit
import com.deepak.paisapilot.domain.profile.resolveDisplayName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()
    private var pendingSubmit: NameSubmit = NameSubmit.Start

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, nameError = null, saveFailed = false) }
    }

    fun onStart() = submit(NameSubmit.Start)

    fun onSkip() = submit(NameSubmit.Skip)

    fun onRetry() = submit(pendingSubmit)

    private fun submit(submit: NameSubmit) {
        val current = _state.value
        if (current.saving) return
        pendingSubmit = submit
        when (val result = resolveDisplayName(current.name, submit)) {
            is DisplayNameResult.Valid -> {
                _state.update { it.copy(saving = true, nameError = null, saveFailed = false) }
                viewModelScope.launch {
                    try {
                        settingsRepository.completeOnboarding(result.name)
                        _state.update { it.copy(saving = false, completed = true) }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        _state.update { it.copy(saving = false, saveFailed = true) }
                    }
                }
            }
            DisplayNameResult.Blank, DisplayNameResult.TooLong -> {
                _state.update { it.copy(nameError = result, saveFailed = false) }
            }
        }
    }
}
