package com.example.unimarket.ui.post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.model.Category
import com.example.unimarket.data.model.ProductDefaults
import com.example.unimarket.data.model.ProductInput
import com.example.unimarket.data.repository.CategoryRepository
import com.example.unimarket.data.repository.OwnershipResult
import com.example.unimarket.data.repository.ProductRepository
import com.example.unimarket.data.repository.SessionRepository
import com.example.unimarket.ui.util.MAX_PRICE
import com.example.unimarket.ui.util.formatRupiah
import com.example.unimarket.ui.util.parsePrice
import com.example.unimarket.ui.util.ErrorMessages
import com.example.unimarket.ui.util.rethrowIfCancellation
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PostUiState(
    val title: String = "",
    /** Hanya berisi digit; diparse ke Long saat publish. */
    val price: String = "",
    val description: String = "",
    val categories: List<Category> = emptyList(),
    val categoryId: Int? = null,
    val condition: String = ProductDefaults.DEFAULT_CONDITION,
    /** Kosong = memakai ProductDefaults.LOCATION saat disimpan. */
    val location: String = "",
    val allowCod: Boolean = true,
    val allowGoSend: Boolean = true,
    val imageUri: String? = null,
    /** Terisi jika sedang mengedit barang lama; null = pasang iklan baru. */
    val editingProductId: Int? = null,
    val isLoadingProduct: Boolean = false,
    /** true jika barang yang mau diedit tidak ada / bukan milik user; layar harus ditutup. */
    val loadFailed: Boolean = false,
    val isSaving: Boolean = false,
    val showSuccess: Boolean = false,
    val errorMessage: String? = null,
    val showValidationErrors: Boolean = false
) {
    val isEditMode: Boolean
        get() = editingProductId != null

    val parsedPrice: Long?
        get() = parsePrice(price)

    val titleError: String?
        get() = if (showValidationErrors && title.isBlank()) "Judul barang wajib diisi" else null

    val priceError: String?
        get() = when {
            !showValidationErrors -> null
            price.isBlank() -> "Harga barang wajib diisi"
            parsedPrice == null -> "Harga harus angka antara 1 dan ${formatRupiah(MAX_PRICE)}"
            else -> null
        }

    val categoryError: String?
        get() = if (showValidationErrors && categoryId == null) "Pilih kategori barang" else null

    val deliveryError: String?
        get() = if (showValidationErrors && !allowCod && !allowGoSend) "Pilih minimal 1 opsi pengiriman" else null

    val isValid: Boolean
        get() = title.isNotBlank() && parsedPrice != null && categoryId != null && (allowCod || allowGoSend)

    val deliveryMethodFormatted: String
        get() {
            val list = mutableListOf<String>()
            if (allowCod) list.add("COD Kampus")
            if (allowGoSend) list.add("GoSend/GrabExpress")
            return list.joinToString(" & ")
        }
}

