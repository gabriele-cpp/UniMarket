package com.example.unimarket.ui.myitems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.model.ProductStatus
import com.example.unimarket.data.repository.OwnershipResult
import com.example.unimarket.data.repository.ProductRepository
import com.example.unimarket.data.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import com.example.unimarket.ui.util.ErrorMessages
import com.example.unimarket.ui.util.rethrowIfCancellation

data class MyItemsUiState(
    val items: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/** Dialog yang sedang terbuka di layar My Items. */
data class MyItemsDialogState(
    val pendingDelete: Product? = null,
    val statusPickerFor: Product? = null,
    /** Pesan singkat untuk Snackbar (berhasil/gagal). */
    val message: String? = null
)

class MyItemsViewModel(
    private val productRepository: ProductRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MyItemsUiState> = sessionRepository.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) flowOf(emptyList())
            else productRepository.getProductsBySeller(userId)
        }
        .map { MyItemsUiState(items = it, isLoading = false) }
        .catch { e ->
            ErrorMessages.log(e)
            emit(MyItemsUiState(isLoading = false, errorMessage = ErrorMessages.LOAD_FAILED))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyItemsUiState())

    private val _dialogState = MutableStateFlow(MyItemsDialogState())
    val dialogState: StateFlow<MyItemsDialogState> = _dialogState.asStateFlow()

    /** Pemeriksaan kepemilikan di ViewModel (selain juga dicek ulang di repository). */
    private fun isOwner(product: Product): Boolean =
        product.sellerId == sessionRepository.currentUserId.value

    fun onDeleteRequest(product: Product) {
        if (!isOwner(product)) {
            showMessage(OwnershipResult.NOT_OWNER.message)
            return
        }
        _dialogState.update { it.copy(pendingDelete = product) }
    }

    fun onDeleteDismiss() {
        _dialogState.update { it.copy(pendingDelete = null) }
    }

    fun onDeleteConfirm() {
        val product = _dialogState.value.pendingDelete ?: return
        val userId = sessionRepository.currentUserId.value
        _dialogState.update { it.copy(pendingDelete = null) }
        if (userId == null || !isOwner(product)) {
            showMessage(OwnershipResult.NOT_OWNER.message)
            return
        }
        viewModelScope.launch {
            try {
                val result = productRepository.deleteProduct(product.id, userId)
                showMessage(result.message ?: "\"${product.title}\" dihapus")
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                ErrorMessages.log(e)
                showMessage(ErrorMessages.DELETE_FAILED)
            }
        }
    }

    fun onStatusRequest(product: Product) {
        if (!isOwner(product)) {
            showMessage(OwnershipResult.NOT_OWNER.message)
            return
        }
        _dialogState.update { it.copy(statusPickerFor = product) }
    }

    fun onStatusDismiss() {
        _dialogState.update { it.copy(statusPickerFor = null) }
    }

    fun onStatusSelected(status: ProductStatus) {
        val product = _dialogState.value.statusPickerFor ?: return
        val userId = sessionRepository.currentUserId.value
        _dialogState.update { it.copy(statusPickerFor = null) }
        if (status == product.status) return
        if (userId == null || !isOwner(product)) {
            showMessage(OwnershipResult.NOT_OWNER.message)
            return
        }
        viewModelScope.launch {
            try {
                val result = productRepository.updateStatus(product.id, userId, status)
                showMessage(result.message ?: "Status diubah menjadi ${status.label}")
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                ErrorMessages.log(e)
                showMessage(ErrorMessages.STATUS_FAILED)
            }
        }
    }

    fun onMessageShown() {
        _dialogState.update { it.copy(message = null) }
    }

    private fun showMessage(message: String?) {
        _dialogState.update { it.copy(message = message) }
    }
}
