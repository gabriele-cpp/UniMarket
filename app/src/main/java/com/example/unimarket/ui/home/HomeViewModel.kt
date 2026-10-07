package com.example.unimarket.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.repository.ProductRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val products: List<Product> = emptyList(),
    val selectedCategory: String = "Semua"
)

class HomeViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _selectedCategory = kotlinx.coroutines.flow.MutableStateFlow("Semua")

    init {
        viewModelScope.launch {
            repository.populateInitialDataIfNeeded()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = _selectedCategory
        .flatMapLatest { category ->
            repository.getProductsByCategory(category).map { products ->
                HomeUiState(products = products, selectedCategory = category)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    fun onCategorySelect(category: String) {
        _selectedCategory.value = category
    }
}
