package com.example.unimarket.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.repository.AuthResult
import com.example.unimarket.data.repository.SessionRepository
import com.example.unimarket.data.repository.UserRepository
import com.example.unimarket.ui.util.ErrorMessages
import com.example.unimarket.ui.util.Validators
import com.example.unimarket.ui.util.rethrowIfCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    /** Error dari database, misalnya "Email atau password salah". */
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false
)

class LoginViewModel(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, generalError = null) }
    }

    fun onLogin() {
        val state = _uiState.value
        if (state.isLoading) return

        val emailError = Validators.email(state.email)
        val passwordError = if (state.password.isEmpty()) "Password wajib diisi" else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, generalError = null) }
        viewModelScope.launch {
            try {
                when (val result = userRepository.login(state.email, state.password)) {
                    is AuthResult.Success -> {
                        sessionRepository.startSession(result.user.id)
                        _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                    }
                    is AuthResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, generalError = result.message) }
                    }
                }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                ErrorMessages.log(e)
                _uiState.update { it.copy(isLoading = false, generalError = ErrorMessages.LOGIN_FAILED) }
            }
        }
    }
}
