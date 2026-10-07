package com.example.unimarket.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class SearchUiState(
    val query: String = "",
    val selectedCategory: String = "Semua",
    val results: List<Product> = emptyList()
)

class SearchViewModel(private val repository: ProductRepository) : ViewModel() {
    private val queryState = MutableStateFlow("")
    private val categoryState = MutableStateFlow("Semua")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val resultsFlow = categoryState.flatMapLatest { category ->
        if (category == "Semua") {
            repository.getAllProducts()
        } else {
            repository.getProductsByCategory(category)
        }
    }.map { products ->
        val q = queryState.value
        if (q.isBlank()) products else products.filter {
            it.title.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true)
        }
    }

    val uiState: StateFlow<SearchUiState> = kotlinx.coroutines.flow.combine(
        queryState, categoryState, resultsFlow
    ) { q, c, r ->
        SearchUiState(query = q, selectedCategory = c, results = r)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun onQueryChange(newQuery: String) {
        queryState.value = newQuery
    }

    fun onCategoryChange(newCategory: String) {
        categoryState.value = newCategory
    }
}
