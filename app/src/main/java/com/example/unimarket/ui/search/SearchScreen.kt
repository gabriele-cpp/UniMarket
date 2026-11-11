package com.example.unimarket.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unimarket.data.model.ProductSort
import com.example.unimarket.ui.AppViewModelProvider
import com.example.unimarket.ui.components.*
import com.example.unimarket.ui.util.ALL_CATEGORIES_LABEL

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onProductClick: (Int) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()

    Scaffold(
        topBar = {
            UniMarketTopBar(
                title = "Cari barang",
                onBack = onBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    label = { Text("Kata kunci") },
                    placeholder = { Text("Buku, kalkulator, jas lab...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp)
                )

                Caption("KATEGORI")
                Spacer(modifier = Modifier.height(7.dp))
                CategoryGrid(
                    selected = uiState.selectedCategoryName,
                    onSelected = { name ->
                        // "Semua" tidak ada di tabel -> id null
                        viewModel.onCategoryChange(uiState.categories.firstOrNull { it.name == name }?.id)
                    },
                    categories = listOf(ALL_CATEGORIES_LABEL) + uiState.categories.map { it.name }
                )

                Spacer(modifier = Modifier.height(17.dp))
                Caption("URUTKAN")
                Spacer(modifier = Modifier.height(7.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(ProductSort.entries) { sort ->
                        FilterButton(
                            title = sort.label,
                            active = uiState.sort == sort,
                            onClick = { viewModel.onSortChange(sort) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Section(
                    title = "Hasil Pencarian",
                    action = "${uiState.results.size} barang"
                )
            }

            when {
                uiState.errorMessage != null -> item {
                    EmptyState(title = "Terjadi kesalahan", note = uiState.errorMessage!!)
                }

                uiState.results.isEmpty() -> item {
                    EmptyState(
                        title = "Tidak ditemukan",
                        note = "Coba gunakan kata kunci atau kategori lain."
                    )
                }

                else -> items(uiState.results, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onClick = { onProductClick(product.id) }
                    )
                }
            }
        }
    }
}
