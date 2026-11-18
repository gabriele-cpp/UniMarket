package com.example.unimarket.ui.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.model.Order
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.model.PurchaseRules
import com.example.unimarket.data.repository.PaymentErrors
import com.example.unimarket.data.repository.PaymentRepository
import com.example.unimarket.data.repository.PaymentResult
import com.example.unimarket.data.repository.ProductRepository
import com.example.unimarket.data.repository.SessionRepository
import com.example.unimarket.ui.util.ErrorMessages
import com.example.unimarket.ui.util.rethrowIfCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val product: Product? = null,
    val isLoading: Boolean = true,
    /** true selama meminta halaman pembayaran ke server. */
    val isProcessing: Boolean = false,
    /** Alasan barang tidak bisa dibeli (milik sendiri, sudah terjual, tidak ditemukan). */
    val blockReason: String? = null,
    /** Error saat memuat atau membayar, mis. "Gagal terhubung ke server pembayaran". */
    val errorMessage: String? = null,
    /** Terisi setelah pesanan berhasil dibuat: UI pindah ke layar pembayaran. */
    val createdOrder: Order? = null
) {
    val canPay: Boolean
        get() = product != null && blockReason == null && !isProcessing && createdOrder == null
}

class CheckoutViewModel(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val sessionRepository: SessionRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val productId: Int = checkNotNull(savedStateHandle.get<Int>(PRODUCT_ID_ARG))

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        loadProduct()
    }

    private fun loadProduct() {
        viewModelScope.launch {
            try {
                val product = productRepository.getProductById(productId)
                val userId = sessionRepository.currentUserId.value
                _uiState.value = CheckoutUiState(
                    product = product,
                    isLoading = false,
                    blockReason = when {
                        product == null -> PaymentErrors.PRODUCT_NOT_FOUND
                        userId == null -> ErrorMessages.SESSION_EXPIRED
                        else -> PurchaseRules.blockReason(product, userId)
                    }
                )
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                ErrorMessages.log(e)
                _uiState.value = CheckoutUiState(isLoading = false, errorMessage = ErrorMessages.LOAD_FAILED)
            }
        }
    }

    fun onPay() {
        if (!_uiState.value.canPay) return
        val userId = sessionRepository.currentUserId.value
        if (userId == null) {
            _uiState.update { it.copy(errorMessage = ErrorMessages.SESSION_EXPIRED) }
            return
        }
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

        viewModelScope.launch {
            val result = try {
                paymentRepository.createOrder(productId, userId)
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                PaymentResult.Error(PaymentErrors.UNKNOWN, e)
            }
            _uiState.update {
                when (result) {
                    is PaymentResult.Success -> it.copy(isProcessing = false, createdOrder = result.data)
                    is PaymentResult.Error -> {
                        result.cause?.let(ErrorMessages::log)
                        it.copy(isProcessing = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    /** Dipanggil UI setelah pindah ke layar pembayaran, agar tidak pindah dua kali. */
    fun onPaymentOpened() {
        _uiState.update { it.copy(createdOrder = null) }
    }

    companion object {
        const val PRODUCT_ID_ARG = "productId"
    }
}
