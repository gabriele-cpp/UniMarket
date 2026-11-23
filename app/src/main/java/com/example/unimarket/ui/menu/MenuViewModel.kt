package com.example.unimarket.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.repository.MessageRepository
import com.example.unimarket.data.repository.SessionRepository
import com.example.unimarket.ui.util.initials
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import com.example.unimarket.ui.util.ErrorMessages
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MenuViewModel(
    private val sessionRepository: SessionRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    /** Inisial nama user yang sedang login untuk avatar di Menu. */
    val userInitials: StateFlow<String> = sessionRepository.currentUser
        .map { user -> user?.let { initials(it.name) } ?: "" }
        .catch { e ->
            ErrorMessages.log(e)
            emit("") // avatar kosong lebih baik daripada crash
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Jumlah pesan belum dibaca untuk badge di menu Pesan. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val unreadCount: StateFlow<Int> = sessionRepository.currentUserId.flatMapLatest { userId ->
        if (userId == null) flowOf(0) else messageRepository.observeUnreadCount(userId)
    }.catch { e ->
        ErrorMessages.log(e)
        emit(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun logout() {
        sessionRepository.logout()
    }
}
