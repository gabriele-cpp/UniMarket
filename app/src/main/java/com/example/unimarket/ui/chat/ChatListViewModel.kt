package com.example.unimarket.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.model.Conversation
import com.example.unimarket.data.repository.MessageRepository
import com.example.unimarket.data.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import com.example.unimarket.ui.util.ErrorMessages
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class ChatListUiState(
    val conversations: List<Conversation> = emptyList(),
    val currentUserId: Long? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ChatListViewModel(
    messageRepository: MessageRepository,
    sessionRepository: SessionRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val conversationsFlow = sessionRepository.currentUserId.flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList()) else messageRepository.getConversations(userId)
    }

    val uiState: StateFlow<ChatListUiState> = combine(
        conversationsFlow,
        sessionRepository.currentUserId
    ) { conversations, userId ->
        ChatListUiState(conversations = conversations, currentUserId = userId, isLoading = false)
    }.catch { e ->
        ErrorMessages.log(e)
        emit(ChatListUiState(isLoading = false, errorMessage = ErrorMessages.LOAD_FAILED))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatListUiState())
}
