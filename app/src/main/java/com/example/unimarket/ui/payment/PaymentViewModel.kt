package com.example.unimarket.ui.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.model.Order
import com.example.unimarket.data.model.OrderStatus
import com.example.unimarket.data.repository.PaymentErrors
import com.example.unimarket.data.repository.PaymentRepository
import com.example.unimarket.data.repository.PaymentResult
import com.example.unimarket.ui.util.ErrorMessages
import com.example.unimarket.ui.util.rethrowIfCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** WEBVIEW = halaman Midtrans Snap terbuka; RESULT = ringkasan status pembayaran. */
enum class PaymentPhase { WEBVIEW, RESULT }

data class PaymentUiState(
    val order: Order? = null,
    val isLoading: Boolean = true,
    val phase: PaymentPhase = PaymentPhase.RESULT,
    /** true selama menanyakan status ke server. */
    val isChecking: Boolean = false,
    /** Pesan sekali tampil (Snackbar). */
    val message: String? = null
) {
    /** Halaman Midtrans masih bisa dibuka lagi (pesanan belum selesai). */
    val canContinuePayment: Boolean
        get() = order?.status == OrderStatus.PENDING && !order.redirectUrl.isNullOrBlank()
}

class PaymentViewModel(
    savedStateHandle: SavedStateHandle,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle.get<String>(ORDER_ID_ARG))
    private val openWebView: Boolean = savedStateHandle.get<Boolean>(OPEN_WEB_VIEW_ARG) ?: false

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            paymentRepository.observeOrder(orderId)
                .catch { e ->
                    ErrorMessages.log(e)
                    _uiState.update { it.copy(isLoading = false, message = ErrorMessages.LOAD_FAILED) }
                }
                .collect { order -> onOrderLoaded(order) }
        }
    }

    private fun onOrderLoaded(order: Order?) {
        val firstLoad = _uiState.value.isLoading
        _uiState.update { state ->
            val phase = if (firstLoad) {
                // Dari Checkout: langsung ke halaman Midtrans. Dari Riwayat: tampilkan status dulu.
                if (openWebView && state.copy(order = order).canContinuePayment) PaymentPhase.WEBVIEW
                else PaymentPhase.RESULT
            } else {
                state.phase
            }
            state.copy(order = order, isLoading = false, phase = phase)
        }
        // Dibuka dari Riwayat dengan status PENDING: langsung tanyakan status terbaru.
        if (firstLoad && _uiState.value.phase == PaymentPhase.RESULT && order?.status == OrderStatus.PENDING) {
            checkStatus()
        }
    }

    /**
     * Halaman Midtrans ditutup: karena Snap sudah mengarah ke URL finish,
     * atau user memilih keluar. Tampilkan hasil dan cek status ke server.
     */
    fun onLeavePaymentPage() {
        _uiState.update { it.copy(phase = PaymentPhase.RESULT) }
        checkStatus()
    }

    /** Membuka lagi halaman Midtrans untuk pesanan yang masih PENDING. */
    fun onContinuePayment() {
        if (_uiState.value.canContinuePayment) {
            _uiState.update { it.copy(phase = PaymentPhase.WEBVIEW) }
        }
    }

    fun checkStatus() {
        if (_uiState.value.isChecking) return
        _uiState.update { it.copy(isChecking = true) }
        viewModelScope.launch {
            val result = try {
                paymentRepository.refreshStatus(orderId)
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                PaymentResult.Error(PaymentErrors.UNKNOWN, e)
            }
            // Status baru sampai ke UI lewat observeOrder; di sini cukup urus pesan error.
            _uiState.update {
                if (result is PaymentResult.Error) {
                    result.cause?.let(ErrorMessages::log)
                    it.copy(isChecking = false, message = result.message)
                } else {
                    it.copy(isChecking = false)
                }
            }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    companion object {
        const val ORDER_ID_ARG = "orderId"
        const val OPEN_WEB_VIEW_ARG = "openWebView"
    }
}
