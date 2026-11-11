package com.example.unimarket.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.repository.SessionRepository
import com.example.unimarket.ui.util.initials
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import com.example.unimarket.ui.util.ErrorMessages
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MenuViewModel(private val sessionRepository: SessionRepository) : ViewModel() {

    /** Inisial nama user yang sedang login untuk avatar di Menu. */
    val userInitials: StateFlow<String> = sessionRepository.currentUser
        .map { user -> user?.let { initials(it.name) } ?: "" }
        .catch { e ->
            ErrorMessages.log(e)
            emit("") // avatar kosong lebih baik daripada crash
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun logout() {
        sessionRepository.logout()
    }
}
