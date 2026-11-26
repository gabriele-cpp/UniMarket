package com.example.unimarket.ui.orders

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.model.Order
import com.example.unimarket.data.repository.PaymentRepository
import com.example.unimarket.data.repository.SessionRepository
import com.example.unimarket.ui.util.ErrorMessages
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class RiwayatPembelianUiState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class RiwayatPembelianViewModel @Inject constructor(
    paymentRepository: PaymentRepository,
    sessionRepository: SessionRepository
) : ViewModel() {

    /** Pesanan milik user yang sedang login, ikut ter-update saat status berubah. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<RiwayatPembelianUiState> = sessionRepository.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) flowOf(emptyList()) else paymentRepository.observeOrdersByBuyer(userId)
        }
        .map { RiwayatPembelianUiState(orders = it, isLoading = false) }
        .catch { e ->
            ErrorMessages.log(e)
            emit(RiwayatPembelianUiState(isLoading = false, errorMessage = ErrorMessages.LOAD_FAILED))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RiwayatPembelianUiState())
}
