package com.example.unimarket.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.repository.SessionRepository
import com.example.unimarket.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.unimarket.ui.util.ErrorMessages
import com.example.unimarket.ui.util.rethrowIfCancellation

/**
 * Menentukan layar awal aplikasi berdasarkan sesi login.
 */
class SessionViewModel(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    /** null = masih memeriksa sesi (tampilkan loading). */
    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = sessionRepository.currentUserId.value
            // Sesi dianggap valid hanya jika user-nya masih ada di database
            // (misalnya database bisa saja direset karena perubahan versi).
            val isValid = try {
                userId != null && userRepository.getUserById(userId) != null
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                ErrorMessages.log(e)
                false // database bermasalah: arahkan ke Login daripada aplikasi crash
            }
            if (userId != null && !isValid) sessionRepository.logout()
            _startDestination.value = if (isValid) Routes.HOME else Routes.LOGIN
        }
    }
}