class PostViewModel(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    categoryRepository: CategoryRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    /** Argumen navigasi "post?productId={productId}"; -1 / tidak ada = barang baru. */
    private val editProductId: Int? =
        savedStateHandle.get<Int>(PRODUCT_ID_ARG)?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(
        PostUiState(editingProductId = editProductId, isLoadingProduct = editProductId != null)
    )
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    init {
        editProductId?.let { loadProductForEdit(it) }

        viewModelScope.launch {
            categoryRepository.getAllCategories().catch { e ->
                ErrorMessages.log(e)
                _uiState.update { it.copy(errorMessage = ErrorMessages.LOAD_FAILED) }
            }.collect { categories ->
                _uiState.update { state ->
                    val stillValid = categories.any { it.id == state.categoryId }
                    state.copy(
                        categories = categories,
                        categoryId = if (stillValid) state.categoryId else categories.firstOrNull()?.id
                    )
                }
            }
        }
    }

    private fun loadProductForEdit(productId: Int) {
        viewModelScope.launch {
            val product = try {
                productRepository.getProductById(productId)
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                ErrorMessages.log(e)
                failLoad(ErrorMessages.LOAD_FAILED)
                return@launch
            }
            val userId = sessionRepository.currentUserId.value
            when {
                product == null -> failLoad("Barang tidak ditemukan.")
                // Cek kepemilikan di ViewModel: hanya pemilik yang boleh mengedit.
                product.sellerId != userId -> failLoad(OwnershipResult.NOT_OWNER.message!!)
                else -> _uiState.update {
                    it.copy(
                        title = product.title,
                        price = product.price.toString(),
                        description = product.description,
                        categoryId = product.categoryId,
                        condition = product.condition,
                        location = product.location,
                        allowCod = product.deliveryMethod.contains("COD", ignoreCase = true),
                        allowGoSend = product.deliveryMethod.contains("GoSend", ignoreCase = true),
                        imageUri = product.imageUri,
                        isLoadingProduct = false
                    )
                }
            }
        }
    }

    private fun failLoad(message: String) {
        _uiState.update { it.copy(isLoadingProduct = false, loadFailed = true, errorMessage = message) }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    /** Hanya menerima digit, maksimal 10 digit (cukup untuk MAX_PRICE). */
    fun onPriceChange(newPrice: String) {
        val digitsOnly = newPrice.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(price = digitsOnly) }
    }

    fun onDescriptionChange(newDesc: String) {
        _uiState.update { it.copy(description = newDesc) }
    }

    fun onCategoryChange(categoryId: Int) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun onConditionChange(newCondition: String) {
        _uiState.update { it.copy(condition = newCondition) }
    }

    fun onLocationChange(newLocation: String) {
        _uiState.update { it.copy(location = newLocation) }
    }

    fun onToggleCod(enabled: Boolean) {
        _uiState.update { it.copy(allowCod = enabled) }
    }

    fun onToggleGoSend(enabled: Boolean) {
        _uiState.update { it.copy(allowGoSend = enabled) }
    }

    fun onImageSelected(uri: String?) {
        _uiState.update { it.copy(imageUri = uri) }
    }

    fun onPublish() {
        val state = _uiState.value
        if (state.isSaving || state.isLoadingProduct || state.loadFailed) return
        val price = state.parsedPrice
        val categoryId = state.categoryId
        if (!state.isValid || price == null || categoryId == null) {
            _uiState.update { it.copy(showValidationErrors = true) }
            return
        }

        val sellerId = sessionRepository.currentUserId.value
        if (sellerId == null) {
            _uiState.update { it.copy(errorMessage = ErrorMessages.SESSION_EXPIRED) }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val input = ProductInput(
                    title = state.title.trim(),
                    price = price,
                    categoryId = categoryId,
                    condition = state.condition,
                    sellerId = sellerId,
                    description = state.description.trim(),
                    imageUri = state.imageUri,
                    location = state.location.trim().ifEmpty { ProductDefaults.LOCATION },
                    deliveryMethod = state.deliveryMethodFormatted
                )
                val editingId = state.editingProductId
                if (editingId == null) {
                    productRepository.addProduct(input)
                } else {
                    // Repository juga menolak jika sellerId bukan pemilik barang.
                    val result = productRepository.updateProduct(editingId, input)
                    if (result != OwnershipResult.SUCCESS) {
                        _uiState.update { it.copy(isSaving = false, errorMessage = result.message) }
                        return@launch
                    }
                }
                _uiState.update { it.copy(isSaving = false, showSuccess = true, showValidationErrors = false) }
            } catch (e: IOException) {
                // Foto dari galeri gagal disalin ke penyimpanan internal.
                ErrorMessages.log(e)
                _uiState.update { it.copy(isSaving = false, errorMessage = ErrorMessages.IMAGE_FAILED) }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                ErrorMessages.log(e)
                _uiState.update { it.copy(isSaving = false, errorMessage = ErrorMessages.SAVE_FAILED) }
            }
        }
    }

    fun onDismissSuccess() {
        _uiState.update { it.copy(showSuccess = false) }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val PRODUCT_ID_ARG = "productId"
    }
}
