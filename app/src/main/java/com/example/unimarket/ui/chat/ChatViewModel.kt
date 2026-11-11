package com.example.unimarket.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.model.Message
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.repository.ChatErrors
import com.example.unimarket.data.repository.MessageRepository
import com.example.unimarket.data.repository.ProductRepository
import com.example.unimarket.data.repository.SendMessageResult
import com.example.unimarket.data.repository.SessionRepository
import com.example.unimarket.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.unimarket.ui.util.ErrorMessages
import com.example.unimarket.ui.util.rethrowIfCancellation
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val product: Product? = null,
    /** Nama lawan bicara: penjual (jika saya pembeli) atau pembeli (jika saya penjual). */
    val otherUserName: String = "",
    val messages: List<Message> = emptyList(),
    val currentUserId: Long? = null,
    val isLoading: Boolean = true,
    /** Terisi jika user tidak boleh membuka percakapan ini. */
    val accessError: String? = null
)

data class ChatInputState(
    val draft: String = "",
    val isSending: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    savedStateHandle: SavedStateHandle,
    productRepository: ProductRepository,
    userRepository: UserRepository,
    private val messageRepository: MessageRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    /** Argumen navigasi "chat/{productId}/{buyerId}". */
    private val productId: Int = checkNotNull(savedStateHandle.get<Int>(PRODUCT_ID_ARG))
    private val buyerId: Long = checkNotNull(savedStateHandle.get<Long>(BUYER_ID_ARG))

    val uiState: StateFlow<ChatUiState> = combine(
        productRepository.observeProduct(productId),
        userRepository.observeUser(buyerId),
        messageRepository.getMessages(productId, buyerId),
        sessionRepository.currentUserId
    ) { product, buyer, messages, currentUserId ->
        val accessError = when {
            product == null -> ChatErrors.PRODUCT_NOT_FOUND
            buyerId == product.sellerId -> ChatErrors.OWN_PRODUCT
            currentUserId != buyerId && currentUserId != product.sellerId -> ChatErrors.NOT_PARTICIPANT
            else -> null
        }
        ChatUiState(
            product = product,
            otherUserName = if (currentUserId == buyerId) product?.sellerName.orEmpty() else buyer?.name.orEmpty(),
            // Jangan tampilkan isi percakapan jika user tidak berhak.
            messages = if (accessError == null) messages else emptyList(),
            currentUserId = currentUserId,
            isLoading = false,
            accessError = accessError
        )
    }.catch { e ->
        ErrorMessages.log(e)
        // Ditampilkan layar sebagai "Chat tidak tersedia" + pesan ini, input disembunyikan.
        emit(ChatUiState(isLoading = false, accessError = ErrorMessages.LOAD_FAILED))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    private val _inputState = MutableStateFlow(ChatInputState())
    val inputState: StateFlow<ChatInputState> = _inputState.asStateFlow()

    fun onDraftChange(value: String) {
        _inputState.update { it.copy(draft = value.take(ChatErrors.MAX_LENGTH), error = null) }
    }

    fun sendMessage() {
        val input = _inputState.value
        if (input.isSending) return
        if (input.draft.isBlank()) {
            _inputState.update { it.copy(error = ChatErrors.EMPTY) }
            return
        }
        val senderId = sessionRepository.currentUserId.value
        if (senderId == null || uiState.value.accessError != null) {
            _inputState.update { it.copy(error = uiState.value.accessError ?: ChatErrors.NOT_PARTICIPANT) }
            return
        }

        _inputState.update { it.copy(isSending = true, error = null) }
        viewModelScope.launch {
            try {
                when (val result = messageRepository.sendMessage(productId, buyerId, senderId, input.draft)) {
                    is SendMessageResult.Success -> _inputState.value = ChatInputState()
                    is SendMessageResult.Error -> _inputState.update { it.copy(isSending = false, error = result.message) }
                }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                ErrorMessages.log(e)
                // Draft tetap disimpan agar user bisa mencoba kirim ulang.
                _inputState.update { it.copy(isSending = false, error = ErrorMessages.SEND_FAILED) }
            }
        }
    }

    companion object {
        const val PRODUCT_ID_ARG = "productId"
        const val BUYER_ID_ARG = "buyerId"
    }
}
