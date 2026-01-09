package com.org.doctorchakravue.features.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.org.doctorchakravue.data.DoctorRepository

// Simple State Management
data class LoginState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class LoginViewModel(private val repository: DoctorRepository) {
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    fun login(email: String, pass: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                withContext(Dispatchers.Default) {
                    repository.login(email, pass)
                }
                _state.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
