package com.kronnoz.health.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HeartRateViewModel(private val repo: HeartRateRepository) : ViewModel() {
    val heartRate = MutableStateFlow(0.0)
    val available = MutableStateFlow(false)
    private val enabled = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            if (repo.isAvailable()) {
                enabled.collect { isEnabled ->
                    if (isEnabled) {
                        repo.flow().collect {
                            when (it) {
                                is HeartRateResult.Data -> heartRate.value = it.bpm
                                is HeartRateResult.Available -> available.value = it.isAvailable
                            }
                        }
                    }
                }
            }
        }
    }

    fun toggle() {
        enabled.value = !enabled.value
    }

    class Factory(private val repo: HeartRateRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HeartRateViewModel(repo) as T
        }
    }
}
