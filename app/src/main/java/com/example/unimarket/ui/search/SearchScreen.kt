package com.example.unimarket.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.unimarket.data.repository.ProductRepository
import com.example.unimarket.ui.AppViewModelProvider
import com.example.unimarket.ui.components.*

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onProductClick: (Int) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    value = uiState.query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    label = { Text("Kata kunci") },
                    placeholder = { Text("Buku, kalkulator, jas lab...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp)
                )
                
                CategoryGrid(
                    selected = uiState.selectedCategory,
                    onSelected = { viewModel.onCategoryChange(it) },
                    categories = ProductRepository.getCategories()
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                Section(
                    title = "Hasil Pencarian",
                    action = "${uiState.results.size} barang"
                )
            }
            
            if (uiState.results.isEmpty()) {
                item {
                    EmptyState(
                        title = "Tidak ditemukan",
                        note = "Coba gunakan kata kunci lain."
                    )
                }
            } else {
                items(uiState.results) { product ->
                    ProductCard(
                        product = product,
                        onClick = { onProductClick(product.id) }
                    )
                }
            }
        }
    }
}
