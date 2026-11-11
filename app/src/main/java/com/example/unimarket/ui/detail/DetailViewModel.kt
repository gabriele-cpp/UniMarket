package com.example.unimarket.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.repository.ProductRepository
import com.example.unimarket.data.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import com.example.unimarket.ui.util.ErrorMessages
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class DetailUiState(
    val product: Product? = null,
    val isLoading: Boolean = true,
    /** true jika barang ini milik user yang sedang login. */
    val isOwner: Boolean = false,
    /** Dipakai sebagai buyerId saat membuka chat. */
    val currentUserId: Long? = null,
    val errorMessage: String? = null
)

class DetailViewModel(
    private val productRepository: ProductRepository,
    sessionRepository: SessionRepository
) : ViewModel() {
    private val productId = MutableStateFlow<Int?>(null)

    /** Diamati terus, sehingga perubahan dari layar Edit/My Items langsung terlihat. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DetailUiState> = combine(
        productId.filterNotNull().flatMapLatest { productRepository.observeProduct(it) },
        sessionRepository.currentUserId
    ) { product, userId ->
        DetailUiState(
            product = product,
            isLoading = false,
            isOwner = product != null && product.sellerId == userId,
            currentUserId = userId
        )
    }.catch { e ->
        ErrorMessages.log(e)
        emit(DetailUiState(isLoading = false, errorMessage = ErrorMessages.LOAD_FAILED))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun loadProduct(id: Int) {
        productId.value = id
    }
}
