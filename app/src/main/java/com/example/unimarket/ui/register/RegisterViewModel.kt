package com.example.unimarket.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.repository.AuthErrors
import com.example.unimarket.data.repository.AuthResult
import com.example.unimarket.data.repository.SessionRepository
import com.example.unimarket.data.repository.UserRepository
import com.example.unimarket.ui.util.ErrorMessages
import com.example.unimarket.ui.util.Validators
import com.example.unimarket.ui.util.rethrowIfCancellation
import com.example.unimarket.ui.util.normalizePhone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val isRegistered: Boolean = false
)

class RegisterViewModel(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null, generalError = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }
    }

    /** Hanya menerima digit, maksimal 14. */
    fun onPhoneChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(14)
        _uiState.update { it.copy(phone = digits, phoneError = null, generalError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, generalError = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null, generalError = null) }
    }

    fun onRegister() {
        val s = _uiState.value
        if (s.isLoading) return

        val validated = s.copy(
            nameError = Validators.name(s.name),
            emailError = Validators.email(s.email),
            phoneError = Validators.phone(s.phone),
            passwordError = Validators.password(s.password),
            confirmPasswordError = Validators.confirmPassword(s.password, s.confirmPassword)
        )
        val hasError = listOf(
            validated.nameError, validated.emailError, validated.phoneError,
            validated.passwordError, validated.confirmPasswordError
        ).any { it != null }
        if (hasError) {
            _uiState.value = validated
            return
        }

        _uiState.update { it.copy(isLoading = true, generalError = null) }
        viewModelScope.launch {
            val result = try {
                userRepository.register(
                    name = s.name,
                    email = s.email,
                    password = s.password,
                    phone = normalizePhone(s.phone)
                )
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                ErrorMessages.log(e)
                AuthResult.Error(ErrorMessages.REGISTER_FAILED)
            }
            when (result) {
                is AuthResult.Success -> {
                    // Setelah daftar, user langsung login.
                    sessionRepository.startSession(result.user.id)
                    _uiState.update { it.copy(isLoading = false, isRegistered = true) }
                }
                is AuthResult.Error -> _uiState.update {
                    if (result.message == AuthErrors.EMAIL_TAKEN) {
                        it.copy(isLoading = false, emailError = result.message)
                    } else {
                        it.copy(isLoading = false, generalError = result.message)
                    }
                }
            }
        }
    }
}
