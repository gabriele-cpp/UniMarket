package com.example.unimarket.ui.home

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.model.Category
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.repository.CategoryRepository
import com.example.unimarket.data.repository.MessageRepository
import com.example.unimarket.data.repository.ProductRepository
import com.example.unimarket.data.repository.SessionRepository
import com.example.unimarket.ui.util.ALL_CATEGORIES_LABEL
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import com.example.unimarket.ui.util.ErrorMessages
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    /** null = "Semua". */
    val selectedCategoryId: Int? = null,
    val isRefreshing: Boolean = false,
    val hasUnread: Boolean = false,
    val errorMessage: String? = null
) {
    val selectedCategoryName: String
        get() = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: ALL_CATEGORIES_LABEL
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    categoryRepository: CategoryRepository,
    sessionRepository: SessionRepository? = null,
    messageRepository: MessageRepository? = null
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<Int?>(null)
    private val _isRefreshing = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val productsFlow = selectedCategoryId.flatMapLatest { categoryId ->
        productRepository.getProductsByCategory(categoryId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val unreadFlow = if (sessionRepository != null && messageRepository != null) {
        sessionRepository.currentUserId.flatMapLatest { userId ->
            if (userId == null) flowOf(0) else messageRepository.observeUnreadCount(userId)
        }
    } else {
        flowOf(0)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        productsFlow,
        categoryRepository.getAllCategories(),
        selectedCategoryId,
        _isRefreshing,
        unreadFlow
    ) { products, categories, selectedId, isRefreshing, unread ->
        HomeUiState(
            products = products,
            categories = categories,
            selectedCategoryId = selectedId,
            isRefreshing = isRefreshing,
            hasUnread = unread > 0
        )
    }.catch { e ->
        ErrorMessages.log(e)
        emit(HomeUiState(errorMessage = ErrorMessages.LOAD_FAILED))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    /** categoryId null = tampilkan semua kategori. */
    fun onCategorySelect(categoryId: Int?) {
        selectedCategoryId.value = categoryId
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                productRepository.refresh()
            } catch (e: Exception) {
                ErrorMessages.log(e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
