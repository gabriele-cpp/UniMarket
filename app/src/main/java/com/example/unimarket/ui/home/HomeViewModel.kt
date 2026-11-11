package com.example.unimarket.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.model.Category
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.repository.CategoryRepository
import com.example.unimarket.data.repository.ProductRepository
import com.example.unimarket.ui.util.ALL_CATEGORIES_LABEL
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import com.example.unimarket.ui.util.ErrorMessages
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    /** null = "Semua". */
    val selectedCategoryId: Int? = null,
    val errorMessage: String? = null
) {
    val selectedCategoryName: String
        get() = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: ALL_CATEGORIES_LABEL
}

class HomeViewModel(
    private val productRepository: ProductRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val productsFlow = selectedCategoryId.flatMapLatest { categoryId ->
        productRepository.getProductsByCategory(categoryId)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        productsFlow,
        categoryRepository.getAllCategories(),
        selectedCategoryId
    ) { products, categories, selectedId ->
        HomeUiState(products = products, categories = categories, selectedCategoryId = selectedId)
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
}
