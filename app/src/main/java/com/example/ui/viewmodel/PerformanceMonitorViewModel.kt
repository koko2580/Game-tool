package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.TelemetryRepository
import com.example.domain.model.SystemTelemetrySnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PerformanceMonitorViewModel(
    private val telemetryRepository: TelemetryRepository
) : ViewModel() {

    val snapshot: StateFlow<SystemTelemetrySnapshot> = telemetryRepository.telemetrySnapshot
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(2_000L),
            initialValue = telemetryRepository.telemetrySnapshot.value
        )

    fun refreshNow() {
        telemetryRepository.refreshNow()
    }

    class Factory(private val repository: TelemetryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PerformanceMonitorViewModel(repository) as T
        }
    }
}
