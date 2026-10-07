package com.example.unimarket.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val currentMessage: String = "",
    val sentMessages: List<String> = emptyList(),
    val product: Product? = null
)

class ChatViewModel(private val repository: ProductRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun loadProduct(productId: Int) {
        viewModelScope.launch {
            val product = repository.getProductById(productId)
            _uiState.update { it.copy(product = product) }
        }
    }

    fun onMessageChange(message: String) {
        _uiState.update { it.copy(currentMessage = message) }
    }

    fun sendMessage() {
        val msg = _uiState.value.currentMessage.trim()
        if (msg.isNotBlank()) {
            _uiState.update {
                it.copy(sentMessages = it.sentMessages + msg, currentMessage = "")
            }
        }
    }
}
