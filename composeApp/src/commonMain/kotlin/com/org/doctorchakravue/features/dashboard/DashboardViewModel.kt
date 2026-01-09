package com.org.doctorchakravue.features.dashboard

import com.org.doctorchakravue.data.DoctorRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: DoctorRepository) {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var autoRefreshJob: Job? = null

    companion object {
        private const val AUTO_REFRESH_INTERVAL = 30_000L // 30 seconds
    }

    init {
        loadDashboard()
        startAutoRefresh()
    }

    private fun loadDashboard() {
        scope.launch {
            val doctorId = repository.getDoctorId()
            val name = repository.getDoctorName()

            if (doctorId.isNotEmpty()) {
                // Load data
                val urgent = repository.getUrgentSubmissions(doctorId)
                val hist = repository.getHistory(doctorId)

                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        doctorName = name,
                        urgentReviews = urgent,
                        history = hist
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false, isRefreshing = false) }
            }
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = scope.launch {
            while (isActive) {
                delay(AUTO_REFRESH_INTERVAL)
                refreshData(silent = true)
            }
        }
    }

    fun refresh() {
        refreshData(silent = false)
    }

    private fun refreshData(silent: Boolean) {
        scope.launch {
            if (!silent) {
                _state.update { it.copy(isRefreshing = true) }
            }

            val doctorId = repository.getDoctorId()
            if (doctorId.isNotEmpty()) {
                val urgent = repository.getUrgentSubmissions(doctorId)
                val hist = repository.getHistory(doctorId)

                _state.update {
                    it.copy(
                        isRefreshing = false,
                        urgentReviews = urgent,
                        history = hist
                    )
                }
            } else {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    fun logout() {
        stopAutoRefresh()
        repository.logout()
    }
}
