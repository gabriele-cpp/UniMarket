package com.example.unimarket.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.model.Category
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.model.ProductSort
import com.example.unimarket.data.repository.CategoryRepository
import com.example.unimarket.data.repository.ProductRepository
import com.example.unimarket.ui.util.ALL_CATEGORIES_LABEL
import com.example.unimarket.ui.util.ErrorMessages
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
    val categories: List<Category> = emptyList(),
    /** null = "Semua". */
    val selectedCategoryId: Int? = null,
    val sort: ProductSort = ProductSort.NEWEST,
    val results: List<Product> = emptyList(),
    val errorMessage: String? = null
) {
    val selectedCategoryName: String
        get() = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: ALL_CATEGORIES_LABEL
}

private data class SearchParams(val query: String, val categoryId: Int?, val sort: ProductSort)

/** Hasil pencarian atau error, agar error tidak menghentikan seluruh layar. */
private data class SearchResults(val products: List<Product> = emptyList(), val error: String? = null)

class SearchViewModel(
    private val productRepository: ProductRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {
    private val queryState = MutableStateFlow("")
    private val categoryState = MutableStateFlow<Int?>(null)
    private val sortState = MutableStateFlow(ProductSort.NEWEST)

    /**
     * Teks kata kunci untuk TextField. Dipisah dari uiState dan diperbarui langsung (sinkron)
     * agar kursor tidak meloncat saat mengetik.
     */
    val query: StateFlow<String> = queryState.asStateFlow()

    /**
     * Query (di-debounce 300 ms), kategori, dan urutan digabung lalu dicari di database.
     * Setiap perubahan salah satunya otomatis memicu pencarian ulang lewat flatMapLatest.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val resultsFlow = combine(
        // Mengosongkan kata kunci langsung diproses; mengetik ditunda 300 ms.
        queryState.debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MS },
        categoryState,
        sortState
    ) { q, c, s -> SearchParams(q.trim(), c, s) }
        .distinctUntilChanged()
        .flatMapLatest { p ->
            productRepository.searchProducts(p.query, p.categoryId, p.sort)
                .map { SearchResults(products = it) }
                .catch { e ->
                    ErrorMessages.log(e)
                    emit(SearchResults(error = ErrorMessages.LOAD_FAILED))
                }
        }

    val uiState: StateFlow<SearchUiState> = combine(
        categoryState,
        sortState,
        categoryRepository.getAllCategories(),
        resultsFlow
    ) { c, s, categories, results ->
        SearchUiState(
            categories = categories,
            selectedCategoryId = c,
            sort = s,
            results = results.products,
            errorMessage = results.error
        )
    }.catch { e ->
        ErrorMessages.log(e)
        emit(SearchUiState(errorMessage = ErrorMessages.LOAD_FAILED))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(newQuery: String) {
        queryState.value = newQuery
    }

    /** categoryId null = semua kategori. */
    fun onCategoryChange(categoryId: Int?) {
        categoryState.value = categoryId
    }

    fun onSortChange(sort: ProductSort) {
        sortState.value = sort
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
